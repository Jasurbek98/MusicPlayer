package uz.jsoft.mediaplayer.utils

/**
 * Created by Jasurbek Kurganbaev on 1/7/2022 3:43 PM
 **/

class Constants {
    companion object {

        //Intent Constants
        const val MUSIC_POSITION = "MUSIC_POSITION"
        const val COMMAND_DATA: String = "COMMAND_DATA"
        const val ACTION_PLAYER = "uz.jsoft.mediaplayer.utils.ACTION_PLAYER"
        const val NOTIFICATION_ACTION_PLAYER = "uz.jsoft.mediaplayer.util.NOTIFICATION_ACTION"

        const val FOR_IMAGE_LIST = "FOR_IMAGE_LIST"


        //Notification Constants
        const val channelId = "music_player_notification_channel_id"
        const val foregroundServiceNotificationTitle = "MusicPlayer"
        const val notificationId = 1
        const val foregroundIntentServiceNotificationTitle = "My Foreground Intent Service"
        const val notificationChannelName = "Music Player Service Channel"
    }
}

