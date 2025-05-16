package src.cordova.plugin.videocall.LocalParticipantManager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.cloud9.telehealth.R
import com.twilio.video.*
import com.twilio.video.ktx.createLocalAudioTrack
import com.twilio.video.ktx.createLocalVideoTrack
import src.cordova.plugin.videocall.CameraCapturerCompat.CameraCapturerCompat
import src.cordova.plugin.videocall.Preferences.Preferences.VIDEO_CAPTURE_RESOLUTION
import src.cordova.plugin.videocall.Preferences.Preferences.VIDEO_CAPTURE_RESOLUTION_DEFAULT
import src.cordova.plugin.videocall.Preferences.Preferences.VIDEO_DIMENSIONS
import src.cordova.plugin.videocall.RoomEvent.RoomEvent
import src.cordova.plugin.videocall.RoomManager.CAMERA_TRACK_NAME
import src.cordova.plugin.videocall.RoomManager.MICROPHONE_TRACK_NAME
import src.cordova.plugin.videocall.RoomManager.RoomManager
import src.cordova.plugin.videocall.RoomManager.SCREEN_TRACK_NAME
import src.cordova.plugin.videocall.SharedPreferencesUtil.get
import timber.log.Timber

class LocalParticipantManager(
    private val context: Context,
    private val roomManager: RoomManager,
    private val sharedPreferences: SharedPreferences
) {

    private var localAudioTrack: LocalAudioTrack? = null
        set(value) {
            field = value
            roomManager.sendRoomEvent(if (value == null) RoomEvent.LocalParticipantEvent.AudioOff else RoomEvent.LocalParticipantEvent.AudioOn)
        }
    internal var localParticipant: LocalParticipant? = null
    private var cameraVideoTrack: LocalVideoTrack? = null
        set(value) {
            field = value
            roomManager.sendRoomEvent(RoomEvent.LocalParticipantEvent.VideoTrackUpdated(value))
        }
    private var cameraCapturer: CameraCapturerCompat? = null
    private var screenCapturer: ScreenCapturer? = null
    private val screenCapturerListener: ScreenCapturer.Listener = object : ScreenCapturer.Listener {
        override fun onScreenCaptureError(errorDescription: String) {
            Timber.e(RuntimeException(), "Screen capturer error: %s", errorDescription)
            stopScreenCapture()
        }

        override fun onFirstFrameAvailable() {}
    }
    private var screenVideoTrack: LocalVideoTrack? = null
        set(value) {
            field = value
            roomManager.sendRoomEvent(if (value == null) RoomEvent.LocalParticipantEvent.ScreenCaptureOff else RoomEvent.LocalParticipantEvent.ScreenCaptureOn)
        }

    companion object {
        var isAudioMuted = false
        var isVideoMuted = false
    }

    internal val localVideoTrackNames: MutableMap<String, String> = HashMap()

    fun onResume() {
        if (!isAudioMuted) setupLocalAudioTrack()
        if (!isVideoMuted) setupLocalVideoTrack()
    }

    fun onPause() {
        removeCameraTrack()
    }

    fun toggleLocalVideo() {
        if (!isVideoMuted) {
            isVideoMuted = true
            removeCameraTrack()
        } else {
            isVideoMuted = false
            setupLocalVideoTrack()
        }
    }

    fun enableLocalVideo() {
        cameraVideoTrack?.enable(true)
        roomManager.sendRoomEvent(RoomEvent.LocalParticipantEvent.VideoEnabled)
    }

    fun disableLocalVideo() {
        cameraVideoTrack?.enable(false)
        roomManager.sendRoomEvent(RoomEvent.LocalParticipantEvent.VideoDisabled)
    }

    fun enableLocalAudio() {
        localAudioTrack?.enable(true)
        roomManager.sendRoomEvent(RoomEvent.LocalParticipantEvent.AudioEnabled)
    }

    fun disableLocalAudio() {
        localAudioTrack?.enable(false)
        roomManager.sendRoomEvent(RoomEvent.LocalParticipantEvent.AudioDisabled)
    }

    fun toggleLocalAudio() {
        if (!isAudioMuted) {
            isAudioMuted = true
            removeAudioTrack()
        } else {
            isAudioMuted = false
            setupLocalAudioTrack()
        }
    }

    fun startScreenCapture(captureResultCode: Int, captureIntent: Intent) {
        screenCapturer = ScreenCapturer(
            context, captureResultCode, captureIntent,
            screenCapturerListener
        )
        screenCapturer?.let { screenCapturer ->
            screenVideoTrack = createLocalVideoTrack(
                context, true, screenCapturer,
                name = SCREEN_TRACK_NAME
            )
            screenVideoTrack?.let { screenVideoTrack ->
                localVideoTrackNames[screenVideoTrack.name] =
                    context.getString(R.string.screen_video_track)
                localParticipant?.publishTrack(
                    screenVideoTrack,
                    LocalTrackPublicationOptions(TrackPriority.HIGH)
                )
            } ?: Timber.e(RuntimeException(), "Failed to add screen video track")
        }
    }

    fun stopScreenCapture() {
        screenVideoTrack?.let { screenVideoTrack ->
            localParticipant?.unpublishTrack(screenVideoTrack)
            screenVideoTrack.release()
            localVideoTrackNames.remove(screenVideoTrack.name)
            this.screenVideoTrack = null
        }
    }

    fun publishLocalTracks() {
        publishAudioTrack(localAudioTrack)
        publishCameraTrack(cameraVideoTrack)
    }

    fun switchCamera() = cameraCapturer?.switchCamera()

    private fun setupLocalAudioTrack() {
        if (localAudioTrack == null && !isAudioMuted) {
            localAudioTrack = createLocalAudioTrack(context, true, MICROPHONE_TRACK_NAME)
            localAudioTrack?.let { publishAudioTrack(it) }
                ?: Timber.e(RuntimeException(), "Failed to create local audio track")
        }
    }

    private fun publishCameraTrack(localVideoTrack: LocalVideoTrack?) {
        if (!isVideoMuted) {
            localVideoTrack?.let {
                localParticipant?.publishTrack(
                    it,
                    LocalTrackPublicationOptions(TrackPriority.LOW)
                )
            }
        }
    }

    private fun publishAudioTrack(localAudioTrack: LocalAudioTrack?) {
        if (!isAudioMuted) {
            localAudioTrack?.let { localParticipant?.publishTrack(it) }
        }
    }

    private fun unpublishTrack(localVideoTrack: LocalVideoTrack?) =
        localVideoTrack?.let { localParticipant?.unpublishTrack(it) }

    private fun unpublishTrack(localAudioTrack: LocalAudioTrack?) =
        localAudioTrack?.let { localParticipant?.unpublishTrack(it) }

    private fun setupLocalVideoTrack() {
        val dimensionsIndex = sharedPreferences.get(
            VIDEO_CAPTURE_RESOLUTION,
            VIDEO_CAPTURE_RESOLUTION_DEFAULT
        ).toInt()
        val videoFormat = VideoFormat(VIDEO_DIMENSIONS[dimensionsIndex], 30)

        cameraCapturer = CameraCapturerCompat.newInstance(context)
        cameraVideoTrack = cameraCapturer?.let { cameraCapturer ->
            LocalVideoTrack.create(
                context,
                true,
                cameraCapturer,
                videoFormat,
                CAMERA_TRACK_NAME
            )
        }
        cameraVideoTrack?.let { cameraVideoTrack ->
            localVideoTrackNames[cameraVideoTrack.name] =
                context.getString(R.string.camera_video_track)
            publishCameraTrack(cameraVideoTrack)
        } ?: run {
            Timber.e(RuntimeException(), "Failed to create the local camera video track")
        }
    }

    private fun removeCameraTrack() {
        cameraVideoTrack?.let { cameraVideoTrack ->
            unpublishTrack(cameraVideoTrack)
            localVideoTrackNames.remove(cameraVideoTrack.name)
            cameraVideoTrack.release()
            this.cameraVideoTrack = null
        }
    }

    private fun removeAudioTrack() {
        localAudioTrack?.let { localAudioTrack ->
            unpublishTrack(localAudioTrack)
            localAudioTrack.release()
            this.localAudioTrack = null
        }
    }
}
