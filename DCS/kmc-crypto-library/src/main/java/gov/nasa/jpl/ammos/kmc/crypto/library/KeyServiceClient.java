package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.security.Key;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;

/**
 * The KeyServiceClient provides an interface to retrieving keys.
 *
 *
 */
public interface KeyServiceClient {
    // KMIP State (see page 119 of kmip-spec-v1.1.pdf)
    String PRE_ACTIVE = "PreActive";
    String ACTIVE = "Active";
    String DEACTIVATED = "Deactivated";
    String COMPROMISED = "Compromised";
    String DESTROYED = "Destroyed";
    String DESTROYED_COMPROMISED = "DestroyedCompromised";

    // KMIP Usage Mask (see page 126 of kmip-spec-v1.1.pdf)
    int USAGE_MASK_SIGN = 0x01;
    int USAGE_MASK_VERIFY = 0x02;
    int USAGE_MASK_ENCRYPT = 0x04;
    int USAGE_MASK_DECRYPT = 0x08;
    int USAGE_MASK_MAC_GENERATE = 0x0080;
    int USAGE_MASK_MAC_VERIFY = 0x0100;

    // KMC Usage Mask
    int USAGE_MASK_ENCRYPTION = USAGE_MASK_ENCRYPT | USAGE_MASK_DECRYPT;
    int USAGE_MASK_ICV = USAGE_MASK_MAC_GENERATE | USAGE_MASK_MAC_VERIFY;
    int USAGE_MASK_SIGNATURE = USAGE_MASK_SIGN | USAGE_MASK_VERIFY;
    int USAGE_MASK_PUBLIC_KEY = USAGE_MASK_VERIFY | USAGE_MASK_ENCRYPT;
    int USAGE_MASK_PRIVATE_KEY = USAGE_MASK_SIGN | USAGE_MASK_DECRYPT;
    int USAGE_MASK_SYMMETRIC_CRYPTO = USAGE_MASK_ENCRYPTION | USAGE_MASK_ICV;
    int USAGE_MASK_ASYMMETRIC_CRYPTO = USAGE_MASK_ENCRYPTION | USAGE_MASK_SIGNATURE;

    /**
     * Returns the KMC key object retrieved from keystore or KMS of the specified keyRef.
     * The KMC key object contains the key material as well as attributes of the key.
     * @param keyRef The key reference of the key.
     * @return the retrieved key.
     * @throws KmcCryptoException if key not found or any error occurred during retrieving the key from keystore or KMS.
     */
    KmcKey getKmcKey(final String keyRef) throws KmcCryptoException;

    /**
     * Returns the KMC key object retrieved from a keystore of the specified keyRef.
     * The KMC key object contains the key material as well as attributes of the key.
     * @param keyRef The key reference of the key.
     * @param keyPass The password of the key.  Pass in null for no key password.
     * @return the retrieved key.
     * @throws KmcCryptoException if key not found or any error occurred during retrieving the key from KMS.
     */
    KmcKey getKmcKey(final String keyRef, final String keyPassword) throws KmcCryptoException;

    /**
     * Returns the Java key object retrieved from keystore or KMS using the specified keyRef and keyUsage.
     * The result could be a Java SecretKey for symmetric encryption/decryption and CMAC/HMAC ICV,
     * PublicKey for RSA encryption and digital signature verification, or
     * PrivateKey for RSA decryption and digital signature signing.
     * The value of keyUsage follows the KMIP protocol specification.
     * @param keyRef The key reference of the key.
     * @param keyUsage The usage of the secret key.
     * @return the retrieved key.
     * @throws KmcCryptoException if key not found or any error occurred during retrieving the key from KMS.
     */
    Key getCryptoKey(final String keyRef, final int keyUsage) throws KmcCryptoException;

    /**
     * Returns the Java key object retrieved from a keystore using the specified keyRef,
     * keyPass, and keyUsage.
     * The result could be a Java SecretKey for symmetric encryption/decryption and CMAC/HMAC ICV,
     * PublicKey for RSA encryption and digital signature verification, or
     * PrivateKey for RSA decryption and digital signature signing.
     * @param keyRef The key reference (i.e. alias) of the key.
     * @param keyPass The password associated with the key.
     * @param keyUsage The usage of the key.
     * @return the key retrieved from keystore.
     * @throws KmcCryptoException if any error occurred during retrieving the key from keystore.
     */
    Key getCryptoKey(final String keyRef, final String keyPass, final int keyUsage) throws KmcCryptoException;

}
