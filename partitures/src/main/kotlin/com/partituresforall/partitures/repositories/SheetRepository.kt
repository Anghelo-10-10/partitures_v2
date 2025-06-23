package com.partituresforall.partitures.repositories

import com.partituresforall.partitures.models.entities.Sheet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : JpaRepository<Sheet, Long>