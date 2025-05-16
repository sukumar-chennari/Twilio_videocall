package src.cordova.plugin.videocall.SecurePreferences

interface SecurePreferences {

    fun putSecureString(key: String, value: String)

    fun getSecureString(key: String): String?
}
