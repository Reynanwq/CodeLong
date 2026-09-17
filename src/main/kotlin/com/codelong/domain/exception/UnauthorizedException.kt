package com.codelong.domain.exception

class UnauthorizedException(
    code: String,
    message: String
) : DomainException(code, message)