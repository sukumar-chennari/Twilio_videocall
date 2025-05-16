package src.cordova.plugin.videocall.RoomViewEffect

import com.twilio.video.Room
import io.uniflow.core.flow.data.UIEvent

sealed class RoomViewEffect : UIEvent() {

    object PermissionsDenied : RoomViewEffect()
    data class Connected(val room: Room) : RoomViewEffect()
    object Disconnected : RoomViewEffect()

    object ShowConnectFailureDialog : RoomViewEffect()
    object ShowMaxParticipantFailureDialog : RoomViewEffect()

}
