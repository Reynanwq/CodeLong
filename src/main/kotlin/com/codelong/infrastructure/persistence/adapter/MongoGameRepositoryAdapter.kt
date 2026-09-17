package com.codelong.infrastructure.persistence.adapter

import com.codelong.domain.exception.DomainException

import com.codelong.domain.model.Game
import com.codelong.domain.port.GamePage
import com.codelong.domain.port.GameRepository
import com.codelong.domain.port.GameSearch
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameStatus
import com.codelong.domain.valueobject.UserId
import com.codelong.infrastructure.persistence.document.GameDocument
import com.codelong.infrastructure.persistence.mapper.GamePersistenceMapper
import com.codelong.infrastructure.persistence.repository.SpringGameDataRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class MongoGameRepositoryAdapter(
    private val repository: SpringGameDataRepository,
    private val mongoTemplate: MongoTemplate
) : GameRepository {

    override fun save(game: Game): Game {
        val document = GamePersistenceMapper.toDocument(game)
        val saved = try {
            repository.save(document)
        } catch (ex: OptimisticLockingFailureException) {
            throw DomainException.concurrentModification()
        }
        return GamePersistenceMapper.toDomain(saved)
    }

    override fun findById(id: GameId): Game? =
        repository.findById(id.value).orElse(null)?.let(GamePersistenceMapper::toDomain)

    override fun findInProgressByUserId(userId: UserId): Game? {
        val criteria = Criteria.where("userId").`is`(userId.value)
            .and("status").`is`(GameStatus.IN_PROGRESS.name)
        return mongoTemplate
            .find(Query(criteria).with(defaultSort()).limit(1), GameDocument::class.java)
            .firstOrNull()
            ?.let(GamePersistenceMapper::toDomain)
    }

    override fun search(search: GameSearch): GamePage {
        val criteria = buildCriteria(search)
        val pageable = PageRequest.of(search.page, search.size, defaultSort())

        val items = mongoTemplate
            .find(Query(criteria).with(pageable), GameDocument::class.java)
            .map(GamePersistenceMapper::toDomain)
        val total = mongoTemplate.count(Query(criteria), GameDocument::class.java)

        return GamePage(
            items = items,
            totalElements = total,
            page = search.page,
            size = search.size
        )
    }

    private fun buildCriteria(search: GameSearch): Criteria {
        val criteria = Criteria.where("userId").`is`(search.userId.value)
        search.status?.let { criteria.and("status").`is`(it.name) }
        return criteria
    }

    private fun defaultSort(): Sort = Sort.by(Sort.Direction.DESC, "startedAt")
}
