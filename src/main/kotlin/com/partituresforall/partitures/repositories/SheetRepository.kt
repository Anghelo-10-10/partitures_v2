package com.partituresforall.partitures.repositories

import com.partituresforall.partitures.models.entities.Sheet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : JpaRepository<Sheet, Long> {

    fun findByIsPublic(isPublic: Boolean): List<Sheet>


    fun findByArtistContainingIgnoreCase(artist: String): List<Sheet>
    fun findByGenreIgnoreCase(genre: String): List<Sheet>
    fun findByInstrumentIgnoreCase(instrument: String): List<Sheet>
    fun findByTitleContainingIgnoreCase(title: String): List<Sheet>


    fun findByGenreIgnoreCaseAndInstrumentIgnoreCase(genre: String, instrument: String): List<Sheet>
    fun findByArtistContainingIgnoreCaseAndGenreIgnoreCase(artist: String, genre: String): List<Sheet>


    fun findByIsPublicAndGenreIgnoreCase(isPublic: Boolean, genre: String): List<Sheet>
    fun findByIsPublicAndInstrumentIgnoreCase(isPublic: Boolean, instrument: String): List<Sheet>
    fun findByIsPublicAndArtistContainingIgnoreCase(isPublic: Boolean, artist: String): List<Sheet>


    @Query("SELECT DISTINCT s.genre FROM Sheet s WHERE s.isPublic = true ORDER BY s.genre")
    fun findDistinctGenres(): List<String>

    @Query("SELECT DISTINCT s.instrument FROM Sheet s WHERE s.isPublic = true ORDER BY s.instrument")
    fun findDistinctInstruments(): List<String>

    @Query("SELECT DISTINCT s.artist FROM Sheet s WHERE s.isPublic = true ORDER BY s.artist")
    fun findDistinctArtists(): List<String>


    @Query("SELECT s FROM Sheet s WHERE s.isPublic = true AND " +
            "(LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.artist) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    fun searchPublicSheets(@Param("search") searchTerm: String): List<Sheet>

    @Query(value = """
    SELECT * FROM sheets s 
    WHERE s.is_public = true 
    AND (:searchTerm IS NULL OR 
         LOWER(s.title) ILIKE CONCAT('%', LOWER(:searchTerm), '%') OR 
         LOWER(s.artist) ILIKE CONCAT('%', LOWER(:searchTerm), '%') OR 
         (s.description IS NOT NULL AND LOWER(s.description) ILIKE CONCAT('%', LOWER(:searchTerm), '%')))
    AND (:artist IS NULL OR LOWER(s.artist) ILIKE CONCAT('%', LOWER(:artist), '%'))
    AND (:genre IS NULL OR LOWER(s.genre) = LOWER(:genre))
    AND (:instrument IS NULL OR LOWER(s.instrument) = LOWER(:instrument))
    ORDER BY s.created_at DESC
    """, nativeQuery = true)
    fun findByAdvancedSearch(
        @Param("searchTerm") searchTerm: String?,
        @Param("artist") artist: String?,
        @Param("genre") genre: String?,
        @Param("instrument") instrument: String?
    ): List<Sheet>


    fun findByIsPublicOrderByCreatedAtDesc(isPublic: Boolean): List<Sheet>


    fun findByIsPublicOrderByTitleAsc(isPublic: Boolean): List<Sheet>


    fun findByIsPublicOrderByArtistAsc(isPublic: Boolean): List<Sheet>
}