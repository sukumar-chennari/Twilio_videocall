package src.cordova.plugin.videocall.SharedPreferencesWrapper

import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferencesWrapper(private val sharedPreferences: SharedPreferences) :
    SharedPreferences by sharedPreferences {

    fun edit(action: SharedPreferences.Editor.() -> Unit) {
        sharedPreferences.edit(action = action)
    }
}
