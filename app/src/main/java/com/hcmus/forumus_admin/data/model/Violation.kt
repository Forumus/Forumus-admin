package com.hcmus.forumus_admin.data.model

data class Violation(
    val violation: String = "",  // e.g., "vio_001"
    val name: String = "",        // e.g., "Hate Speech"
    val description: String = ""  // Description of the violation
)
