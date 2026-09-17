package com.codelong.infrastructure.persistence.adapter

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
            Aggregation.match(Criteria.where("status").`is`(GameStatus.COMPLETED.name)),
            Aggregation.addFields()
                .addField("totalTimeMillis")
                .withValue(Document("\$subtract", listOf("\$completedAt", "\$startedAt")))
                .build(),
            Aggregation.sort(preGroupSort()),
            Aggregation.group("userId")
                .first("userId").`as`("userId")
                .first("username").`as`("username")
                .first("score").`as`("score")
                .first("correctAnswers").`as`("correctAnswers")
                .first("totalTimeMillis").`as`("totalTimeMillis")
                .first("completedAt").`as`("achievedAt"),
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
        Sort.Order.desc("score"),
        Sort.Order.desc("correctAnswers"),
        Sort.Order.asc("totalTimeMillis"),
        Sort.Order.asc("completedAt")
    )

    private fun postGroupSort(): Sort = Sort.by(
        Sort.Order.desc("score"),
        Sort.Order.desc("correctAnswers"),
        Sort.Order.asc("totalTimeMillis"),
        Sort.Order.asc("achievedAt")
    )
}