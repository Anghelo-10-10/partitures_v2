package com.partituresforall.partitures.models.requests

data class LoginRequest(
    val email: String,
    val password: String
)