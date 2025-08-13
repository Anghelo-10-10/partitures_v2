package com.partituresforall.partitures.services

import com.partituresforall.partitures.exceptions.exceptions.files.InvalidFileTypeException
import com.partituresforall.partitures.exceptions.exceptions.sheets.*
import com.partituresforall.partitures.exceptions.exceptions.users.*
import com.partituresforall.partitures.models.entities.Sheet
import com.partituresforall.partitures.models.entities.User
import com.partituresforall.partitures.models.entities.UserSheet
import com.partituresforall.partitures.models.requests.CreateSheetWithFileRequest
import com.partituresforall.partitures.models.requests.UpdateSheetRequest
import com.partituresforall.partitures.models.requests.AdvanceSearchRequest
import com.partituresforall.partitures.repositories.SheetRepository
import com.partituresforall.partitures.repositories.UserRepository
import com.partituresforall.partitures.repositories.UserSheetRepository
import org.junit.jupiter.api.*
import org.mockito.Mockito.*
import org.springframework.web.multipart.MultipartFile
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SheetServiceTest {

    private lateinit var sheetRepository: SheetRepository
    private lateinit var userRepository: UserRepository
    private lateinit var userSheetRepository: UserSheetRepository
    private lateinit var fileValidationService: FileValidationService
    private lateinit var service: SheetService

    @BeforeEach
    fun carga() {
        sheetRepository = mock(SheetRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        userSheetRepository = mock(UserSheetRepository::class.java)
        fileValidationService = mock(FileValidationService::class.java)

        service = SheetService(sheetRepository, userRepository, userSheetRepository, fileValidationService)
    }

    private fun sampleUser(id: Long = 1L, email: String = "test@example.com"): User {
        val user = User(
            name = "Test User",
            email = email,
            password = "hashed_password"
        )
        return user.apply { this.id = id }
    }

    private fun sampleSheet(id: Long = 1L, title: String = "Test Sheet"): Sheet {
        val sheet = Sheet(
            title = title,
            description = "Test description",
            artist = "Test Artist",
            genre = "Classical",
            instrument = "Piano",
            pdfContent = "fake_pdf_content".toByteArray(),
            pdfFilename = "test.pdf",
            pdfSize = 1024L,
            pdfContentType = "application/pdf",
            isPublic = true
        )
        return sheet.apply { this.id = id }
    }

    private fun sampleUserSheet(user: User, sheet: Sheet, isOwner: Boolean = false, isFavorite: Boolean = false): UserSheet {
        val userSheet = UserSheet(
            user = user,
            sheet = sheet,
            isOwner = isOwner,
            isFavorite = isFavorite
        )
        return userSheet.apply { this.id = 1L }
    }

    private fun createPdfFileMock(
        filename: String = "test.pdf",
        content: String = "fake_pdf_content",
        contentType: String = "application/pdf",
        isEmpty: Boolean = false,
        size: Long? = null
    ): MultipartFile {
        return object : MultipartFile {
            override fun getName(): String = "file"
            override fun getOriginalFilename(): String? = filename
            override fun getContentType(): String? = contentType
            override fun isEmpty(): Boolean = isEmpty
            override fun getSize(): Long = size ?: content.length.toLong()
            override fun getBytes(): ByteArray = content.toByteArray()
            override fun getInputStream() = java.io.ByteArrayInputStream(content.toByteArray())
            override fun transferTo(dest: java.io.File) {}
            override fun transferTo(dest: java.nio.file.Path) {}
        }
    }

    // ✅ MOCK para SheetOwnerProjection
    private fun mockSheetOwnerProjection(sheetId: Long, ownerId: Long): UserSheetRepository.SheetOwnerProjection {
        return object : UserSheetRepository.SheetOwnerProjection {
            override fun getSheetId(): Long = sheetId
            override fun getOwnerId(): Long = ownerId
        }
    }

    // ===== TESTS PARA createSheetWithFile =====
    @Test
    fun should_create_sheet_with_file() {
        val request = CreateSheetWithFileRequest(
            title = "Test Sheet",
            description = "Test description",
            artist = "Test Artist",
            genre = "Classical",
            instrument = "Piano",
            isPublic = true,
            ownerId = 1L
        )
        val file = createPdfFileMock()
        val owner = sampleUser()
        val sheet = sampleSheet()
        val userSheet = sampleUserSheet(owner, sheet, isOwner = true)

        doNothing().`when`(fileValidationService).validatePdfFile(file)
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(owner))
        `when`(sheetRepository.save(any(Sheet::class.java))).thenReturn(sheet)
        `when`(userSheetRepository.save(any(UserSheet::class.java))).thenReturn(userSheet)

        val response = service.createSheetWithFile(request, file)

        assertEquals("Test Sheet", response.title)
        assertEquals("Test description", response.description)
        assertEquals("Test Artist", response.artist)
        assertEquals("Classical", response.genre)
        assertEquals("Piano", response.instrument)
        assertEquals(true, response.isPublic)
        assertEquals(1L, response.ownerId)

        verify(fileValidationService).validatePdfFile(file)
    }

    @Test
    fun should_throw_user_not_found_on_create_sheet_with_file() {
        val request = CreateSheetWithFileRequest(
            title = "Test Sheet",
            description = "Test description",
            artist = "Test Artist",
            genre = "Classical",
            instrument = "Piano",
            isPublic = true,
            ownerId = 999L
        )
        val file = createPdfFileMock()
        `when`(userRepository.findById(999L)).thenReturn(Optional.empty())

        assertFailsWith<UserNotFoundException> {
            service.createSheetWithFile(request, file)
        }
    }

    @Test
    fun should_throw_invalid_file_type_when_file_validation_fails() {
        val request = CreateSheetWithFileRequest(
            title = "Test Sheet",
            description = "Test description",
            artist = "Test Artist",
            genre = "Classical",
            instrument = "Piano",
            isPublic = true,
            ownerId = 1L
        )
        val file = createPdfFileMock()
        val owner = sampleUser()

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(owner))

        doThrow(InvalidFileTypeException("El archivo está vacío"))
            .`when`(fileValidationService).validatePdfFile(file)

        assertFailsWith<InvalidFileTypeException> {
            service.createSheetWithFile(request, file)
        }

        verify(fileValidationService).validatePdfFile(file)
    }

    // ===== TESTS PARA getSheetById =====
    @Test
    fun should_get_sheet_by_id() {
        val sheet = sampleSheet()
        val owner = sampleUser()
        val userSheet = sampleUserSheet(owner, sheet, isOwner = true)

        `when`(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet))
        `when`(userSheetRepository.findBySheetIdAndIsOwner(1L, true)).thenReturn(userSheet)
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.getSheetById(1L)

        assertEquals("Test Sheet", response.title)
        assertEquals(1L, response.ownerId)
    }

    @Test
    fun should_throw_sheet_not_found_on_get() {
        `when`(sheetRepository.findById(999L)).thenReturn(Optional.empty())

        assertFailsWith<SheetNotFoundException> {
            service.getSheetById(999L)
        }
    }

    @Test
    fun should_throw_sheet_not_found_when_no_owner_found() {
        val sheet = sampleSheet()
        `when`(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet))
        `when`(userSheetRepository.findBySheetIdAndIsOwner(1L, true)).thenReturn(null)

        assertFailsWith<SheetNotFoundException> {
            service.getSheetById(1L)
        }
    }

    // ===== TESTS PARA getSheetPdfContent =====
    @Test
    fun should_get_sheet_pdf_content() {
        val sheet = sampleSheet()
        `when`(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet))

        val response = service.getSheetPdfContent(1L)

        assertEquals("fake_pdf_content".toByteArray().contentToString(), response.contentToString())
    }

    @Test
    fun should_throw_sheet_not_found_on_get_pdf_content() {
        `when`(sheetRepository.findById(999L)).thenReturn(Optional.empty())

        assertFailsWith<SheetNotFoundException> {
            service.getSheetPdfContent(999L)
        }
    }

    // ===== TESTS PARA updateSheet =====
    @Test
    fun should_update_sheet_all_fields() {
        val sheet = sampleSheet()
        val owner = sampleUser()
        val userSheet = sampleUserSheet(owner, sheet, isOwner = true)
        val request = UpdateSheetRequest(
            title = "Updated Title",
            description = "Updated description",
            artist = "Updated Artist",
            genre = "Updated Genre",
            instrument = "Updated Instrument",
            isPublic = false
        )

        `when`(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet))
        `when`(userSheetRepository.findBySheetIdAndIsOwner(1L, true)).thenReturn(userSheet)
        `when`(sheetRepository.save(any(Sheet::class.java))).thenReturn(sheet)
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.updateSheet(1L, request)

        assertEquals("Updated Title", response.title)
        verify(sheetRepository).save(sheet)
    }

    // ===== ✅ TESTS PARA updateSheetFile CORREGIDO =====
    @Test
    fun should_update_sheet_file() {
        val sheet = sampleSheet()
        val owner = sampleUser()
        val userSheet = sampleUserSheet(owner, sheet, isOwner = true)
        val file = createPdfFileMock()

        `when`(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet))
        `when`(userSheetRepository.findBySheetIdAndIsOwner(1L, true)).thenReturn(userSheet)
        `when`(sheetRepository.save(any(Sheet::class.java))).thenReturn(sheet)
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB") // ✅ CORREGIDO: usar valor específico

        doNothing().`when`(fileValidationService).validatePdfFile(file)

        val response = service.updateSheetFile(1L, file)

        assertEquals("Test Sheet", response.title)
        verify(sheetRepository).save(sheet)
        verify(fileValidationService).validatePdfFile(file)
    }

    // ===== TESTS PARA deleteSheet =====
    @Test
    fun should_delete_sheet() {
        `when`(sheetRepository.existsById(1L)).thenReturn(true)
        doNothing().`when`(userSheetRepository).deleteBySheetId(1L)
        doNothing().`when`(sheetRepository).deleteById(1L)

        service.deleteSheet(1L)

        verify(userSheetRepository).deleteBySheetId(1L)
        verify(sheetRepository).deleteById(1L)
    }

    @Test
    fun should_throw_sheet_not_found_on_delete() {
        `when`(sheetRepository.existsById(999L)).thenReturn(false)

        assertFailsWith<SheetNotFoundException> {
            service.deleteSheet(999L)
        }
    }

    // ===== ✅ TESTS OPTIMIZADOS CORREGIDOS =====
    @Test
    fun should_get_public_sheets_optimized() {
        val sheet1 = sampleSheet(1L, "Sheet 1")
        val sheet2 = sampleSheet(2L, "Sheet 2")
        val sheetOwner1 = mockSheetOwnerProjection(1L, 1L)
        val sheetOwner2 = mockSheetOwnerProjection(2L, 1L)

        `when`(sheetRepository.findByIsPublic(true)).thenReturn(listOf(sheet1, sheet2))
        `when`(userSheetRepository.findSheetOwners(listOf(1L, 2L))).thenReturn(listOf(sheetOwner1, sheetOwner2))
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.getPublicSheets()

        assertEquals(2, response.size)
        assertEquals("Sheet 1", response[0].title)
        assertEquals("Sheet 2", response[1].title)
        assertEquals(1L, response[0].ownerId)
        assertEquals(1L, response[1].ownerId)

        // ✅ CORREGIDO: Verificación específica en lugar de any()
        verify(userSheetRepository).findSheetOwners(listOf(1L, 2L))
        verify(userSheetRepository, never()).findBySheetIdAndIsOwner(1L, true)
        verify(userSheetRepository, never()).findBySheetIdAndIsOwner(2L, true)
    }

    @Test
    fun should_search_sheets_optimized() {
        val sheet1 = sampleSheet(1L, "Piano Sonata")
        val sheetOwner1 = mockSheetOwnerProjection(1L, 1L)

        `when`(sheetRepository.searchPublicSheets("piano")).thenReturn(listOf(sheet1))
        `when`(userSheetRepository.findSheetOwners(listOf(1L))).thenReturn(listOf(sheetOwner1))
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.searchSheets("piano")

        assertEquals(1, response.size)
        assertEquals("Piano Sonata", response[0].title)
        assertEquals(1L, response[0].ownerId)

        verify(userSheetRepository).findSheetOwners(listOf(1L))
    }

    @Test
    fun should_get_sheets_by_genre_optimized() {
        val sheet1 = sampleSheet(1L, "Classical Piece")
        val sheetOwner1 = mockSheetOwnerProjection(1L, 1L)

        `when`(sheetRepository.findByIsPublicAndGenreIgnoreCase(true, "Classical")).thenReturn(listOf(sheet1))
        `when`(userSheetRepository.findSheetOwners(listOf(1L))).thenReturn(listOf(sheetOwner1))
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.getSheetsByGenre("Classical")

        assertEquals(1, response.size)
        assertEquals("Classical Piece", response[0].title)
        assertEquals(1L, response[0].ownerId)

        verify(userSheetRepository).findSheetOwners(listOf(1L))
    }

    @Test
    fun should_get_sheets_by_instrument_optimized() {
        val sheet1 = sampleSheet(1L, "Piano Piece")
        val sheetOwner1 = mockSheetOwnerProjection(1L, 1L)

        `when`(sheetRepository.findByIsPublicAndInstrumentIgnoreCase(true, "Piano")).thenReturn(listOf(sheet1))
        `when`(userSheetRepository.findSheetOwners(listOf(1L))).thenReturn(listOf(sheetOwner1))
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.getSheetsByInstrument("Piano")

        assertEquals(1, response.size)
        assertEquals("Piano Piece", response[0].title)
        assertEquals(1L, response[0].ownerId)

        verify(userSheetRepository).findSheetOwners(listOf(1L))
    }

    @Test
    fun should_get_sheets_by_artist_optimized() {
        val sheet1 = sampleSheet(1L, "Mozart Piece")
        val sheetOwner1 = mockSheetOwnerProjection(1L, 1L)

        `when`(sheetRepository.findByIsPublicAndArtistContainingIgnoreCase(true, "Mozart")).thenReturn(listOf(sheet1))
        `when`(userSheetRepository.findSheetOwners(listOf(1L))).thenReturn(listOf(sheetOwner1))
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.getSheetsByArtist("Mozart")

        assertEquals(1, response.size)
        assertEquals("Mozart Piece", response[0].title)
        assertEquals(1L, response[0].ownerId)

        verify(userSheetRepository).findSheetOwners(listOf(1L))
    }

    @Test
    fun should_get_recent_sheets_optimized() {
        val sheet1 = sampleSheet(1L, "Recent Sheet 1")
        val sheet2 = sampleSheet(2L, "Recent Sheet 2")
        val sheetOwner1 = mockSheetOwnerProjection(1L, 1L)
        val sheetOwner2 = mockSheetOwnerProjection(2L, 1L)

        `when`(sheetRepository.findByIsPublicOrderByCreatedAtDesc(true)).thenReturn(listOf(sheet1, sheet2))
        `when`(userSheetRepository.findSheetOwners(listOf(1L, 2L))).thenReturn(listOf(sheetOwner1, sheetOwner2))
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.getRecentSheets()

        assertEquals(2, response.size)
        assertEquals("Recent Sheet 1", response[0].title)
        assertEquals("Recent Sheet 2", response[1].title)

        verify(userSheetRepository).findSheetOwners(listOf(1L, 2L))
    }

    @Test
    fun should_get_favorite_sheets_optimized() {
        val user = sampleUser()
        val sheet1 = sampleSheet(1L, "Sheet 1")
        val sheet2 = sampleSheet(2L, "Sheet 2")
        val userSheet1 = sampleUserSheet(user, sheet1, isFavorite = true)
        val userSheet2 = sampleUserSheet(user, sheet2, isFavorite = true)
        val sheetOwner1 = mockSheetOwnerProjection(1L, 2L)
        val sheetOwner2 = mockSheetOwnerProjection(2L, 2L)

        `when`(userRepository.existsById(1L)).thenReturn(true)
        `when`(userSheetRepository.findByUserIdAndIsFavorite(1L, true)).thenReturn(listOf(userSheet1, userSheet2))
        `when`(userSheetRepository.findSheetOwners(listOf(1L, 2L))).thenReturn(listOf(sheetOwner1, sheetOwner2))
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.getFavoriteSheets(1L)

        assertEquals(2, response.size)
        assertEquals("Sheet 1", response[0].title)
        assertEquals("Sheet 2", response[1].title)
        assertEquals(2L, response[0].ownerId)
        assertEquals(2L, response[1].ownerId)

        verify(userSheetRepository).findSheetOwners(listOf(1L, 2L))
    }

    // ===== ✅ TESTS PARA advancedSearch OPTIMIZADO CORREGIDO =====
    @Test
    fun should_perform_advanced_search_optimized() {
        val request = AdvanceSearchRequest(
            searchTerm = "piano",
            artist = "Mozart",
            genre = "Classical",
            instrument = "Piano",
            sortBy = "title"
        )
        val sheet1 = sampleSheet(1L, "Piano Sonata")
        val sheetOwner1 = mockSheetOwnerProjection(1L, 1L)

        `when`(sheetRepository.findByAdvancedSearch("piano", "Mozart", "Classical", "Piano")).thenReturn(listOf(sheet1))
        `when`(userSheetRepository.findSheetOwners(listOf(1L))).thenReturn(listOf(sheetOwner1))
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.advancedSearch(request)

        assertEquals(1, response.size)
        assertEquals("Piano Sonata", response[0].title)
        assertEquals(1L, response[0].ownerId)

        // ✅ CORREGIDO: Verificaciones específicas
        verify(userSheetRepository).findSheetOwners(listOf(1L))
        verify(userSheetRepository, never()).findBySheetIdAndIsOwner(1L, true)
    }

    @Test
    fun should_perform_advanced_search_with_null_values() {
        val request = AdvanceSearchRequest(
            searchTerm = null,
            artist = null,
            genre = null,
            instrument = null,
            sortBy = "recent"
        )
        val sheet1 = sampleSheet(1L, "Test Sheet")
        val sheetOwner1 = mockSheetOwnerProjection(1L, 1L)

        `when`(sheetRepository.findByAdvancedSearch(null, null, null, null)).thenReturn(listOf(sheet1))
        `when`(userSheetRepository.findSheetOwners(listOf(1L))).thenReturn(listOf(sheetOwner1))
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.advancedSearch(request)

        assertEquals(1, response.size)
        assertEquals("Test Sheet", response[0].title)

        verify(userSheetRepository).findSheetOwners(listOf(1L))
    }

    // ===== ✅ TESTS PARA EDGE CASES OPTIMIZADOS CORREGIDOS =====
    @Test
    fun should_handle_empty_sheets_list_in_optimized_methods() {
        `when`(sheetRepository.findByIsPublic(true)).thenReturn(emptyList())

        val response = service.getPublicSheets()

        assertEquals(0, response.size)

        // ✅ CORREGIDO: Verificar que NO se llama findSheetOwners con lista vacía
        verify(userSheetRepository, never()).findSheetOwners(emptyList())
    }

    @Test
    fun should_handle_sheets_without_owners_in_optimized_methods() {
        val sheet1 = sampleSheet(1L, "Orphan Sheet")

        `when`(sheetRepository.findByIsPublic(true)).thenReturn(listOf(sheet1))
        `when`(userSheetRepository.findSheetOwners(listOf(1L))).thenReturn(emptyList())

        val response = service.getPublicSheets()

        assertEquals(0, response.size)
        verify(userSheetRepository).findSheetOwners(listOf(1L))
    }

    // ===== TESTS RESTANTES SIN CAMBIOS SIGNIFICATIVOS =====
    @Test
    fun should_get_available_genres() {
        `when`(sheetRepository.findDistinctGenres()).thenReturn(listOf("Classical", "Jazz", "Rock"))

        val response = service.getAvailableGenres()

        assertEquals(3, response.size)
        assertTrue(response.contains("Classical"))
        assertTrue(response.contains("Jazz"))
        assertTrue(response.contains("Rock"))
    }

    @Test
    fun should_get_available_instruments() {
        `when`(sheetRepository.findDistinctInstruments()).thenReturn(listOf("Piano", "Guitar", "Violin"))

        val response = service.getAvailableInstruments()

        assertEquals(3, response.size)
        assertTrue(response.contains("Piano"))
        assertTrue(response.contains("Guitar"))
        assertTrue(response.contains("Violin"))
    }

    @Test
    fun should_get_available_artists() {
        `when`(sheetRepository.findDistinctArtists()).thenReturn(listOf("Mozart", "Bach", "Beethoven"))

        val response = service.getAvailableArtists()

        assertEquals(3, response.size)
        assertTrue(response.contains("Mozart"))
        assertTrue(response.contains("Bach"))
        assertTrue(response.contains("Beethoven"))
    }

    @Test
    fun should_get_user_owned_sheets() {
        val user = sampleUser()
        val sheet1 = sampleSheet(1L, "Owned Sheet 1")
        val sheet2 = sampleSheet(2L, "Owned Sheet 2")
        val userSheet1 = sampleUserSheet(user, sheet1, isOwner = true)
        val userSheet2 = sampleUserSheet(user, sheet2, isOwner = true)

        `when`(userRepository.existsById(1L)).thenReturn(true)
        `when`(userSheetRepository.findByUserIdAndIsOwner(1L, true)).thenReturn(listOf(userSheet1, userSheet2))
        `when`(fileValidationService.formatFileSize(1024L)).thenReturn("1.00 KB")

        val response = service.getUserOwnedSheets(1L)

        assertEquals(2, response.size)
        assertEquals("Owned Sheet 1", response[0].title)
        assertEquals("Owned Sheet 2", response[1].title)
        assertEquals(1L, response[0].ownerId)
        assertEquals(1L, response[1].ownerId)
    }

    @Test
    fun should_throw_user_not_found_on_get_owned_sheets() {
        `when`(userRepository.existsById(999L)).thenReturn(false)

        assertFailsWith<UserNotFoundException> {
            service.getUserOwnedSheets(999L)
        }
    }

    @Test
    fun should_add_to_favorites_new_relation() {
        val user = sampleUser()
        val sheet = sampleSheet()

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet))
        `when`(userSheetRepository.findByUserIdAndSheetId(1L, 1L)).thenReturn(null)
        `when`(userSheetRepository.save(any(UserSheet::class.java))).thenReturn(sampleUserSheet(user, sheet, isFavorite = true))

        service.addToFavorites(1L, 1L)

        verify(userSheetRepository).save(any(UserSheet::class.java))
    }

    @Test
    fun should_add_to_favorites_existing_relation() {
        val user = sampleUser()
        val sheet = sampleSheet()
        val existingRelation = sampleUserSheet(user, sheet, isFavorite = false)

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet))
        `when`(userSheetRepository.findByUserIdAndSheetId(1L, 1L)).thenReturn(existingRelation)
        `when`(userSheetRepository.save(existingRelation)).thenReturn(existingRelation)

        service.addToFavorites(1L, 1L)

        assertTrue(existingRelation.isFavorite)
        verify(userSheetRepository).save(existingRelation)
    }

    @Test
    fun should_remove_from_favorites() {
        val user = sampleUser()
        val sheet = sampleSheet()
        val relation = sampleUserSheet(user, sheet, isOwner = false, isFavorite = true)

        `when`(userSheetRepository.findByUserIdAndSheetId(1L, 1L)).thenReturn(relation)
        doNothing().`when`(userSheetRepository).delete(relation)

        service.removeFromFavorites(1L, 1L)

        verify(userSheetRepository).delete(relation)
    }

    @Test
    fun should_throw_illegal_state_when_removing_own_sheet_from_favorites() {
        val user = sampleUser()
        val sheet = sampleSheet()
        val relation = sampleUserSheet(user, sheet, isOwner = true, isFavorite = true)

        `when`(userSheetRepository.findByUserIdAndSheetId(1L, 1L)).thenReturn(relation)

        assertFailsWith<IllegalStateException> {
            service.removeFromFavorites(1L, 1L)
        }
    }

    @Test
    fun should_return_true_when_sheet_is_favorite() {
        `when`(userRepository.existsById(1L)).thenReturn(true)
        `when`(sheetRepository.existsById(1L)).thenReturn(true)
        `when`(userSheetRepository.existsByUserIdAndSheetIdAndIsFavorite(1L, 1L, true)).thenReturn(true)

        val result = service.isSheetFavorite(1L, 1L)

        assertTrue(result)
    }

    @Test
    fun should_return_false_when_sheet_is_not_favorite() {
        `when`(userRepository.existsById(1L)).thenReturn(true)
        `when`(sheetRepository.existsById(1L)).thenReturn(true)
        `when`(userSheetRepository.existsByUserIdAndSheetIdAndIsFavorite(1L, 1L, true)).thenReturn(false)

        val result = service.isSheetFavorite(1L, 1L)

        assertFalse(result)
    }

    @AfterEach
    fun descarga() {
        // limpieza si se requiere
    }
}