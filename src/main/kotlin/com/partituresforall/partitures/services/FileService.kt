package com.partituresforall.partitures.services

import com.partituresforall.partitures.exceptions.exceptions.files.FileNotFoundException
import com.partituresforall.partitures.exceptions.exceptions.files.FileStorageException
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class FileService(
    private val fileValidationService: FileValidationService // ✅ NUEVA DEPENDENCIA
) {

    @Value("\${app.file.upload-dir:uploads}")
    private lateinit var uploadDir: String

    @PostConstruct
    fun init() {
        try {
            // Crear directorio principal
            val uploadPath = Paths.get(uploadDir)
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath)
            }

            // Crear subdirectorios
            val pdfsPath = Paths.get(uploadDir, "pdfs")
            if (!Files.exists(pdfsPath)) {
                Files.createDirectories(pdfsPath)
            }

        } catch (ex: Exception) {
            throw FileStorageException("Could not create upload directories", ex)
        }
    }

    // ===== MÉTODO PRINCIPAL (MANTIENE COMPATIBILIDAD) =====
    fun storeFile(file: MultipartFile): String {
        return storePdfFile(file)
    }

    // ===== ALMACENAR PDF CON VALIDACIÓN CENTRALIZADA =====
    fun storePdfFile(file: MultipartFile): String {
        // ✅ USAR VALIDACIÓN CENTRALIZADA en lugar de validatePdfFile()
        fileValidationService.validatePdfFile(file)

        val fileName = generateUniqueFileName(file.originalFilename ?: "file.pdf")
        val subDir = "pdfs"

        return try {
            val targetLocation = Paths.get(uploadDir, subDir).resolve(fileName)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            "$subDir/$fileName" // Retorna ruta relativa
        } catch (ex: IOException) {
            throw FileStorageException("Could not store PDF file $fileName", ex)
        }
    }

    // ===== ALMACENAR PDF CON VALIDACIÓN ESTRICTA =====
    /**
     * ✅ NUEVO: Almacenar PDF con validación de contenido (magic bytes)
     */
    fun storePdfFileWithContentValidation(file: MultipartFile): String {
        // Validación más estricta que incluye magic bytes
        fileValidationService.validatePdfContent(file)

        val fileName = generateUniqueFileName(file.originalFilename ?: "file.pdf")
        val subDir = "pdfs"

        return try {
            val targetLocation = Paths.get(uploadDir, subDir).resolve(fileName)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            "$subDir/$fileName"
        } catch (ex: IOException) {
            throw FileStorageException("Could not store PDF file $fileName", ex)
        }
    }

    // ===== CARGAR ARCHIVO COMO RESOURCE =====
    fun loadFileAsResource(fileName: String): Resource {
        return try {
            // fileName puede ser "pdfs/file.pdf" o "images/image.jpg" o solo "file.pdf" (legacy)
            val filePath = if (fileName.contains("/")) {
                // Nueva estructura con subdirectorios
                Paths.get(uploadDir).resolve(fileName).normalize()
            } else {
                // Compatibilidad con archivos antiguos en raíz
                Paths.get(uploadDir).resolve(fileName).normalize()
            }

            val resource = UrlResource(filePath.toUri())

            if (resource.exists() && resource.isReadable) {
                resource
            } else {
                throw FileNotFoundException("File not found: $fileName")
            }
        } catch (ex: Exception) {
            throw FileNotFoundException("File not found: $fileName", ex)
        }
    }

    // ===== ELIMINAR ARCHIVO =====
    fun deleteFile(fileName: String): Boolean {
        return try {
            val filePath = if (fileName.contains("/")) {
                Paths.get(uploadDir).resolve(fileName).normalize()
            } else {
                Paths.get(uploadDir).resolve(fileName).normalize()
            }
            Files.deleteIfExists(filePath)
        } catch (ex: IOException) {
            false
        }
    }

    // ===== ❌ VALIDACIONES ELIMINADAS =====
    // Ya no necesitamos validatePdfFile() porque usamos fileValidationService

    // ===== ✅ UTILIDADES ACTUALIZADAS =====

    private fun generateUniqueFileName(originalFilename: String): String {
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        val extension = originalFilename.substringAfterLast(".", "")
        return "${timestamp}_${uuid}.$extension"
    }

    fun getFileUrl(fileName: String): String {
        return "/api/files/$fileName"
    }

    // ✅ USAR SERVICIO CENTRALIZADO para verificar si es PDF
    fun isPdfFile(contentType: String?): Boolean {
        return fileValidationService.isPdfFile(contentType)
    }

    // ✅ NUEVOS MÉTODOS USANDO EL SERVICIO CENTRALIZADO

    /**
     * ✅ Obtener información detallada del archivo antes de almacenarlo
     */
    fun getFileInfo(file: MultipartFile): FileInfo {
        return fileValidationService.getFileInfo(file)
    }

    /**
     * ✅ Validar archivo sin almacenarlo
     */
    fun validatePdfFile(file: MultipartFile) {
        fileValidationService.validatePdfFile(file)
    }

    /**
     * ✅ Formatear tamaño de archivo
     */
    fun formatFileSize(bytes: Long): String {
        return fileValidationService.formatFileSize(bytes)
    }

    /**
     * ✅ Almacenar archivo con logging detallado
     */
    fun storePdfFileWithLogging(file: MultipartFile): String {
        println("=== FILE STORAGE DEBUG ===")
        val fileInfo = getFileInfo(file)
        println("File info: $fileInfo")

        try {
            validatePdfFile(file)
            println("✅ File validation passed")

            val storedPath = storePdfFile(file)
            println("✅ File stored at: $storedPath")

            return storedPath
        } catch (e: Exception) {
            println("❌ File storage failed: ${e.message}")
            throw e
        }
    }
}