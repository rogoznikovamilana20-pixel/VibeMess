package com.vibe.common.model

/**
 * Basic Data Transfer Object for a user in the Vibe system.
 */
data class UserDto(
    val id: Long,
    val username: String?,
    val firstName: String,
    val lastName: String?,
    val photoUrl: String?
)
