package uz.jsoft.mediaplayer.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import uz.jsoft.mediaplayer.BuildConfig
import uz.jsoft.mediaplayer.data.local.LocalStorage
import uz.jsoft.mediaplayer.utils.Constants.Companion.notificationChannelName
import uz.jsoft.mediaplayer.utils.Constants.Companion.channelId

/**
 * Created by Jasurbek Kurganbaev on 12/14/2021 2:29 PM
 **/

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        LocalStorage.init(this)

        createNotificationChannel()

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                notificationChannelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        lateinit var instance: App
    }
}