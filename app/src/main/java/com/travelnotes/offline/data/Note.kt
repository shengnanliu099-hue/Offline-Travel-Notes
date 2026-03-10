package com.travelnotes.offline.data

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val imagePaths: List<String>,
    val tags: List<String>,
    val updatedAt: Long
)
