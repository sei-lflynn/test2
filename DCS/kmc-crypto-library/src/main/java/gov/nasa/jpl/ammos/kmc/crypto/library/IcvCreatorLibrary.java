package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.IcvCreator;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;

/**
 * This class implements IcvCreator for creating integrity check value from input data.
 *
 */
public class IcvCreatorLibrary implements IcvCreator {
    private static final int BUFFER_SIZE = 1024;
    private static final int BYTE_SIZE = 8;

    // Message Digest for integrity check
    private MessageDigest mdIcv;
    // Message Authentication Code for integrity check
    private Mac macIcv;
    // Digital Signature for integrity check
    private Signature dsIcv;

    private final KmcCryptoManager cryptoManager;
    private KeyServiceClient keyClient;

    private String keyRef;
    private String algorithm;
    private String provider;
    private IntegrityCheckMetadata metadata;

    private static final Logger logger = LoggerFactory.getLogger(IcvCreatorLibrary.class);
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    /**
     * Constructor of the {@link IcvCreator} implementation that uses the Message Digest algorithm for creating ICV.
     * To specify a particular algorithm other than the default, set the default Message Digest algorithm in
     * the KmcCryptoManager prior to calling this constructor.
     *
     * @param cryptoManager The KmcCryptoManager for accessing the configuration parameters.
     * @throws KmcCryptoException if the default Message Digest algorithm is invalid.
     */
    public IcvCreatorLibrary(final KmcCryptoManager cryptoManager) throws KmcCryptoException {
        this.cryptoManager = cryptoManager;
        mdIcv = createMessageDigest();
        audit.info("IcvCreatorLibrary: User created local-library ICV Creator that uses Message Digest algorithm");
    }

    /**
     * Constructor of the {@link IcvCreator} implementation that uses a crypto key for creating ICV.
     * The cryptographic key is retrieved from the keystore or Key Management Service (KMS).
     * If the retrieved key is a symmetric key a Message Authentication Code (MAC)
     * will be created.  If the retrieved key is an asymmetric key a Digital Signature
     * will be created.
     * <p>
     * The HMAC algorithm to be used for MAC generation is determined by the retrieved symmetric key.
     * However, the digital signature algorithm is not determined by the key.
     * To specify a particular algorithm other than the default, set the default digital signature
     * algorithm in the KmcCryptoManager prior to calling this constructor.
     * </p>
     * @param cryptoManager The KmcCryptoManager for accessing the configuration parameters.
     * @param keyRef A string for identifying the key, i.e. the name of the key.
     * @throws KmcCryptoException if error in retrieving the key.
     */
    public IcvCreatorLibrary(final KmcCryptoManager cryptoManager, final String keyRef) throws KmcCryptoException {
        this.cryptoManager = cryptoManager;

        if (keyRef == null) {
            String msg = "Null keyRef.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        } else if ("null".equals(keyRef)) {
            mdIcv = createMessageDigest();
            return;
        }
        this.keyRef = keyRef;

        keyClient = new KmcKeyServiceClient(this.cryptoManager);
        Key key;
        try {
            key = keyClient.getCryptoKey(keyRef,
                    KeyServiceClient.USAGE_MASK_MAC_GENERATE | KeyServiceClient.USAGE_MASK_SIGN);
        } catch (KmcCryptoException e) {
            audit.info("IcvCreatorLibrary: Failed to obtain key " + keyRef + " from keystore or KMS for ICV creation.");
            throw e;
        }

        if (key instanceof SecretKey) {
            macIcv = createMac(key);
        } else if (key instanceof RSAPrivateKey) {
            if (cryptoManager.getMacLength() != -1) {
                String msg = "Digital Signature does not support macLength.";
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
            }
            dsIcv = createDigitalSignature((PrivateKey) key);
        } else {
            String msg = "The retrieved key (keyRef = " + keyRef + ") is not a symmetric key or RSA private key.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
        }
        audit.info("IcvCreatorLibrary: User created local-library ICV Creator that uses KMS at "
                + cryptoManager.getKeyManagementServiceURI());
        audit.info("IcvCreatorLibrary: User obtained key " + keyRef + " from keystore or KMS for ICV creation.");
    }

    /**
     * Constructor of the IcvCreator implementation that used HMAC algorithm for creating ICV.
     * During construction of the instance the symmetric key as specified in keyRef is retrieved
     * from the specified keystore.  However, the retrieved key does not determine the HMAC algorithm.
     * To specify a particular algorithm other than the default, set the HMAC algorithm
     * in the KmcCryptoManager prior to calling this constructor.
     * <p>
     * The keystorePass or keyPass can be null if not used by the keystore.
     * All keys in the keystore must have the same key password (keyPass) if a key password is used.
     * </p>
     * @param cryptoManager The KmcCryptoManager for accessing the configuration parameters.
     * @param keystoreLocation The path to the location of the keystore.
     * @param keystorePass The password of the keystore.
     * @param keystoreType The keystore type.
     * @param keyRef A string for identifying the key, also known as alias.
     * @param keyPass The password of the key to be retrieved.
     * @throws KmcCryptoException if error in retrieving the key.
     */
    public IcvCreatorLibrary(final KmcCryptoManager cryptoManager,
            final String keystoreLocation, final String keystorePass,
            final String keystoreType, final String keyRef, final String keyPass)
                    throws KmcCryptoException {
        this.cryptoManager = cryptoManager;
        this.keyRef = keyRef;

        if (keyRef == null) {
            String msg = "Null keyRef.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        } else if ("null".equals(keyRef)) {
            mdIcv = createMessageDigest();
            return;
        }

        KeystoreKeyServiceClient keystoreClient = new KeystoreKeyServiceClient(
                keystoreLocation, keystorePass, keystoreType);
        Key key;
        try {
            key = keystoreClient.getCryptoKey(keyRef, keyPass,
                    KeyServiceClient.USAGE_MASK_MAC_GENERATE | KeyServiceClient.USAGE_MASK_SIGN);
        } catch (KmcCryptoException e) {
            audit.info("IcvCreatorLibrary: User failed to obtain key " + keyRef + " from keystore " + keystoreLocation + " for ICV creation.");
            throw e;
        }
        if (key instanceof SecretKey) {
            macIcv = createMac(key);
        } else if (key instanceof RSAPrivateKey) {
            dsIcv = createDigitalSignature((PrivateKey) key);
        } else {
            String msg = "The retrieved key (keyRef = " + keyRef + ") is not a symmetric key or RSA private key.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
        }
        audit.info("IcvCreatorLibrary: User created local-library ICV Creator that uses keystore in " + keystoreLocation);
        audit.info("IcvCreatorLibrary: User obtained key " + keyRef + " from keystore for ICV creation.");
    }

    /**
     * This constructor is for KMC internal use.
     * This IcvCreator is only used by IcvVerifier to generate the ICV for verification.
     *
     * @param cryptoManager The KmcCryptoManager for accessing the configuration parameters.
     * @param metadata The metadata that is used for ICV verification.
     * @param verify This boolean must be true, otherwise exception is thrown.
     * @throws KmcCryptoException if error in retrieving the key.
     */
    IcvCreatorLibrary(final KmcCryptoManager cryptoManager, final IntegrityCheckMetadata metadata, final boolean verify)
            throws KmcCryptoException {
        this.cryptoManager = cryptoManager;
        this.metadata = metadata;

        if (!verify) {
            String msg = "This constructor can only be used for ICV verification.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }

        keyRef = metadata.getKeyRef();
        if (keyRef == null || keyRef.equals("null")) {
            mdIcv = createMessageDigest();
            return;
        }

        KeyServiceClient keyService = new KmcKeyServiceClient(cryptoManager);
        Key key;
        try {
            key = keyService.getCryptoKey(keyRef, KeyServiceClient.USAGE_MASK_MAC_VERIFY);
        } catch (KmcCryptoException e) {
            audit.info("IcvVerifyLibrary: User failed to obtain key " + keyRef + " from KMS for ICV verify.");
            throw e;
        }
        if (key instanceof SecretKey) {
            macIcv = createMac(key);
        } else {
            String msg = "The retrieved key (keyRef = " + keyRef + ") is not a symmetric key or RSA private key.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
        }
        audit.info("IcvVerifyLibrary: User obtained key " + keyRef + " from KMS for ICV verify.");
    }

    /**
     * Constructor of the IcvCreator implementation that used HMAC algorithm for creating ICV.
     * During construction of the instance the symmetric key as specified in keyRef is retrieved
     * from the specified keystore.  However, the retrieved key does not determine the HMAC algorithm.
     * To specify a particular algorithm other than the default, set the HMAC algorithm
     * in the KmcCryptoManager prior to calling this constructor.
     * <p/>
     * The keystorePass or keyPass can be null if not used by the keystore.
     * All keys in the keystore must have the same key password (keyPass) if a key password is used.
     *
     * @param cryptoManager The KmcCryptoManager for accessing the configuration parameters.
     * @param keystoreLocation The path to the location of the keystore.
     * @param keystorePass The password of the keystore.
     * @param keystoreType The keystore type.
     * @param keyPass The password of the key to be retrieved.
     * @param metadata The metadata that is used for ICV verification.
     * @param verify This boolean must be true, otherwise exception is thrown.
     * @throws KmcCryptoException if error in retrieving the key.
     */
    IcvCreatorLibrary(final KmcCryptoManager cryptoManager,
            final String keystoreLocation, final String keystorePass,
            final String keystoreType, final String keyPass,
            final IntegrityCheckMetadata metadata, final boolean verify)
                    throws KmcCryptoException {
        this.cryptoManager = cryptoManager;
        this.metadata = metadata;
        keyRef = metadata.getKeyRef();

        if (!verify) {
            String msg = "This constructor can only be used for ICV verification.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }

        if (keyRef == null) {
            String msg = "Null keyRef.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        } else if ("null".equals(keyRef)) {
            mdIcv = createMessageDigest();
            return;
        }

        KeystoreKeyServiceClient keystoreClient = new KeystoreKeyServiceClient(
                keystoreLocation, keystorePass, keystoreType);
        Key key;
        try {
            key = keystoreClient.getCryptoKey(keyRef, keyPass, KeyServiceClient.USAGE_MASK_MAC_VERIFY);
            audit.info("IcvVerifyLibrary: User obtained key " + keyRef + " from keystore for ICV verification.");
        } catch (KmcCryptoException e) {
            audit.info("IcvVerifyLibrary: User failed to obtain key " + keyRef + " from keystore for ICV verification.");
            throw e;
        }
        if (key instanceof SecretKey) {
            macIcv = createMac(key);
        } else {
            String msg = "The retrieved key (keyRef = " + keyRef + ") is not a symmetric key.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
        }
    }

    private MessageDigest createMessageDigest() throws KmcCryptoException {
        if (metadata == null) {
            algorithm = cryptoManager.getMessageDigestAlgorithm();
        } else {
            algorithm = metadata.getCryptoAlgorithm();
        }

        provider = cryptoManager.getAlgorithmProvider(algorithm);
        if (provider != null) {
            try {
                logger.info("Message Digest algorithm " + algorithm + " provider = " + provider);
                String className = cryptoManager.getProviderClass(provider);
                if (className != null) {
                    logger.debug("Class name for provider " + provider + " = " + className);
                    CryptoLibraryUtilities.addCryptoProvider(className);
                }
            } catch (KmcCryptoException e) {
                throw e;
            }
        }
        try {
            if (provider == null) {
                return MessageDigest.getInstance(algorithm);
            } else {
                logger.info("Provider " + provider + " is used for Message Digest algorithm " + algorithm);
                return MessageDigest.getInstance(algorithm, provider);
            }
        } catch (NoSuchProviderException e) {
            String msg = "Invalid provider " + provider + " for Message Digest algorithm " + algorithm;
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } catch (NoSuchAlgorithmException e) {
            String msg;
            if (provider == null) {
                msg = "Invalid Message Digest algorithm " + algorithm;
            } else {
                msg = "Invalid Message Digest provider " + provider + " or algorithm " + algorithm;
            }
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        }
    }

    /**
     * Creates MAC with the key.
     * @param key The key used for HMAC or CMAC.
     * @throws KmcCryptoException if error occurs creating MAC.
     */
    private Mac createMac(final Key key) throws KmcCryptoException {
        if (metadata == null) {
            String keyAlgorithm = key.getAlgorithm();
            if ("AES".equals(keyAlgorithm) || "DESede".equals(keyAlgorithm)) {
                algorithm = keyAlgorithm + "CMAC";
            } else {
                algorithm = getHmacAlgorithm(keyAlgorithm);
            }
        } else {
            algorithm = metadata.getCryptoAlgorithm();
        }
        logger.info("MAC algorithm = " + algorithm
                + ", key length = " + key.getEncoded().length * BYTE_SIZE);

        provider = cryptoManager.getAlgorithmProvider(algorithm);
        if (provider != null) {
            try {
                logger.info("MAC algorithm " + algorithm + " provider = " + provider);
                String className = cryptoManager.getProviderClass(provider);
                if (className != null) {
                    logger.debug("Class name for provider " + provider + " = " + className);
                    CryptoLibraryUtilities.addCryptoProvider(className);
                }
            } catch (KmcCryptoException e) {
                throw e;
            }
        }

        Mac mac;
        try {
            if (provider == null) {
                mac = Mac.getInstance(algorithm);
            } else {
                mac = Mac.getInstance(algorithm, provider);
            }
        } catch (NoSuchAlgorithmException e) {
            String msg;
            if (provider == null) {
                msg = "Invalid Mac algorithm " + algorithm;
            } else {
                msg = "Invalid Mac provider " + provider + " or algorithm " + algorithm;
            }
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } catch (NoSuchProviderException e) {
            String msg = "Invalid provider " + provider + " for MAC algorithm " + algorithm;
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        }

        try {
            mac.init(key);
        } catch (InvalidKeyException e) {
            String message = "Exception occurred in initializing Mac: " + e;
            logger.error(message);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, message, e);
        } catch (IllegalArgumentException e) {
            // when ICV algorithm is DESede but key is AES
            String message = "Exception occurred in initializing Mac: " + e;
            logger.error(message);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, message, e);
        }

        return mac;
    }

    private Signature createDigitalSignature(final PrivateKey privateKey)
            throws KmcCryptoException {
        if (metadata == null) {
            algorithm = cryptoManager.getDefaultDigitalSignatureAlgorithm();
        } else {
            algorithm = metadata.getCryptoAlgorithm();
        }

        provider = cryptoManager.getAlgorithmProvider(algorithm);
        if (provider != null) {
            try {
                logger.info("Digital Signature algorithm " + algorithm + " provider = " + provider);
                String className = cryptoManager.getProviderClass(provider);
                if (className != null) {
                    logger.debug("Class name for provider " + provider + " = " + className);
                    CryptoLibraryUtilities.addCryptoProvider(className);
                }
            } catch (KmcCryptoException e) {
                throw e;
            }
        }

        Signature signature;
        try {
            if (provider == null) {
                signature = Signature.getInstance(algorithm);
            } else {
                logger.info("Provider " + provider + " is used for Digital Signature algorithm " + algorithm);
                signature = Signature.getInstance(algorithm, provider);
            }
        } catch (NoSuchAlgorithmException e) {
            String msg;
            if (provider == null) {
                msg = "Invalid Digital Signature algorithm " + algorithm;
            } else {
                msg = "Invalid Digital Signature provider " + provider + " or algorithm " + algorithm;
            }
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } catch (NoSuchProviderException e) {
            String msg = "Invalid provider " + provider + " for Digital Signature algorithm " + algorithm;
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        }
        try {
            signature.initSign(privateKey);
        } catch (InvalidKeyException e) {
            String msg = "Exception in initializing Signature";
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        }
        return signature;
    }

    @Override
    public final String createIntegrityCheckValue(final InputStream inputStream) throws KmcCryptoException {
        if (inputStream == null) {
            String msg = "Null input stream.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }

        byte[] icvBytes;
        byte[] data = new byte[BUFFER_SIZE];
        try {
            // Not to accept empty input so that it's consistent with Encrypter.
            if (inputStream.available() == 0) {
                String msg = "Input stream cannot be empty.";
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
            }
            int totalBytes = 0;
            while (true) {
                if (totalBytes > KmcCryptoManager.MAX_CRYPTO_SIZE) {
                    String msg = "Inupt stream exceeds maximum size of " + KmcCryptoManager.MAX_CRYPTO_SIZE + " bytes.";
                    logger.error(msg);
                    throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
                }
                int nData = inputStream.read(data);
                if (nData == -1) {
                    break;
                }
                logger.trace("createIntegrityCheckValue() read " + nData + " bytes.");
                if (mdIcv != null) {
                    mdIcv.update(data, 0, nData);
                } else if (macIcv != null) {
                    macIcv.update(data, 0, nData);
                } else {
                    dsIcv.update(data, 0, nData);
                }
                totalBytes = totalBytes + nData;
            }
            logger.info("createIntegrityCheckValue() total number of bytes in data = " + totalBytes);
            if (mdIcv != null) {
                icvBytes = mdIcv.digest();
            } else if (macIcv != null) {
                icvBytes = macIcv.doFinal();
            } else {
                icvBytes = dsIcv.sign();
            }
            audit.info("IcvCreatorLibrary: User created ICV for " + totalBytes + " bytes of data using algorithm " + algorithm);
        } catch (SignatureException e) {
            audit.info("IcvCreatorLibrary: User failed to create ICV using algorithm " + algorithm);
            String msg = "Exception in generating signature: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } catch (IOException e) {
            audit.info("IcvCreatorLibrary: User failed to create ICV using algorithm " + algorithm);
            String msg = "Exception in reading the input stream: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } finally {
            closeStream(inputStream);
        }

        int macLength = cryptoManager.getMacLength();
        if (macLength > icvBytes.length * 8) {
            String error = "Requested MAC length (" + macLength
                    + ") is longer than the full MAC length (" + icvBytes.length * 8 + ")";
            logger.error(error);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, error, null);
        }

        IntegrityCheckMetadata icvMetadata;
        if (mdIcv != null) {
            icvMetadata = new IntegrityCheckMetadata("null", algorithm, macLength, icvBytes);
        } else {
            icvMetadata = new IntegrityCheckMetadata(keyRef, algorithm, macLength, icvBytes);
        }
        logger.info("createIntegrityCheckValue() metadata = {}", icvMetadata);
        return icvMetadata.toString();
    }

    private void closeStream(final Closeable stream) {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e) {
            logger.error("Failed to close a stream in the finally block: " + e);
        }
    }

    /**
     * Returns the Java HMAC  algorithm name from the KMIP name.
     * @param keyAlgorithmName The KMIP HMAC name.
     * @return The Java HMAC algorithm name.
     */
    private String getHmacAlgorithm(final String keyAlgorithmName) {
        // KMIP key
        if ("HMAC_SHA1".equals(keyAlgorithmName)) {
            return "HmacSHA1";
        } else if ("HMAC_SHA256".equals(keyAlgorithmName)) {
            return "HmacSHA256";
        } else if ("HMAC_SHA384".equals(keyAlgorithmName)) {
            return "HmacSHA384";
        } else if ("HMAC_SHA512".equals(keyAlgorithmName)) {
            return "HmacSHA512";
        }
        // Java key returns OID.  See http://oid-info.com/get/1.2.840.113549.2
        if ("1.2.840.113549.2.7".equals(keyAlgorithmName)) {
            return "HmacSHA1";
        } else if ("1.2.840.113549.2.9".equals(keyAlgorithmName)) {
            return "HmacSHA256";
        } else if ("1.2.840.113549.2.10".equals(keyAlgorithmName)) {
            return "HmacSHA384";
        } else if ("1.2.840.113549.2.11".equals(keyAlgorithmName)) {
            return "HmacSHA512";
        }
        // Unexpected - return back the keyAlgorithmName.
        return keyAlgorithmName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadCryptoKey(final String keyRef) throws KmcCryptoException {
        if (keyClient == null) {
            String msg = "IcvCreator that uses keystore does not retrieve keys.";
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_MISC_ERROR, msg, null);
        }
        // get the key and cache it
        keyClient.getKmcKey(keyRef);
        logger.info("IcvCreatorLibrary: User loaded crypto key " + keyRef);
        audit.info("IcvCreatorLibrary: User loaded crypto key " + keyRef);
    }

}
