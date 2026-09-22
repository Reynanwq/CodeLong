package com.codelong.infrastructure.persistence.adapter

import com.codelong.domain.port.RankingFilter
import com.codelong.domain.port.RankingRepository
import com.codelong.domain.service.RankingPolicy
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.infrastructure.persistence.document.GameDocument
import com.codelong.infrastructure.persistence.document.MongoSchema
import com.codelong.infrastructure.persistence.document.RankEntryDocument
import com.codelong.infrastructure.persistence.mapper.RankingPersistenceMapper
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.MatchOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository

/**
 * Ranking calculado por agregacao no MongoDB.
 *
 * Entram **todas as tentativas** concluidas, abandonadas ou derrotadas que
 * atendam ao minimo de respostas do seu modo (ver
 * [RankingPolicy.minimumAnswers]). Cada partida ocupa uma linha, ordenada pela
 * politica de desempate do dominio.
 *
 * O recorte vem de [RankingFilter]: por modo e, no APRENDIZADO, por tema. Sem
 * modo (ranking global) apenas CLASSIC e GENOCIDA entram.
 */
@Repository
class MongoRankingRepositoryAdapter(
    private val mongoTemplate: MongoTemplate
) : RankingRepository {

    override fun findRanking(page: Int, size: Int, filter: RankingFilter): List<RankEntry> =
        rankedEntries(filter).drop(page * size).take(size)

    override fun findUserBestScore(userId: UserId, filter: RankingFilter): RankEntry? =
        rankedEntries(filter).firstOrNull { it.userId == userId }

    override fun countUsersBetterThan(entry: RankEntry, filter: RankingFilter): Long =
        rankedEntries(filter).count { RankingPolicy.isBetter(it, entry) }.toLong()

    override fun countRankedEntries(filter: RankingFilter): Long = rankedEntries(filter).size.toLong()

    private fun rankedEntries(filter: RankingFilter): List<RankEntry> {
        val stages = listOfNotNull(
            Aggregation.match(Criteria.where(MongoSchema.Field.STATUS).`in`(rankedStatuses())),
            modeStage(filter.mode),
            Aggregation.addFields()
                .addField(MongoSchema.Field.THEME)
                .withValue(themeExpression())
                .build(),
            filter.theme?.let { Aggregation.match(Criteria.where(MongoSchema.Field.THEME).`is`(it.name)) },
            Aggregation.addFields()
                .addField(MongoSchema.Field.ANSWERED_QUESTIONS)
                .withValue(Document(OPERATOR_SIZE, DOCUMENT_ANSWERS))
                .build(),
            Aggregation.match(eligibilityCriteria()),
            Aggregation.addFields()
                .addField(MongoSchema.Field.TOTAL_TIME_MILLIS)
                .withValue(Document(MongoSchema.Operator.SUBTRACT, listOf(DOCUMENT_COMPLETED_AT, DOCUMENT_STARTED_AT)))
                .build(),
            Aggregation.addFields()
                .addField(MongoSchema.Field.ACHIEVED_AT)
                .withValue(DOCUMENT_COMPLETED_AT)
                .build(),
            Aggregation.sort(rankingSort())
        )

        return mongoTemplate
            .aggregate(
                Aggregation.newAggregation(stages),
                GameDocument::class.java,
                RankEntryDocument::class.java
            )
            .mappedResults
            .map(RankingPersistenceMapper::toDomain)
    }

    /** Tema da partida = categoria da primeira pergunta (partidas de um so tema). */
    private fun themeExpression(): Document =
        Document(MongoSchema.Operator.ARRAY_ELEM_AT, listOf(MongoSchema.Operator.QUESTIONS_CATEGORY, 0))

    /**
     * Quando um modo e informado, restringe a ele. Sem modo (ranking global),
     * considera apenas CLASSIC e GENOCIDA — Aprendizado e Gubee tem rankings
     * proprios.
     */
    private fun modeStage(mode: GameMode?): MatchOperation =
        if (mode != null) {
            Aggregation.match(Criteria.where(MongoSchema.Field.MODE).`is`(mode.name))
        } else {
            Aggregation.match(
                Criteria.where(MongoSchema.Field.MODE)
                    .nin(GameMode.APRENDIZADO.name, GameMode.GUBEE.name)
            )
        }

    /** O minimo de respostas depende do modo (genocida/aprendizado exigem menos). */
    private fun eligibilityCriteria(): Criteria = Criteria().orOperator(
        Criteria.where(MongoSchema.Field.MODE).`is`(GameMode.GENOCIDA.name)
            .and(MongoSchema.Field.ANSWERED_QUESTIONS)
            .gte(RankingPolicy.minimumAnswers(GameMode.GENOCIDA)),
        Criteria.where(MongoSchema.Field.MODE).`is`(GameMode.APRENDIZADO.name)
            .and(MongoSchema.Field.ANSWERED_QUESTIONS)
            .gte(RankingPolicy.minimumAnswers(GameMode.APRENDIZADO)),
        Criteria.where(MongoSchema.Field.MODE).`is`(GameMode.GUBEE.name)
            .and(MongoSchema.Field.ANSWERED_QUESTIONS)
            .gte(RankingPolicy.minimumAnswers(GameMode.GUBEE)),
        Criteria.where(MongoSchema.Field.MODE)
            .nin(GameMode.GENOCIDA.name, GameMode.APRENDIZADO.name, GameMode.GUBEE.name)
            .and(MongoSchema.Field.ANSWERED_QUESTIONS)
            .gte(RankingPolicy.minimumAnswers(GameMode.CLASSIC))
    )

    private fun rankingSort(): Sort = Sort.by(
        Sort.Order.desc(MongoSchema.Field.SCORE),
        Sort.Order.asc(MongoSchema.Field.TOTAL_TIME_MILLIS),
        Sort.Order.desc(MongoSchema.Field.CORRECT_ANSWERS),
        Sort.Order.asc(MongoSchema.Field.ACHIEVED_AT)
    )

    private companion object {
        const val OPERATOR_SIZE = "\$size"
        const val DOCUMENT_ANSWERS = "\$" + MongoSchema.Field.ANSWERS
        const val DOCUMENT_COMPLETED_AT = "\$" + MongoSchema.Field.COMPLETED_AT
        const val DOCUMENT_STARTED_AT = "\$" + MongoSchema.Field.STARTED_AT

        fun rankedStatuses(): List<String> =
            listOf(GameStatus.COMPLETED.name, GameStatus.ABANDONED.name, GameStatus.DEFEATED.name)
    }
}
