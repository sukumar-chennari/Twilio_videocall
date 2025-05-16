package src.cordova.plugin.videocall.VideoSdkModule

import android.app.Application
import android.content.SharedPreferences
import cordova.plugin.videocall.ApplicationModule.ApplicationModule
import cordova.plugin.videocall.ApplicationScope.ApplicationScope
import dagger.Module
import dagger.Provides
import src.cordova.plugin.videocall.ConnectOptionsFactory.ConnectOptionsFactory
import src.cordova.plugin.videocall.DataModule.DataModule
import src.cordova.plugin.videocall.RoomManager.RoomManager
import src.cordova.plugin.videocall.VideoClient.VideoClient

@Module(includes = [
    ApplicationModule::class,
    DataModule::class])
class VideoSdkModule {

    @Provides
    fun providesConnectOptionsFactory(
        application: Application,
        sharedPreferences: SharedPreferences
    ): ConnectOptionsFactory =
            ConnectOptionsFactory(application, sharedPreferences)

    @Provides
    fun providesRoomFactory(
        application: Application,
        connectOptionsFactory: ConnectOptionsFactory
    ): VideoClient =
            VideoClient(application, connectOptionsFactory)

    @Provides
    @ApplicationScope
    fun providesRoomManager(
        application: Application,
        videoClient: VideoClient,
        sharedPreferences: SharedPreferences
    ): RoomManager =
            RoomManager(application, videoClient, sharedPreferences)
}
