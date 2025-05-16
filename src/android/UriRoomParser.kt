package src.cordova.plugin.videocall.UriRoomParser

import src.cordova.plugin.videocall.UriWrapper.UriWrapper

class UriRoomParser(private val uri: UriWrapper) {

    fun parseRoom(): String? =
            uri.pathSegments?.let {
                if (it.size >= 2) {
                    it[1]
                } else null
            }
}
