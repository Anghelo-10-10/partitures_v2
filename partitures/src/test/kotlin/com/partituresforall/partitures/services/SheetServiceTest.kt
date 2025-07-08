package com.partituresforall.partitures.services

import com.partituresforall.partitures.exceptions.exceptions.sheets.*
import com.partituresforall.partitures.exceptions.exceptions.users.*
import com.partituresforall.partitures.models.entities.Sheet
import com.partituresforall.partitures.models.entities.User
import com.partituresforall.partitures.models.entities.UserSheet
import com.partituresforall.partitures.models.requests.CreateSheetRequest
import com.partituresforall.partitures.models.requests.UpdateSheetRequest
import com.partituresforall.partitures.repositories.SheetRepository
import com.partituresforall.partitures.repositories.UserRepository
import com.partituresforall.partitures.repositories.UserSheetRepository
import org.junit.jupiter.api.*
import org.mockito.Mockito.*
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SheetServiceTest {

    private lateinit var sheetRepository: SheetRepository
    private lateinit var userRepository: UserRepository
    private lateinit var userSheetRepository: UserSheetRepository
    private lateinit var service: SheetService

    @BeforeEach
    fun carga() {
        sheetRepository = mock(SheetRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        userSheetRepository = mock(UserSheetRepository::class.java)
        service = SheetService(sheetRepository, userRepository, userSheetRepository)
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
            pdfReference = "test.pdf",
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

    // Tests para createSheet
    @Test
    fun should_create_a_sheet() {
        val request = CreateSheetRequest(
            title = "Test Sheet",
            description = "Test description",
            pdfReference = "test.pdf",
            isPublic = true,
            ownerId = 1L
        )
        val owner = sampleUser()
        val sheet = sampleSheet()
        val userSheet = sampleUserSheet(owner, sheet, isOwner = true)

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(owner))
        `when`(sheetRepository.save(any(Sheet::class.java))).thenReturn(sheet)
        `when`(userSheetRepository.save(any(UserSheet::class.java))).thenReturn(userSheet)

        val response = service.createSheet(request)

        assertEquals("Test Sheet", response.title)
        assertEquals("Test description", response.description)
        assertEquals("test.pdf", response.pdfReference)
        assertEquals(true, response.isPublic)
        assertEquals(1L, response.ownerId)
        verify(sheetRepository).save(any(Sheet::class.java))
        verify(userSheetRepository).save(any(UserSheet::class.java))
    }

    @Test
    fun should_throw_user_not_found_on_create_sheet() {
        val request = CreateSheetRequest(
            title = "Test Sheet",
            description = "Test description",
            pdfReference = "test.pdf",
            isPublic = true,
            ownerId = 999L
        )
        `when`(userRepository.findById(999L)).thenReturn(Optional.empty())

        assertFailsWith<UserNotFoundException> {
            service.createSheet(request)
        }
    }

    @Test
    fun should_throw_invalid_pdf_reference() {
        val request = CreateSheetRequest(
            title = "Test Sheet",
            description = "Test description",
            pdfReference = "test.txt",
            isPublic = true,
            ownerId = 1L
        )
        val owner = sampleUser()
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(owner))

        assertFailsWith<InvalidPdfReferenceException> {
            service.createSheet(request)
        }
    }

    // Tests para getSheetById
    @Test
    fun should_get_sheet_by_id() {
        val sheet = sampleSheet()
        val owner = sampleUser()
        val userSheet = sampleUserSheet(owner, sheet, isOwner = true)

        `when`(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet))
        `when`(userSheetRepository.findBySheetIdAndIsOwner(1L, true)).thenReturn(userSheet)

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

    // Tests para updateSheet
    @Test
    fun should_update_sheet_all_fields() {
        val sheet = sampleSheet()
        val owner = sampleUser()
        val userSheet = sampleUserSheet(owner, sheet, isOwner = true)
        val request = UpdateSheetRequest(
            title = "Updated Title",
            description = "Updated description",
            isPublic = false
        )

        `when`(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet))
        `when`(userSheetRepository.findBySheetIdAndIsOwner(1L, true)).thenReturn(userSheet)
        `when`(sheetRepository.save(any(Sheet::class.java))).thenReturn(sheet)

        val response = service.updateSheet(1L, request)

        assertEquals("Updated Title", response.title)
        verify(sheetRepository).save(sheet)
    }

    @Test
    fun should_update_sheet_partial_fields() {
        val sheet = sampleSheet()
        val owner = sampleUser()
        val userSheet = sampleUserSheet(owner, sheet, isOwner = true)
        val request = UpdateSheetRequest(
            title = "Updated Title",
            description = null,
            isPublic = null
        )

        `when`(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet))
        `when`(userSheetRepository.findBySheetIdAndIsOwner(1L, true)).thenReturn(userSheet)
        `when`(sheetRepository.save(any(Sheet::class.java))).thenReturn(sheet)

        val response = service.updateSheet(1L, request)

        assertEquals("Updated Title", response.title)
        verify(sheetRepository).save(sheet)
    }

    @Test
    fun should_throw_sheet_not_found_on_update() {
        val request = UpdateSheetRequest(
            title = "Updated Title",
            description = null,
            isPublic = null
        )
        `when`(sheetRepository.findById(999L)).thenReturn(Optional.empty())

        assertFailsWith<SheetNotFoundException> {
            service.updateSheet(999L, request)
        }
    }

    @Test
    fun should_throw_sheet_not_found_when_no_owner_found_on_update() {
        val sheet = sampleSheet()
        val request = UpdateSheetRequest(
            title = "Updated Title",
            description = null,
            isPublic = null
        )
        `when`(sheetRepository.findById(1L)).thenReturn(Optional.of(sheet))
        `when`(userSheetRepository.findBySheetIdAndIsOwner(1L, true)).thenReturn(null)

        assertFailsWith<SheetNotFoundException> {
            service.updateSheet(1L, request)
        }
    }

    // Tests para deleteSheet
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

    // Tests para addToFavorites
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
    fun should_throw_user_not_found_on_add_to_favorites() {
        `when`(userRepository.findById(999L)).thenReturn(Optional.empty())

        assertFailsWith<UserNotFoundException> {
            service.addToFavorites(999L, 1L)
        }
    }

    @Test
    fun should_throw_sheet_not_found_on_add_to_favorites() {
        val user = sampleUser()
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(sheetRepository.findById(999L)).thenReturn(Optional.empty())

        assertFailsWith<SheetNotFoundException> {
            service.addToFavorites(1L, 999L)
        }
    }

    // Tests para removeFromFavorites
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
    fun should_throw_sheet_not_found_on_remove_from_favorites() {
        `when`(userSheetRepository.findByUserIdAndSheetId(1L, 999L)).thenReturn(null)

        assertFailsWith<SheetNotFoundException> {
            service.removeFromFavorites(1L, 999L)
        }
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

    // Tests para getFavoriteSheets
    @Test
    fun should_get_favorite_sheets() {
        val user = sampleUser()
        val sheet1 = sampleSheet(1L, "Sheet 1")
        val sheet2 = sampleSheet(2L, "Sheet 2")
        val owner = sampleUser(2L, "owner@example.com")
        val userSheet1 = sampleUserSheet(user, sheet1, isFavorite = true)
        val userSheet2 = sampleUserSheet(user, sheet2, isFavorite = true)
        val ownerRelation = sampleUserSheet(owner, sheet1, isOwner = true)
        val ownerRelation2 = sampleUserSheet(owner, sheet2, isOwner = true)

        `when`(userRepository.existsById(1L)).thenReturn(true)
        `when`(userSheetRepository.findByUserIdAndIsFavorite(1L, true)).thenReturn(listOf(userSheet1, userSheet2))
        `when`(userSheetRepository.findBySheetIdAndIsOwner(1L, true)).thenReturn(ownerRelation)
        `when`(userSheetRepository.findBySheetIdAndIsOwner(2L, true)).thenReturn(ownerRelation2)

        val response = service.getFavoriteSheets(1L)

        assertEquals(2, response.size)
        assertEquals("Sheet 1", response[0].title)
        assertEquals("Sheet 2", response[1].title)
    }

    @Test
    fun should_get_empty_favorite_sheets_list() {
        `when`(userRepository.existsById(1L)).thenReturn(true)
        `when`(userSheetRepository.findByUserIdAndIsFavorite(1L, true)).thenReturn(emptyList())

        val response = service.getFavoriteSheets(1L)

        assertEquals(0, response.size)
    }

    @Test
    fun should_throw_user_not_found_on_get_favorite_sheets() {
        `when`(userRepository.existsById(999L)).thenReturn(false)

        assertFailsWith<UserNotFoundException> {
            service.getFavoriteSheets(999L)
        }
    }

    @Test
    fun should_throw_sheet_not_found_when_no_owner_found_on_get_favorites() {
        val user = sampleUser()
        val sheet = sampleSheet()
        val userSheet = sampleUserSheet(user, sheet, isFavorite = true)

        `when`(userRepository.existsById(1L)).thenReturn(true)
        `when`(userSheetRepository.findByUserIdAndIsFavorite(1L, true)).thenReturn(listOf(userSheet))
        `when`(userSheetRepository.findBySheetIdAndIsOwner(1L, true)).thenReturn(null)

        assertFailsWith<SheetNotFoundException> {
            service.getFavoriteSheets(1L)
        }
    }

    // Tests para isSheetFavorite
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

    @Test
    fun should_throw_user_not_found_on_is_sheet_favorite() {
        `when`(userRepository.existsById(999L)).thenReturn(false)

        assertFailsWith<UserNotFoundException> {
            service.isSheetFavorite(999L, 1L)
        }
    }

    @Test
    fun should_throw_sheet_not_found_on_is_sheet_favorite() {
        `when`(userRepository.existsById(1L)).thenReturn(true)
        `when`(sheetRepository.existsById(999L)).thenReturn(false)

        assertFailsWith<SheetNotFoundException> {
            service.isSheetFavorite(1L, 999L)
        }
    }

    @AfterEach
    fun descarga() {
        // limpieza si se requiere
    }
}