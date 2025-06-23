package com.partituresforall.partitures.models.requests

data class UpdateUserRequest(
    val name: String?,
    val email: String?,
    val password: String?
)