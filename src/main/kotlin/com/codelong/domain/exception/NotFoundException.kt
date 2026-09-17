package com.codelong.domain.exception

class NotFoundException(
    code: String,
    message: String
) : DomainException(code, message)