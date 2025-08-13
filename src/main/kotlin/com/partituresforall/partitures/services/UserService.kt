package com.partituresforall.partitures.services

import com.partituresforall.partitures.config.PasswordEncoder  // ✅ NUESTRO PasswordEncoder (no Spring Security)
import com.partituresforall.partitures.exceptions.exceptions.users.DuplicateEmailException
import com.partituresforall.partitures.exceptions.exceptions.users.InvalidPasswordException
import com.partituresforall.partitures.exceptions.exceptions.users.UserNotFoundException
import com.partituresforall.partitures.exceptions.exceptions.users.InvalidCredentialsException
import com.partituresforall.partitures.models.entities.User
import com.partituresforall.partitures.models.requests.CreateUserRequest
import com.partituresforall.partitures.models.requests.LoginRequest
import com.partituresforall.partitures.models.requests.UpdateProfileRequest
import com.partituresforall.partitures.models.requests.UpdateUserRequest
import com.partituresforall.partitures.models.responses.UserProfileResponse
import com.partituresforall.partitures.models.responses.UserResponse
import com.partituresforall.partitures.repositories.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,  // ✅ NUESTRO PasswordEncoder independiente
) {

    // ===== VALIDACIÓN DE CONTRASEÑA ROBUSTA =====
    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw InvalidPasswordException("La contraseña debe tener al menos 8 caracteres")
        }

        if (!password.any { it.isDigit() }) {
            throw InvalidPasswordException("La contraseña debe contener al menos un número")
        }

        if (!password.any { it.isLowerCase() }) {
            throw InvalidPasswordException("La contraseña debe contener al menos una letra minúscula")
        }

        if (!password.any { it.isUpperCase() }) {
            throw InvalidPasswordException("La contraseña debe contener al menos una letra mayúscula")
        }
    }

    // ===== MÉTODO DE LOGIN =====
    fun loginUser(request: LoginRequest): UserResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException()
        }

        return user.toResponse()
    }

    // ===== MÉTODOS CRUD ACTUALIZADOS =====

    fun createUser(request: CreateUserRequest): UserResponse {
        userRepository.findByEmail(request.email)?.let {
            throw DuplicateEmailException(request.email)
        }

        // ✅ Validación robusta de contraseña (reemplaza la validación simple)
        validatePassword(request.password)

        val user = userRepository.save(
            User(
                name = request.name,
                email = request.email,
                password = passwordEncoder.encode(request.password)
            )
        )
        return user.toResponse()
    }

    fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id).orElseThrow {
            UserNotFoundException(id)
        }
        return user.toResponse()
    }

    fun updateUser(id: Long, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(id).orElseThrow {
            UserNotFoundException(id)
        }

        request.email?.let { newEmail ->
            if (newEmail != user.email) {
                userRepository.findByEmail(newEmail)?.let {
                    throw DuplicateEmailException(newEmail)
                }
                user.email = newEmail
            }
        }

        request.name?.let { user.name = it }

        request.password?.let { newPassword ->
            // ✅ Validación robusta de contraseña (reemplaza la validación simple)
            validatePassword(newPassword)
            user.password = passwordEncoder.encode(newPassword)
        }

        return userRepository.save(user).toResponse()
    }

    fun deleteUser(id: Long) {
        if (!userRepository.existsById(id)) {
            throw UserNotFoundException(id)
        }
        userRepository.deleteById(id)
    }

    // ===== MÉTODOS DE PERFIL =====

    fun getUserProfile(id: Long): UserProfileResponse {
        val user = userRepository.findById(id).orElseThrow {
            UserNotFoundException(id)
        }
        return user.toProfileResponse()
    }

    fun updateProfile(userId: Long, request: UpdateProfileRequest): UserResponse {
        val user = userRepository.findById(userId).orElseThrow {
            UserNotFoundException(userId)
        }

        request.name?.let { user.name = it }
        request.bio?.let { user.bio = it }

        return userRepository.save(user).toResponse()
    }

    // ===== MÉTODOS HELPER =====

    private fun User.toResponse() = UserResponse(
        id = this.id,
        name = this.name,
        email = this.email,
        bio = this.bio,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun User.toProfileResponse() = UserProfileResponse(
        id = this.id,
        name = this.name,
        bio = this.bio,
        createdAt = this.createdAt
    )
}