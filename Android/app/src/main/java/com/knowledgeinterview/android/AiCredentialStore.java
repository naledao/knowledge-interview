package com.knowledgeinterview.android;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** 仅在本机保存 AI 服务凭据；升级保留，卸载清除，不参与系统备份。 */
final class AiCredentialStore {
    private static final String ALIAS = "knowledge_ai_http_auth_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final byte[] SCOPE = "https://frp.kangnasi.xyz:4400/|Antigravity".getBytes(StandardCharsets.UTF_8);
    private final AtomicFile file;

    static final class Credentials {
        final String username;
        final String password;

        Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    AiCredentialStore(Context context) {
        file = new AtomicFile(new File(context.getNoBackupFilesDir(), "ai-auth-v1.enc"));
    }

    private SecretKey key(boolean create) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        java.security.Key existing = store.getKey(ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        if (!create) return null;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }

    synchronized Credentials read() {
        byte[] plaintext = null;
        try {
            byte[] data = file.readFully();
            if (data.length < 30 || data.length > 65536 || data[0] != 1) return null;
            int ivLength = data[1] & 0xff;
            if (ivLength != 12 || data.length <= 2 + ivLength + 16) return null;
            SecretKey key = key(false);
            if (key == null) return null;
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, data, 2, ivLength));
            cipher.updateAAD(SCOPE);
            plaintext = cipher.doFinal(data, 2 + ivLength, data.length - 2 - ivLength);
            JSONObject json = new JSONObject(new String(plaintext, StandardCharsets.UTF_8));
            String username = json.getString("username");
            String password = json.getString("password");
            return username.isEmpty() || password.isEmpty() ? null : new Credentials(username, password);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (plaintext != null) Arrays.fill(plaintext, (byte) 0);
        }
    }

    synchronized boolean save(Credentials credentials) {
        byte[] plaintext = null;
        FileOutputStream output = null;
        try {
            JSONObject json = new JSONObject();
            json.put("username", credentials.username);
            json.put("password", credentials.password);
            plaintext = json.toString().getBytes(StandardCharsets.UTF_8);
            if (plaintext.length > 60000) return false;
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key(true));
            cipher.updateAAD(SCOPE);
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plaintext);
            if (iv.length != 12) return false;
            output = file.startWrite();
            output.write(1);
            output.write(iv.length);
            output.write(iv);
            output.write(encrypted);
            file.finishWrite(output);
            output = null;
            return true;
        } catch (Exception ignored) {
            if (output != null) file.failWrite(output);
            return false;
        } finally {
            if (plaintext != null) Arrays.fill(plaintext, (byte) 0);
        }
    }

    synchronized void clear() {
        file.delete();
    }
}
