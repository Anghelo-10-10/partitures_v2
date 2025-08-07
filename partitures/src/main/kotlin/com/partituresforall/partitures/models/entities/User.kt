package com.partituresforall.partitures.models.entities

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    @Column(nullable = false, length = 255)
    var password: String,

    @Column(length = 500)
    var bio: String? = null,

    @Column(name = "profile_image_url", length = 255)
    var profileImageUrl: String? = null

) : BaseEntity()