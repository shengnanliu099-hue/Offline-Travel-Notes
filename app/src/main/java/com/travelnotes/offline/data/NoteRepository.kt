package com.travelnotes.offline.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.Locale
import java.util.UUID

class NoteRepository(private val context: Context) {
    private val dbHelper = NoteDatabaseHelper(context)

    fun addNote(
        title: String,
        content: String,
        imageUris: List<Uri>,
        imageLayoutMode: ImageLayoutMode
    ) {
        val tags = extractTags("$title\n$content")
        val noteId = dbHelper.insertNote(
            title = title,
            content = content,
            tagsCsv = tags.joinToString(","),
            imageLayoutMode = imageLayoutMode,
            updatedAt = System.currentTimeMillis()
        )

        val copiedImagePaths = copyImagesToAppStorage(imageUris)
        dbHelper.replaceNoteImages(noteId, copiedImagePaths)
    }

    fun updateNote(
        noteId: Long,
        title: String,
        content: String,
        keptImagePaths: List<String>,
        newImageUris: List<Uri>,
        imageLayoutMode: ImageLayoutMode
    ) {
        val tags = extractTags("$title\n$content")
        val oldImagePaths = dbHelper.loadNoteImagePaths(noteId)
        val copiedNewImages = copyImagesToAppStorage(newImageUris)
        val finalImagePaths = keptImagePaths + copiedNewImages

        dbHelper.updateNote(
            noteId = noteId,
            title = title,
            content = content,
            tagsCsv = tags.joinToString(","),
            imageLayoutMode = imageLayoutMode,
            updatedAt = System.currentTimeMillis()
        )
        dbHelper.replaceNoteImages(noteId, finalImagePaths)

        val removedPaths = oldImagePaths.filterNot { old -> keptImagePaths.contains(old) }
        deleteFiles(removedPaths)
    }

    fun deleteNote(note: Note) {
        dbHelper.markNoteDeleted(note.id, System.currentTimeMillis())
    }

    fun restoreNote(note: Note) {
        dbHelper.restoreNote(note.id, System.currentTimeMillis())
    }

    fun deleteNotePermanently(note: Note) {
        dbHelper.deleteNote(note.id)
        deleteFiles(note.imagePaths)
    }

    fun getNotes(searchQuery: String, inRecycleBin: Boolean): List<Note> {
        val allNotes = dbHelper.loadAllNotes(onlyDeleted = inRecycleBin)
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        if (query.isBlank()) return allNotes

        return if (query.startsWith("#")) {
            val wanted = query.removePrefix("#")
            if (wanted.isBlank()) return allNotes
            allNotes.filter { note -> note.tags.any { it.lowercase(Locale.ROOT) == wanted } }
        } else {
            allNotes.filter { note ->
                note.title.lowercase(Locale.ROOT).contains(query) ||
                    note.content.lowercase(Locale.ROOT).contains(query) ||
                    note.tags.any { it.lowercase(Locale.ROOT).contains(query) }
            }
        }
    }

    private fun extractTags(text: String): List<String> {
        val regex = Regex("#([\\p{L}0-9_-]+)")
        return regex.findAll(text)
            .map { match -> match.groupValues[1].lowercase(Locale.ROOT) }
            .distinct()
            .toList()
    }

    private fun copyImagesToAppStorage(uris: List<Uri>): List<String> {
        return uris.mapNotNull(::copyImageToAppStorage)
    }

    private fun copyImageToAppStorage(uri: Uri): String? {
        val noteImageDir = File(context.filesDir, IMAGE_FOLDER)
        if (!noteImageDir.exists()) {
            noteImageDir.mkdirs()
        }

        val outFile = File(noteImageDir, "${UUID.randomUUID()}.jpg")

        return try {
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) return null
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun deleteFiles(paths: List<String>) {
        paths.forEach { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    companion object {
        private const val IMAGE_FOLDER = "note_images"
    }
}
