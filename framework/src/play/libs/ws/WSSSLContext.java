package play.libs.ws;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

public final class WSSSLContext {

    private static final TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {
        private static final X509Certificate[] EMPTY_ACCEPTED_ISSUERS = {};

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return EMPTY_ACCEPTED_ISSUERS;
        }
    };

    private WSSSLContext() {
        throw new AssertionError("No play.libs.ws.WSSSLContext.WSSSLContext instances for you!");
    }

    public static SSLContext getSslContext(String keyStore, String keyStorePass, boolean CAValidation) {
        try {
            // Keystore
            KeyStore ks = createKeyStore(keyStore, keyStorePass);

            // SSL context
            var sslCTX = SSLContext.getInstance("TLS");
            if (CAValidation) {
                sslCTX.init(createKeyManagers(ks, keyStorePass), createTrustManagerFactory(ks).getTrustManagers(), new SecureRandom());
            } else {
                sslCTX.init(createKeyManagers(ks, keyStorePass), new TrustManager[] { TRUST_ALL_MANAGER }, new SecureRandom());
            }

            return sslCTX;
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Error creating SSL context", e);
        }
    }

    public static SslContext getNettySslContext(String keyStore, String keyStorePass, boolean CAValidation) {
        try {
            KeyStore ks = createKeyStore(keyStore, keyStorePass);

            SslContextBuilder builder = SslContextBuilder.forClient()
                    .keyManager(createKeyManagerFactory(ks, keyStorePass));
            if (CAValidation) {
                builder.trustManager(createTrustManagerFactory(ks));
            } else {
                builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Error creating SSL context", e);
        }
    }

    private static KeyStore createKeyStore(String keyStore, String keyStorePass) throws GeneralSecurityException, IOException {
        try (InputStream kss = new FileInputStream(keyStore)) {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(kss, keyStorePass.toCharArray());

            return ks;
        }
    }

    private static KeyManager[] createKeyManagers(KeyStore ks, String keyStorePass) throws GeneralSecurityException {
        return createKeyManagerFactory(ks, keyStorePass).getKeyManagers();
    }

    private static KeyManagerFactory createKeyManagerFactory(KeyStore ks, String keyStorePass)
            throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keyStorePass.toCharArray());

        return kmf;
    }

    private static TrustManagerFactory createTrustManagerFactory(KeyStore ks) throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        return tmf;
    }
}