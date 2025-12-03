package com.ttic.camera.ui.album

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ttic.camera.data.MediaFolder
import com.ttic.camera.data.MediaItem
import com.ttic.camera.data.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlbumUiState(
    val permissionGranted: Boolean = false,
    val loading: Boolean = false,
    val items: List<MediaItem> = emptyList(),
    val folders: List<MediaFolder> = emptyList(),
    val selectedFolderId: Long? = null
) {
    val selectedFolderItems: List<MediaItem>
        get() = if (selectedFolderId == null) emptyList() else items.filter { it.bucketId == selectedFolderId }

    val isEmpty: Boolean
        get() = items.isEmpty()
}

class AlbumViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    fun refresh(hasPermission: Boolean) {
        if (!hasPermission) {
            _uiState.value = AlbumUiState(permissionGranted = false, loading = false)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(permissionGranted = true, loading = true) }
            val items = runCatching { repository.loadImages() }.getOrElse { emptyList() }
            val folders = repository.foldersFrom(items)
            val selectedFolder = _uiState.value.selectedFolderId ?: folders.firstOrNull()?.bucketId
            _uiState.update {
                it.copy(
                    loading = false,
                    items = items,
                    folders = folders,
                    selectedFolderId = selectedFolder
                )
            }
        }
    }

    fun selectFolder(bucketId: Long?) {
        _uiState.update { it.copy(selectedFolderId = bucketId) }
    }
}
