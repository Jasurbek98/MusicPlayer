package uz.jsoft.mediaplayer.data.model.enums

import uz.jsoft.mediaplayer.data.model.Music

sealed class MusicState(val position: Int, val data: Music?) {
    class PLAYING(position: Int, data: Music?) : MusicState(position, data)
    class PAUSE(position: Int, data: Music?) : MusicState(position, data)
    class STOP(position: Int, data: Music?) : MusicState(position, data)
    class NEXT_OR_PREV(position: Int, data: Music?) : MusicState(position, data)

}