package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;

/**
 * The KeystoreKeyServiceClient retrieves keys from the keystore.
 *
 *
 */
public class KeystoreKeyServiceClient implements KeyServiceClient {

    public static final String KEYSTORE_TYPE_JKS = "JKS";
    public static final String KEYSTORE_TYPE_JCEKS = "JCEKS";
    public static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";
    public static final String KEYSTORE_TYPE_BCFKS = "BCFKS";

    private final String keystoreLocation;
    private final String keystorePass;
    private KeyStore keystore;
    private InputStream keystoreStream;

    private static final Logger logger = LoggerFactory.getLogger(KeystoreKeyServiceClient.class);

    /**
     * Constructor of KeystoreKeyServiceClient.
     *
     * @param keystoreLocation The path to the location of the keystore.
     * @param keystorePass The password of the keystore.
     * @param keystoreType The keystore type.
     * @throws KmcCryptoException if error in loading keystore.
     *
     */
    public KeystoreKeyServiceClient(final String keystoreLocation, final String keystorePass,
            final String keystoreType) throws KmcCryptoException {
        this.keystoreLocation = keystoreLocation;
        if (keystorePass == null) {
            this.keystorePass = "changeit";
        } else {
            this.keystorePass = keystorePass;
        }

        try {
            keystoreStream = new FileInputStream(keystoreLocation);
            logger.debug("Load keystore " + keystoreLocation);
        } catch (FileNotFoundException e) {
            keystoreStream = getClass().getResourceAsStream(keystoreLocation);
            if (keystoreStream == null) {
                String msg = "Failed to open keystore from " + keystoreLocation + " and from CLASSPATH: " + e;
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
            }
            logger.info("Load keystore " + keystoreLocation + " from CLASSPATH");
        }
        try {
            if (KEYSTORE_TYPE_BCFKS.equals(keystoreType)) {
                Provider provider = new BouncyCastleFipsProvider();
                Security.addProvider(provider);
                keystore = KeyStore.getInstance(keystoreType, provider.getName());
                logger.debug("Opened keystore of type {} with provider {}", keystoreType, provider.getName());
            } else if (KEYSTORE_TYPE_PKCS12.equals(keystoreType) ||
                       KEYSTORE_TYPE_JCEKS.equals(keystoreType) ||
                       KEYSTORE_TYPE_JKS.equals(keystoreType)) {
                keystore = KeyStore.getInstance(keystoreType);
                logger.debug("Opened {} keystore", keystoreType);
            } else {
                String msg = "Unsupported keystore type: " + keystoreType + ", support only BCFKS, PKCS12, and JKS keystores";
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
            }
            keystore.load(keystoreStream, this.keystorePass.toCharArray());
        } catch (KeyStoreException e) {
            String msg = "Exception in getting instance of keystore of type: " + keystoreType + ": " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        } catch (NoSuchProviderException e) {
            String msg = "Exception in creating BCFIPS provider: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        } catch (NoSuchAlgorithmException e) {
            String msg = "Exception in loading " + keystoreLocation + ": " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        } catch (CertificateException e) {
            String msg = "Exception in loading " + keystoreLocation + ": " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        } catch (IOException e) {
            String msg = "Exception in loading " + keystoreLocation + ": " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        } finally {
            try {
                keystoreStream.close();
            } catch (IOException e) {
                logger.error("Exception in closing keystore {}: {}", keystoreLocation, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getKmcKey(final String keyRef) throws KmcCryptoException {
        return getKmcKey(keyRef, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getKmcKey(final String keyRef, final String keyPass) throws KmcCryptoException {
        int keyUsage = 0x01FF;    // all usage: encryption, ICV, digital signature
        Key javaKey = getCryptoKey(keyRef, keyPass, keyUsage);
        KmcKey kmcKey = new KmcKey(keyRef, javaKey);
        return kmcKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Key getCryptoKey(final String keyRef, final int keyUsage)
            throws KmcCryptoException {
        Key key = getKeyFromKeystore(keyRef, null);
        if (key instanceof PrivateKey) {
            if ((keyUsage & USAGE_MASK_ENCRYPT) == USAGE_MASK_ENCRYPT) {
                return getPublicKey(keyRef, null);
            } else if ((keyUsage & USAGE_MASK_VERIFY) == USAGE_MASK_VERIFY) {
                return getPublicKey(keyRef, null);
            }
        }
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Key getCryptoKey(final String keyRef, final String keyPass, final int keyUsage)
            throws KmcCryptoException {
        Key key = getKeyFromKeystore(keyRef, keyPass);
        if (key instanceof PrivateKey) {
            if ((keyUsage & USAGE_MASK_ENCRYPT) == USAGE_MASK_ENCRYPT) {
                return getPublicKey(keyRef, keyPass);
            } else if ((keyUsage & USAGE_MASK_VERIFY) == USAGE_MASK_VERIFY) {
                return getPublicKey(keyRef, keyPass);
            }
        }
        return key;
    }

    /**
     * Retrieves a key from the keystore with the specified keyRef and keyPass.
     * @param keyRef The keyRef (also known as alias) of the key.
     * @param keyPass The password of the key, null if no key password.
     * @return A Java Key object.
     * @throws KmcCryptoException if any error occurred in retrieving the key.
     */
    private Key getKeyFromKeystore(final String keyRef, final String keyPass)
                throws KmcCryptoException {
        try {
            if (!keystore.containsAlias(keyRef)) {
                String msg = "Cryptographic key " + keyRef + " does not exist in keystore: " + keystoreLocation;
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
            }
        } catch (KeyStoreException e) {
            String msg = "Exception in retrieving keystore: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        }
        try {
            Key key = null;
            if (keyPass == null) {
                key = keystore.getKey(keyRef, keystorePass.toCharArray());
            } else {
                key = keystore.getKey(keyRef, keyPass.toCharArray());
            }
            // BCFKS returns null when key is not found
            if (key == null) {
                String msg = "Retrieved null for key " + keyRef + " from keystore " + keystoreLocation;
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
            }
            logger.info("Retrieved key {} from keystore {}", keyRef, keystoreLocation);
            return key;
        } catch (UnrecoverableKeyException e) {
            String msg = "Exception in retrieving key " + keyRef + " from keystore " + keystoreLocation + ": " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        } catch (KeyStoreException e) {
            String msg = "Exception in retrieving key " + keyRef + " from keystore " + keystoreLocation + ": " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        } catch (NoSuchAlgorithmException e) {
            String msg = "Exception in retrieving key " + keyRef + " from keystore " + keystoreLocation + ": " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        }
    }

    /**
     * Retrieves the public key of a key pair from the keystore with the specified keyRef and keyPass.
     * @param keyRef The keyRef (also known as alias) of the key.
     * @param keyPass The password of the key, null if no key password.
     * @return A Java PublicKey object.
     * @throws KmcCryptoException if error in retrieving the key.
     */
    private PublicKey getPublicKey(final String keyRef, final String keyPass)
            throws KmcCryptoException {
        Certificate cert = null;
        try {
            cert = keystore.getCertificate(keyRef);
        } catch (KeyStoreException e) {
            String msg = "Can't find public key for " + keyRef + ": " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        }
        return cert.getPublicKey();
    }

}
