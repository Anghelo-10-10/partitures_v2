package com.partituresforall.partitures.services

import com.partituresforall.partitures.exceptions.exceptions.files.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.mockito.Mockito.*
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

    private lateinit var fileValidationService: FileValidationService
    private lateinit var fileService: FileService
    private lateinit var tempUploadDir: Path

    @BeforeEach
    fun carga() {
        try {
            // ✅ CREAR MOCK del FileValidationService
            fileValidationService = mock(FileValidationService::class.java)

            // Crear directorio temporal para pruebas
            tempUploadDir = Files.createTempDirectory("test-uploads")

            // ✅ USAR NUEVO CONSTRUCTOR con FileValidationService
            fileService = FileService(fileValidationService)

            // Usar reflexión para establecer el uploadDir
            val uploadDirField = FileService::class.java.getDeclaredField("uploadDir")
            uploadDirField.isAccessible = true
            uploadDirField.set(fileService, tempUploadDir.toString())

            // Inicializar el servicio
            fileService.init()
        } catch (e: Exception) {
            // Si falla la inicialización, asegurarnos de que tempUploadDir tenga un valor
            if (!::tempUploadDir.isInitialized) {
                tempUploadDir = Files.createTempDirectory("test-uploads-fallback")
            }
            throw e
        }
    }

    @AfterEach
    fun descarga() {
        // ✅ VERIFICAR que tempUploadDir esté inicializado antes de limpiar
        if (::tempUploadDir.isInitialized && Files.exists(tempUploadDir)) {
            tempUploadDir.toFile().deleteRecursively()
        }
    }

    // ===== INIT TESTS (SIN CAMBIOS) =====
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
        val invalidService = FileService(fileValidationService)
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

    // ✅ CORREGIDO: Usar mocks correctamente
    @Test
    fun should_check_if_content_type_is_pdf_correctly() {
        `when`(fileValidationService.isPdfFile("application/pdf")).thenReturn(true)
        `when`(fileValidationService.isPdfFile("text/plain")).thenReturn(false)
        `when`(fileValidationService.isPdfFile(null)).thenReturn(false)

        assertTrue(fileService.isPdfFile("application/pdf"))
        assertFalse(fileService.isPdfFile("text/plain"))
        assertFalse(fileService.isPdfFile(null))
    }

    // ===== LOAD FILE AS RESOURCE TESTS (SIN CAMBIOS) =====
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

    // ===== DELETE FILE TESTS (SIN CAMBIOS) =====
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

    // ===== SIMPLE MULTIPART FILE IMPLEMENTATION (SIN CAMBIOS) =====
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

        override fun transferTo(dest: java.io.File) {
            dest.writeBytes(content)
        }

        override fun transferTo(dest: java.nio.file.Path) {
            Files.write(dest, content)
        }
    }

    // ===== ✅ STORE FILE TESTS CORREGIDOS =====
    @Test
    fun should_store_valid_pdf_file_successfully() {
        val file = TestMultipartFile("document.pdf", "application/pdf", "test content".toByteArray())

        // ✅ MOCK: FileValidationService no lanza excepción
        doNothing().`when`(fileValidationService).validatePdfFile(file)

        val result = fileService.storeFile(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))
        assertTrue(result.endsWith(".pdf"))

        // Verificar que el archivo fue creado
        val storedFilePath = tempUploadDir.resolve(result)
        assertTrue(Files.exists(storedFilePath))

        val storedContent = Files.readAllBytes(storedFilePath)
        assertContentEquals("test content".toByteArray(), storedContent)

        // ✅ VERIFICAR que se llamó la validación
        verify(fileValidationService).validatePdfFile(file)
    }

    @Test
    fun should_generate_unique_filename_for_stored_files() {
        val file1 = TestMultipartFile("same.pdf", "application/pdf", "content1".toByteArray())
        val file2 = TestMultipartFile("same.pdf", "application/pdf", "content2".toByteArray())

        // ✅ MOCK: FileValidationService no lanza excepción para ambos archivos
        doNothing().`when`(fileValidationService).validatePdfFile(file1)
        doNothing().`when`(fileValidationService).validatePdfFile(file2)

        val result1 = fileService.storeFile(file1)
        val result2 = fileService.storeFile(file2)

        assertNotEquals(result1, result2)
        assertTrue(result1.startsWith("pdfs/"))
        assertTrue(result2.startsWith("pdfs/"))
        assertTrue(result1.endsWith(".pdf"))
        assertTrue(result2.endsWith(".pdf"))

        // ✅ CORREGIDO: Verificar llamadas específicas en lugar de any()
        verify(fileValidationService).validatePdfFile(file1)
        verify(fileValidationService).validatePdfFile(file2)
    }

    @Test
    fun should_store_pdf_file_specifically() {
        val file = TestMultipartFile("test.pdf", "application/pdf", "pdf content".toByteArray())

        // ✅ MOCK: FileValidationService no lanza excepción
        doNothing().`when`(fileValidationService).validatePdfFile(file)

        val result = fileService.storePdfFile(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))
        assertTrue(result.endsWith(".pdf"))

        val storedFilePath = tempUploadDir.resolve(result)
        assertTrue(Files.exists(storedFilePath))

        verify(fileValidationService).validatePdfFile(file)
    }

    // ===== ✅ VALIDATION TESTS CORREGIDOS =====
    @Test
    fun should_throw_invalid_file_type_when_file_is_empty() {
        val file = TestMultipartFile("test.pdf", "application/pdf", byteArrayOf(), empty = true)

        // ✅ MOCK: FileValidationService lanza excepción
        doThrow(InvalidFileTypeException("El archivo está vacío"))
            .`when`(fileValidationService).validatePdfFile(file)

        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }

        verify(fileValidationService).validatePdfFile(file)
    }

    @Test
    fun should_throw_invalid_file_type_when_file_is_too_large() {
        val file = TestMultipartFile(
            "large.pdf",
            "application/pdf",
            "content".toByteArray(),
            size = 11 * 1024 * 1024L // 11MB > límite
        )

        // ✅ MOCK: FileValidationService lanza excepción
        doThrow(InvalidFileTypeException("El archivo es demasiado grande"))
            .`when`(fileValidationService).validatePdfFile(file)

        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }

        verify(fileValidationService).validatePdfFile(file)
    }

    @Test
    fun should_throw_invalid_file_type_when_content_type_is_not_pdf() {
        val file = TestMultipartFile("document.txt", "text/plain", "content".toByteArray())

        // ✅ MOCK: FileValidationService lanza excepción
        doThrow(InvalidFileTypeException("Tipo de archivo no permitido"))
            .`when`(fileValidationService).validatePdfFile(file)

        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }

        verify(fileValidationService).validatePdfFile(file)
    }

    @Test
    fun should_throw_invalid_file_type_when_filename_extension_is_not_pdf() {
        val file = TestMultipartFile("document.txt", "application/pdf", "content".toByteArray())

        // ✅ MOCK: FileValidationService lanza excepción
        doThrow(InvalidFileTypeException("El archivo debe tener extensión .pdf"))
            .`when`(fileValidationService).validatePdfFile(file)

        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }

        verify(fileValidationService).validatePdfFile(file)
    }

    @Test
    fun should_accept_pdf_files_with_uppercase_extension() {
        val file = TestMultipartFile("document.PDF", "application/pdf", "pdf content".toByteArray())

        // ✅ MOCK: FileValidationService no lanza excepción
        doNothing().`when`(fileValidationService).validatePdfFile(file)

        val result = fileService.storeFile(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))

        verify(fileValidationService).validatePdfFile(file)
    }

    @Test
    fun should_handle_null_original_filename() {
        val file = TestMultipartFile(null, "application/pdf", "content".toByteArray())

        // ✅ MOCK: FileValidationService lanza excepción
        doThrow(InvalidFileTypeException("El archivo debe tener extensión .pdf"))
            .`when`(fileValidationService).validatePdfFile(file)

        assertFailsWith<InvalidFileTypeException> {
            fileService.storeFile(file)
        }

        verify(fileValidationService).validatePdfFile(file)
    }

    // ===== ✅ TESTS PARA FileValidationService CORREGIDOS =====
    @Test
    fun should_use_file_validation_service_for_file_info() {
        val file = TestMultipartFile("test.pdf", "application/pdf", "content".toByteArray())
        val expectedFileInfo = FileInfo(
            originalName = "test.pdf",
            size = 7L,
            sizeFormatted = "7.00 KB",
            contentType = "application/pdf",
            extension = "pdf",
            isEmpty = false
        )

        // ✅ MOCK: FileValidationService retorna FileInfo
        `when`(fileValidationService.getFileInfo(file)).thenReturn(expectedFileInfo)

        val result = fileService.getFileInfo(file)

        assertEquals(expectedFileInfo, result)
        verify(fileValidationService).getFileInfo(file)
    }

    @Test
    fun should_use_file_validation_service_for_format_file_size() {
        val bytes = 1024L
        val expectedSize = "1.00 KB"

        // ✅ MOCK: FileValidationService retorna tamaño formateado
        `when`(fileValidationService.formatFileSize(bytes)).thenReturn(expectedSize)

        val result = fileService.formatFileSize(bytes)

        assertEquals(expectedSize, result)
        verify(fileValidationService).formatFileSize(bytes)
    }

    @Test
    fun should_use_file_validation_service_for_validate_pdf_file() {
        val file = TestMultipartFile("test.pdf", "application/pdf", "content".toByteArray())

        // ✅ MOCK: FileValidationService no lanza excepción
        doNothing().`when`(fileValidationService).validatePdfFile(file)

        fileService.validatePdfFile(file)

        verify(fileValidationService).validatePdfFile(file)
    }

    @Test
    fun should_store_pdf_file_with_content_validation() {
        val file = TestMultipartFile("test.pdf", "application/pdf", "pdf content".toByteArray())

        // ✅ MOCK: FileValidationService no lanza excepción
        doNothing().`when`(fileValidationService).validatePdfContent(file)

        val result = fileService.storePdfFileWithContentValidation(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))
        assertTrue(result.endsWith(".pdf"))

        verify(fileValidationService).validatePdfContent(file)
    }

    @Test
    fun should_store_pdf_file_with_logging() {
        val file = TestMultipartFile("test.pdf", "application/pdf", "pdf content".toByteArray())
        val expectedFileInfo = FileInfo(
            originalName = "test.pdf",
            size = 11L,
            sizeFormatted = "11.00 KB",
            contentType = "application/pdf",
            extension = "pdf",
            isEmpty = false
        )

        // ✅ MOCK: FileValidationService methods
        `when`(fileValidationService.getFileInfo(file)).thenReturn(expectedFileInfo)
        doNothing().`when`(fileValidationService).validatePdfFile(file)

        val result = fileService.storePdfFileWithLogging(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))

        verify(fileValidationService).getFileInfo(file)
        // ✅ CORREGIDO: Se llama 2 veces - en validatePdfFile() y en storePdfFile()
        verify(fileValidationService, times(2)).validatePdfFile(file)
    }

    // ===== INTEGRATION TESTS (SIN CAMBIOS) =====
    @Test
    fun should_load_stored_file_as_resource() {
        val file = TestMultipartFile("test.pdf", "application/pdf", "test content".toByteArray())

        doNothing().`when`(fileValidationService).validatePdfFile(file)

        val storedPath = fileService.storeFile(file)
        val resource = fileService.loadFileAsResource(storedPath)

        assertNotNull(resource)
        assertTrue(resource.exists())
        assertTrue(resource.isReadable)
    }

    @Test
    fun should_delete_stored_file_successfully() {
        val file = TestMultipartFile("test.pdf", "application/pdf", "test content".toByteArray())

        doNothing().`when`(fileValidationService).validatePdfFile(file)

        val storedPath = fileService.storeFile(file)
        val filePath = tempUploadDir.resolve(storedPath)
        assertTrue(Files.exists(filePath))

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
            size = 5 * 1024 * 1024L // 5MB (límite)
        )

        doNothing().`when`(fileValidationService).validatePdfFile(file)

        val result = fileService.storeFile(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))
    }

    @Test
    fun should_handle_very_small_pdf_file() {
        val file = TestMultipartFile("tiny.pdf", "application/pdf", "x".toByteArray())

        doNothing().`when`(fileValidationService).validatePdfFile(file)

        val result = fileService.storeFile(file)

        assertNotNull(result)
        assertTrue(result.startsWith("pdfs/"))
        assertTrue(result.endsWith(".pdf"))
    }
}