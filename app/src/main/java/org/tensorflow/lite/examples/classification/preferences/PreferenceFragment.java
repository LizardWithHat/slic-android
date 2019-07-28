package org.tensorflow.lite.examples.classification.preferences;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import org.tensorflow.lite.examples.classification.R;

public class PreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);
    }
}
