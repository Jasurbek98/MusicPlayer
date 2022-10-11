package uz.jsoft.mediaplayer.utils

import uz.jsoft.mediaplayer.data.model.Music

/**
 * Created by Jasurbek Kurganbaev on 1/7/2022 4:21 PM
 **/
fun Int.nextIndex(musicList: ArrayList<Music>) = if (this < musicList.lastIndex) this + 1 else 0
fun Int.prevIndex(musicList: ArrayList<Music>) = if (this > 0) this - 1 else musicList.lastIndex