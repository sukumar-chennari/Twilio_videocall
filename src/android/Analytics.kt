package src.cordova.plugin.videocall

import com.uxcam.UXCam

class Analytics {
    companion object {
        @JvmStatic
        fun trackEvent(message: String, event: Map<String, String>) {
            UXCam.logEvent(message, event)
//            Smartlook.trackCustomEvent(message, JSONObject(event))
        }
    }
}
