package com.codelong.infrastructure.persistence.adapter

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
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository

/**
 * Ranking calculado por agregacao no MongoDB.
 *
 * Entram **todas as tentativas** concluidas, abandonadas ou derrotadas que
 * atendam ao minimo de respostas do seu modo (ver
 * [RankingPolicy.minimumAnswers]). Cada partida ocupa uma linha, ordenada pela
 * politica de desempate do dominio. Quando um modo e informado, apenas as
 * partidas daquele modo entram no ranking (classico e genocida sao separados).
 */
@Repository
class MongoRankingRepositoryAdapter(
    private val mongoTemplate: MongoTemplate
) : RankingRepository {

    override fun findRanking(page: Int, size: Int, mode: GameMode?): List<RankEntry> =
        rankedEntries(mode).drop(page * size).take(size)

    override fun findUserBestScore(userId: UserId, mode: GameMode?): RankEntry? =
        rankedEntries(mode).firstOrNull { it.userId == userId }

    override fun countUsersBetterThan(entry: RankEntry, mode: GameMode?): Long =
        rankedEntries(mode).count { RankingPolicy.isBetter(it, entry) }.toLong()

    override fun countRankedEntries(mode: GameMode?): Long = rankedEntries(mode).size.toLong()

    private fun rankedEntries(mode: GameMode?): List<RankEntry> {
        val stages = listOfNotNull(
            Aggregation.match(Criteria.where(MongoSchema.Field.STATUS).`in`(rankedStatuses())),
            mode?.let { Aggregation.match(Criteria.where(MongoSchema.Field.MODE).`is`(it.name)) },
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

    /** O minimo de respostas depende do modo (genocida exige menos). */
    private fun eligibilityCriteria(): Criteria = Criteria().orOperator(
        Criteria.where(MongoSchema.Field.MODE).`is`(GameMode.GENOCIDA.name)
            .and(MongoSchema.Field.ANSWERED_QUESTIONS)
            .gte(RankingPolicy.minimumAnswers(GameMode.GENOCIDA)),
        Criteria.where(MongoSchema.Field.MODE).ne(GameMode.GENOCIDA.name)
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
