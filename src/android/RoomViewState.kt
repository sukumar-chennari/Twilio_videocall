package src.cordova.plugin.videocall.RoomViewState



import com.twilio.audioswitch.AudioDevice
import io.uniflow.core.flow.data.UIState
import src.cordova.plugin.videocall.ParticipantViewState.ParticipantViewState
import src.cordova.plugin.videocall.RoomStats.RoomStats
import src.cordova.plugin.videocall.VideoTrackViewState.VideoTrackViewState

data class RoomViewState(
  val primaryParticipant: ParticipantViewState,
  val title: String? = null,
  val participantThumbnails: List<ParticipantViewState>? = null,
  val selectedDevice: AudioDevice? = null,
  val availableAudioDevices: List<AudioDevice>? = null,
  val configuration: RoomViewConfiguration = RoomViewConfiguration.Lobby,
  val isCameraEnabled: Boolean = false,
  val localVideoTrack: VideoTrackViewState? = null,
  val isMicEnabled: Boolean = false,
  val isAudioMuted: Boolean = false,
  val isAudioEnabled: Boolean = true,
  val isVideoEnabled: Boolean = true,
  val isVideoOff: Boolean = false,
  val isScreenCaptureOn: Boolean = false,
  val isRecording: Boolean = false,
  val roomStats: RoomStats? = null
) : UIState()

sealed class RoomViewConfiguration {
    object Connecting : RoomViewConfiguration()
    object Connected : RoomViewConfiguration()
    object Lobby : RoomViewConfiguration()
}
