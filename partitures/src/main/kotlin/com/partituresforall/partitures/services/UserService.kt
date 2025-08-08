package com.partituresforall.partitures.services

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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    // ===== NUEVO MÉTODO DE LOGIN =====
    fun loginUser(request: LoginRequest): UserResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException()
        }

        return user.toResponse()
    }

    // ===== MÉTODOS EXISTENTES (SIN CAMBIOS) =====

    fun createUser(request: CreateUserRequest): UserResponse {
        userRepository.findByEmail(request.email)?.let {
            throw DuplicateEmailException(request.email)
        }

        if (request.password.length < 8) {
            throw InvalidPasswordException()
        }

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

        request.password?.let {
            if (it.length < 8) throw InvalidPasswordException()
            user.password = passwordEncoder.encode(it)
        }

        return userRepository.save(user).toResponse()
    }

    fun deleteUser(id: Long) {
        if (!userRepository.existsById(id)) {
            throw UserNotFoundException(id)
        }
        userRepository.deleteById(id)
    }

    private fun User.toResponse() = UserResponse(
        id = this.id,
        name = this.name,
        email = this.email,
        bio = this.bio,
        profileImageUrl = this.profileImageUrl,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

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
        request.profileImageUrl?.let { user.profileImageUrl = it }

        return userRepository.save(user).toResponse()
    }

    private fun User.toProfileResponse() = UserProfileResponse(
        id = this.id,
        name = this.name,
        bio = this.bio,
        profileImageUrl = this.profileImageUrl,
        createdAt = this.createdAt
    )
}