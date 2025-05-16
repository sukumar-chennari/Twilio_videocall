package src.cordova.plugin.videocall.VideoTrackViewState

import com.twilio.video.VideoTrack


data class VideoTrackViewState constructor (
  val videoTrack: VideoTrack,
  val isSwitchedOff: Boolean = false
)
