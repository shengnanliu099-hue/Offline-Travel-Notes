package com.travelnotes.offline.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.travelnotes.offline.data.ImageLayoutMode
import com.travelnotes.offline.data.Note
import com.travelnotes.offline.data.NoteRepository

sealed interface DraftImage {
    val id: String

    data class Existing(
        override val id: String,
        val path: String
    ) : DraftImage

    data class New(
        override val id: String,
        val uri: Uri
    ) : DraftImage
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NoteRepository(application.applicationContext)
    private var draftImageCounter: Long = 0
    var isComposerVisible by mutableStateOf(false)
        private set

    var titleInput by mutableStateOf("")
        private set

    var contentInput by mutableStateOf("")
        private set

    var searchInput by mutableStateOf("")
        private set

    var draftImages by mutableStateOf<List<DraftImage>>(emptyList())
        private set

    var selectedImageLayoutMode by mutableStateOf(ImageLayoutMode.AUTO)
        private set

    var isRecycleBinVisible by mutableStateOf(false)
        private set

    var editingNoteId by mutableStateOf<Long?>(null)
        private set

    var notes by mutableStateOf<List<Note>>(emptyList())
        private set

    val isEditing: Boolean
        get() = editingNoteId != null

    init {
        refreshNotes()
    }

    fun onTitleChange(value: String) {
        titleInput = value
    }

    fun onContentChange(value: String) {
        contentInput = value
    }

    fun onSearchChange(value: String) {
        searchInput = value
        refreshNotes()
    }

    fun startCreate() {
        if (isRecycleBinVisible) {
            isRecycleBinVisible = false
        }
        clearEditor()
        isComposerVisible = true
        refreshNotes()
    }

    fun addPickedImages(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val newItems = uris.map { uri ->
            DraftImage.New(
                id = nextDraftImageId("new"),
                uri = uri
            )
        }
        draftImages = draftImages + newItems
        normalizeSelectedLayoutMode()
    }

    fun removeDraftImage(image: DraftImage) {
        draftImages = draftImages.filterNot { it.id == image.id }
        normalizeSelectedLayoutMode()
    }

    fun moveDraftImage(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in draftImages.indices || toIndex !in draftImages.indices) return

        val current = draftImages.toMutableList()
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        draftImages = current
    }

    fun startEdit(note: Note) {
        editingNoteId = note.id
        titleInput = note.title
        contentInput = note.content
        draftImages = note.imagePaths.mapIndexed { index, path ->
            DraftImage.Existing(
                id = "existing-${note.id}-$index",
                path = path
            )
        }
        selectedImageLayoutMode = note.imageLayoutMode
        normalizeSelectedLayoutMode()
        isComposerVisible = true
    }

    fun onImageLayoutModeChange(mode: ImageLayoutMode) {
        selectedImageLayoutMode = mode
        normalizeSelectedLayoutMode()
    }

    fun cancelEdit() {
        clearEditor()
        isComposerVisible = false
    }

    fun saveNote() {
        val cleanTitle = titleInput.trim()
        val cleanContent = contentInput.trim()
        if (cleanTitle.isBlank() && cleanContent.isBlank()) return

        val keptPaths = draftImages
            .filterIsInstance<DraftImage.Existing>()
            .map { it.path }
        val newUris = draftImages
            .filterIsInstance<DraftImage.New>()
            .map { it.uri }

        val editingId = editingNoteId
        if (editingId == null) {
            repository.addNote(
                title = cleanTitle,
                content = cleanContent,
                imageUris = newUris,
                imageLayoutMode = selectedImageLayoutMode
            )
        } else {
            repository.updateNote(
                noteId = editingId,
                title = cleanTitle,
                content = cleanContent,
                keptImagePaths = keptPaths,
                newImageUris = newUris,
                imageLayoutMode = selectedImageLayoutMode
            )
        }

        clearEditor()
        isComposerVisible = false
        refreshNotes()
    }

    fun deleteNote(note: Note) {
        repository.deleteNote(note)
        if (editingNoteId == note.id) {
            clearEditor()
            isComposerVisible = false
        }
        refreshNotes()
    }

    fun restoreNote(note: Note) {
        repository.restoreNote(note)
        if (editingNoteId == note.id) {
            clearEditor()
            isComposerVisible = false
        }
        refreshNotes()
    }

    fun deleteNotePermanently(note: Note) {
        repository.deleteNotePermanently(note)
        if (editingNoteId == note.id) {
            clearEditor()
            isComposerVisible = false
        }
        refreshNotes()
    }

    fun showRecycleBin(visible: Boolean) {
        if (isRecycleBinVisible == visible) return
        isRecycleBinVisible = visible
        refreshNotes()
    }

    fun toggleRecycleBin() {
        isRecycleBinVisible = !isRecycleBinVisible
        refreshNotes()
    }

    private fun clearEditor() {
        editingNoteId = null
        titleInput = ""
        contentInput = ""
        draftImages = emptyList()
        selectedImageLayoutMode = ImageLayoutMode.AUTO
    }

    private fun refreshNotes() {
        notes = repository.getNotes(
            searchQuery = searchInput,
            inRecycleBin = isRecycleBinVisible
        )
    }

    private fun nextDraftImageId(prefix: String): String {
        draftImageCounter += 1
        return "$prefix-${System.currentTimeMillis()}-$draftImageCounter"
    }

    private fun normalizeSelectedLayoutMode() {
        val availableModes = ImageLayoutMode.availableForImageCount(draftImages.size)
        if (selectedImageLayoutMode !in availableModes) {
            selectedImageLayoutMode = ImageLayoutMode.AUTO
        }
    }

    companion object {
        const val MAX_IMAGE_PICKER_SELECTION = 99
    }
}
