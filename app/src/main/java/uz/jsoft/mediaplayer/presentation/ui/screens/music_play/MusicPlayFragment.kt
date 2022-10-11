package uz.jsoft.mediaplayer.presentation.ui.screens.music_play

import android.annotation.SuppressLint
import android.app.SharedElementCallback
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uz.jsoft.mediaplayer.R
import uz.jsoft.mediaplayer.data.local.LocalStorage
import uz.jsoft.mediaplayer.data.model.Music
import uz.jsoft.mediaplayer.data.model.enums.MusicState
import uz.jsoft.mediaplayer.data.model.enums.ServiceCommand
import uz.jsoft.mediaplayer.databinding.FragmentMusicListBinding
import uz.jsoft.mediaplayer.databinding.FragmentMusicPlayBinding
import uz.jsoft.mediaplayer.playback.EventBus
import uz.jsoft.mediaplayer.playback.MusicService
import uz.jsoft.mediaplayer.presentation.ui.adapters.info_images.ImagesAdapter
import uz.jsoft.mediaplayer.utils.Constants.Companion.COMMAND_DATA
import uz.jsoft.mediaplayer.utils.custom.SlowdownRecyclerView
import uz.jsoft.mediaplayer.utils.custom.SnapOnScrollListener
import uz.jsoft.mediaplayer.utils.extensions.getPlayListCursor
import uz.jsoft.mediaplayer.utils.extensions.hideAppBar
import uz.jsoft.mediaplayer.utils.extensions.toFormattedString
import uz.jsoft.mediaplayer.utils.extensions.toMusicData
import uz.jsoft.mediaplayer.utils.helper.checkPermissions
import uz.jsoft.mediaplayer.utils.helper.timberErrorLog
import javax.inject.Inject

@AndroidEntryPoint
class MusicPlayFragment : Fragment(R.layout.fragment_music_play) {


/*    private val binding: FragmentMusicPlayBinding by viewBinding(FragmentMusicPlayBinding::bind)

    @Inject
    lateinit var storage: LocalStorage

    private val adapter by lazy {
        ImagesAdapter(storage) {
            startPostponedEnterTransition()
        }
    }

    private val snapHelper = LinearSnapHelper()*/

    private val binding: FragmentMusicPlayBinding by viewBinding(FragmentMusicPlayBinding::bind)

    @Inject
    lateinit var storage: LocalStorage

    private val adapter by lazy {
        ImagesAdapter(storage) {
            startPostponedEnterTransition()
        }
    }
    private val snapHelper = LinearSnapHelper()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.shared_image)

        setEnterSharedElementCallback(object : androidx.core.app.SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>,
                sharedElements: MutableMap<String, View>
            ) {
                try {
                    // Locate the ViewHolder for the clicked position.
                    val selectedViewHolder: RecyclerView.ViewHolder = binding.list
                        .findViewHolderForAdapterPosition(storage.lastPlayedPosition) ?: return

                    val image = selectedViewHolder.itemView.findViewById<View>(R.id.image)

                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] = image
                } catch (e: Exception) {
                    timberErrorLog(e.message.toString())
                }
            }
        })
        return /*binding.root*/ super.onCreateView(inflater, container, savedInstanceState)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        hideAppBar()

        loadViews()
        loadData()
        loadObservers()

    }


    @SuppressLint("FragmentLiveDataObserve")
    private fun loadObservers() {
        EventBus.musicStateLiveData.observe(this) {
            binding.apply {
                when (it) {
                    is MusicState.PAUSE -> {
                        currentMusicPlay.setImageResource(R.drawable.ic_pause_white)
                    }
                    is MusicState.PLAYING -> {
                        currentMusicPlay.setImageResource(R.drawable.ic_play_white)
                        it.data?.let { it1 -> loadPlayingData(it1) }
                    }
                    is MusicState.STOP -> {
                        currentMusicPlay.setImageResource(R.drawable.ic_pause_white)
                        seekbar.progress = 0
                    }
                    is MusicState.NEXT_OR_PREV -> {
                        currentMusicPlay.setImageResource(R.drawable.ic_play_white)
                        it.data?.let { it1 -> loadPlayingData(it1) }
                        list.smoothScrollToPosition(it.position)
                    }
                }
            }
        }
        EventBus.currentValueLiveData.observe(viewLifecycleOwner) {
            binding.apply {
                seekbar.progress = it.toInt()
                tvCurrentTime.text = it.toLong().toFormattedString()
            }
        }
    }

    private fun loadViews() {
        binding.apply {
            list.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            list.adapter = adapter
            snapHelper.attachToRecyclerView(list)
            list.addOnScrollListener(snapOnScrollListener)
            /**
             * Its used to make author name and title textview s scrollable(horizontally)
             */
            /*textNameBottom.isSelected = true
            textAuthorNameBottom.isSelected = true*/

            if (storage.isPlaying) {
                currentMusicPlay.setImageResource(R.drawable.ic_pause)
            } else {
                currentMusicPlay.setImageResource(R.drawable.ic_play)
            }

            currentMusicNext.setOnClickListener {
                requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    startMusicService(serviceCommand = ServiceCommand.NEXT)
                }
            }

            currentMusicPrev.setOnClickListener {
                requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    startMusicService(serviceCommand = ServiceCommand.PREV)
                }
            }

            currentMusicPlay.setOnClickListener {
                requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    startMusicService(serviceCommand = ServiceCommand.PLAY_PAUSE)
                }
            }

            arrowBackButton.setOnClickListener { findNavController().navigateUp() }

            seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        seekbar.progress = progress
                        tvCurrentTime.text = progress.toLong().toFormattedString()
                        EventBus.progressChangeLiveData.postValue(progress)
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {

                }

                override fun onStopTrackingTouch(p0: SeekBar?) {

                }

            })
        }
    }

    private fun loadData() {
        requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requireActivity().getPlayListCursor()
                .onEach {
                    adapter.swapCursor(it)

                    if (EventBus.musicStateLiveData.value != null) {
                        EventBus.musicStateLiveData.value?.let { musicState ->
                            musicState.data?.let { it2 -> loadPlayingData(it2) }
                            binding.list.scrollToPosition(musicState.position)
                        }
                    } else {
                        storage.lastPlayedPosition.let { pos ->
                            if (it.moveToPosition(pos)) {
                                val data = it.toMusicData()
                                binding.list.scrollToPosition(pos)
                                loadPlayingData(data)
                                startMusicService(serviceCommand = ServiceCommand.INIT)
                            }
                        }
                    }
                }
                .catch { timberErrorLog(this.toString()) }
                .launchIn(lifecycleScope)
        }
    }


    private val snapOnScrollListener = SnapOnScrollListener(
        snapHelper,
        SnapOnScrollListener.Behavior.NOTIFY_ON_SCROLL_STATE_IDLE,
        object : SnapOnScrollListener.OnSnapPositionChangeListener {
            override fun onSnapPositionChange(position: Int) {
                if (position != storage.lastPlayedPosition) {
                    storage.lastPlayedPosition = position
                    storage.lastPlayedDuration = 0
                    startMusicService(serviceCommand = ServiceCommand.PLAY_NEW)
                }
            }
        })

    private fun loadPlayingData(it: Music) {
        binding.apply {
            currentMusicName.text = it.title
            currentMusicAuthor.text = it.artist
            it.duration?.let {
                seekbar.max = it.toInt()
                tvDuration.text = it.toFormattedString()
                storage.lastPlayedDuration.let { progress ->
                    seekbar.progress = progress
                    tvCurrentTime.text = progress.toLong().toFormattedString()
                }
            }
        }
    }

    private fun startMusicService(serviceCommand: ServiceCommand) {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra(COMMAND_DATA, serviceCommand)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }


    override fun onDestroyView() {
        view?.findViewById<SlowdownRecyclerView>(R.id.list)?.adapter = null
        view?.findViewById<SlowdownRecyclerView>(R.id.list)
            ?.removeOnScrollListener(snapOnScrollListener)
        super.onDestroyView()
    }


}