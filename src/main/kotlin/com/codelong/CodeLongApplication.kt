package com.codelong

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CodeLongApplication

fun main(args: Array<String>) {
    runApplication<CodeLongApplication>(*args)
}