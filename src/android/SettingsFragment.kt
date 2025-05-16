package src.cordova.plugin.videocall.SettingsFragment

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import com.cloud9.telehealth.R
import com.twilio.video.Video
import cordova.plugin.videocall.BuildConfig.BuildConfig
import dagger.android.support.AndroidSupportInjection
import src.cordova.plugin.videocall.BaseSettingsFragment.BaseSettingsFragment
import src.cordova.plugin.videocall.Preferences.Preferences

class SettingsFragment : BaseSettingsFragment() {


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Add our preference from resources
        addPreferencesFromResource(R.xml.preferences)

        setHasOptionsMenu(true)

        val versionCode = BuildConfig.VERSION_CODE.toString()
        findPreference<Preference>(Preferences.VERSION_NAME)?.summary = "${BuildConfig.VERSION_NAME} ($versionCode)"
        findPreference<Preference>(Preferences.VIDEO_LIBRARY_VERSION)?.summary = Video.getVersion()
        findPreference<Preference>(Preferences.LOGOUT)?.onPreferenceClickListener = Preference.OnPreferenceClickListener { logout(); true }
    }

    private fun logout() {
        /*requireActivity().let { activity ->
            val loginIntent = Intent(activity, screenSelector.loginScreen)

            // Clear all preferences and set defaults
            sharedPreferences.edit().clear().apply()
            PreferenceManager.setDefaultValues(activity, R.xml.preferences, true)

            // Return to login activity
            loginIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

            startActivity(loginIntent)
            activity.finishAffinity()
        }*/
    }
}
