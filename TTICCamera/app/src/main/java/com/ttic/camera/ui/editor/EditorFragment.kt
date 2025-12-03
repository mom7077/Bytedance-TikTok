package com.ttic.camera.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ttic.camera.R
import com.ttic.camera.databinding.FragmentEditorBinding
import com.ttic.camera.edit.EditorOperation
import com.ttic.camera.ui.widgets.OverlayText
import kotlinx.coroutines.launch

class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditorViewModel by viewModels()
    private var currentPreview: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uriString = arguments?.getString("imageUri").orEmpty()
        if (uriString.isBlank()) {
            Toast.makeText(requireContext(), R.string.editor_no_image, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse(uriString)
        loadBitmap(uri)?.let { bitmap ->
            viewModel.setBaseBitmap(bitmap)
            binding.ivEditor.setImageBitmap(bitmap)
        } ?: run {
            Toast.makeText(requireContext(), R.string.editor_no_image, Toast.LENGTH_SHORT).show()
        }

        bindPreviewListener()
        setupToolTabs()
        setupAdjustControls()
        setupRotateControls()
        setupCropControls()
        setupTextControls()
        setupUndoRedo()
        observeState()

        binding.btnSavePreview.setOnClickListener {
            Toast.makeText(requireContext(), R.string.btn_apply_changes, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindPreviewListener() {
        viewModel.setOnPreviewRendered { bitmap ->
            currentPreview?.takeIf { it != bitmap && !it.isRecycled }?.recycle()
            currentPreview = bitmap
            binding.ivEditor.setImageBitmap(bitmap)
        }
    }

    private fun setupToolTabs() {
        binding.groupTools.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.panelAdjust.isVisible = checkedId == binding.btnToolAdjust.id
            binding.panelRotate.isVisible = checkedId == binding.btnToolRotate.id
            binding.panelCrop.isVisible = checkedId == binding.btnToolCrop.id
            binding.panelText.isVisible = checkedId == binding.btnToolText.id
        }
        binding.groupTools.check(binding.btnToolAdjust.id)
    }

    private fun setupAdjustControls() {
        binding.seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val brightness = progress - 100
                val contrast = binding.seekContrast.progress - 50
                viewModel.updateAdjust(brightness, contrast)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.seekContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val brightness = binding.seekBrightness.progress - 100
                val contrast = progress - 50
                viewModel.updateAdjust(brightness, contrast)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.btnResetAdjust.setOnClickListener {
            binding.seekBrightness.progress = 100
            binding.seekContrast.progress = 50
            viewModel.resetAdjust()
        }
    }

    private fun setupRotateControls() {
        binding.btnRotateLeft.setOnClickListener {
            viewModel.applyOperation(EditorOperation.Rotate(-90))
        }
        binding.btnRotateRight.setOnClickListener {
            viewModel.applyOperation(EditorOperation.Rotate(90))
        }
        binding.btnRotate180.setOnClickListener {
            viewModel.applyOperation(EditorOperation.Rotate(180))
        }
        binding.btnFlipH.setOnClickListener {
            viewModel.applyOperation(EditorOperation.Flip(horizontal = true))
        }
        binding.btnFlipV.setOnClickListener {
            viewModel.applyOperation(EditorOperation.Flip(horizontal = false))
        }
    }

    private fun setupCropControls() {
        binding.btnApplyCrop.setOnClickListener {
            val ratio = when {
                binding.chipCrop1to1.isChecked -> 1 to 1
                binding.chipCrop4to3.isChecked -> 4 to 3
                binding.chipCrop16to9.isChecked -> 16 to 9
                binding.chipCrop3to4.isChecked -> 3 to 4
                binding.chipCrop9to16.isChecked -> 9 to 16
                else -> null
            }
            ratio?.let {
                viewModel.applyOperation(EditorOperation.Crop(it.first, it.second))
            }
        }
    }

    private fun setupTextControls() {
        val fonts = listOf(
            Typeface.SANS_SERIF to "Sans",
            Typeface.SERIF to "Serif",
            Typeface.MONOSPACE to "Mono"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, fonts.map { it.second })
        binding.dropdownFont.setAdapter(adapter)
        binding.dropdownFont.setText(fonts.first().second, false)
        var selectedTypeface = fonts.first().first
        binding.dropdownFont.setOnItemClickListener { parent, _, position, _ ->
            selectedTypeface = fonts.getOrNull(position)?.first ?: fonts.first().first
        }

        binding.btnAddText.setOnClickListener {
            val text = binding.inputText.text?.toString().orEmpty()
            if (text.isBlank()) {
                Toast.makeText(requireContext(), R.string.text_hint, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val size = binding.inputSize.text?.toString()?.toIntOrNull()?.coerceIn(12, 36) ?: 24
            val typeface = selectedTypeface
            val color = selectedColor()
            val alpha = maxOf(50, binding.seekTextAlpha.progress)
            val rotation = binding.seekTextRotation.progress.toFloat()
            val overlay = OverlayText(
                text = text,
                sizeSp = size.toFloat(),
                color = color,
                alpha = alpha,
                rotation = rotation,
                typeface = typeface,
                centerX = (binding.textOverlay.width.takeIf { it > 0 } ?: binding.canvasContainer.width / 2).toFloat(),
                centerY = (binding.textOverlay.height.takeIf { it > 0 } ?: binding.canvasContainer.height / 2).toFloat()
            )
            binding.textOverlay.setOverlay(overlay)
        }

        binding.btnApplyText.setOnClickListener {
            val overlay = binding.textOverlay.currentOverlay() ?: return@setOnClickListener
            viewModel.applyOperation(EditorOperation.Text(overlay))
            binding.textOverlay.clearOverlay()
        }

        binding.seekTextAlpha.setOnSeekBarChangeListener(simpleSeek { progress ->
            val overlay = binding.textOverlay.currentOverlay() ?: return@simpleSeek
            overlay.alpha = maxOf(50, progress)
            binding.textOverlay.invalidate()
        })
        binding.seekTextRotation.setOnSeekBarChangeListener(simpleSeek { progress ->
            binding.textOverlay.updateRotation(progress.toFloat())
        })
    }

    private fun selectedColor(): Int {
        return when {
            binding.chipColorWhite.isChecked -> Color.WHITE
            binding.chipColorBlack.isChecked -> Color.BLACK
            binding.chipColorRed.isChecked -> Color.RED
            binding.chipColorBlue.isChecked -> Color.BLUE
            binding.chipColorGreen.isChecked -> Color.GREEN
            else -> Color.WHITE
        }
    }

    private fun setupUndoRedo() {
        binding.btnUndo.setOnClickListener { viewModel.undo() }
        binding.btnRedo.setOnClickListener { viewModel.redo() }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnUndo.isEnabled = state.canUndo
                    binding.btnRedo.isEnabled = state.canRedo
                }
            }
        }
    }

    private fun simpleSeek(onChange: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onChange(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            val resolver = requireContext().contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val sample = calculateInSampleSize(bounds, 2048, 2048)
            val decodeOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = sample
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clear()
        currentPreview?.takeIf { !it.isRecycled }?.recycle()
        currentPreview = null
        _binding = null
    }
}
