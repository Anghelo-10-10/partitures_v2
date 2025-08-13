package com.partituresforall.partitures.services

import com.partituresforall.partitures.config.PasswordEncoder
import com.partituresforall.partitures.exceptions.exceptions.users.DuplicateEmailException
import com.partituresforall.partitures.exceptions.exceptions.users.InvalidPasswordException
import com.partituresforall.partitures.exceptions.exceptions.users.UserNotFoundException
import com.partituresforall.partitures.exceptions.exceptions.users.InvalidCredentialsException
import com.partituresforall.partitures.models.entities.User
import com.partituresforall.partitures.models.requests.CreateUserRequest
import com.partituresforall.partitures.models.requests.UpdateUserRequest
import com.partituresforall.partitures.models.requests.LoginRequest
import com.partituresforall.partitures.models.requests.UpdateProfileRequest
import com.partituresforall.partitures.repositories.UserRepository
import org.junit.jupiter.api.*
import org.mockito.Mockito.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var service: UserService

    @BeforeEach
    fun carga() {
        userRepository = mock(UserRepository::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)
        service = UserService(userRepository, passwordEncoder)
    }

    private fun sampleUser(email: String = "test@example.com"): User {
        val user = User(
            name = "Test User",
            email = email,
            password = "hashed_password"
        )
        user.id = 1L
        return user
    }

    // ===== TESTS EXISTENTES CORREGIDOS =====

    @Test
    fun should_create_a_user() {
        // ✅ CORREGIDO: "password123" → "Password123" (con mayúscula)
        val request = CreateUserRequest("Test User", "test@example.com", "Password123")
        `when`(userRepository.findByEmail(request.email)).thenReturn(null)
        `when`(passwordEncoder.encode(request.password)).thenReturn("hashed_password")
        `when`(userRepository.save(any(User::class.java))).thenReturn(sampleUser())

        val response = service.createUser(request)

        assertEquals("Test User", response.name)
        assertEquals("test@example.com", response.email)
    }

    @Test
    fun should_throw_duplicate_email() {
        // ✅ CORREGIDO: "password123" → "Password123" (con mayúscula)
        val request = CreateUserRequest("User", "taken@example.com", "Password123")
        `when`(userRepository.findByEmail(request.email)).thenReturn(sampleUser(email = request.email))

        assertFailsWith<DuplicateEmailException> {
            service.createUser(request)
        }
    }

    @Test
    fun should_throw_invalid_password() {
        // ✅ Este test está correcto - usa "123" que debe fallar
        val request = CreateUserRequest("User", "user@example.com", "123")

        // Mock que el email no existe para llegar a la validación de password
        `when`(userRepository.findByEmail(request.email)).thenReturn(null)

        assertFailsWith<InvalidPasswordException> {
            service.createUser(request)
        }
    }

    @Test
    fun should_get_user_by_id() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser()))

        val response = service.getUserById(1L)

        assertEquals("Test User", response.name)
    }

    @Test
    fun should_throw_not_found_on_get() {
        `when`(userRepository.findById(2L)).thenReturn(Optional.empty())

        assertFailsWith<UserNotFoundException> {
            service.getUserById(2L)
        }
    }

    @Test
    fun should_update_user_name_and_password() {
        val user = sampleUser()
        // ✅ CORREGIDO: "newpassword123" → "NewPassword123" (con mayúscula)
        val request = UpdateUserRequest(name = "Updated", email = null, password = "NewPassword123")

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.encode("NewPassword123")).thenReturn("hashed_new_password")
        `when`(userRepository.save(any(User::class.java))).thenReturn(
            sampleUser().apply {
                name = "Updated"
                password = "hashed_new_password"
            }
        )

        val response = service.updateUser(1L, request)

        assertEquals("Updated", response.name)
    }

    @Test
    fun should_update_user_email_successfully() {
        val user = sampleUser()
        val request = UpdateUserRequest(name = null, email = "anghelo@gmail.com", password = null)

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(userRepository.findByEmail("anghelo@gmail.com")).thenReturn(null) // Email no existe
        `when`(userRepository.save(any(User::class.java))).thenReturn(
            sampleUser().apply {
                email = "anghelo@gmail.com"
            }
        )

        val response = service.updateUser(1L, request)

        assertEquals("anghelo@gmail.com", response.email)
    }

    @Test
    fun should_throw_duplicate_email_on_update() {
        val user = sampleUser()
        // ✅ CORREGIDO: "31547599" → "Password123" (era solo números, ahora válida)
        val request = UpdateUserRequest(name = "anghelo", email = "new@example.com", password = "Password123")
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(userRepository.findByEmail("new@example.com")).thenReturn(sampleUser(email = "new@example.com"))

        assertFailsWith<DuplicateEmailException> {
            service.updateUser(1L, request)
        }
    }

    @Test
    fun should_throw_invalid_password_on_update() {
        val user = sampleUser()
        // ✅ Este test está correcto - usa "123" que debe fallar
        val request = UpdateUserRequest(name = "12345678", email = "testing@gmail.com", password = "123")
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))

        assertFailsWith<InvalidPasswordException> {
            service.updateUser(1L, request)
        }
    }

    @Test
    fun should_throw_not_found_on_update() {
        // ✅ CORREGIDO: "12asd7890283" → "Password123" (sin mayúscula → con mayúscula)
        val request = UpdateUserRequest(name = "X", email = "ejemplo@puce.edu.ec", password = "Password123")
        `when`(userRepository.findById(1L)).thenReturn(Optional.empty())

        assertFailsWith<UserNotFoundException> {
            service.updateUser(1L, request)
        }
    }

    @Test
    fun should_delete_user() {
        val user = sampleUser()
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(userRepository.existsById(1L)).thenReturn(true)
        doNothing().`when`(userRepository).deleteById(1L)

        service.deleteUser(1L)

        verify(userRepository).existsById(1L)
        verify(userRepository).deleteById(1L)
    }

    @Test
    fun should_throw_not_found_on_delete() {
        `when`(userRepository.existsById(1L)).thenReturn(false)

        assertFailsWith<UserNotFoundException> {
            service.deleteUser(1L)
        }

        verify(userRepository).existsById(1L)
        // No verificamos deleteById porque no debería llamarse
    }

    // ===== TESTS NUEVOS PARA MÉTODOS NUEVOS =====

    @Test
    fun should_login_user_successfully() {
        // ✅ CORREGIDO: "password123" → "Password123" (con mayúscula)
        val request = LoginRequest("test@example.com", "Password123")
        val user = sampleUser()

        `when`(userRepository.findByEmail(request.email)).thenReturn(user)
        `when`(passwordEncoder.matches(request.password, user.password)).thenReturn(true)

        val response = service.loginUser(request)

        assertEquals("Test User", response.name)
        assertEquals("test@example.com", response.email)
    }

    @Test
    fun should_throw_invalid_credentials_when_user_not_found() {
        // ✅ CORREGIDO: "password123" → "Password123" (con mayúscula)
        val request = LoginRequest("notfound@example.com", "Password123")

        `when`(userRepository.findByEmail(request.email)).thenReturn(null)

        assertFailsWith<InvalidCredentialsException> {
            service.loginUser(request)
        }
    }

    @Test
    fun should_throw_invalid_credentials_when_password_wrong() {
        // ✅ CORREGIDO: "wrongpassword" → "WrongPassword1" (con mayúscula y número)
        val request = LoginRequest("test@example.com", "WrongPassword1")
        val user = sampleUser()

        `when`(userRepository.findByEmail(request.email)).thenReturn(user)
        `when`(passwordEncoder.matches(request.password, user.password)).thenReturn(false)

        assertFailsWith<InvalidCredentialsException> {
            service.loginUser(request)
        }
    }

    @Test
    fun should_get_user_profile() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser()))

        val response = service.getUserProfile(1L)

        assertEquals("Test User", response.name)
        assertEquals(1L, response.id)
    }

    @Test
    fun should_throw_not_found_on_get_profile() {
        `when`(userRepository.findById(999L)).thenReturn(Optional.empty())

        assertFailsWith<UserNotFoundException> {
            service.getUserProfile(999L)
        }
    }

    @Test
    fun should_update_profile() {
        val user = sampleUser()
        val request = UpdateProfileRequest(name = "Updated Name", bio = "Updated bio")

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(User::class.java))).thenReturn(
            user.apply {
                name = "Updated Name"
                bio = "Updated bio"
            }
        )

        val response = service.updateProfile(1L, request)

        assertEquals("Updated Name", response.name)
    }

    @Test
    fun should_throw_not_found_on_update_profile() {
        val request = UpdateProfileRequest(name = "Test", bio = "Test bio")

        `when`(userRepository.findById(999L)).thenReturn(Optional.empty())

        assertFailsWith<UserNotFoundException> {
            service.updateProfile(999L, request)
        }
    }

    // ===== 🆕 TESTS ADICIONALES PARA VALIDACIONES DE CONTRASEÑA =====

    @Test
    fun should_throw_invalid_password_when_no_uppercase() {
        // Test específico para validar que falla sin mayúscula
        val request = CreateUserRequest("User", "test@example.com", "password123") // Sin mayúscula
        `when`(userRepository.findByEmail(request.email)).thenReturn(null)

        val exception = assertFailsWith<InvalidPasswordException> {
            service.createUser(request)
        }
        assertEquals("La contraseña debe contener al menos una letra mayúscula", exception.message)
    }

    @Test
    fun should_throw_invalid_password_when_no_lowercase() {
        // Test específico para validar que falla sin minúscula
        val request = CreateUserRequest("User", "test@example.com", "PASSWORD123") // Sin minúscula
        `when`(userRepository.findByEmail(request.email)).thenReturn(null)

        val exception = assertFailsWith<InvalidPasswordException> {
            service.createUser(request)
        }
        assertEquals("La contraseña debe contener al menos una letra minúscula", exception.message)
    }

    @Test
    fun should_throw_invalid_password_when_no_digit() {
        // Test específico para validar que falla sin número
        val request = CreateUserRequest("User", "test@example.com", "Password") // Sin número
        `when`(userRepository.findByEmail(request.email)).thenReturn(null)

        val exception = assertFailsWith<InvalidPasswordException> {
            service.createUser(request)
        }
        assertEquals("La contraseña debe contener al menos un número", exception.message)
    }

    @Test
    fun should_throw_invalid_password_when_too_short() {
        // Test específico para validar que falla con menos de 8 caracteres
        val request = CreateUserRequest("User", "test@example.com", "Pass1") // Solo 5 caracteres
        `when`(userRepository.findByEmail(request.email)).thenReturn(null)

        val exception = assertFailsWith<InvalidPasswordException> {
            service.createUser(request)
        }
        assertEquals("La contraseña debe tener al menos 8 caracteres", exception.message)
    }

    @AfterEach
    fun descarga() {
        // limpieza si se requiere
    }
}