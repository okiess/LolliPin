package com.github.orangegangsters.lollipin.lib.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import com.github.orangegangsters.lollipin.lib.PinActivity;
import com.github.orangegangsters.lollipin.lib.PinCompatActivity;
import com.github.orangegangsters.lollipin.lib.PinFragmentActivity;
import com.github.orangegangsters.lollipin.lib.encryption.Encryptor;
import com.github.orangegangsters.lollipin.lib.enums.Algorithm;
import com.github.orangegangsters.lollipin.lib.interfaces.LifeCycleInterface;

import java.security.SecureRandom;
import java.util.Arrays;

public class AppLockImpl<T extends AppLockActivity> extends AppLock implements LifeCycleInterface {

    public static final String TAG = "AppLockImpl";

    /**
     * The {@link android.content.SharedPreferences} key used to store the password
     */
    private static final String PASSWORD_PREFERENCE_KEY = "PASSCODE";
    private static final String ANDROID_ID_PREFERENCE_KEY = "ANDROID_ID";
    /**
     * The {@link android.content.SharedPreferences} key used to store the {@link Algorithm}
     */
    private static final String PASSWORD_ALGORITHM_PREFERENCE_KEY = "ALGORITHM";
    /**
     * The {@link android.content.SharedPreferences} key used to store the last active time
     */
    private static final String LAST_ACTIVE_MILLIS_PREFERENCE_KEY = "LAST_ACTIVE_MILLIS";
    /**
     * The {@link android.content.SharedPreferences} key used to store the timeout
     */
    private static final String TIMEOUT_MILLIS_PREFERENCE_KEY = "TIMEOUT_MILLIS_PREFERENCE_KEY";
    /**
     * The {@link android.content.SharedPreferences} key used to store the logo resource id
     */
    private static final String LOGO_ID_PREFERENCE_KEY = "LOGO_ID_PREFERENCE_KEY";
    /**
     * The {@link android.content.SharedPreferences} key used to store the forgot option
     */
    private static final String SHOW_FORGOT_PREFERENCE_KEY = "SHOW_FORGOT_PREFERENCE_KEY";
    private static final String SHOW_FINGERPRINT_PREFERENCE_KEY = "SHOW_USE_FINGERPRINT_KEY";
    private static final String PIN_PERSIST_PREFERENCE_KEY = "PERSIST_APP_PIN_KEY";
    /**
     * The {@link SharedPreferences} key used to store whether the user has backed out of the {@link AppLockActivity}
     */
    private static final String PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY = "PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY";
    /**
     * The {@link android.content.SharedPreferences} key used to store the dynamically generated password salt
     */
    private static final String PASSWORD_SALT_PREFERENCE_KEY = "PASSWORD_SALT_PREFERENCE_KEY";

    /**
     * The default password salt
     */
    private static final String DEFAULT_PASSWORD_SALT = "hQXV3YfjivYzsqAzqH7oK^]JYLxgDfcRKiwFzsJDsigfxfnzEU";
    /**
     * The key algorithm used to generating the dynamic salt
     */
    private static final String KEY_ALGORITHM = "PBEWithMD5AndDES";
    /**
     * The key length of the salt
     */
    private static final int KEY_LENGTH = 256;
    /**
     * The number of iterations used to generate a dynamic salt
     */
    private static final int KEY_ITERATIONS = 20;

    /**
     * The {@link android.content.SharedPreferences} used to store the password, the last active time etc...
     */
    private SharedPreferences mSharedPreferences;

    /**
     * The activity class that extends {@link com.github.orangegangsters.lollipin.lib.managers.AppLockActivity}
     */
    private Class<T> mActivityClass;
    private Context mContext;

    /**
     * Static instance of {@link AppLockImpl}
     */
    private static AppLockImpl mInstance;

    private String externalSecret = ""; // Set your own per app installation!!!!

    /**
     * Static method that allows to get back the current static Instance of {@link AppLockImpl}
     *
     * @param context       The current context of the {@link Activity}
     * @param activityClass The activity extending {@link AppLockActivity}
     * @return The instance.
     */
    public static AppLockImpl getInstance(Context context, Class<? extends AppLockActivity> activityClass) {
        synchronized (LockManager.class) {
            if (mInstance == null) {
                mInstance = new AppLockImpl<>(context, activityClass);
            }
        }
        return mInstance;
    }

    private AppLockImpl(Context context, Class<T> activityClass) {
        super();
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.mActivityClass = activityClass;
        this.mContext = context;
    }

    @Override
    public void setTimeout(long timeout) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(TIMEOUT_MILLIS_PREFERENCE_KEY, timeout);
        editor.apply();
    }

    public String getSalt() {
        String salt = mSharedPreferences.getString(PASSWORD_SALT_PREFERENCE_KEY, null);
        if (salt == null) {
            salt = generateSalt();
            setSalt(salt);
        }
        return salt;
    }

    private void setSalt(String salt) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PASSWORD_SALT_PREFERENCE_KEY, salt);
        editor.apply();
    }

    private String generateSalt() {
        byte[] salt = new byte[KEY_LENGTH];
        try {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(System.currentTimeMillis());
            sr.nextBytes(salt);
            return Arrays.toString(salt);
        } catch (Exception e) {
            salt = DEFAULT_PASSWORD_SALT.getBytes();
        }
        return Base64.encodeToString(salt, Base64.DEFAULT);
    }

    @Override
    public long getTimeout() {
        return mSharedPreferences.getLong(TIMEOUT_MILLIS_PREFERENCE_KEY, DEFAULT_TIMEOUT);
    }

    @Override
    public void setLogoId(int logoId) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(LOGO_ID_PREFERENCE_KEY, logoId);
        editor.apply();
    }

    @Override
    public int getLogoId() {
        return mSharedPreferences.getInt(LOGO_ID_PREFERENCE_KEY, LOGO_ID_NONE);
    }

    @Override
    public void setShouldShowForgot(boolean showForgot) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(SHOW_FORGOT_PREFERENCE_KEY, showForgot);
        editor.apply();
    }

    public void setShouldShowFingerprint(boolean showFingerprint) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(SHOW_FINGERPRINT_PREFERENCE_KEY, showFingerprint);
        editor.apply();
    }

    @Override
    public void setShouldPersistPin(boolean persistPin) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(PIN_PERSIST_PREFERENCE_KEY, persistPin);
        editor.apply();
    }

    @Override
    public void setExternalSecret(String nonPersistantUniqueAppInstanceString) {
        this.externalSecret = nonPersistantUniqueAppInstanceString;
    }

    @Override
    public boolean pinChallengeCancelled() {
        return mSharedPreferences.getBoolean(PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY, false);
    }

    @Override
    public void setPinChallengeCancelled(boolean backedOut) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY, backedOut);
        editor.apply();
    }

    @Override
    public boolean shouldShowForgot() {
        return mSharedPreferences.getBoolean(SHOW_FORGOT_PREFERENCE_KEY, true);
    }

    @Override
    public boolean shouldShowFingerprint() {
        return mSharedPreferences.getBoolean(SHOW_FINGERPRINT_PREFERENCE_KEY, false);
    }

    @Override
    public boolean shouldPersistPin() {
        return mSharedPreferences.getBoolean(PIN_PERSIST_PREFERENCE_KEY, false);
    }

    @Override
    public void enable() {
        PinActivity.setListener(this);
        PinCompatActivity.setListener(this);
        PinFragmentActivity.setListener(this);
    }

    @Override
    public void disable() {
        PinActivity.clearListeners();
        PinCompatActivity.clearListeners();
        PinFragmentActivity.clearListeners();
    }

    @Override
    public void disableAndRemoveConfiguration() {
        PinActivity.clearListeners();
        PinCompatActivity.clearListeners();
        PinFragmentActivity.clearListeners();
        mSharedPreferences.edit().remove(PASSWORD_PREFERENCE_KEY)
                .remove(ANDROID_ID_PREFERENCE_KEY)
                .remove(LAST_ACTIVE_MILLIS_PREFERENCE_KEY)
                .remove(PASSWORD_ALGORITHM_PREFERENCE_KEY)
                .remove(TIMEOUT_MILLIS_PREFERENCE_KEY)
                .remove(LOGO_ID_PREFERENCE_KEY)
                .remove(SHOW_FORGOT_PREFERENCE_KEY)
                .remove(PIN_PERSIST_PREFERENCE_KEY)
                .remove(SHOW_FINGERPRINT_PREFERENCE_KEY)
                .apply();
    }

    @Override
    public long getLastActiveMillis() {
        return mSharedPreferences.getLong(LAST_ACTIVE_MILLIS_PREFERENCE_KEY, 0);
    }

    @Override
    public void setLastActiveMillis() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(LAST_ACTIVE_MILLIS_PREFERENCE_KEY, System.currentTimeMillis());
        editor.apply();
    }

    @Override
    public boolean checkPasscode(String passcode) {
        String salt = getSalt();
        String androidId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        String storedPasscode = "";
        setAlgorithm(Algorithm.SHA256);
        if (mSharedPreferences.contains(PASSWORD_PREFERENCE_KEY)) {
            storedPasscode = mSharedPreferences.getString(PASSWORD_PREFERENCE_KEY, "");
        }
        if (shouldPersistPin()) {
            final String androidIdUsed = mSharedPreferences.getString(ANDROID_ID_PREFERENCE_KEY, "");
            final String currentAndroidId = Encryptor.getSHA(androidId, Algorithm.SHA256);
            if (!androidIdUsed.equals(currentAndroidId)) {
                Log.e(TAG, "Device changed, app pin is now unreadable! PLEASE REINSTALL!");
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("androidIdChanged"));
                return false;
            }
            final String secret = salt + externalSecret + androidId;
            String decStoredPasscode = Encryptor.decryptString(secret, storedPasscode);
            return (decStoredPasscode != null && decStoredPasscode.equals(passcode));
        } else {
            passcode = salt + passcode + androidId;
            passcode = Encryptor.getSHA(passcode, Algorithm.SHA256);
            return (storedPasscode != null && storedPasscode.equals(passcode));
        }
    }

    @Override
    protected String readPasscode() {
        setAlgorithm(Algorithm.SHA256);
        if (shouldPersistPin() && isPasscodeSet() && externalSecret != null && externalSecret.length() > 0) {
            String salt = getSalt();
            String androidId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            final String androidIdUsed = mSharedPreferences.getString(ANDROID_ID_PREFERENCE_KEY, "");
            final String currentAndroidId = Encryptor.getSHA(androidId, Algorithm.SHA256);
            if (!androidIdUsed.equals(currentAndroidId)) {
                Log.e(TAG, "Device changed, app pin is now unreadable! PLEASE REINSTALL!");
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("androidIdChanged"));
                return null;
            }
            String storedPasscode = mSharedPreferences.getString(PASSWORD_PREFERENCE_KEY, "");
            final String secret = salt + externalSecret + androidId;
            return Encryptor.decryptString(secret, storedPasscode);
        }
        return null;
    }

    @Override
    public boolean setPasscode(String passcode) {
        String salt = getSalt();
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        String androidId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        setAlgorithm(Algorithm.SHA256);

        if (passcode == null) {
            editor.remove(PASSWORD_PREFERENCE_KEY);
            editor.apply();
            this.disable();
        } else {
            if (shouldPersistPin()) {
                if (externalSecret.length() > 0) {
                    final String secret = salt + externalSecret + androidId;
                    final String currentAndroidId = Encryptor.getSHA(androidId, Algorithm.SHA256);
                    editor.putString(ANDROID_ID_PREFERENCE_KEY, currentAndroidId);
                    editor.putString(PASSWORD_PREFERENCE_KEY, Encryptor.encryptString(secret, passcode));
                    editor.apply();
                    this.enable();
                } else {
                    this.disable();
                    return false;
                }
            } else {
                passcode = salt + passcode + androidId;
                passcode = Encryptor.getSHA(passcode, Algorithm.SHA256);
                editor.putString(PASSWORD_PREFERENCE_KEY, passcode);
                editor.apply();
                this.enable();
            }
        }
        return true;
    }

    /**
     * Set the algorithm used in {@link #setPasscode(String)}
     */
    private void setAlgorithm(Algorithm algorithm) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PASSWORD_ALGORITHM_PREFERENCE_KEY, algorithm.getValue());
        editor.apply();
    }

    @Override
    public boolean isPasscodeSet() {
        if (mSharedPreferences.contains(PASSWORD_PREFERENCE_KEY)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isIgnoredActivity(Activity activity) {
        String clazzName = activity.getClass().getName();

        // ignored activities
        if (mIgnoredActivities.contains(clazzName)) {
            Log.d(TAG, "ignore activity " + clazzName);
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldLockSceen(Activity activity) {
        Log.d(TAG, "Lollipin shouldLockSceen() called");

        // previously backed out of pin screen
        if (pinChallengeCancelled()) {
            return true;
        }

        // already unlock
        if (activity instanceof AppLockActivity) {
            AppLockActivity ala = (AppLockActivity) activity;
            if (ala.getType() == AppLock.UNLOCK_PIN) {
                Log.d(TAG, "already unlock activity");
                return false;
            }
        }

        // no pass code set
        if (!isPasscodeSet()) {
            Log.d(TAG, "lock passcode not set.");
            return false;
        }

        // no enough timeout
        long lastActiveMillis = getLastActiveMillis();
        long passedTime = System.currentTimeMillis() - lastActiveMillis;
        long timeout = getTimeout();
        if (lastActiveMillis > 0 && passedTime <= timeout) {
            Log.d(TAG, "no enough timeout " + passedTime + " for "
                    + timeout);
            return false;
        }

        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("pinCodeEntryShowing"));

        return true;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (isIgnoredActivity(activity)) {
            return;
        }

        String clazzName = activity.getClass().getName();
        Log.d(TAG, "onActivityPaused " + clazzName);

        setLastActiveMillis();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (isIgnoredActivity(activity)) {
            return;
        }

        String clazzName = activity.getClass().getName();
        Log.d(TAG, "onActivityResumed " + clazzName);

        if (shouldLockSceen(activity)) {
            Log.d(TAG, "mActivityClass.getClass() " + mActivityClass);
            Intent intent = new Intent(activity.getApplicationContext(),
                    mActivityClass);
            intent.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.getApplication().startActivity(intent);
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            return;
        }

        setLastActiveMillis();
    }
}
