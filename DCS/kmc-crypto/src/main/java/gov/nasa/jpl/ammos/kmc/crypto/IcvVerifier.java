package gov.nasa.jpl.ammos.kmc.crypto;

import java.io.InputStream;

/**
 * The IcvVerifier checks the integrity of the input data based on its associated metadata.
 * The metadata contains the integrity check value, cryptographic key (for keyed algorithms), and
 * the other attributes that provides all the needed information to perform verification.
 *
 * A cryptographic key is needed to perform HMAC verification.  They key is obtained either
 * from a KMS or a keystore, which is set when the IcvVerifier instance is created.
 *
 */
public interface IcvVerifier {

    /**
     * Verifies the input data read from the input stream against the integrity check value.
     *
     * @param inputStream input stream for data to be verified against the integrity check metadata.
     *          The inputStream will be closed upon completion.
     * @param integrityMetadata metadata containing integrity check value and its associated
     *        key and cryptographic attributes for verification.
     * @return true if verification is successful.
     * @throws KmcCryptoException if error in verifying the input data.
     */
    boolean verifyIntegrityCheckValue(final InputStream inputStream, final String integrityMetadata)
            throws KmcCryptoException;


    /**
     * Load the cryptographic key to the key cache.  The key will be refreshed if it is already existed in the cache.
     * The key is also cached when it is used by cryptographic functions if key caching is enabled.
     * Having the key in the cache allows the key to be available even if KMS is not accessible in performing cryptographic functions.
     *
     * @param keyRef The key reference of the key.
     * @throws KmcCryptoException if
     */
    void loadCryptoKey(String keyRef) throws KmcCryptoException;

}
