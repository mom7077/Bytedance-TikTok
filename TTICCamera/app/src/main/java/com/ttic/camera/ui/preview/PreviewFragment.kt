package com.ttic.camera.ui.preview

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.ttic.camera.R
import com.ttic.camera.databinding.FragmentPreviewBinding

class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uriString = arguments?.getString("imageUri").orEmpty()
        if (uriString.isBlank()) {
            Toast.makeText(requireContext(), R.string.editor_no_image, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }
        val uri = Uri.parse(uriString)
        binding.ivPreview.load(uri) {
            crossfade(true)
        }
        binding.btnToEditor.setOnClickListener {
            val bundle = Bundle().apply {
                putString("imageUri", uriString)
            }
            findNavController().navigate(R.id.action_preview_to_editor, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
