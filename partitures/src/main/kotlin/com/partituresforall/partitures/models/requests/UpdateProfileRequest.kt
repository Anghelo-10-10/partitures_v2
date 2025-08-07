package com.partituresforall.partitures.models.requests

data class UpdateProfileRequest(
    val name: String?,
    val bio: String?,
    val profileImageUrl: String?
)