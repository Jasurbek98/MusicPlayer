package uz.jsoft.mediaplayer.presentation.ui.screens.music_list

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
import uz.jsoft.mediaplayer.playback.EventBus
import uz.jsoft.mediaplayer.playback.MusicService
import uz.jsoft.mediaplayer.presentation.ui.adapters.MusicAdapter
import uz.jsoft.mediaplayer.utils.Constants
import uz.jsoft.mediaplayer.utils.Constants.Companion.COMMAND_DATA
import uz.jsoft.mediaplayer.utils.Constants.Companion.FOR_IMAGE_LIST
import uz.jsoft.mediaplayer.utils.custom.ItemDecorationWithLeftPadding
import uz.jsoft.mediaplayer.utils.extensions.getPlayListCursor
import uz.jsoft.mediaplayer.utils.extensions.loadImage
import uz.jsoft.mediaplayer.utils.extensions.showAppBar
import uz.jsoft.mediaplayer.utils.extensions.toMusicData
import uz.jsoft.mediaplayer.utils.helper.checkPermissions
import uz.jsoft.mediaplayer.utils.helper.dpToPx
import uz.jsoft.mediaplayer.utils.helper.timberErrorLog
import javax.inject.Inject


@AndroidEntryPoint
class MusicListFragment : Fragment(R.layout.fragment_music_list) {

    private val binding: FragmentMusicListBinding by viewBinding(FragmentMusicListBinding::bind)

    @Inject
    lateinit var storage: LocalStorage

    private val adapter by lazy { MusicAdapter(storage) }
    private val itemDecoration by lazy {
        ItemDecorationWithLeftPadding(requireContext(), 85.dpToPx(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        exitTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.grid_exit_transition)

        // A similar mapping is set at the ImagePagerFragment with a setEnterSharedElementCallback.
        setExitSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>,
                    sharedElements: MutableMap<String, View>
                ) {
                    try {
                        val image = view?.findViewById<View>(R.id.img_album)
                        // Map the first shared element name to the child ImageView.
                        image?.let { sharedElements[names[0]] = image }
                    } catch (e: Exception) {
                        timberErrorLog(e.message.toString())
                    }
                }
            })

        if (adapter.itemCount != 0) {
            postponeEnterTransition()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        showAppBar()

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
                        btnPlayPause.setImageResource(R.drawable.ic_pause)
                    }
                    is MusicState.PLAYING -> {
                        btnPlayPause.setImageResource(R.drawable.ic_play)
                        it.data?.let { it1 -> loadPlayingData(it1) }

                        adapter.notifyItemChanged(it.position)
                        adapter.notifyItemChanged(adapter.lastSelected)
                    }
                    is MusicState.STOP -> {
                        btnPlayPause.setImageResource(R.drawable.ic_pause)
                    }
                    is MusicState.NEXT_OR_PREV -> {
                        btnPlayPause.setImageResource(R.drawable.ic_pause)
                        it.data?.let { it1 -> loadPlayingData(it1) }
                        musicList.scrollToPosition(it.position)

                        adapter.notifyItemChanged(it.position)
                        adapter.notifyItemChanged(adapter.lastSelected)
                    }
                }
            }
        }
    }

    private fun loadPlayingData(it: Music) {
        binding.apply {
            tvAuthor.text = it.title
            tvTitle.text = it.artist
            if (it.imageUri != null) {
                imgAlbum.loadImage(it.imageUri) {
                    startPostponedEnterTransition()
                }
            } else {
                imgAlbum.loadImage(R.drawable.ic_music_icon)
                startPostponedEnterTransition()
            }
        }
    }

    private fun loadViews() {

        binding.apply {
            musicList.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            musicList.addItemDecoration(itemDecoration)
            musicList.adapter = adapter

            adapter.setOnItemClickListener {
                requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    if (it == storage.lastPlayedPosition) {
                        startMusicService(serviceCommand = ServiceCommand.PLAY_PAUSE)
                    } else {
                        storage.lastPlayedPosition = it
                        storage.lastPlayedDuration = 0
                        startMusicService(serviceCommand = ServiceCommand.PLAY_NEW)
                    }
                }
            }

            btnNext.setOnClickListener {
                requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    startMusicService(serviceCommand = ServiceCommand.NEXT)
                }
            }

            btnPrev.setOnClickListener {
                requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    startMusicService(serviceCommand = ServiceCommand.PREV)
                }
            }

            btnPlayPause.setOnClickListener {
                requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    startMusicService(serviceCommand = ServiceCommand.PLAY_PAUSE)
                }
            }

            /**
             * Its used to make author name and title textview s scrollable(horizontally)
             */
            tvTitle.isSelected = true
            tvAuthor.isSelected = true

            if (storage.isPlaying) {
                btnPlayPause.setImageResource(R.drawable.ic_pause)
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_play)
            }

            bottomViewLayout.setOnClickListener {
                Log.d("ZZZZ", "setOnClickListener")

                try {
                    Log.d("ZZZZ", "try")
                    val extras = FragmentNavigatorExtras(
                        imgAlbum to (FOR_IMAGE_LIST + storage.lastPlayedPosition)
                    )
                    findNavController().navigate(
                        MusicListFragmentDirections.actionMusicListFragmentToMusicPlayFragment(),
                        extras
                    )
                } catch (e: Exception) {
                    Log.d("ZZZZ", "catch: ${e.message.toString()}")
                    timberErrorLog(e.message.toString())
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

    private fun loadData() {
        requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requireActivity().getPlayListCursor()
                .catch { timberErrorLog(this.toString()) }
                .onEach {
                    adapter.swapCursor(it)

                    if (EventBus.musicStateLiveData.value != null) {
                        EventBus.musicStateLiveData.value?.let { musicState ->
                            musicState.data?.let { it2 -> loadPlayingData(it2) }
                            binding.musicList.scrollToPosition(musicState.position)
                        }
                    } else {
                        storage.lastPlayedPosition.let { pos ->
                            if (it.moveToPosition(pos)) {
                                val data = it.toMusicData()
                                binding.musicList.scrollToPosition(pos)
                                loadPlayingData(data)
                                // TODO: 24.09.2021 next after start
                                startMusicService(serviceCommand = ServiceCommand.INIT)
                            }
                        }
                    }
                }.launchIn(lifecycleScope)
        }
    }

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.musicList)?.adapter = null
        view?.findViewById<RecyclerView>(R.id.musicList)?.removeItemDecoration(itemDecoration)
        super.onDestroyView()
    }


}