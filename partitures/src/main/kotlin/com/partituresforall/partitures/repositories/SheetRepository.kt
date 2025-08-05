package com.partituresforall.partitures.repositories

import com.partituresforall.partitures.models.entities.Sheet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : JpaRepository<Sheet, Long> {
    // Búsquedas básicas
    fun findByIsPublic(isPublic: Boolean): List<Sheet>

    // Búsquedas por campos musicales
    fun findByArtistContainingIgnoreCase(artist: String): List<Sheet>
    fun findByGenreIgnoreCase(genre: String): List<Sheet>
    fun findByInstrumentIgnoreCase(instrument: String): List<Sheet>
    fun findByTitleContainingIgnoreCase(title: String): List<Sheet>

    // Búsquedas combinadas
    fun findByGenreIgnoreCaseAndInstrumentIgnoreCase(genre: String, instrument: String): List<Sheet>
    fun findByArtistContainingIgnoreCaseAndGenreIgnoreCase(artist: String, genre: String): List<Sheet>

    // Búsquedas públicas con filtros
    fun findByIsPublicAndGenreIgnoreCase(isPublic: Boolean, genre: String): List<Sheet>
    fun findByIsPublicAndInstrumentIgnoreCase(isPublic: Boolean, instrument: String): List<Sheet>
    fun findByIsPublicAndArtistContainingIgnoreCase(isPublic: Boolean, artist: String): List<Sheet>

    // Obtener géneros, artistas e instrumentos únicos (para filtros en la app)
    @Query("SELECT DISTINCT s.genre FROM Sheet s WHERE s.isPublic = true ORDER BY s.genre")
    fun findDistinctGenres(): List<String>

    @Query("SELECT DISTINCT s.instrument FROM Sheet s WHERE s.isPublic = true ORDER BY s.instrument")
    fun findDistinctInstruments(): List<String>

    @Query("SELECT DISTINCT s.artist FROM Sheet s WHERE s.isPublic = true ORDER BY s.artist")
    fun findDistinctArtists(): List<String>

    // Búsqueda general (título, artista o descripción)
    @Query("SELECT s FROM Sheet s WHERE s.isPublic = true AND " +
            "(LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.artist) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    fun searchPublicSheets(@Param("search") searchTerm: String): List<Sheet>
}