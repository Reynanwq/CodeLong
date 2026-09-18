package com.codelong.infrastructure.persistence.adapter

import com.codelong.domain.GameRules
import com.codelong.domain.port.RankingRepository
import com.codelong.domain.service.RankingPolicy
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
 * Entram partidas concluidas ou abandonadas com pelo menos
 * [GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING] respostas; vale a melhor
 * partida de cada usuario, ordenada pela politica de desempate do dominio.
 */
@Repository
class MongoRankingRepositoryAdapter(
    private val mongoTemplate: MongoTemplate
) : RankingRepository {

    override fun findRanking(page: Int, size: Int): List<RankEntry> =
        rankedEntries().drop(page * size).take(size)

    override fun findUserBestScore(userId: UserId): RankEntry? =
        rankedEntries().firstOrNull { it.userId == userId }

    override fun countUsersBetterThan(entry: RankEntry): Long =
        rankedEntries().count { RankingPolicy.isBetter(it, entry) }.toLong()

    override fun countRankedUsers(): Long = rankedEntries().size.toLong()

    private fun rankedEntries(): List<RankEntry> {
        val stages = listOf(
            Aggregation.match(Criteria.where(MongoSchema.Field.STATUS).`in`(rankedStatuses())),
            Aggregation.addFields()
                .addField(MongoSchema.Field.ANSWERED_QUESTIONS)
                .withValue(Document(OPERATOR_SIZE, DOCUMENT_ANSWERS))
                .build(),
            Aggregation.match(
                Criteria.where(MongoSchema.Field.ANSWERED_QUESTIONS)
                    .gte(GameRules.MIN_ANSWERED_QUESTIONS_FOR_RANKING)
            ),
            Aggregation.addFields()
                .addField(MongoSchema.Field.TOTAL_TIME_MILLIS)
                .withValue(Document(MongoSchema.Operator.SUBTRACT, listOf(DOCUMENT_COMPLETED_AT, DOCUMENT_STARTED_AT)))
                .build(),
            Aggregation.sort(preGroupSort()),
            Aggregation.group(MongoSchema.Field.USER_ID)
                .first(MongoSchema.Field.USER_ID).`as`(MongoSchema.Field.USER_ID)
                .first(MongoSchema.Field.USERNAME).`as`(MongoSchema.Field.USERNAME)
                .first(MongoSchema.Field.SCORE).`as`(MongoSchema.Field.SCORE)
                .first(MongoSchema.Field.CORRECT_ANSWERS).`as`(MongoSchema.Field.CORRECT_ANSWERS)
                .first(MongoSchema.Field.ANSWERED_QUESTIONS).`as`(MongoSchema.Field.ANSWERED_QUESTIONS)
                .first(MongoSchema.Field.TOTAL_TIME_MILLIS).`as`(MongoSchema.Field.TOTAL_TIME_MILLIS)
                .first(MongoSchema.Field.COMPLETED_AT).`as`(MongoSchema.Field.ACHIEVED_AT),
            Aggregation.sort(postGroupSort())
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

    private fun preGroupSort(): Sort = Sort.by(
        Sort.Order.desc(MongoSchema.Field.SCORE),
        Sort.Order.asc(MongoSchema.Field.TOTAL_TIME_MILLIS),
        Sort.Order.desc(MongoSchema.Field.CORRECT_ANSWERS),
        Sort.Order.asc(MongoSchema.Field.COMPLETED_AT)
    )

    private fun postGroupSort(): Sort = Sort.by(
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
            listOf(GameStatus.COMPLETED.name, GameStatus.ABANDONED.name)
    }
}
