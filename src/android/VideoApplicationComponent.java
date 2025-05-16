package cordova.plugin.videocall.VideoApplicationComponent;


import cordova.plugin.videocall.ApplicationModule.ApplicationModule;
import cordova.plugin.videocall.ApplicationScope.ApplicationScope;
import cordova.plugin.videocall.CommunityTreeModule.CommunityTreeModule;
import cordova.plugin.videocall.RoomActivityModule.RoomActivityModule;
import cordova.plugin.videocall.SettingsActivityModule.SettingsActivityModule;
import cordova.plugin.videocall.SettingsFragmentModule.SettingsFragmentModule;
import cordova.plugin.videocall.VideoServiceModule.VideoServiceModule;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import src.cordova.plugin.videocall.AudioSwitchModule.AudioSwitchModule;
import src.cordova.plugin.videocall.CommunityVideoSdkModule.CommunityVideoSdkModule;
import src.cordova.plugin.videocall.SecurityModule.SecurityModule;
import src.cordova.plugin.videocall.VideoApplication.VideoApplication;

@ApplicationScope
@Component(
        modules = {
            AndroidInjectionModule.class,
            ApplicationModule.class,
            CommunityTreeModule.class,
            RoomActivityModule.class,
            SettingsActivityModule.class,
            SettingsFragmentModule.class,
            VideoServiceModule.class,
            CommunityVideoSdkModule.class,
            SecurityModule.class,
            AudioSwitchModule.class
        })
public interface VideoApplicationComponent {
    void inject(VideoApplication application);
}
