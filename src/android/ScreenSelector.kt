package src.cordova.plugin.videocall.ScreenSelector


import cordova.plugin.videocall.BaseActivity.BaseActivity

interface ScreenSelector {

    val loginScreen: Class<out BaseActivity>
}
