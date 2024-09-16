package sh.damon.fridamgr.preferences;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserPreferences {
    public interface UserPreferenceChangedCallback {
        void call(UserPreferences prefs, String key);
    }

    @SerializedName("settings")
    private final Map<String, JsonElement> mSettings = new LinkedHashMap<>();
    private transient  UserPreferenceChangedCallback mCallback = null;

    @SuppressWarnings("unchecked")
    public static <T> T get(String id, T def) {
        final UserPreferences inst = get();
        if (inst.mSettings.containsKey(id)) {
            final Gson gson = new Gson();
            return (T) gson.fromJson(inst.mSettings.get(id), def.getClass());
        }
        return def;
    }

    public static <T> void set(String id, T val) {
        final UserPreferences inst = get();
        final Gson gson = new Gson();
        inst.mSettings.put(id, gson.toJsonTree(val));

        if (inst.mCallback != null) {
            inst.mCallback.call(inst, id);
        }
    }

    private static File mSaveFile;
    public static void load(File saveFile) {
        mSaveFile = saveFile;

        if (!mSaveFile.exists()) {
            mInstance = new UserPreferences();

            return;
        }

        final Gson gson = new Gson();
        final JsonReader jsonReader;
        try {
            jsonReader = new JsonReader(new FileReader(saveFile));

            if (mInstance != null ){
                Log.w("UserPreferences", "UserPreference object has already been instantiated, double call to load?");

                return;
            }
            mInstance = gson.fromJson(jsonReader, UserPreferences.class);
        } catch (FileNotFoundException e) {
            Log.e("UserPreferences", "This should never happen as we've already checked for the file existing or not.");
        }
    }

    public static void save() {
        if (mInstance == null) {
            throw new RuntimeException("UserPreference object has not been instantiated!");
        }

        try (final FileWriter file = new FileWriter(mSaveFile)) {
            Gson gson = new Gson();
            gson.toJson(mInstance, file);
        } catch (IOException e) {
            Log.e("UserPreferences", "Failed to save UserPreferences to disk.");
        }
    }

    private static UserPreferences mInstance = null;
    public static UserPreferences get() throws RuntimeException {
        if (mInstance == null) {
            throw new RuntimeException("UserPreference object has not been instantiated!");
        }
        return mInstance;
    }

    public static void setOnPrefChangeCallback(UserPreferenceChangedCallback onUserPrefChangedCallback) {
        get().mCallback = onUserPrefChangedCallback;
    }
}
