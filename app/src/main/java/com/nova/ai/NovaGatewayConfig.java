package com.nova.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Optional encrypted shared secret for trusted NOVA node pairing. */
public final class NovaGatewayConfig {
    private static final String PREFS="nova_gateway";private static final String TOKEN="token";private static final String ALIAS="nova_gateway_key_v1";private final SharedPreferences prefs;
    public NovaGatewayConfig(Context c){prefs=c.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public String token(){return decrypt(prefs.getString(TOKEN,""));}
    public boolean saveToken(String token){try{return prefs.edit().putString(TOKEN,encrypt(token==null?"":token.trim())).commit();}catch(Exception e){return false;}}
    public void clear(){prefs.edit().remove(TOKEN).apply();}
    private SecretKey key()throws Exception{KeyStore s=KeyStore.getInstance("AndroidKeyStore");s.load(null);if(s.containsAlias(ALIAS))return((KeyStore.SecretKeyEntry)s.getEntry(ALIAS,null)).getSecretKey();KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();}
    private String encrypt(String v)throws Exception{if(v.isEmpty())return"";Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());byte[] x=c.doFinal(v.getBytes(StandardCharsets.UTF_8)),iv=c.getIV(),all=new byte[iv.length+x.length];System.arraycopy(iv,0,all,0,iv.length);System.arraycopy(x,0,all,iv.length,x.length);return Base64.encodeToString(all,Base64.NO_WRAP);}
    private String decrypt(String v){if(v==null||v.isEmpty())return"";try{byte[] all=Base64.decode(v,Base64.NO_WRAP),iv=new byte[12],x=new byte[all.length-12];System.arraycopy(all,0,iv,0,12);System.arraycopy(all,12,x,0,x.length);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));return new String(c.doFinal(x),StandardCharsets.UTF_8);}catch(Exception e){return"";}}
}
