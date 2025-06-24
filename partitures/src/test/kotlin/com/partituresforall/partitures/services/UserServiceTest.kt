package com.partituresforall.partitures.services
/*terminado*/
import com.partituresforall.partitures.exceptions.exceptions.users.DuplicateEmailException
import com.partituresforall.partitures.exceptions.exceptions.users.InvalidPasswordException
import com.partituresforall.partitures.exceptions.exceptions.users.UserNotFoundException
import com.partituresforall.partitures.models.entities.User
import com.partituresforall.partitures.models.requests.CreateUserRequest
import com.partituresforall.partitures.models.requests.UpdateUserRequest
import com.partituresforall.partitures.repositories.UserRepository
import org.junit.jupiter.api.*
import org.mockito.Mockito.*
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
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

    @Test
    fun should_create_a_user() {
        val request = CreateUserRequest("Test User", "test@example.com", "password123")
        `when`(userRepository.findByEmail(request.email)).thenReturn(null)
        `when`(passwordEncoder.encode(request.password)).thenReturn("hashed_password")
        `when`(userRepository.save(any(User::class.java))).thenReturn(sampleUser())

        val response = service.createUser(request)

        assertEquals("Test User", response.name)
        assertEquals("test@example.com", response.email)
    }

    @Test
    fun should_throw_duplicate_email() {
        val request = CreateUserRequest("User", "taken@example.com", "password123")
        `when`(userRepository.findByEmail(request.email)).thenReturn(sampleUser(email = request.email))

        assertFailsWith<DuplicateEmailException> {
            service.createUser(request)
        }
    }

    @Test
    fun should_throw_invalid_password() {
        val request = CreateUserRequest("User", "user@example.com", "123")

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
        val request = UpdateUserRequest(email = "anghelo@gmail.com",name = "Updated", password = "newpassword123")

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.encode("newpassword123")).thenReturn("hashed_new_password")
        `when`(userRepository.save(any(User::class.java))).thenReturn(
            user.copy(name = "Updated", password = "hashed_new_password")
        )

        val response = service.updateUser(1L, request)

        assertEquals("Updated", response.name)
    }

    @Test
    fun should_throw_duplicate_email_on_update() {
        val user = sampleUser()
        val request = UpdateUserRequest(email = "new@example.com", password = "31547599", name = "anghelo")
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(userRepository.findByEmail("new@example.com")).thenReturn(sampleUser(email = "new@example.com"))

        assertFailsWith<DuplicateEmailException> {
            service.updateUser(1L, request)
        }
    }

    @Test
    fun should_throw_invalid_password_on_update() {
        val user = sampleUser()
        val request = UpdateUserRequest(email = "testing@gmail.com",password = "123", name = "12345678")
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))

        assertFailsWith<InvalidPasswordException> {
            service.updateUser(1L, request)
        }
    }

    @Test
    fun should_throw_not_found_on_update() {
        val request = UpdateUserRequest(email = "ejemplo@puce.edu.ec", password = "12asd7890283", name = "X")
        `when`(userRepository.findById(1L)).thenReturn(Optional.empty())

        assertFailsWith<UserNotFoundException> {
            service.updateUser(1L, request)
        }
    }

    @Test
    fun should_delete_user() {
        `when`(userRepository.existsById(1L)).thenReturn(true)
        doNothing().`when`(userRepository).deleteById(1L)

        service.deleteUser(1L)

        verify(userRepository).deleteById(1L)
    }

    @Test
    fun should_throw_not_found_on_delete() {
        `when`(userRepository.existsById(1L)).thenReturn(false)

        assertFailsWith<UserNotFoundException> {
            service.deleteUser(1L)
        }
    }

    @AfterEach
    fun descarga() {
        // limpieza si se requiere
    }
}
