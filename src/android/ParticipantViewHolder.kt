package src.cordova.plugin.videocall.ParticipantViewHolder

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.cloud9.telehealth.R
import com.twilio.video.NetworkQualityLevel
import com.twilio.video.NetworkQualityLevel.*
import com.twilio.video.VideoTrack
import cordova.plugin.videocall.ParticipantThumbView.ParticipantThumbView
import cordova.plugin.videocall.ParticipantView.ParticipantView
import cordova.plugin.videocall.videocall.videocall
import src.cordova.plugin.videocall.Analytics.Companion.trackEvent
import src.cordova.plugin.videocall.LocalParticipantManager.LocalParticipantManager
import src.cordova.plugin.videocall.ParticipantViewState.ParticipantViewState
import src.cordova.plugin.videocall.RoomViewEvent.RoomViewEvent
import src.cordova.plugin.videocall.VideoTrackViewState.VideoTrackViewState
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

internal class ParticipantViewHolder(private val thumb: ParticipantThumbView) :
    RecyclerView.ViewHolder(thumb) {

    private val localParticipantIdentity = thumb.context.getString(R.string.you)

    fun bind(participantViewState: ParticipantViewState, viewEventAction: (RoomViewEvent) -> Unit) {
        Timber.d("bind ParticipantViewHolder with data item: %s", participantViewState)
        Timber.d("thumb: %s", thumb)
        thumb.run {
            participantViewState.sid?.let { sid ->
                setOnClickListener {
                    viewEventAction(RoomViewEvent.PinParticipant(sid))
                }
            }
            val identity = if (participantViewState.isLocalParticipant)
                localParticipantIdentity else participantViewState.identity!!.split("@")[0]

            setIdentity(identity)
            setMuted(
                if (participantViewState.isLocalParticipant) LocalParticipantManager.isAudioMuted
                else participantViewState.isMuted
            )
//            setMuted(participantViewState.isMuted)
            setPinned(participantViewState.isPinned)

//            if(identity != localParticipantIdentity)
            updateVideoTrack(participantViewState)

            networkQualityLevelImg?.let {
                setNetworkQualityLevelImage(it, participantViewState.networkQualityLevel, identity)
            }
        }
    }

    private fun updateVideoTrack(participantViewState: ParticipantViewState) {
        thumb.run {
            val videoTrackViewState = participantViewState.videoTrack
            val newVideoTrack = videoTrackViewState?.let { it.videoTrack }
            if (videoTrack !== newVideoTrack) {
                removeRender(videoTrack, this)
                videoTrack = newVideoTrack
                videoTrack?.let { videoTrack ->
                    setVideoState(videoTrackViewState)
                    if (videoTrack.isEnabled) videoTrack.addSink(this)
                } ?: setState(ParticipantView.State.NO_VIDEO)
            } else {
                setVideoState(videoTrackViewState)
            }
        }
    }

    private fun ParticipantThumbView.setVideoState(videoTrackViewState: VideoTrackViewState?) {
        if (videoTrackViewState?.let { it.isSwitchedOff } == true) {
            setState(ParticipantView.State.SWITCHED_OFF)
        } else {
            videoTrackViewState?.videoTrack?.let { setState(ParticipantView.State.VIDEO) }
                ?: setState(ParticipantView.State.NO_VIDEO)
        }
    }

    private fun removeRender(videoTrack: VideoTrack?, view: ParticipantView) {
        if (videoTrack == null || !videoTrack.sinks.contains(view)) return
        videoTrack.removeSink(view)
    }

    private fun setNetworkQualityLevelImage(
        networkQualityImage: ImageView,
        networkQualityLevel: NetworkQualityLevel?,
        identity: String
    ) {
        val simpleDate = SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
        val dateAndTime = simpleDate.format(Date())
        val nameAndRole = videocall.identity.split("@")[0]+"@"+videocall.identity.split("@")[1]
        when (networkQualityLevel) {
            NETWORK_QUALITY_LEVEL_ZERO -> {
                if (identity == localParticipantIdentity) {
                    val event: MutableMap<String, String> = HashMap()
                    event["message"] = "$nameAndRole is Very Bad $dateAndTime"
                    trackEvent("Network Quality", event)
                }
                R.drawable.network_quality_level_0
            }
            NETWORK_QUALITY_LEVEL_ONE -> {
                if (identity == localParticipantIdentity) {
                    val event: MutableMap<String, String> = HashMap()
                    event["message"] = "$nameAndRole is Bad $dateAndTime"
                    trackEvent("Network Quality", event)
                }
                R.drawable.network_quality_level_1
            }
            NETWORK_QUALITY_LEVEL_TWO -> {
                if (identity == localParticipantIdentity){
                    val event: MutableMap<String, String> = HashMap()
                    event["message"] = "$nameAndRole is Good $dateAndTime"
                    trackEvent("Network Quality", event)
                }
                R.drawable.network_quality_level_2
            }
            NETWORK_QUALITY_LEVEL_THREE -> {
                if (identity == localParticipantIdentity){
                    val event: MutableMap<String, String> = HashMap()
                    event["message"] = "$nameAndRole is Very Good $dateAndTime"
                    trackEvent("Network Quality", event)
                }
                R.drawable.network_quality_level_3
            }
            NETWORK_QUALITY_LEVEL_FOUR -> {
                if (identity == localParticipantIdentity){
                    val event: MutableMap<String, String> = HashMap()
                    event["message"] = "$nameAndRole is Excellent $dateAndTime"
                    trackEvent("Network Quality", event)
                }
                R.drawable.network_quality_level_4
            }
            NETWORK_QUALITY_LEVEL_FIVE -> {
                if (identity == localParticipantIdentity){
                    val event: MutableMap<String, String> = HashMap()
                    event["message"] = "$nameAndRole is Marvellous $dateAndTime"
                    trackEvent("Network Quality", event)
                }
                R.drawable.network_quality_level_5
            }
            else -> null
        }?.let { image ->
            networkQualityImage.visibility = View.VISIBLE
            networkQualityImage.setImageResource(image)
        } ?: run { networkQualityImage.visibility = View.GONE }
    }
}
