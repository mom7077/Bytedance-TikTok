package com.ttic.camera.ui.editor

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import coil.load
import com.ttic.camera.R
import com.ttic.camera.databinding.FragmentEditorBinding

class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!

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
            binding.tvEditorHint.setText(R.string.editor_no_image)
            return
        }
        val uri = Uri.parse(uriString)
        binding.ivEditor.load(uri) { crossfade(true) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
