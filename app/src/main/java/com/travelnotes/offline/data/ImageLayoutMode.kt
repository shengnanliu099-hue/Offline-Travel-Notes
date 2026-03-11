package com.travelnotes.offline.data

enum class ImageLayoutMode(val storageValue: String) {
    AUTO("auto"),
    SMALL("small"),
    TWO_COLUMNS("two_columns"),
    LARGE_FIRST("large_first"),
    LARGE_LAST("large_last");

    companion object {
        fun fromStorage(value: String?): ImageLayoutMode {
            return entries.firstOrNull { it.storageValue == value } ?: AUTO
        }

        fun availableForImageCount(imageCount: Int): List<ImageLayoutMode> {
            if (imageCount <= 0) return listOf(AUTO)
            if (imageCount == 1) {
                return listOf(
                    AUTO,
                    SMALL,
                    LARGE_FIRST
                )
            }
            return listOf(
                AUTO,
                SMALL,
                TWO_COLUMNS,
                LARGE_FIRST,
                LARGE_LAST
            )
        }
    }
}
