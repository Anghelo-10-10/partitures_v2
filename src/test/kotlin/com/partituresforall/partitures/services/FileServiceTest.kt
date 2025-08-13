package com.partituresforall.partitures.services

import com.partituresforall.partitures.exceptions.exceptions.files.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertContentEquals

class FileServiceTest {

    private lateinit var fileService: FileService
    private lateinit var tempUploadDir: Path

    @BeforeEach
    fun carga() {
        // Crear directorio temporal para pruebas
        tempUploadDir = Files.createTempDirectory("test-uploads")

        fileService = FileService()

        // Usar reflexión para establecer el uploadDir
        val uploadDirField = FileService::class.java.getDeclaredField("uploadDir")
        uploadDirField.isAccessible = true
        uploadDirField.set(fileService, tempUploadDir.toString())

        // Inicializar el servicio
        fileService.init()
    }

    @AfterEach
    fun descarga() {
        // Limpiar directorio temporal
        tempUploadDir.toFile().deleteRecursively()
    }

    // ===== INIT TESTS =====
    @Test
    fun should_create_upload_directories_on_init() {
        val pdfsDir = tempUploadDir.resolve("pdfs")

        assertTrue(Files.exists(tempUploadDir))
        assertTrue(Files.exists(pdfsDir))
        assertTrue(Files.isDirectory(tempUploadDir))
        assertTrue(Files.isDirectory(pdfsDir))
    }

    @Test
    fun should_throw_file_storage_exception_when_cannot_create_directories() {
        // Crear un nuevo servicio con un directorio inválido
        val invalidService = FileService()
        val uploadDirField = FileService::class.java.getDeclaredField("uploadDir")
        uploadDirField.isAccessible = true
        uploadDirField.set(invalidService, "///invalid:path<<>>")

        assertFailsWith<FileStorageException> {
            invalidService.init()
        }
    }

    // ===== UTILITY METHODS TESTS =====
    @Test
    fun should_generate_correct_file_url() {
        val fileName = "pdfs/test_file.pdf"

        val url = fileService.getFileUrl(fileName)

        assertEquals("/api/files/pdfs/test_file.pdf", url)
    }

    @Test
    fun should_generate_correct_file_url_for_simple_filename() {
        val fileName = "simple_file.pdf"

        val url = fileService.getFileUrl(fileName)

        assertEquals("/api/files/simple_file.pdf", url)
    }

    @Test
    fun should_check_if_content_type_is_pdf_correctly() {
        assertTrue(fileService.isPdfFile("application/pdf"))
        assertFalse(fileService.isPdfFile("text/plain"))
        assertFalse(fileService.isPdfFile(null))
    }

    // ===== LOAD FILE AS RESOURCE TESTS =====
    @Test
    fun should_load_legacy_file_as_resource() {
        // Crear archivo directamente en el directorio raíz (simulando archivo legacy)
        val legacyFileName = "legacy_file.pdf"
        val legacyFilePath = tempUploadDir.resolve(legacyFileName)
        Files.write(legacyFilePath, "legacy content".toByteArray())

        val resource = fileService.loadFileAsResource(legacyFileName)

        assertNotNull(resource)
        assertTrue(resource.exists())
        assertTrue(resource.isReadable)
    }

    @Test
    fun should_load_file_from_subdirectory_as_resource() {
        // Crear archivo en subdirectorio pdfs
        val fileName = "test_file.pdf"
        val filePath = tempUploadDir.resolve("pdfs").resolve(fileName)
        Files.write(filePath, "test content".toByteArray())

        val resource = fileService.loadFileAsResource("pdfs/$fileName")

        assertNotNull(resource)
        assertTrue(resource.exists())
        assertTrue(resource.isReadable)
    }

    @Test
    fun should_throw_file_not_found_when_file_does_not_exist() {
        assertFailsWith<FileNotFoundException> {
            fileService.loadFileAsResource("nonexistent.pdf")
        }
    }

    @Test
    fun should_throw_file_not_found_when_subdirectory_file_does_not_exist() {
        assertFailsWith<FileNotFoundException> {
            fileService.loadFileAsResource("pdfs/nonexistent.pdf")
        }
    }

    // ===== DELETE FILE TESTS =====
    @Test
    fun should_delete_legacy_file_successfully() {
        // Crear archivo directamente en el directorio raíz
        val legacyFileName = "legacy_file.pdf"
        val legacyFilePath = tempUploadDir.resolve(legacyFileName)
        Files.write(legacyFilePath, "legacy content".toByteArray())

        assertTrue(Files.exists(legacyFilePath))

        val result = fileService.deleteFile(legacyFileName)

        assertTrue(result)
        assertFalse(Files.exists(legacyFilePath))
    }

    @Test
    fun should_delete_file_from_subdirectory_successfully() {
        // Crear archivo en subdirectorio
        val fileName = "test_file.pdf"
        val filePath = tempUploadDir.resolve("pdfs").resolve(fileName)
        Files.write(filePath, "test content".toByteArray())

        assertTrue(Files.exists(filePath))

        val result = fileService.deleteFile("pdfs/$fileName")

        assertTrue(result)
        assertFalse(Files.exists(filePath))
    }

    @Test
    fun should_return_false_when_deleting_non_existent_file() {
        val result = fileService.deleteFile("nonexistent.pdf")

        assertFalse(result)
    }

    @Test
    fun should_return_false_when_deleting_file_with_invalid_path() {
        val result = fileService.deleteFile("pdfs/nonexistent.pdf")

        assertFalse(result)
    }

    // ===== SIMPLE MULTIPART FILE IMPLEMENTATION =====
    // Implementación mínima para evitar conflictos de sobrecarga
    private class TestMultipartFile(
        private val filename: String?,
        private val contentType: String?,
        private val content: ByteArray,
        private val empty: Boolean = false,
        private val size: Long? = null
    ) : MultipartFile {

        override fun getName(): String = "file"
        override fun getOriginalFilename(): String? = filename
        override fun getContentType(): String? = contentType
        override fun isEmpty(): Boolean = empty
        override fun getSize(): Long = size ?: content.size.toLong()
        override fun getBytes(): ByteArray = content
        override fun getInputStream(): InputStream = ByteArrayInputStream(content)

        // Implementaciones vacías para evitar conflictos
        override fun transferTo(dest: java.io.File) {
            dest.writeBytes(content)
        }

        override fun transferTo(dest: java.nio.file.Path) {
            Files.write(dest, content)
        }
    }

    // ===== STORE FILE TESTS CON IMPLEMENTACIÓN SIMPLE =====
    @Test
    fun should_store_valid_pdf_file_successfully() {
        val file = TestMultipartFile("document.pdf", "application/pdf", "test content".toByteArray())

        val result = fileService.storeFile(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))
        assertTrue(result.endsWith(".pdf"))

        // Verificar que el archivo fue creado
        val storedFilePath = tempUploadDir.resolve(result)
        assertTrue(Files.exists(storedFilePath))

        val storedContent = Files.readAllBytes(storedFilePath)
        assertContentEquals("test content".toByteArray(), storedContent)
    }

    @Test
    fun should_generate_unique_filename_for_stored_files() {
        val file1 = TestMultipartFile("same.pdf", "application/pdf", "content1".toByteArray())
        val file2 = TestMultipartFile("same.pdf", "application/pdf", "content2".toByteArray())

        val result1 = fileService.storeFile(file1)
        val result2 = fileService.storeFile(file2)

        assertNotEquals(result1, result2)
        assertTrue(result1.startsWith("pdfs/"))
        assertTrue(result2.startsWith("pdfs/"))
        assertTrue(result1.endsWith(".pdf"))
        assertTrue(result2.endsWith(".pdf"))
    }

    @Test
    fun should_store_pdf_file_specifically() {
        val file = TestMultipartFile("test.pdf", "application/pdf", "pdf content".toByteArray())

        val result = fileService.storePdfFile(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))
        assertTrue(result.endsWith(".pdf"))

        val storedFilePath = tempUploadDir.resolve(result)
        assertTrue(Files.exists(storedFilePath))
    }

    // ===== VALIDATION TESTS =====
    @Test
    fun should_throw_invalid_file_type_when_file_is_empty() {
        val file = TestMultipartFile("test.pdf", "application/pdf", byteArrayOf(), empty = true)

        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }
    }

    @Test
    fun should_throw_invalid_file_type_when_file_is_too_large() {
        val file = TestMultipartFile(
            "large.pdf",
            "application/pdf",
            "content".toByteArray(),
            size = 11 * 1024 * 1024L // 11MB > 10MB limit
        )

        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }
    }

    @Test
    fun should_throw_invalid_file_type_when_content_type_is_not_pdf() {
        val file = TestMultipartFile("document.txt", "text/plain", "content".toByteArray())

        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }
    }

    @Test
    fun should_throw_invalid_file_type_when_filename_extension_is_not_pdf() {
        val file = TestMultipartFile("document.txt", "application/pdf", "content".toByteArray())

        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }
    }

    @Test
    fun should_accept_pdf_files_with_uppercase_extension() {
        val file = TestMultipartFile("document.PDF", "application/pdf", "pdf content".toByteArray())

        val result = fileService.storeFile(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))
    }

    @Test
    fun should_handle_null_original_filename() {
        val file = TestMultipartFile(null, "application/pdf", "content".toByteArray())

        // El FileService real lanza excepción cuando originalFilename es null
        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }
    }

    @Test
    fun should_handle_empty_original_filename() {
        val file = TestMultipartFile("", "application/pdf", "content".toByteArray())

        // El FileService real lanza excepción cuando originalFilename es vacío
        // porque "" no termina en ".pdf"
        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }
    }

    @Test
    fun should_handle_filename_without_extension() {
        val file = TestMultipartFile("filename_without_extension", "application/pdf", "content".toByteArray())

        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }
    }

    @Test
    fun should_load_stored_file_as_resource() {
        // Primero almacenamos un archivo
        val file = TestMultipartFile("test.pdf", "application/pdf", "test content".toByteArray())
        val storedPath = fileService.storeFile(file)

        // Luego lo cargamos como resource
        val resource = fileService.loadFileAsResource(storedPath)

        assertNotNull(resource)
        assertTrue(resource.exists())
        assertTrue(resource.isReadable)
    }

    @Test
    fun should_delete_stored_file_successfully() {
        // Primero almacenamos un archivo
        val file = TestMultipartFile("test.pdf", "application/pdf", "test content".toByteArray())
        val storedPath = fileService.storeFile(file)

        // Verificar que existe
        val filePath = tempUploadDir.resolve(storedPath)
        assertTrue(Files.exists(filePath))

        // Eliminarlo
        val result = fileService.deleteFile(storedPath)

        assertTrue(result)
        assertFalse(Files.exists(filePath))
    }

    @Test
    fun should_handle_maximum_allowed_file_size() {
        val file = TestMultipartFile(
            "max_size.pdf",
            "application/pdf",
            "content".toByteArray(),
            size = 10 * 1024 * 1024L // Exactamente 10MB (límite)
        )

        val result = fileService.storeFile(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))
    }

    @Test
    fun should_handle_very_small_pdf_file() {
        val file = TestMultipartFile("tiny.pdf", "application/pdf", "x".toByteArray())

        val result = fileService.storeFile(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))
        assertTrue(result.endsWith(".pdf"))
    }
}