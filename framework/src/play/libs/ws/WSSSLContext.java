package play.libs.ws;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class WSSSLContext {
    public static SSLContext getSslContext(String keyStore, String keyStorePass, Boolean CAValidation) {
        SSLContext sslCTX = null;

        try {
            // Keystore
            InputStream kss = new FileInputStream(keyStore);
            char[] storePass = keyStorePass.toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(kss, storePass);

            // Keymanager
            char[] certPwd = keyStorePass.toCharArray();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, certPwd);
            KeyManager[] keyManagers = kmf.getKeyManagers();

            // Trustmanager
            TrustManager[] trustManagers = null;
            if (CAValidation == true) {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ks);
                trustManagers = tmf.getTrustManagers();
            } else {
                trustManagers = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                        }
                };
            }

            SecureRandom secureRandom = new SecureRandom();

            // SSL context
            sslCTX = SSLContext.getInstance("TLS");
            sslCTX.init(keyManagers, trustManagers, secureRandom);
        } catch (Exception e) {
            throw new RuntimeException("Error setting SSL context " + e.toString());
        }
        return sslCTX;
    }
}
