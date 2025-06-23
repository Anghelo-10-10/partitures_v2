package com.partituresforall.partitures.exceptions.exceptions.users

class InvalidPasswordException(
    message: String = "La contraseña debe tener al menos 8 caracteres"
) : RuntimeException(message)