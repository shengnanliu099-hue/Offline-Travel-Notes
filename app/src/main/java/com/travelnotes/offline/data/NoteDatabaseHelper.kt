package com.travelnotes.offline.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NoteDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_CONTENT TEXT NOT NULL,
                $COL_TAGS TEXT NOT NULL,
                $COL_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTE_IMAGES (
                $COL_IMAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_IMAGE_NOTE_ID INTEGER NOT NULL,
                $COL_IMAGE_PATH TEXT NOT NULL,
                $COL_IMAGE_SORT_ORDER INTEGER NOT NULL,
                FOREIGN KEY($COL_IMAGE_NOTE_ID) REFERENCES $TABLE_NOTES($COL_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX $IDX_NOTE_IMAGES_NOTE_ID ON $TABLE_NOTE_IMAGES($COL_IMAGE_NOTE_ID)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTE_IMAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        onCreate(db)
    }

    fun insertNote(title: String, content: String, tagsCsv: String, updatedAt: Long): Long {
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_CONTENT, content)
            put(COL_TAGS, tagsCsv)
            put(COL_UPDATED_AT, updatedAt)
        }
        return writableDatabase.insert(TABLE_NOTES, null, values)
    }

    fun updateNote(noteId: Long, title: String, content: String, tagsCsv: String, updatedAt: Long) {
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_CONTENT, content)
            put(COL_TAGS, tagsCsv)
            put(COL_UPDATED_AT, updatedAt)
        }
        writableDatabase.update(
            TABLE_NOTES,
            values,
            "$COL_ID = ?",
            arrayOf(noteId.toString())
        )
    }

    fun replaceNoteImages(noteId: Long, imagePaths: List<String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_NOTE_IMAGES, "$COL_IMAGE_NOTE_ID = ?", arrayOf(noteId.toString()))
            imagePaths.forEachIndexed { index, path ->
                val values = ContentValues().apply {
                    put(COL_IMAGE_NOTE_ID, noteId)
                    put(COL_IMAGE_PATH, path)
                    put(COL_IMAGE_SORT_ORDER, index)
                }
                db.insert(TABLE_NOTE_IMAGES, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadNoteImagePaths(noteId: Long): List<String> {
        val imagePaths = mutableListOf<String>()
        val cursor = readableDatabase.query(
            TABLE_NOTE_IMAGES,
            arrayOf(COL_IMAGE_PATH),
            "$COL_IMAGE_NOTE_ID = ?",
            arrayOf(noteId.toString()),
            null,
            null,
            "$COL_IMAGE_SORT_ORDER ASC"
        )

        cursor.use {
            val pathIndex = it.getColumnIndexOrThrow(COL_IMAGE_PATH)
            while (it.moveToNext()) {
                imagePaths += it.getString(pathIndex)
            }
        }
        return imagePaths
    }

    fun deleteNote(noteId: Long) {
        writableDatabase.delete(
            TABLE_NOTES,
            "$COL_ID = ?",
            arrayOf(noteId.toString())
        )
    }

    fun loadAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val cursor = readableDatabase.query(
            TABLE_NOTES,
            null,
            null,
            null,
            null,
            null,
            "$COL_UPDATED_AT DESC"
        )

        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(COL_ID)
            val titleIndex = it.getColumnIndexOrThrow(COL_TITLE)
            val contentIndex = it.getColumnIndexOrThrow(COL_CONTENT)
            val tagsIndex = it.getColumnIndexOrThrow(COL_TAGS)
            val updatedAtIndex = it.getColumnIndexOrThrow(COL_UPDATED_AT)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val rawTags = it.getString(tagsIndex)
                val tags = rawTags
                    .split(',')
                    .map { tag -> tag.trim() }
                    .filter { tag -> tag.isNotBlank() }

                notes += Note(
                    id = id,
                    title = it.getString(titleIndex),
                    content = it.getString(contentIndex),
                    imagePaths = loadNoteImagePaths(id),
                    tags = tags,
                    updatedAt = it.getLong(updatedAtIndex)
                )
            }
        }
        return notes
    }

    companion object {
        private const val DATABASE_NAME = "notes.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_NOTES = "notes"
        private const val TABLE_NOTE_IMAGES = "note_images"

        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_CONTENT = "content"
        private const val COL_TAGS = "tags"
        private const val COL_UPDATED_AT = "updated_at"

        private const val COL_IMAGE_ID = "id"
        private const val COL_IMAGE_NOTE_ID = "note_id"
        private const val COL_IMAGE_PATH = "image_path"
        private const val COL_IMAGE_SORT_ORDER = "sort_order"

        private const val IDX_NOTE_IMAGES_NOTE_ID = "idx_note_images_note_id"
    }
}
