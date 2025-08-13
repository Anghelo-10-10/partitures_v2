package com.partituresforall.partitures.services

import com.partituresforall.partitures.exceptions.exceptions.files.InvalidFileTypeException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import kotlin.math.round

@Service
class FileValidationService {

    companion object {
        // ✅ Configuración centralizada y consistente
        const val MAX_PDF_SIZE_MB = 5L // Unificar en 5MB (como en SheetService)
        const val MAX_PDF_SIZE_BYTES = MAX_PDF_SIZE_MB * 1024 * 1024L

        private val ALLOWED_PDF_CONTENT_TYPES = setOf("application/pdf")
        private val ALLOWED_PDF_EXTENSIONS = setOf("pdf")
    }

    /**
     * ✅ Validación unificada para archivos PDF
     * Usada tanto por FileService como por SheetService
     * Mensajes consistentes en español
     */
    fun validatePdfFile(file: MultipartFile) {
        // 1. Validar que no esté vacío
        if (file.isEmpty) {
            throw InvalidFileTypeException("El archivo está vacío")
        }

        // 2. Validar tamaño (límite unificado en 5MB)
        if (file.size > MAX_PDF_SIZE_BYTES) {
            throw InvalidFileTypeException(
                "El archivo es demasiado grande. Máximo permitido: ${MAX_PDF_SIZE_MB}MB (archivo actual: ${formatFileSize(file.size)})"
            )
        }

        // 3. Validar content type
        val contentType = file.contentType
        if (contentType !in ALLOWED_PDF_CONTENT_TYPES) {
            throw InvalidFileTypeException(
                "Tipo de archivo no permitido. Solo se aceptan archivos PDF. Tipo recibido: ${contentType ?: "desconocido"}"
            )
        }

        // 4. Validar extensión
        val originalFilename = file.originalFilename ?: ""
        val extension = originalFilename.substringAfterLast(".", "").lowercase()
        if (extension !in ALLOWED_PDF_EXTENSIONS) {
            throw InvalidFileTypeException(
                "El archivo debe tener extensión .pdf. Extensión recibida: .${extension}"
            )
        }
    }

    /**
     * ✅ Validación adicional: verificar que el archivo sea realmente un PDF
     * Opcional: valida los magic bytes del archivo
     */
    fun validatePdfContent(file: MultipartFile) {
        validatePdfFile(file) // Validaciones básicas primero

        val bytes = file.bytes
        if (bytes.size < 4) {
            throw InvalidFileTypeException("El archivo está corrupto o es demasiado pequeño")
        }

        // Verificar magic bytes de PDF (%PDF)
        val pdfHeader = bytes.take(4)
        if (pdfHeader[0] != 0x25.toByte() || // %
            pdfHeader[1] != 0x50.toByte() || // P
            pdfHeader[2] != 0x44.toByte() || // D
            pdfHeader[3] != 0x46.toByte()) { // F
            throw InvalidFileTypeException("El archivo no es un PDF válido")
        }
    }

    /**
     * ✅ Formateo consistente de tamaño de archivo
     * Reemplaza el método duplicado en SheetService
     */
    fun formatFileSize(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return when {
            mb >= 1.0 -> "${round(mb * 100) / 100} MB"
            else -> "${round(bytes.toDouble() / 1024 * 100) / 100} KB"
        }
    }

    /**
     * ✅ Utility para verificar si es PDF
     */
    fun isPdfFile(contentType: String?): Boolean {
        return contentType in ALLOWED_PDF_CONTENT_TYPES
    }

    /**
     * ✅ Obtener información detallada del archivo
     */
    fun getFileInfo(file: MultipartFile): FileInfo {
        return FileInfo(
            originalName = file.originalFilename ?: "archivo_sin_nombre",
            size = file.size,
            sizeFormatted = formatFileSize(file.size),
            contentType = file.contentType ?: "desconocido",
            extension = file.originalFilename?.substringAfterLast(".", "") ?: "",
            isEmpty = file.isEmpty
        )
    }
}

/**
 * ✅ Data class para información de archivos
 */
data class FileInfo(
    val originalName: String,
    val size: Long,
    val sizeFormatted: String,
    val contentType: String,
    val extension: String,
    val isEmpty: Boolean
)