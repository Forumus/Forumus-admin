package com.hcmus.forumus_admin.data.model

data class ViolationDescription(
    val description: String = "",
    val id: String = "",
    val name: String = ""
)

data class Report(
    val id: String = "",
    val authorId: String = "",
    val descriptionViolation: ViolationDescription = ViolationDescription(),
    val nameViolation: String = "",
    val postId: String = ""
)
