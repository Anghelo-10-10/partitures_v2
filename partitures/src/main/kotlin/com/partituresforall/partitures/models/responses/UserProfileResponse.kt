package com.partituresforall.partitures.models.responses

import java.time.LocalDateTime

data class UserProfileResponse(
    val id: Long,
    val name: String,
    val bio: String?,
    val profileImageUrl: String?,
    val createdAt: LocalDateTime
)