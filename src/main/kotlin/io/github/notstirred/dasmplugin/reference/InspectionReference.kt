package io.github.notstirred.dasmplugin.reference

interface InspectionReference {
    val description: String
    val unresolved: Boolean
    val referenceType: ReferenceType
}

enum class ReferenceType {
    CLASS,
    FIELD,
    METHOD
}