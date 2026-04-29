package de.onyxbits.remotekeyboard;

import android.os.Build;
import android.util.Log;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.security.auth.x500.X500Principal;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

public class SslHelper {

    private static final String TAG = "SslHelper";
    private static final String KEY_ALIAS = "remotekeyboard_tls";

    /**
     * Returns an SSLServerSocketFactory backed by a self-signed ECDSA certificate
     * stored in the Android KeyStore.  The key pair is generated on first use and
     * persisted across restarts.
     *
     * Requires API 23+.  Returns null on older devices (caller falls back to plain TCP).
     */
    public static SSLServerSocketFactory getServerSocketFactory() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(TAG, "Android < 6.0: TLS unavailable, falling back to plain socket");
            return null;
        }
        try {
            KeyStore androidKS = KeyStore.getInstance("AndroidKeyStore");
            androidKS.load(null);

            if (!androidKS.containsAlias(KEY_ALIAS)) {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
                kpg.initialize(new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setCertificateSubject(new X500Principal("CN=RemoteKeyboard"))
                        .setCertificateSerialNumber(BigInteger.ONE)
                        .setCertificateNotBefore(new Date())
                        .setCertificateNotAfter(new Date(
                                System.currentTimeMillis() + 10L * 365 * 24 * 3600 * 1000))
                        .build());
                kpg.generateKeyPair();
                Log.i(TAG, "Generated new TLS key pair in Android KeyStore");
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(androidKS, null);

            SSLContext sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(kmf.getKeyManagers(), null, null);

            Log.i(TAG, "TLS server socket factory ready");
            return sslCtx.getServerSocketFactory();

        } catch (Exception e) {
            Log.e(TAG, "Failed to set up TLS, falling back to plain socket: " + e.getMessage(), e);
            return null;
        }
    }
}
