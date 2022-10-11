package uz.jsoft.mediaplayer.data.model

import android.net.Uri
import android.os.Parcelable

/**
 * Created by Jasurbek Kurganbaev on 1/3/2022 4:08 PM
 **/
data class Music(
    var id: Long? = null,
    var artist: String? = null,
    var title: String? = null,
    var data: String? = null,
    var displayName: String,
    var duration: Long? = null,
    var imageUri: Uri? = null
)