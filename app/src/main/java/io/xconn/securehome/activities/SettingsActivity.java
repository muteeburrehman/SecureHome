package io.xconn.securehome.activities;

import android.os.Bundle;
import android.text.InputType;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import io.xconn.securehome.R;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            setupUrlPreference("camera1_url");
            setupUrlPreference("camera2_url");
        }

        private void setupUrlPreference(String key) {
            EditTextPreference urlPreference = findPreference(key);
            if (urlPreference != null) {
                urlPreference.setOnBindEditTextListener(editText -> {
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                    editText.selectAll();
                });

                // Show summary as the current value
                urlPreference.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
            }
        }
    }
}