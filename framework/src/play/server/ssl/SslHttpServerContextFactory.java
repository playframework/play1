package play.server.ssl;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import play.Logger;
import play.Play;

import java.security.cert.X509Certificate;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.util.Enumeration;

public class SslHttpServerContextFactory {

    private static final String PROTOCOL = "SSL";
    private static final SSLContext SERVER_CONTEXT;

    static {
        
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        SSLContext serverContext = null;
        KeyStore ks = null;
        try {

            // Look if we have key and cert files. If we do, we use our own keymanager
            if (Play.getFile(System.getProperty("certificate.file", "conf/host.key")).exists() && Play.getFile(System.getProperty("certificate.file", "conf/host.cert")).exists()) {
                Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            } else {
                // Try to load it from the keystore
                ks = KeyStore.getInstance(System.getProperty("certificate.algorithm", "JKS"));
                // Load the file from the conf
                char[] certificatePassword = System.getProperty("certificate.password", "secret").toCharArray();

                ks.load(new FileInputStream(Play.getFile(System.getProperty("certificate.file", "conf/certificate.jks"))),
                        certificatePassword);

                // Set up key manager factory to use our key store
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
                char[] keyStorePassword = System.getProperty("keystore.password", "secret").toCharArray();

                kmf.init(ks, keyStorePassword);

            }
            // Initialize the SSLContext to work with our key managers.
            serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(new KeyManager[]{PEMKeyManager.instance}, null, null);

        } catch (Exception e) {
            throw new Error(
                    "Failed to initialize the server-side SSLContext", e);
        }

        SERVER_CONTEXT = serverContext;
    }

    public static SSLContext getServerContext() {
        return SERVER_CONTEXT;
    }

    public static class PEMKeyManager extends X509ExtendedKeyManager {

        static PEMKeyManager instance = new PEMKeyManager();
        PrivateKey key;
        X509Certificate cert;

        public PEMKeyManager() {
            try {
                PEMReader keyReader = new PEMReader(new FileReader(Play.getFile(System.getProperty("certificate.file", "conf/host.key"))), new PasswordFinder() {
                    public char[] getPassword() {
                        return System.getProperty("certificate.password", "secret").toCharArray();
                    }
                });
                key = ((KeyPair) keyReader.readObject()).getPrivate();

                PEMReader reader = new PEMReader(new FileReader(Play.getFile(System.getProperty("certificate.file", "conf/host.cert"))));
                cert = (X509Certificate) reader.readObject();
            } catch (Exception e) {
                e.printStackTrace();
                Logger.error(e, "");
            }
        }

        public String chooseEngineServerAlias(java.lang.String s, java.security.Principal[] principals, javax.net.ssl.SSLEngine sslEngine) {
            return "";
        }


        public String[] getClientAliases(String s, Principal[] principals) {
            return new String[]{""};
        }

        public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
            return "";
        }

        public String[] getServerAliases(String s, Principal[] principals) {
            return new String[]{""};
        }

        public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
            return "";
        }

        public java.security.cert.X509Certificate[] getCertificateChain(String s) {
            return new java.security.cert.X509Certificate[]{cert};
        }

        public PrivateKey getPrivateKey(String s) {
            return key;
        }
    }

}
