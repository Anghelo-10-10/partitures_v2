package com.partituresforall.partitures.exceptions.exceptions.users

class DuplicateEmailException(val email: String) :
    RuntimeException("El email $email ya está registrado")