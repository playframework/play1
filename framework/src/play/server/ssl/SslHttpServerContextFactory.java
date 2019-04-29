package play.server.ssl;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import play.Logger;
import play.Play;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Properties;

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
            Properties p = Play.configuration;

            // Made sure play reads the properties
            // Look if we have key and cert files. If we do, we use our own keymanager
            if (Play.getFile(p.getProperty("certificate.key.file", "conf/host.key")).exists()
                    && Play.getFile(p.getProperty("certificate.file", "conf/host.cert")).exists()) {
                Security.addProvider(new BouncyCastleProvider());

                // Initialize the SSLContext to work with our key managers.
                serverContext = SSLContext.getInstance(PROTOCOL);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                tmf.init(KeyStore.getInstance(p.getProperty("trustmanager.algorithm", "JKS")));

                serverContext.init(new KeyManager[]{PEMKeyManager.instance}, tmf.getTrustManagers(), null);
            } else {
                // Try to load it from the keystore
                ks = KeyStore.getInstance(p.getProperty("keystore.algorithm", "JKS"));
                // Load the file from the conf
                char[] certificatePassword = p.getProperty("keystore.password", "secret").toCharArray();
                ks.load(new FileInputStream(Play.getFile(p.getProperty("keystore.file", "conf/certificate.jks"))),
                        certificatePassword);

                // Set up key manager factory to use our key store
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
                kmf.init(ks, certificatePassword);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                tmf.init(ks);

                // Initialize the SSLContext to work with our key managers.
                serverContext = SSLContext.getInstance(PROTOCOL);
                serverContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            }
        } catch (Exception e) {
            throw new Error("Failed to initialize the server-side SSLContext", e);
        }

        SERVER_CONTEXT = serverContext;
    }

    public static SSLContext getServerContext() {
        return SERVER_CONTEXT;
    }

    public static class PEMKeyManager extends X509ExtendedKeyManager {

        static PEMKeyManager instance = new PEMKeyManager();
        PrivateKey key;
        X509Certificate[] chain;

        public PEMKeyManager() {
            final Properties p = Play.configuration;
            String keyFile = p.getProperty("certificate.key.file", "conf/host.key");

            try (PEMParser keyReader = new PEMParser(new FileReader(Play.getFile(keyFile)))) {
                final Object object = keyReader.readObject();

                PrivateKeyInfo privateKeyInfo = null;
                if (object instanceof PrivateKeyInfo) {
                	privateKeyInfo = (PrivateKeyInfo)object;
                } else if (object instanceof PEMKeyPair) {
                	privateKeyInfo = ((PEMKeyPair)object).getPrivateKeyInfo();
                } else if (object instanceof PEMEncryptedKeyPair) {
                    PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
                            .build(Play.configuration.getProperty("certificate.password", "secret").toCharArray());
                    privateKeyInfo = ((PEMEncryptedKeyPair) object).decryptKeyPair(decProv).getPrivateKeyInfo();
                } else {
                	throw new UnsupportedOperationException("Unsupported PEM content '" + object.getClass() + "'");
                }
                key = BouncyCastleProvider.getPrivateKey(privateKeyInfo);

                final File hostCertFile = Play.getFile(p.getProperty("certificate.file", "conf/host.cert"));
                final Collection collection = new CertificateFactory().engineGenerateCertificates(new FileInputStream(hostCertFile));
                chain = (X509Certificate[]) collection.toArray(new X509Certificate[collection.size()]);
            } catch (Exception e) {
                Logger.error(e, "Failed to initialize PEMKeyManager from file %s", keyFile);
            }
        }

        @Override
        public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
            return "";
        }

        @Override
        public String[] getClientAliases(String s, Principal[] principals) {
            return new String[]{""};
        }

        @Override
        public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
            return "";
        }

        @Override
        public String[] getServerAliases(String s, Principal[] principals) {
            return new String[]{""};
        }

        @Override
        public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
            return "";
        }

        @Override
        public X509Certificate[] getCertificateChain(String s) {
            return chain;
        }

        @Override
        public PrivateKey getPrivateKey(String s) {
            return key;
        }
    }
}
