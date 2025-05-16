package src.cordova.plugin.videocall.RoomManager

import android.content.*
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.appcompat.app.AlertDialog
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import src.cordova.plugin.videocall.Analytics
import com.twilio.video.*
import com.twilio.video.TwilioException.ROOM_MAX_PARTICIPANTS_EXCEEDED_EXCEPTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import src.cordova.plugin.videocall.LocalParticipantListener.LocalParticipantListener
import src.cordova.plugin.videocall.LocalParticipantManager.LocalParticipantManager
import src.cordova.plugin.videocall.RemoteParticipantListener.RemoteParticipantListener
import src.cordova.plugin.videocall.RoomActivity.RoomActivity
import src.cordova.plugin.videocall.RoomEvent.RoomEvent
import src.cordova.plugin.videocall.RoomStats.RoomStats
import src.cordova.plugin.videocall.StatsScheduler.StatsScheduler
import src.cordova.plugin.videocall.VideoClient.VideoClient
import src.cordova.plugin.videocall.VideoService.VideoService.Companion.startService
import src.cordova.plugin.videocall.VideoService.VideoService.Companion.stopService
import timber.log.Timber

const val MICROPHONE_TRACK_NAME = "microphone"
const val CAMERA_TRACK_NAME = "camera"
const val SCREEN_TRACK_NAME = "screen"

class RoomManager(
    private val context: Context,
    private val videoClient: VideoClient,
    sharedPreferences: SharedPreferences,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    public var isThreeParticipantsJoined: Boolean = false
    private var statsScheduler: StatsScheduler? = null
    private val roomListener = RoomListener()

    @VisibleForTesting(otherwise = PRIVATE)
    internal var roomScope = CoroutineScope(coroutineDispatcher)
    private val mutableRoomEvents: MutableSharedFlow<RoomEvent> = MutableSharedFlow()
    val roomEvents: SharedFlow<RoomEvent> = mutableRoomEvents

    @VisibleForTesting(otherwise = PRIVATE)
    internal var localParticipantManager: LocalParticipantManager =
        LocalParticipantManager(context, this, sharedPreferences)
    var room: Room? = null

    fun disconnect() {
        room?.disconnect()
    }

    suspend fun connect(identity: String, roomName: String) {
        sendRoomEvent(RoomEvent.Connecting)
        connectToRoom(identity, roomName)
    }

    private suspend fun connectToRoom(identity: String, roomName: String) {
        roomScope.launch {
            try {
                videoClient.connect(identity, roomName, roomListener)
            } catch (e: Exception) {
                e.message
            }
        }
    }

    fun sendRoomEvent(roomEvent: RoomEvent) {
        Timber.d("sendRoomEvent: $roomEvent")
        roomScope.launch { mutableRoomEvents.emit(roomEvent) }
    }


    fun onResume() {
        localParticipantManager.onResume()
    }

    fun onPause() {
        localParticipantManager.onPause()
    }

    fun toggleLocalVideo() {
        localParticipantManager.toggleLocalVideo()
    }

    fun toggleLocalAudio() {
        localParticipantManager.toggleLocalAudio()
    }

    fun startScreenCapture(captureResultCode: Int, captureIntent: Intent) {
        localParticipantManager.startScreenCapture(captureResultCode, captureIntent)
    }

    fun stopScreenCapture() {
        localParticipantManager.stopScreenCapture()
    }

    fun switchCamera() = localParticipantManager.switchCamera()

    fun sendStatsUpdate(statsReports: List<StatsReport>) {
        room?.let { room ->
            val roomStats = RoomStats(
                room.remoteParticipants,
                localParticipantManager.localVideoTrackNames,
                statsReports
            )
            sendRoomEvent(RoomEvent.StatsUpdate(roomStats))
        }
    }

    fun enableLocalAudio() = localParticipantManager.enableLocalAudio()

    fun disableLocalAudio() = localParticipantManager.disableLocalAudio()

    fun enableLocalVideo() = localParticipantManager.enableLocalVideo()

    fun disableLocalVideo() = localParticipantManager.disableLocalVideo()

    lateinit var receiver: BroadcastReceiver
    fun listenBroadcast() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                try {
                    if (!intent.extras!!.isEmpty) {
                        val s3 = intent.extras!!.getString("RejectedParticipant")
                        val alertDialog = AlertDialog.Builder(RoomActivity.activity)
                        alertDialog.apply {
                            setCancelable(false)
                            setTitle("I am currently unavailable")
                            setMessage("Please feel free to message me in Chat and I will get back to you as soon as possible.")
                            setPositiveButton(
                                "OK"
                            ) { p0, p1 -> p0.dismiss() }
                        }.create().show()
                        val event: MutableMap<String, String> = HashMap()
                        event["message"] = s3!!
                        Analytics.trackEvent("ParticipantRejectedTheCall", event)
                    }
                } catch (e: Exception) {
                    val event: MutableMap<String, String> = HashMap()
                    event["message"] = e.message!!
                    Analytics.trackEvent("ParticipantRejectedTheCallException", event)
                }
            }
        }
        LocalBroadcastManager.getInstance(RoomActivity.activity)
            .registerReceiver(receiver, IntentFilter("ParticipantRejectedTheCall"))
    }

    fun stopBroadcast() {
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.e("reject participant BRE", e.message + "")
        }
    }

    inner class RoomListener : Room.Listener {
        override fun onConnected(room: Room) {
            Timber.i(
                "onConnected -> room sid: %s",
                room.sid
            )
            Log.e("Room Details->", room.toString())
            listenBroadcast()
            RoomActivity.roomSid = room.sid
            startService(context, room.name)
            isThreeParticipantsJoined = false
            when (room.remoteParticipants.size == 2) {
                true -> isThreeParticipantsJoined = true
            }
            setupParticipants(room)

            statsScheduler = StatsScheduler(this@RoomManager, room).apply { start() }
            this@RoomManager.room = room
        }

        override fun onDisconnected(room: Room, twilioException: TwilioException?) {
            Timber.i(
                "Disconnected from room -> sid: %s, state: %s",
                room.sid, room.state
            )

            stopService(context)
            stopBroadcast()
            sendRoomEvent(RoomEvent.Disconnected)

            localParticipantManager.localParticipant = null

            statsScheduler?.stop()
            statsScheduler = null
        }

        override fun onConnectFailure(room: Room, twilioException: TwilioException) {
            Timber.e(
                "Failed to connect to room -> sid: %s, state: %s, code: %d, error: %s",
                room.sid,
                room.state,
                twilioException.code,
                twilioException.message
            )

            if (twilioException.code == ROOM_MAX_PARTICIPANTS_EXCEEDED_EXCEPTION) {
                sendRoomEvent(RoomEvent.MaxParticipantFailure)
            } else {
                sendRoomEvent(RoomEvent.ConnectFailure)
            }
        }

        override fun onParticipantConnected(room: Room, remoteParticipant: RemoteParticipant) {
            Timber.i(
                "RemoteParticipant connected -> room sid: %s, remoteParticipant: %s",
                room.sid, remoteParticipant.sid
            )
            when (room.remoteParticipants.size == 2) {
                true -> isThreeParticipantsJoined = true
            }
            remoteParticipant.setListener(RemoteParticipantListener(this@RoomManager))
            sendRoomEvent(
                RoomEvent.RemoteParticipantEvent.RemoteParticipantConnected(
                    remoteParticipant
                )
            )
        }

        override fun onParticipantDisconnected(room: Room, remoteParticipant: RemoteParticipant) {
            Timber.i(
                "RemoteParticipant disconnected -> room sid: %s, remoteParticipant: %s",
                room.sid, remoteParticipant.sid
            )
            sendRoomEvent(
                RoomEvent.RemoteParticipantEvent.RemoteParticipantDisconnected(
                    remoteParticipant.sid
                )
            )
        }

        override fun onDominantSpeakerChanged(room: Room, remoteParticipant: RemoteParticipant?) {
            Timber.i(
                "DominantSpeakerChanged -> room sid: %s, remoteParticipant: %s",
                room.sid, remoteParticipant?.sid
            )

            sendRoomEvent(RoomEvent.DominantSpeakerChanged(remoteParticipant?.sid))
        }

        override fun onRecordingStarted(room: Room) = sendRoomEvent(RoomEvent.RecordingStarted)

        override fun onRecordingStopped(room: Room) = sendRoomEvent(RoomEvent.RecordingStopped)

        override fun onReconnected(room: Room) {
            Timber.i("onReconnected: %s", room.name)
        }

        override fun onReconnecting(room: Room, twilioException: TwilioException) {
            Timber.i("onReconnecting: %s", room.name)
        }

        private fun setupParticipants(room: Room) {
            room.localParticipant?.let { localParticipant ->
                localParticipantManager.localParticipant = localParticipant
                val participants = mutableListOf<Participant>()
                participants.add(localParticipant)
                localParticipant.setListener(LocalParticipantListener(this@RoomManager))

                room.remoteParticipants.forEach {
                    it.setListener(RemoteParticipantListener(this@RoomManager))
                    participants.add(it)
                }

                sendRoomEvent(RoomEvent.Connected(participants, room, room.name))
                localParticipantManager.publishLocalTracks()
            }
        }
    }
}
