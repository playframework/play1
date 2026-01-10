package play.libs.ws;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
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
            sslCTX.init(createKeyManagers(ks, keyStorePass), createTrustManagers(CAValidation, ks), new SecureRandom());

            return sslCTX;
        } catch (GeneralSecurityException | IOException e) {
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
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keyStorePass.toCharArray());

        return kmf.getKeyManagers();
    }

    private static TrustManager[] createTrustManagers(boolean CAValidation, KeyStore ks) throws GeneralSecurityException {
        if (CAValidation) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            return tmf.getTrustManagers();
        } else {
            return new TrustManager[] { TRUST_ALL_MANAGER };
        }
    }
}
