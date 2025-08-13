package com.partituresforall.partitures.config

import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Component

@Component
class PasswordEncoder {

    fun encode(rawPassword: String): String {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt())
    }

    fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return try {
            BCrypt.checkpw(rawPassword, encodedPassword)
        } catch (e: Exception) {
            false
        }
    }
}