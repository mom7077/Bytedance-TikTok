package com.ttic.camera.edit

import com.ttic.camera.ui.widgets.OverlayText

sealed class EditorOperation {
    data class Adjust(val brightness: Int, val contrast: Int) : EditorOperation()
    data class Rotate(val degrees: Int) : EditorOperation()
    data class Flip(val horizontal: Boolean) : EditorOperation()
    data class Crop(val ratioWidth: Int, val ratioHeight: Int) : EditorOperation()
    data class CropRect(val left: Int, val top: Int, val right: Int, val bottom: Int) : EditorOperation()
    data class Text(val overlay: OverlayText) : EditorOperation()
}

data class EditorState(
    val operations: List<EditorOperation> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val brightness: Int = 0,
    val contrast: Int = 0
)
