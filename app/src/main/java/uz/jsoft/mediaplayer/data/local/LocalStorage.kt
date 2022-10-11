package uz.jsoft.mediaplayer.data.local

import android.content.Context
import android.content.SharedPreferences
import uz.jsoft.mediaplayer.utils.helper.BooleanPreference
import uz.jsoft.mediaplayer.utils.helper.IntPreference

class LocalStorage private constructor(context: Context) {
    companion object {

        @Volatile
        lateinit var instance: LocalStorage
            private set

        fun init(context: Context) {
            synchronized(this) {
                instance = LocalStorage(context)
            }
        }
    }

    val pref: SharedPreferences =
        context.getSharedPreferences("LocalStorage", Context.MODE_PRIVATE)
    var isPlaying: Boolean by BooleanPreference(pref, false)

    var lastPlayedPosition: Int by IntPreference(pref, 0)
    var lastPlayedDuration: Int by IntPreference(pref, 0)

    var isShuffled: Boolean by BooleanPreference(pref, false)
    var isRepeatedMode: Boolean by BooleanPreference(pref, false)

    fun clear() {
        pref.edit().clear().apply()
    }
}

