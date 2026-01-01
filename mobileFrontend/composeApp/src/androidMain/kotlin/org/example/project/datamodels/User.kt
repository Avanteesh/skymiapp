package org.example.project.datamodels

import android.graphics.Bitmap
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val username: String,
    val profilepic: String
)
