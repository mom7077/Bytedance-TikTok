package com.ttic.camera.ui.album

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ttic.camera.data.MediaItem
import com.ttic.camera.databinding.ItemPhotoBinding

class PhotoGridAdapter(
    private val onClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, PhotoGridAdapter.PhotoViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MediaItem) {
            binding.ivThumb.load(item.uri) {
                crossfade(true)
            }
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
