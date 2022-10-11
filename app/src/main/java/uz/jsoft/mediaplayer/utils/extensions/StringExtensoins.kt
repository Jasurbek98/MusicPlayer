package uz.jsoft.mediaplayer.utils.extensions

import com.google.gson.Gson

fun <T> String.fromGson(clazz: Class<T>): T? {
    return Gson().fromJson(this, clazz)
}

fun Any.toGson(): String {
    return Gson().toJson(this)
}