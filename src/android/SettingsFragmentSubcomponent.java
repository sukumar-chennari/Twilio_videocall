package cordova.plugin.videocall.SettingsFragmentSubcomponent;

import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import src.cordova.plugin.videocall.SettingsFragment.SettingsFragment;

@Subcomponent
public interface SettingsFragmentSubcomponent extends AndroidInjector<SettingsFragment> {
    @Subcomponent.Factory
    interface Factory extends AndroidInjector.Factory<SettingsFragment> {}
}
