package com.ttic.camera.ui.editor

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ttic.camera.edit.EditorOperation
import com.ttic.camera.edit.EditorProcessor
import com.ttic.camera.edit.EditorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EditorState())
    val uiState: StateFlow<EditorState> = _uiState.asStateFlow()

    private var baseBitmap: Bitmap? = null
    private var renderJob: Job? = null
    private val undoStack = ArrayDeque<List<EditorOperation>>()
    private val redoStack = ArrayDeque<List<EditorOperation>>()
    private var adjustSessionStarted = false

    fun setBaseBitmap(bitmap: Bitmap) {
        baseBitmap = bitmap
        _uiState.value = EditorState(
            operations = emptyList(),
            canUndo = false,
            canRedo = false,
            brightness = 0,
            contrast = 0
        )
        render()
    }

    fun updateAdjust(brightness: Int, contrast: Int) {
        val ops = _uiState.value.operations
        val filtered = ops.filterNot { it is EditorOperation.Adjust }
        val newOps = filtered + EditorOperation.Adjust(brightness, contrast)
        _uiState.update { it.copy(operations = newOps, brightness = brightness, contrast = contrast) }
        render()
    }

    fun beginAdjustSession() {
        if (!adjustSessionStarted) {
            undoStack.addLast(_uiState.value.operations.toList())
            adjustSessionStarted = true
        }
    }

    fun endAdjustSession() {
        adjustSessionStarted = false
    }

    fun applyOperation(op: EditorOperation, pushHistory: Boolean = true) {
        val currentOps = _uiState.value.operations
        if (pushHistory) {
            undoStack.addLast(currentOps.toList())
            redoStack.clear()
        }
        val newOps = currentOps + op
        _uiState.update {
            it.copy(
                operations = newOps,
                canUndo = undoStack.isNotEmpty() || newOps.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        render()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentOps = _uiState.value.operations
        redoStack.addLast(currentOps.toList())
        val previous = undoStack.removeLast()
        _uiState.update {
            it.copy(
                operations = previous,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
        }
        render()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentOps = _uiState.value.operations
        undoStack.addLast(currentOps.toList())
        val next = redoStack.removeLast()
        _uiState.update {
            it.copy(
                operations = next,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
        }
        render()
    }

    fun resetAdjust() {
        val ops = _uiState.value.operations.filterNot { it is EditorOperation.Adjust }
        _uiState.update { it.copy(operations = ops, brightness = 0, contrast = 0) }
        render()
    }

    suspend fun renderFinalBitmap(): Bitmap? {
        val base = baseBitmap ?: return null
        val operations = _uiState.value.operations
        return kotlinx.coroutines.withContext(Dispatchers.Default) {
            EditorProcessor.applyOperations(base, operations)
        }
    }

    private fun render() {
        val base = baseBitmap ?: return
        val operations = _uiState.value.operations
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            val bitmap = EditorProcessor.applyOperations(base, operations)
            val state = EditorState(
                operations = operations,
                canUndo = undoStack.isNotEmpty() || operations.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                brightness = _uiState.value.brightness,
                contrast = _uiState.value.contrast
            )
            _uiState.value = state
            _previewListener?.let { callback ->
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    callback(bitmap)
                }
            }
        }
    }

    private var _previewListener: ((Bitmap) -> Unit)? = null
    fun setOnPreviewRendered(listener: (Bitmap) -> Unit) {
        _previewListener = listener
    }

    fun clear() {
        renderJob?.cancel()
        baseBitmap?.recycle()
        baseBitmap = null
        undoStack.clear()
        redoStack.clear()
    }
}
