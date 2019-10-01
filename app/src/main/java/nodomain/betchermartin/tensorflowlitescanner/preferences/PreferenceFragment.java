package nodomain.betchermartin.tensorflowlitescanner.preferences;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import nodomain.betchermartin.tensorflowlitescanner.R;

public class PreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);
    }
}
