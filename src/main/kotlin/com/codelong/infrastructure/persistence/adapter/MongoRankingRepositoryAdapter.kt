package com.codelong.infrastructure.persistence.adapter

import com.codelong.infrastructure.persistence.document.MongoSchema

import com.codelong.domain.port.RankingRepository
import com.codelong.domain.service.RankingPolicy
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.infrastructure.persistence.document.GameDocument
import com.codelong.infrastructure.persistence.document.RankEntryDocument
import com.codelong.infrastructure.persistence.mapper.RankingPersistenceMapper
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository

/**
 * Ranking calculado por agregacao no MongoDB: considera apenas a melhor partida
 * concluida de cada usuario, ordenada pela politica de desempate do dominio.
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
            Aggregation.match(Criteria.where(MongoSchema.Field.STATUS).`is`(GameStatus.COMPLETED.name)),
            Aggregation.addFields()
                .addField(MongoSchema.Field.TOTAL_TIME_MILLIS)
                .withValue(Document(MongoSchema.Operator.SUBTRACT, listOf(MongoSchema.Operator.COMPLETED_AT, MongoSchema.Operator.STARTED_AT)))
                .build(),
            Aggregation.sort(preGroupSort()),
            Aggregation.group(MongoSchema.Field.USER_ID)
                .first(MongoSchema.Field.USER_ID).`as`(MongoSchema.Field.USER_ID)
                .first(MongoSchema.Field.USERNAME).`as`(MongoSchema.Field.USERNAME)
                .first(MongoSchema.Field.SCORE).`as`(MongoSchema.Field.SCORE)
                .first(MongoSchema.Field.CORRECT_ANSWERS).`as`(MongoSchema.Field.CORRECT_ANSWERS)
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
        Sort.Order.desc(MongoSchema.Field.CORRECT_ANSWERS),
        Sort.Order.asc(MongoSchema.Field.TOTAL_TIME_MILLIS),
        Sort.Order.asc(MongoSchema.Field.COMPLETED_AT)
    )

    private fun postGroupSort(): Sort = Sort.by(
        Sort.Order.desc(MongoSchema.Field.SCORE),
        Sort.Order.desc(MongoSchema.Field.CORRECT_ANSWERS),
        Sort.Order.asc(MongoSchema.Field.TOTAL_TIME_MILLIS),
        Sort.Order.asc(MongoSchema.Field.ACHIEVED_AT)
    )
}