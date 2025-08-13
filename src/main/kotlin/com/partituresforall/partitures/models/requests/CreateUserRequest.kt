package com.partituresforall.partitures.models.requests

data class CreateUserRequest(
    val name: String,
    val email: String,
    val password: String
)