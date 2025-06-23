package com.partituresforall.partitures.exceptions.exceptions.sheets

class InvalidPdfReferenceException(val reference: String) :
    RuntimeException("La referencia PDF '$reference' no es válida")