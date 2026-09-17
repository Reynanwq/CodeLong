package com.codelong.domain.exception

class ConflictException(
    code: String,
    message: String
) : DomainException(code, message)