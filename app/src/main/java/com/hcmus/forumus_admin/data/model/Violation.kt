package com.hcmus.forumus_admin.data.model

/**
 * Represents a violation type from Firebase violations collection
 */
data class Violation(
    val violation: String = "",  // e.g., "vio_001"
    val name: String = "",        // e.g., "Hate Speech"
    val description: String = ""  // Description of the violation
)
