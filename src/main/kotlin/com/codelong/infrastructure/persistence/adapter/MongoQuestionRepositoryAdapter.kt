package com.codelong.infrastructure.persistence.adapter

import com.codelong.domain.model.Question
import com.codelong.domain.port.QuestionPage
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.port.QuestionSearch
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.QuestionStatus
import com.codelong.infrastructure.persistence.document.QuestionDocument
import com.codelong.infrastructure.persistence.mapper.QuestionPersistenceMapper
import com.codelong.infrastructure.persistence.repository.SpringQuestionDataRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class MongoQuestionRepositoryAdapter(
    private val repository: SpringQuestionDataRepository,
    private val mongoTemplate: MongoTemplate
) : QuestionRepository {

    override fun save(question: Question): Question =
        QuestionPersistenceMapper.toDomain(repository.save(QuestionPersistenceMapper.toDocument(question)))

    override fun findById(id: QuestionId): Question? =
        repository.findById(id.value).orElse(null)?.let(QuestionPersistenceMapper::toDomain)

    override fun findAllActive(): List<Question> =
        repository.findByStatus(QuestionStatus.ACTIVE.name).map(QuestionPersistenceMapper::toDomain)

    override fun countActive(): Long =
        repository.countByStatus(QuestionStatus.ACTIVE.name)

    override fun search(search: QuestionSearch): QuestionPage {
        val criteria = buildCriteria(search)
        val pageable = PageRequest.of(search.page, search.size, defaultSort())

        val items = mongoTemplate
            .find(Query(criteria).with(pageable), QuestionDocument::class.java)
            .map(QuestionPersistenceMapper::toDomain)
        val total = mongoTemplate.count(Query(criteria), QuestionDocument::class.java)

        return QuestionPage(
            items = items,
            totalElements = total,
            page = search.page,
            size = search.size
        )
    }

    override fun deleteById(id: QuestionId) = repository.deleteById(id.value)

    private fun buildCriteria(search: QuestionSearch): Criteria {
        val criteria = Criteria()
        search.status?.let { criteria.and("status").`is`(it.name) }
        search.category?.let { criteria.and("category").`is`(it.name) }
        search.difficulty?.let { criteria.and("difficulty").`is`(it.name) }
        return criteria
    }

    private fun defaultSort(): Sort = Sort.by(Sort.Direction.DESC, "createdAt")
}