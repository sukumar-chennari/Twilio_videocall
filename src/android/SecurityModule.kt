package src.cordova.plugin.videocall.SecurityModule

import android.app.Application
import android.content.SharedPreferences
import cordova.plugin.videocall.ApplicationModule.ApplicationModule
import cordova.plugin.videocall.ApplicationScope.ApplicationScope

import dagger.Module
import dagger.Provides
import src.cordova.plugin.videocall.DataModule.DataModule
import src.cordova.plugin.videocall.SecurePreferences.SecurePreferences
import src.cordova.plugin.videocall.SecurePreferencesImpl.SecurePreferencesImpl

@Module(includes = [
    ApplicationModule::class,
    DataModule::class
])
class SecurityModule {

    @Provides
    @ApplicationScope
    fun providesSecurePreferences(app: Application, preferences: SharedPreferences): SecurePreferences {
        return SecurePreferencesImpl(app.applicationContext, preferences)
    }
}
