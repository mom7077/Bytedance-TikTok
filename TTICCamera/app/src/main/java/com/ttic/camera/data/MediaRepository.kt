package com.ttic.camera.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val bucketId: Long?,
    val bucketName: String?
)

data class MediaFolder(
    val bucketId: Long,
    val name: String,
    val coverUri: Uri,
    val count: Int
)

class MediaRepository(private val context: Context) {

    suspend fun loadImages(): List<MediaItem> = withContext(Dispatchers.IO) {
        val images = mutableListOf<MediaItem>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val bucketId = if (!cursor.isNull(bucketIdCol)) cursor.getLong(bucketIdCol) else null
                val bucketName = if (!cursor.isNull(bucketNameCol)) cursor.getString(bucketNameCol) else null
                val contentUri = ContentUris.withAppendedId(collection, id)
                images.add(MediaItem(id, contentUri, bucketId, bucketName))
            }
        }
        images
    }

    fun foldersFrom(items: List<MediaItem>): List<MediaFolder> {
        return items
            .filter { it.bucketId != null }
            .groupBy { it.bucketId!! }
            .map { (bucketId, list) ->
                val name = list.firstOrNull()?.bucketName ?: "未命名"
                val cover = list.first().uri
                MediaFolder(bucketId, name, cover, list.size)
            }
            .sortedByDescending { it.count }
    }
}
