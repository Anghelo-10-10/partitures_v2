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

    private val allowedContentTypes = setOf(
        "application/pdf"
    )

    private val maxFileSize = 10 * 1024 * 1024L

    @PostConstruct
    fun init() {

        try {
            val uploadPath = Paths.get(uploadDir)
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath)
            }
        } catch (ex: Exception) {
            throw FileStorageException("Could not create upload directory", ex)
        }
    }

    fun storeFile(file: MultipartFile): String {
        validateFile(file)

        val fileName = generateUniqueFileName(file.originalFilename ?: "file.pdf")

        return try {
            val targetLocation = Paths.get(uploadDir).resolve(fileName)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            fileName
        } catch (ex: IOException) {
            throw FileStorageException("Could not store file $fileName", ex)
        }
    }

    fun loadFileAsResource(fileName: String): Resource {
        return try {
            val filePath = Paths.get(uploadDir).resolve(fileName).normalize()
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

    fun deleteFile(fileName: String): Boolean {
        return try {
            val filePath = Paths.get(uploadDir).resolve(fileName).normalize()
            Files.deleteIfExists(filePath)
        } catch (ex: IOException) {
            false
        }
    }

    private fun validateFile(file: MultipartFile) {

        if (file.isEmpty) {
            throw InvalidFileTypeException("File is empty")
        }


        if (file.size > maxFileSize) {
            throw InvalidFileTypeException("File size exceeds maximum allowed size of ${maxFileSize / (1024 * 1024)}MB")
        }


        val contentType = file.contentType
        if (contentType !in allowedContentTypes) {
            throw InvalidFileTypeException("File type not allowed. Only PDF files are accepted")
        }


        val originalFilename = file.originalFilename ?: ""
        if (!originalFilename.lowercase().endsWith(".pdf")) {
            throw InvalidFileTypeException("File must have .pdf extension")
        }
    }

    private fun generateUniqueFileName(originalFilename: String): String {
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        val extension = originalFilename.substringAfterLast(".", "pdf")
        return "${timestamp}_${uuid}.$extension"
    }

    fun getFileUrl(fileName: String): String {
        return "/api/files/$fileName"
    }
}