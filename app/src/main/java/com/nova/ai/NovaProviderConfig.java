package com.nova.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.util.Base64;

/** Runtime AI configuration. API keys are encrypted with an Android Keystore key and never logged. */
public final class NovaProviderConfig {
    private static final String PREFS = "nova_ai_provider";
    private static final String ENDPOINT = "endpoint";
    private static final String MODEL = "model";
    private static final String API_KEY = "api_key";
    private static final String KEY_ALIAS = "nova_ai_key_v1";
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-5.6-luna";
    private final SharedPreferences prefs;

    public NovaProviderConfig(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String endpoint() { return prefs.getString(ENDPOINT, DEFAULT_ENDPOINT); }
    public String model() { return prefs.getString(MODEL, DEFAULT_MODEL); }
    public String apiKey() { return decrypt(prefs.getString(API_KEY, "")); }
    public boolean isConfigured() { return !endpoint().trim().isEmpty() && !model().trim().isEmpty() && !apiKey().trim().isEmpty(); }

    public synchronized boolean save(String endpoint, String model, String apiKey) {
        try {
            String encrypted = encrypt(apiKey == null ? "" : apiKey.trim());
            return prefs.edit()
                    .putString(ENDPOINT, endpoint == null ? DEFAULT_ENDPOINT : endpoint.trim())
                    .putString(MODEL, model == null ? DEFAULT_MODEL : model.trim())
                    .putString(API_KEY, encrypted)
                    .commit();
        } catch (Exception e) { return false; }
    }

    public synchronized void clear() { prefs.edit().remove(API_KEY).apply(); }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) return ((KeyStore.SecretKeyEntry) store.getEntry(KEY_ALIAS, null)).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }

    private String encrypt(String value) throws Exception {
        if (value.isEmpty()) return "";
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[cipher.getIV().length + ciphertext.length];
        System.arraycopy(cipher.getIV(), 0, combined, 0, cipher.getIV().length);
        System.arraycopy(ciphertext, 0, combined, cipher.getIV().length, ciphertext.length);
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    private String decrypt(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            byte[] combined = Base64.decode(value, Base64.NO_WRAP);
            byte[] iv = new byte[12];
            byte[] ciphertext = new byte[combined.length - iv.length];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }
}
