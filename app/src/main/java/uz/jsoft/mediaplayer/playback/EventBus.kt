package uz.jsoft.mediaplayer.playback

import androidx.lifecycle.MutableLiveData
import uz.jsoft.mediaplayer.data.model.enums.MusicState

object EventBus {
    val musicStateLiveData = MutableLiveData<MusicState>()
    val currentValueLiveData = MutableLiveData<Int>()
    val progressChangeLiveData = MutableLiveData<Int>()
}