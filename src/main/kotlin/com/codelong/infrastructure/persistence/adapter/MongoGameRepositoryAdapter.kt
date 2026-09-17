package com.codelong.infrastructure.persistence.adapter

import com.codelong.domain.exception.ConcurrentGameModificationException
import com.codelong.domain.model.Game
import com.codelong.domain.port.GameRepository
import com.codelong.domain.valueobject.GameId
import com.codelong.infrastructure.persistence.mapper.GamePersistenceMapper
import com.codelong.infrastructure.persistence.repository.SpringGameDataRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Repository

@Repository
class MongoGameRepositoryAdapter(
    private val repository: SpringGameDataRepository
) : GameRepository {

    override fun save(game: Game): Game {
        val document = GamePersistenceMapper.toDocument(game)
        val saved = try {
            repository.save(document)
        } catch (ex: OptimisticLockingFailureException) {
            throw ConcurrentGameModificationException()
        }
        return GamePersistenceMapper.toDomain(saved)
    }

    override fun findById(id: GameId): Game? =
        repository.findById(id.value).orElse(null)?.let(GamePersistenceMapper::toDomain)
}