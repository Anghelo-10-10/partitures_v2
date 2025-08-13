package com.partituresforall.partitures.exceptions.exceptions.sheets

class UnauthorizedSheetModificationException(val userId: Long, val sheetId: Long) :
    RuntimeException("Usuario $userId no tiene permisos para modificar la partitura $sheetId")