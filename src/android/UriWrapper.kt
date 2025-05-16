package src.cordova.plugin.videocall.UriWrapper

import android.net.Uri

class UriWrapper(uri: Uri?) {

    val pathSegments: MutableList<String>? = uri?.pathSegments
}
