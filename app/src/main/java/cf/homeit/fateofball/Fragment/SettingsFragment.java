package cf.homeit.fateofball.Fragment;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import cf.homeit.fateofball.R;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = SettingsFragment.class.getSimpleName();
    SharedPreferences sharedPreferences;
    private ListPreference mListPreference;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings_fragment);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        onSharedPreferenceChanged(sharedPreferences, getString(R.string.ps_sw_vibro));
        onSharedPreferenceChanged(sharedPreferences, getString(R.string.ps_ls_vibro));
        onSharedPreferenceChanged(sharedPreferences, getString(R.string.ps_sw_sound));
        onSharedPreferenceChanged(sharedPreferences, getString(R.string.ps_sw_ads));
        onSharedPreferenceChanged(sharedPreferences, getString(R.string.ps_sw_vip));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference instanceof ListPreference) {
            mListPreference = (ListPreference) preference;
            int prefIndex = mListPreference.findIndexOfValue(sharedPreferences.getString(key, ""));
            if (prefIndex >= 0) {
                preference.setSummary(mListPreference.getEntries()[prefIndex]);
            }
        } else {
//            preference.setSummary(sharedPreferences.getString(key, ""));
            preference.setEnabled(sharedPreferences.getBoolean(key, false));
        }
        
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

}
