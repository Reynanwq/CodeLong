package com.codelong.integration

import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val MAPPER = ObjectMapper()

data class HttpResult(val status: Int, val body: String) {

    @Suppress("UNCHECKED_CAST")
    fun json(): Map<String, Any?> =
        MAPPER.readValue(body.ifBlank { "{}" }, Map::class.java) as Map<String, Any?>
}

/**
 * Cliente HTTP minimo para os testes ponta a ponta. Usa apenas o HttpClient do
 * JDK e o ObjectMapper do Spring, evitando depender de utilitarios de teste.
 */
class HttpApiClient(private val baseUrl: String) {

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    fun get(path: String, token: String? = null): HttpResult = send("GET", path, null, token)

    fun post(path: String, body: Any? = null, token: String? = null): HttpResult =
        send("POST", path, body, token)

    fun put(path: String, body: Any? = null, token: String? = null): HttpResult =
        send("PUT", path, body, token)

    fun patch(path: String, body: Any? = null, token: String? = null): HttpResult =
        send("PATCH", path, body, token)

    fun delete(path: String, token: String? = null): HttpResult = send("DELETE", path, null, token)

    private fun send(method: String, path: String, body: Any?, token: String?): HttpResult {
        val builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(30))
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }
        if (body != null) {
            builder.header("Content-Type", "application/json")
            builder.method(method, HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody())
        }
        val response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        return HttpResult(response.statusCode(), response.body())
    }
}

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.obj(key: String): Map<String, Any?> = this[key] as Map<String, Any?>

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.arr(key: String): List<Map<String, Any?>> = this[key] as List<Map<String, Any?>>

fun Map<String, Any?>.str(key: String): String = this[key] as String

fun Map<String, Any?>.int(key: String): Int = (this[key] as Number).toInt()

fun Map<String, Any?>.long(key: String): Long = (this[key] as Number).toLong()

fun Map<String, Any?>.bool(key: String): Boolean = this[key] as Boolean