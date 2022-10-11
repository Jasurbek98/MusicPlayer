package uz.jsoft.mediaplayer.utils.extensions

import java.util.concurrent.TimeUnit

/**
 * Created by Jasurbek Kurganbaev on 1/6/2022 2:11 PM
 **/
fun formatDuration(duration: Long): String {
    val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
    val seconds = (TimeUnit.SECONDS.convert(
        duration,
        TimeUnit.MILLISECONDS
    ) - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
    return String.format("%02d:%02d", minutes, seconds)
}