package com.codelong.integration

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * Container MongoDB compartilhado pelos testes de integracao.
 *
 * O [container] e um singleton do JVM: o Surefire roda os testes no mesmo fork,
 * entao apenas uma instancia do MongoDB e iniciada para toda a suite.
 */
object MongoTestEnvironment {

    private const val IMAGE = "mongo:7.0"

    val container: MongoDBContainer by lazy {
        MongoDBContainer(DockerImageName.parse(IMAGE))
    }

    fun register(registry: DynamicPropertyRegistry) {
        container.start()
        registry.add("spring.data.mongodb.uri") { container.replicaSetUrl }
    }
}