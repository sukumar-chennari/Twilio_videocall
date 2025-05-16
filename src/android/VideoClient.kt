package src.cordova.plugin.videocall.VideoClient

import android.content.Context
import com.twilio.video.LogLevel
import com.twilio.video.LogModule
import com.twilio.video.Room
import com.twilio.video.Video
import com.twilio.video.Video.setLogLevel
import src.cordova.plugin.videocall.ConnectOptionsFactory.ConnectOptionsFactory

class VideoClient(
    private val context: Context,
    private val connectOptionsFactory: ConnectOptionsFactory
) {

    suspend fun connect(
        identity: String,
        roomName: String,
        roomListener: Room.Listener
    ): Room {
        setLogLevel(LogLevel.DEBUG)
        Video.setModuleLogLevel(LogModule.CORE, LogLevel.DEBUG)
        Video.setModuleLogLevel(LogModule.PLATFORM, LogLevel.DEBUG)
        Video.setModuleLogLevel(LogModule.SIGNALING, LogLevel.DEBUG)
        Video.setModuleLogLevel(LogModule.WEBRTC, LogLevel.DEBUG)
            return Video.connect(
                    context,
                    connectOptionsFactory.newInstance(identity, roomName),
                    roomListener)
    }
}
