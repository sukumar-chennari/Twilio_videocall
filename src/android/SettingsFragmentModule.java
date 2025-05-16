package cordova.plugin.videocall.SettingsFragmentModule;

import cordova.plugin.videocall.SettingsFragmentSubcomponent.SettingsFragmentSubcomponent;
import dagger.Binds;
import dagger.Module;
import dagger.android.AndroidInjector;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import src.cordova.plugin.videocall.SettingsFragment.SettingsFragment;

@Module(subcomponents = SettingsFragmentSubcomponent.class)
public abstract class SettingsFragmentModule {
    @Binds
    @IntoMap
    @ClassKey(SettingsFragment.class)
    abstract AndroidInjector.Factory<?> bindYourFragmentInjectorFactory(
            SettingsFragmentSubcomponent.Factory factory);
}
