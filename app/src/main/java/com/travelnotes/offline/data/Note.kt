package com.travelnotes.offline.data

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val imagePaths: List<String>,
    val imageLayoutMode: ImageLayoutMode,
    val tags: List<String>,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
