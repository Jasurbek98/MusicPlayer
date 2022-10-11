package uz.jsoft.mediaplayer.playback

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import uz.jsoft.mediaplayer.R
import uz.jsoft.mediaplayer.data.local.LocalStorage
import uz.jsoft.mediaplayer.data.model.Music
import uz.jsoft.mediaplayer.data.model.enums.MusicState
import uz.jsoft.mediaplayer.data.model.enums.ServiceCommand
import uz.jsoft.mediaplayer.utils.Constants.Companion.COMMAND_DATA
import uz.jsoft.mediaplayer.utils.Constants.Companion.MUSIC_POSITION
import uz.jsoft.mediaplayer.utils.Constants.Companion.channelId
import uz.jsoft.mediaplayer.utils.Constants.Companion.foregroundServiceNotificationTitle
import uz.jsoft.mediaplayer.utils.Constants.Companion.notificationId
import uz.jsoft.mediaplayer.utils.extensions.getPlayListCursor
import uz.jsoft.mediaplayer.utils.extensions.toMusicData
import uz.jsoft.mediaplayer.utils.helper.timberErrorLog
import uz.jsoft.mediaplayer.utils.helper.timberLog
import java.io.File
import java.lang.Exception
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : LifecycleService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var durationJob: Job? = null
    private var currentDuration: Int = 0

    private var _mediaPlayer: MediaPlayer? = null
    private var currentMusic: Music? = null

    private var cursor: Cursor? = null

    private val notificationBuilder by lazy {
        NotificationCompat.Builder(this, channelId)
            .setContentTitle(foregroundServiceNotificationTitle)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_music_icon))
            .setSmallIcon(R.drawable.ic_music_icon)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
    }

    private fun getNotification(builder: NotificationCompat.Builder? = null): Notification =
        (builder ?: notificationBuilder)
            .setCustomContentView(createView())
            .build()


    @Inject
    lateinit var storage: LocalStorage

    override fun onCreate() {
        super.onCreate()
        startForeground(notificationId, getNotification())
        EventBus.progressChangeLiveData.observe(this) {
            _mediaPlayer?.seekTo(it)
            currentDuration = it
        }
        timberLog("onCreate")
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPendingIntent(serviceCommand: ServiceCommand): PendingIntent {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(COMMAND_DATA, serviceCommand)
        intent.putExtra(MUSIC_POSITION, storage.lastPlayedPosition)
        return PendingIntent.getService(
            this, serviceCommand.ordinal, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createView(): RemoteViews {
        val remote = RemoteViews(packageName, R.layout.notification_player)
        cursor?.apply {
            if (moveToPosition(storage.lastPlayedPosition)) {
                val data = toMusicData()

                remote.setTextViewText(R.id.ntv_title, data.title)
                remote.setTextViewText(R.id.ntv_author, data.artist)
                if (data.imageUri == null) {
                    remote.setImageViewResource(R.id.image, R.drawable.ic_music_icon)
                } else {
                    data.imageUri.let { remote.setImageViewUri(R.id.image, it) }
                }
                when {
                    _mediaPlayer?.isPlaying == true -> {
                        remote.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_play_white)
                    }
                    _mediaPlayer?.isPlaying != true -> {
                        remote.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_pause_white)
                    }
                }

                remote.setOnClickPendingIntent(
                    R.id.btn_prev,
                    createPendingIntent(ServiceCommand.PREV)
                )
                remote.setOnClickPendingIntent(
                    R.id.btn_play_pause,
                    createPendingIntent(ServiceCommand.PLAY_PAUSE)
                )
                remote.setOnClickPendingIntent(
                    R.id.btn_next,
                    createPendingIntent(ServiceCommand.NEXT)
                )
                remote.setOnClickPendingIntent(
                    R.id.btn_stop,
                    createPendingIntent(ServiceCommand.STOP)
                )
            }
        }
        return remote
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val command =
            intent?.extras?.getSerializable(COMMAND_DATA) as? ServiceCommand

        if (cursor == null) {
            getPlayListCursor()
                .onEach { cursor ->
                    this.cursor = cursor
                    doCommand(command)
                }
                .catch { timberErrorLog("Error on getPlayList: $this") }
                .launchIn(serviceScope)
        } else {
            doCommand(command)
        }
        return START_NOT_STICKY
    }

    private fun doCommand(serviceCommand: ServiceCommand?) {
//        if (checkAudioFocusState())
        when (serviceCommand) {
            ServiceCommand.PLAY_NEW -> {
                prepareMediaPlayer()
                _mediaPlayer?.start()
                startSendDuration()

                startForeground(notificationId, getNotification())

                EventBus.musicStateLiveData.postValue(
                    MusicState.PLAYING(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )
                storage.isPlaying = true
            }
            ServiceCommand.PLAY_PAUSE -> {
                if (_mediaPlayer?.isPlaying == true) {
                    _mediaPlayer?.pause()
                    durationJob?.cancel()
                    notifyNotification()
                    stopForeground(false)

                    EventBus.musicStateLiveData.postValue(
                        MusicState.PAUSE(
                            storage.lastPlayedPosition,
                            currentMusic
                        )
                    )
                    storage.isPlaying = false
                } else {
                    if (_mediaPlayer == null) {
                        prepareMediaPlayer()
                    }
                    _mediaPlayer?.start()
                    startSendDuration()
                    startForeground(notificationId, getNotification())

                    EventBus.musicStateLiveData.postValue(
                        MusicState.PLAYING(
                            storage.lastPlayedPosition,
                            currentMusic
                        )
                    )
                    storage.isPlaying = true
                }
            }
            ServiceCommand.STOP -> {
                _mediaPlayer?.pause()
                durationJob?.cancel()
                stopSelf()

                EventBus.musicStateLiveData.postValue(
                    MusicState.STOP(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )
                storage.isPlaying = false
            }
            ServiceCommand.INIT -> {
                if (_mediaPlayer == null) {
                    prepareMediaPlayer()
                }
                notifyNotification()
                stopForeground(true)
//                stopSelf()

                EventBus.musicStateLiveData.postValue(
                    MusicState.STOP(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )
                storage.isPlaying = false
            }
            ServiceCommand.PREV -> {
                prevMusic()
            }
            ServiceCommand.NEXT -> {
                nextMusic()
            }
            else -> {

            }
        }
    }

    private fun prevMusic() {
        cursor?.let { c ->
            if (c.moveToPrevious()) {
                storage.lastPlayedPosition = c.position
                currentMusic = c.toMusicData()
            } else {
                if (c.moveToLast()) {
                    storage.lastPlayedPosition = c.position
                    currentMusic = c.toMusicData()
                }
            }

            try {
                _mediaPlayer?.apply {
                    stop()
                    prepare()
                }

                _mediaPlayer =
                    MediaPlayer.create(this, Uri.fromFile(File(currentMusic?.data ?: ""))).apply {
                        storage.lastPlayedDuration = 0
                        currentDuration = 0

                        setOnCompletionListener {
                            timberErrorLog("onComplete")
                            nextMusic()
                        }
                        start()
                        startSendDuration()
                    }
                startForeground(notificationId, getNotification())

                EventBus.musicStateLiveData.postValue(
                    MusicState.NEXT_OR_PREV(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )
                storage.isPlaying = true
            } catch (e: Exception) {
                timberErrorLog(e.message.toString())
            }
        }
    }

    private fun nextMusic() {
        cursor?.let { c ->
            if (c.moveToNext()) {
                storage.lastPlayedPosition = c.position
                currentMusic = c.toMusicData()
            } else {
                if (c.moveToFirst()) {
                    storage.lastPlayedPosition = c.position
                    currentMusic = c.toMusicData()
                }
            }

            try {
                _mediaPlayer?.apply {
                    stop()
                    prepare()
                }

                _mediaPlayer =
                    MediaPlayer.create(this, Uri.fromFile(File(currentMusic?.data ?: ""))).apply {
                        storage.lastPlayedDuration = 0
                        currentDuration = 0

                        setOnCompletionListener {
                            timberErrorLog("onComplete")
                            nextMusic()
                        }
                        start()
                        startSendDuration()
                    }
                startForeground(notificationId, getNotification())

                EventBus.musicStateLiveData.postValue(
                    MusicState.NEXT_OR_PREV(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )
                storage.isPlaying = true
            } catch (e: Exception) {
                timberErrorLog(e.message.toString())
            }
        }
    }

    private fun notifyNotification() {
        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        mNotificationManager?.notify(notificationId, getNotification())
    }

    private fun prepareMediaPlayer() {
        timberLog(cursor.toString())
        cursor?.let { c ->
            currentDuration = 0

            val b = storage.lastPlayedPosition.let { c.moveToPosition(it) }
            timberLog(b.toString())
            if (b) {
                currentMusic = c.toMusicData()

                _mediaPlayer?.apply {
                    stop()
                    prepare()
                }

                _mediaPlayer =
                    MediaPlayer.create(this, Uri.fromFile(File(currentMusic?.data ?: ""))).apply {
                        setOnCompletionListener {
                            timberErrorLog("onComplete")
                            nextMusic()
                        }
                        if (storage.lastPlayedDuration != 0) {
                            seekTo(storage.lastPlayedDuration)
                            EventBus.currentValueLiveData.postValue(storage.lastPlayedDuration)
                        }
                    }
            }
        }
    }

    private fun stopMusic() {
        _mediaPlayer?.apply {
            stop()
            prepare()
        }
        durationJob?.cancel()
    }

    private fun startSendDuration() {
        durationJob?.cancel()

        durationJob = getMusicChangesFlow().onEach {
            storage.lastPlayedDuration = it
            currentDuration = it
            EventBus.currentValueLiveData.postValue(it)
        }.launchIn(serviceScope)
    }

    private fun getMusicChangesFlow() = flow {
        for (i in currentDuration..(_mediaPlayer?.duration ?: 0)) {
            emit(_mediaPlayer?.currentPosition ?: 0)
            delay(1000)
        }
    }.flowOn(Dispatchers.Default)

    override fun onDestroy() {
        super.onDestroy()
        timberLog("onDestroy")
        serviceScope.cancel()
        storage.isPlaying = false
    }


    override fun onBind(p0: Intent): IBinder? {
        super.onBind(p0)
        return null
    }


}