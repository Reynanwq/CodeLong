package com.codelong.infrastructure.persistence.adapter

import com.codelong.domain.model.User
import com.codelong.domain.port.UserPage
import com.codelong.domain.port.UserRepository
import com.codelong.domain.port.UserSearch
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.UserId
import com.codelong.domain.valueobject.Username
import com.codelong.infrastructure.persistence.document.UserDocument
import com.codelong.infrastructure.persistence.mapper.UserPersistenceMapper
import com.codelong.infrastructure.persistence.repository.SpringUserDataRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class MongoUserRepositoryAdapter(
    private val repository: SpringUserDataRepository,
    private val mongoTemplate: MongoTemplate
) : UserRepository {

    override fun save(user: User): User =
        UserPersistenceMapper.toDomain(repository.save(UserPersistenceMapper.toDocument(user)))

    override fun findById(id: UserId): User? =
        repository.findById(id.value).orElse(null)?.let(UserPersistenceMapper::toDomain)

    override fun findByUsername(username: Username): User? =
        repository.findByUsername(username.value)?.let(UserPersistenceMapper::toDomain)

    override fun findByEmail(email: Email): User? =
        repository.findByEmail(email.value)?.let(UserPersistenceMapper::toDomain)

    override fun existsByUsername(username: Username): Boolean =
        repository.existsByUsername(username.value)

    override fun existsByEmail(email: Email): Boolean =
        repository.existsByEmail(email.value)

    override fun search(search: UserSearch): UserPage {
        val criteria = buildCriteria(search)
        val pageable = PageRequest.of(search.page, search.size, defaultSort())

        val items = mongoTemplate
            .find(Query(criteria).with(pageable), UserDocument::class.java)
            .map(UserPersistenceMapper::toDomain)
        val total = mongoTemplate.count(Query(criteria), UserDocument::class.java)

        return UserPage(
            items = items,
            totalElements = total,
            page = search.page,
            size = search.size
        )
    }

    private fun buildCriteria(search: UserSearch): Criteria {
        val criteria = Criteria()
        search.status?.let { criteria.and("status").`is`(it.name) }
        search.role?.let { criteria.and("role").`is`(it.name) }
        return criteria
    }

    private fun defaultSort(): Sort = Sort.by(Sort.Direction.ASC, "username")
}
