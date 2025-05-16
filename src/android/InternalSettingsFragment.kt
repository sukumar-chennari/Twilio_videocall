package src.cordova.plugin.videocall.InternalSettingsFragment

import android.os.Bundle
import androidx.preference.ListPreference
import com.cloud9.telehealth.R


import src.cordova.plugin.videocall.BaseSettingsFragment.BaseSettingsFragment
import src.cordova.plugin.videocall.Preferences.Preferences
import src.cordova.plugin.videocall.Topology.Topology

class InternalSettingsFragment : BaseSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.internal_preferences)

        findPreference<ListPreference>(Preferences.ENVIRONMENT)?.run {
            value = sharedPreferences.getString(Preferences.ENVIRONMENT,
                    Preferences.ENVIRONMENT_DEFAULT)
        }

        findPreference<ListPreference>(Preferences.TOPOLOGY)?.run {
            val roomTypes = Topology.values().map { it.value }.toTypedArray()
            entries = roomTypes
            entryValues = roomTypes
            value = sharedPreferences.getString(Preferences.TOPOLOGY,
                    Preferences.TOPOLOGY_DEFAULT)
        }

        setHasOptionsMenu(true)
    }
}
