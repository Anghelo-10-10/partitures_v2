package com.partituresforall.partitures.models.entities

import jakarta.persistence.*

@Entity
@Table(name = "sheets")
data class Sheet(
    // Información básica
    @Column(nullable = false, length = 150)
    var title: String,

    @Column(length = 500, columnDefinition = "TEXT")
    var description: String? = null,

    // Información musical
    @Column(nullable = false, length = 100)
    var artist: String,

    @Column(nullable = false, length = 50)
    var genre: String,

    @Column(nullable = false, length = 50)
    var instrument: String,

    // PDF content
    @Lob
    @Column(name = "pdf_content", nullable = false, columnDefinition = "BYTEA")
    var pdfContent: ByteArray,

    @Column(name = "pdf_filename", nullable = false, length = 255)
    var pdfFilename: String,

    @Column(name = "pdf_size", nullable = false)
    var pdfSize: Long,

    @Column(name = "pdf_content_type", nullable = false, length = 100)
    var pdfContentType: String = "application/pdf",

    // Configuración
    @Column(nullable = false)
    var isPublic: Boolean = false
) : BaseEntity() {

    // Override equals/hashCode para ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Sheet

        if (id != other.id) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (artist != other.artist) return false
        if (genre != other.genre) return false
        if (instrument != other.instrument) return false
        if (!pdfContent.contentEquals(other.pdfContent)) return false
        if (pdfFilename != other.pdfFilename) return false
        if (pdfSize != other.pdfSize) return false
        if (pdfContentType != other.pdfContentType) return false
        if (isPublic != other.isPublic) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + artist.hashCode()
        result = 31 * result + genre.hashCode()
        result = 31 * result + instrument.hashCode()
        result = 31 * result + pdfContent.contentHashCode()
        result = 31 * result + pdfFilename.hashCode()
        result = 31 * result + pdfSize.hashCode()
        result = 31 * result + pdfContentType.hashCode()
        result = 31 * result + isPublic.hashCode()
        return result
    }
}