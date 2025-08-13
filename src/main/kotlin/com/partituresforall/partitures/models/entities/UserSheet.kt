package com.partituresforall.partitures.models.entities

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Column
import jakarta.persistence.FetchType


@Entity
@Table(name = "user_sheets")
data class UserSheet(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    var sheet: Sheet,

    @Column(nullable = false)
    var isOwner: Boolean = false,

    @Column(nullable = false)
    var isFavorite: Boolean = false

) : BaseEntity()
