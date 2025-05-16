package src.cordova.plugin.videocall

import com.google.gson.annotations.SerializedName

data class ParticipantDetails(@SerializedName("user_id") val userId: String,
                              @SerializedName("full_name") val name: String)
