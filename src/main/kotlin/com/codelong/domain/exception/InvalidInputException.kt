package com.codelong.domain.exception

class InvalidInputException(
    code: String,
    message: String
) : DomainException(code, message)