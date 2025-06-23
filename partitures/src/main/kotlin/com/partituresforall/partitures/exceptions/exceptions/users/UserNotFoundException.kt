package com.partituresforall.partitures.exceptions.exceptions.users

class UserNotFoundException(val userId: Long) :
    RuntimeException("Usuario con ID $userId no encontrado")