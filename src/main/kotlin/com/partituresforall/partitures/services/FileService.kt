package com.partituresforall.partitures.services

import com.partituresforall.partitures.exceptions.exceptions.files.FileNotFoundException
import com.partituresforall.partitures.exceptions.exceptions.files.FileStorageException
import com.partituresforall.partitures.exceptions.exceptions.files.InvalidFileTypeException
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
class FileService {

    @Value("\${app.file.upload-dir:uploads}")
    private lateinit var uploadDir: String

    // ===== TIPOS DE ARCHIVO PERMITIDOS =====
    private val allowedPdfContentTypes = setOf(
        "application/pdf"
    )

    // ===== LÍMITES DE TAMAÑO =====
    private val maxFileSize = 10 * 1024 * 1024L // 10MB para PDFs

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

    // ===== ALMACENAR PDF (MÉTODO EXISTENTE) =====
    fun storePdfFile(file: MultipartFile): String {
        validatePdfFile(file)

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

    // ===== VALIDACIONES =====

    private fun validatePdfFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw InvalidFileTypeException("File is empty")
        }

        if (file.size > maxFileSize) {
            throw InvalidFileTypeException("File size exceeds maximum allowed size of ${maxFileSize / (1024 * 1024)}MB")
        }

        val contentType = file.contentType
        if (contentType !in allowedPdfContentTypes) {
            throw InvalidFileTypeException("File type not allowed. Only PDF files are accepted")
        }

        val originalFilename = file.originalFilename ?: ""
        if (!originalFilename.lowercase().endsWith(".pdf")) {
            throw InvalidFileTypeException("File must have .pdf extension")
        }
    }


    // ===== UTILIDADES =====

    private fun generateUniqueFileName(originalFilename: String): String {
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        val extension = originalFilename.substringAfterLast(".", "")
        return "${timestamp}_${uuid}.$extension"
    }

    fun getFileUrl(fileName: String): String {
        return "/api/files/$fileName"
    }

    fun isPdfFile(contentType: String?): Boolean {
        return contentType in allowedPdfContentTypes
    }
}