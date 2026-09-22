package com.codelong.support

import com.codelong.domain.exception.DomainException

import com.codelong.domain.model.Game
import com.codelong.domain.model.Question
import com.codelong.domain.model.User
import com.codelong.domain.port.GamePage
import com.codelong.domain.port.GameRepository
import com.codelong.domain.port.GameSearch
import com.codelong.domain.port.QuestionPage
import com.codelong.domain.port.QuestionRepository
import com.codelong.domain.port.QuestionSearch
import com.codelong.domain.port.RankingFilter
import com.codelong.domain.port.RankingRepository
import com.codelong.domain.port.UserPage
import com.codelong.domain.port.UserRepository
import com.codelong.domain.port.UserSearch
import com.codelong.domain.service.RankingPolicy
import com.codelong.domain.valueobject.Category
import com.codelong.domain.valueobject.Email
import com.codelong.domain.valueobject.GameId
import com.codelong.domain.valueobject.GameMode
import com.codelong.domain.valueobject.QuestionId
import com.codelong.domain.valueobject.RankEntry
import com.codelong.domain.valueobject.UserId
import com.codelong.domain.valueobject.Username

class InMemoryUserRepository : UserRepository {

    private val store = mutableMapOf<String, User>()

    override fun save(user: User): User {
        store[user.id.value] = user
        return user
    }

    override fun findById(id: UserId): User? = store[id.value]

    override fun findByUsername(username: Username): User? =
        store.values.firstOrNull { it.username == username }

    override fun findByEmail(email: Email): User? =
        store.values.firstOrNull { it.email == email }

    override fun existsByUsername(username: Username): Boolean =
        store.values.any { it.username == username }

    override fun existsByEmail(email: Email): Boolean =
        store.values.any { it.email == email }

    override fun search(search: UserSearch): UserPage {
        val filtered = store.values
            .filter { search.status == null || it.status == search.status }
            .filter { search.role == null || it.role == search.role }
            .sortedBy { it.username.value }
        val from = search.page * search.size
        return UserPage(
            items = filtered.drop(from).take(search.size),
            totalElements = filtered.size.toLong(),
            page = search.page,
            size = search.size
        )
    }

    fun all(): List<User> = store.values.toList()
}

class InMemoryQuestionRepository : QuestionRepository {

    private val store = mutableMapOf<String, Question>()

    override fun save(question: Question): Question {
        store[question.id.value] = question
        return question
    }

    override fun findById(id: QuestionId): Question? = store[id.value]

    override fun findAllActive(): List<Question> = store.values.filter { it.isActive }

    override fun findActiveByCategory(category: Category): List<Question> =
        store.values.filter { it.isActive && it.category == category }

    override fun countActive(): Long = store.values.count { it.isActive }.toLong()

    override fun search(search: QuestionSearch): QuestionPage {
        val filtered = store.values.filter { question ->
            (search.status == null || question.status == search.status) &&
                (search.category == null || question.category == search.category) &&
                (search.difficulty == null || question.difficulty == search.difficulty)
        }
        val from = search.page * search.size
        return QuestionPage(
            items = filtered.drop(from).take(search.size),
            totalElements = filtered.size.toLong(),
            page = search.page,
            size = search.size
        )
    }

    override fun deleteById(id: QuestionId) {
        store.remove(id.value)
    }
}

class InMemoryGameRepository : GameRepository {

    private val store = mutableMapOf<String, Game>()

    override fun save(game: Game): Game {
        val state = game.state()
        val stored = store[state.id.value]
        if (stored != null && stored.state().version != state.version) {
            throw DomainException.concurrentModification()
        }
        val newVersion = if (stored == null) 1L else state.version + 1
        val saved = Game.reconstitute(state.copy(version = newVersion))
        store[state.id.value] = saved
        return saved
    }

    override fun findById(id: GameId): Game? = store[id.value]

    override fun findInProgressByUserId(userId: UserId): Game? =
        store.values
            .filter { it.userId == userId && it.isInProgress }
            .maxByOrNull { it.startedAt }

    override fun search(search: GameSearch): GamePage {
        val filtered = store.values
            .filter { it.userId == search.userId }
            .filter { search.status == null || it.status == search.status }
            .sortedByDescending { it.startedAt }
        val from = search.page * search.size
        return GamePage(
            items = filtered.drop(from).take(search.size),
            totalElements = filtered.size.toLong(),
            page = search.page,
            size = search.size
        )
    }

    fun all(): List<Game> = store.values.toList()
}

class InMemoryRankingRepository(
    initial: List<RankEntry> = emptyList()
) : RankingRepository {

    private val entries = initial.toMutableList()

    fun add(entry: RankEntry) {
        entries.add(entry)
    }

    override fun findRanking(page: Int, size: Int, filter: RankingFilter): List<RankEntry> =
        ranked(filter).drop(page * size).take(size)

    override fun findUserBestScore(userId: UserId, filter: RankingFilter): RankEntry? =
        ranked(filter).firstOrNull { it.userId == userId }

    override fun countUsersBetterThan(entry: RankEntry, filter: RankingFilter): Long =
        ranked(filter).count { RankingPolicy.isBetter(it, entry) }.toLong()

    override fun countRankedEntries(filter: RankingFilter): Long = ranked(filter).size.toLong()

    private fun ranked(filter: RankingFilter): List<RankEntry> = entries
        .filter(RankingPolicy::isEligible)
        .filter { entry ->
            filter.mode?.let { entry.mode == it }
                ?: (entry.mode != GameMode.APRENDIZADO && entry.mode != GameMode.GUBEE)
        }
        .filter { entry -> filter.theme?.let { entry.theme == it } ?: true }
        .sortedWith(RankingPolicy.comparator)
}