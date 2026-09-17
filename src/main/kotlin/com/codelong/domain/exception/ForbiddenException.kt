package com.codelong.domain.exception

class ForbiddenException(
    code: String,
    message: String
) : DomainException(code, message)