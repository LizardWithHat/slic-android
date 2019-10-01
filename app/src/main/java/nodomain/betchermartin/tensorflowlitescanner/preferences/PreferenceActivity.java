package nodomain.betchermartin.tensorflowlitescanner.preferences;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import nodomain.betchermartin.tensorflowlitescanner.R;


public class PreferenceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preferences_container, new PreferenceFragment())
                .commit();
    }
}
