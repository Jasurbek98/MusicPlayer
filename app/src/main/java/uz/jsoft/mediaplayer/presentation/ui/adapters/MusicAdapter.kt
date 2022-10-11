package uz.jsoft.mediaplayer.presentation.ui.adapters

import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import uz.jsoft.mediaplayer.R
import uz.jsoft.mediaplayer.data.local.LocalStorage
import uz.jsoft.mediaplayer.databinding.MusicItemBinding
import uz.jsoft.mediaplayer.utils.custom.CursorAdapter
import uz.jsoft.mediaplayer.utils.extensions.loadImage
import uz.jsoft.mediaplayer.utils.extensions.toMusicData

/**
 * Created by Jasurbek Kurganbaev on 1/5/2022 5:40 PM
 **/
class MusicAdapter(val storage: LocalStorage) : CursorAdapter<MusicAdapter.MusicViewHolder>() {


    private var itemClickListener: OnItemClick? = null
    var lastSelected = 0

    inner class MusicViewHolder(private val binding: MusicItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val data = cursor.toMusicData()

                musicOrder.text = (cursor.position + 1).toString()
                musicName.text = data.title
                musicAuthor.text = data.artist

                root.setOnClickListener { itemClickListener?.onClick(position) }
                if (data.imageUri == null) {
                    image.loadImage(R.drawable.ic_music_icon)
                } else {
                    image.loadImage(data.imageUri!!)
                }

                if (storage.lastPlayedPosition == cursor.position) {
                    lastSelected = cursor.position
                    musicName.setTextColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.yellow
                        )
                    )
                    musicAuthor.setTextColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.dark_yellow
                        )
                    )
                } else {
                    musicName.setTextColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.black
                        )
                    )
                    musicAuthor.setTextColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.black
                        )
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        return MusicViewHolder(
            MusicItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    fun setOnItemClickListener(block: OnItemClick) {
        this.itemClickListener = block
    }

    fun interface OnItemClick {
        fun onClick(itemPosition: Int)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, cursor: Cursor, position: Int) {
        holder.bind(position)
    }

}