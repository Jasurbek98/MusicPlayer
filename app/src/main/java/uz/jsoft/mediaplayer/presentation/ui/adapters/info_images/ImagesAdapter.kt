package uz.jsoft.mediaplayer.presentation.ui.adapters.info_images

import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import uz.jsoft.mediaplayer.R
import uz.jsoft.mediaplayer.data.local.LocalStorage
import uz.jsoft.mediaplayer.databinding.ItemInfoImageBinding
import uz.jsoft.mediaplayer.utils.Constants
import uz.jsoft.mediaplayer.utils.Constants.Companion.FOR_IMAGE_LIST
import uz.jsoft.mediaplayer.utils.custom.CursorAdapter
import uz.jsoft.mediaplayer.utils.extensions.ALBUM_ID
import uz.jsoft.mediaplayer.utils.extensions.loadImage
import uz.jsoft.mediaplayer.utils.extensions.songArt

class ImagesAdapter(val storage: LocalStorage, val onLoadingFinished: () -> Unit = {}) :
    CursorAdapter<ImagesAdapter.ImageViewHolder>() {
    inner class ImageViewHolder(private val binding: ItemInfoImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
            }
        }

        fun bind() {
            binding.apply {
                val imgData = binding.root.context.songArt(cursor.getLong(ALBUM_ID))
                if (imgData == null) {
                    image.setImageResource(R.drawable.ic_music_icon)
                    onLoadingFinished.invoke()
                } else {
                    image.loadImage(imgData) {
                        onLoadingFinished.invoke()
                    }
                }
                ViewCompat.setTransitionName(image, FOR_IMAGE_LIST + cursor.position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(
            ItemInfoImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ImageViewHolder, cursor: Cursor, position: Int) {
        holder.bind()
    }
}