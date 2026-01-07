package org.example.project.datamodels

import kotlinx.serialization.Serializable

@Serializable
data class AuthToken(
    val token: String
)
