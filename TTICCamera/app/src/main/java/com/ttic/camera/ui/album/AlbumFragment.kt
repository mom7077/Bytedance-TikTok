package com.ttic.camera.ui.album

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.ttic.camera.R
import com.ttic.camera.data.MediaFolder
import com.ttic.camera.data.MediaItem
import com.ttic.camera.databinding.FragmentAlbumBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlbumFragment : Fragment() {

    private var _binding: FragmentAlbumBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AlbumViewModel by viewModels()

    private val allAdapter = PhotoGridAdapter { openPreview(it) }
    private val folderAdapter = PhotoGridAdapter { openPreview(it) }

    private var pendingCameraUri: Uri? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        val granted = grantResults.any { it.value }
        onMediaPermissionResult(granted)
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            Toast.makeText(requireContext(), R.string.album_camera_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let {
                openEditor(it)
                pendingCameraUri = null
            }
        } else {
            Toast.makeText(requireContext(), R.string.album_camera_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupRecyclerViews()
        setupClicks()
        observeState()
        viewModel.refresh(hasMediaPermission())
    }

    override fun onResume() {
        super.onResume()
        if (hasMediaPermission()) {
            viewModel.refresh(true)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.apply {
            addTab(newTab().setText(R.string.album_tab_all))
            addTab(newTab().setText(R.string.album_tab_folder))
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    switchTab(tab.position)
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {
                    switchTab(tab.position)
                }
            })
        }
        switchTab(0)
    }

    private fun setupRecyclerViews() {
        binding.rvAllPhotos.apply {
            setHasFixedSize(true)
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3)
            adapter = allAdapter
        }
        binding.rvFolderPhotos.apply {
            setHasFixedSize(true)
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3)
            adapter = folderAdapter
        }
    }

    private fun setupClicks() {
        binding.btnRequestPermission.setOnClickListener { requestMediaPermission() }
        binding.btnOpenSettings.setOnClickListener { openAppSettings() }
        binding.fabCamera.setOnClickListener { onCameraClicked() }
        binding.dropdownFolder.setOnItemClickListener { parent, _, position, _ ->
            val folderOption = parent.getItemAtPosition(position) as FolderOption
            viewModel.selectFolder(folderOption.bucketId)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.loading

                    binding.btnRequestPermission.isVisible = !state.permissionGranted
                    binding.btnOpenSettings.isVisible = !state.permissionGranted
                    binding.rvAllPhotos.isVisible = state.permissionGranted
                    binding.tvEmptyAll.isVisible = state.permissionGranted && !state.loading && state.isEmpty

                    allAdapter.submitList(state.items)

                    val folderOptions = state.folders.map { FolderOption(it.bucketId, it) }
                    binding.dropdownFolder.isEnabled = state.permissionGranted && folderOptions.isNotEmpty()
                    binding.rvFolderPhotos.isVisible = state.permissionGranted
                    binding.tvEmptyFolder.text = if (state.permissionGranted) {
                        getString(R.string.album_folder_empty)
                    } else {
                        getString(R.string.album_permission_denied)
                    }
                    binding.dropdownFolder.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            folderOptions
                        )
                    )

                    val selectedFolderId = state.selectedFolderId
                    val selectedIndex = folderOptions.indexOfFirst { it.bucketId == selectedFolderId }
                    if (selectedIndex >= 0) {
                        binding.dropdownFolder.setText(folderOptions[selectedIndex].toString(), false)
                    } else {
                        binding.dropdownFolder.setText("", false)
                    }

                    folderAdapter.submitList(state.selectedFolderItems)
                    binding.tvEmptyFolder.isVisible =
                        !state.permissionGranted || (!state.loading && state.selectedFolderItems.isEmpty())
                }
            }
        }
    }

    private fun switchTab(position: Int) {
        binding.groupAll.isVisible = position == 0
        binding.groupFolder.isVisible = position == 1
    }

    private fun onMediaPermissionResult(granted: Boolean) {
        viewModel.refresh(granted)
        if (!granted) {
            Toast.makeText(requireContext(), R.string.album_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasMediaPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMediaPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    private fun onCameraClicked() {
        if (!requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(requireContext(), R.string.album_no_camera, Toast.LENGTH_SHORT).show()
            return
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCameraCapture() {
        val uri = createImageUri() ?: return
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun createImageUri(): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cacheDir = File(requireContext().cacheDir, "images").apply { mkdirs() }
        val photoFile = File(cacheDir, "IMG_${timeStamp}.jpg")
        return runCatching {
            androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
        }.getOrNull()
    }

    private fun openPreview(item: MediaItem) {
        val bundle = android.os.Bundle().apply {
            putString("imageUri", item.uri.toString())
        }
        findNavController().navigate(R.id.action_album_to_preview, bundle)
    }

    private fun openEditor(uri: Uri) {
        val bundle = android.os.Bundle().apply {
            putString("imageUri", uri.toString())
        }
        findNavController().navigate(R.id.action_album_to_editor, bundle)
    }

    private fun openAppSettings() {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class FolderOption(val bucketId: Long, val folder: MediaFolder) {
        override fun toString(): String = "${folder.name} (${folder.count})"
    }
}
