package gov.nasa.jpl.ammos.kmc.crypto;

import java.io.InputStream;

/**
 * The IcvCreate creates an integrity check value (ICV) of the input data.  The resulting
 * ICV, its associated key, and cryptographic attributes are returned in a metadata string.
 * The integrity of the data can then be verified by the IcvVerifier using the metadata.
 * The key to be used, and the use of a KMS or keystore to get the key, is set when the
 * IcvCreator instance is created.
 *
 */
public interface IcvCreator {

    /**
     * Returns the integrity metadata for the data read from the input stream.
     * The metadata contains the integrity check value, key reference,
     * and other attributes that are used for ICV verification.
     *
     * @param inputStream input stream of the data for integrity check.
     *          The inputStream will be closed upon completion.
     * @return String of the integrity metadata.
     * @throws KmcCryptoException if error in reading the input stream.
     */
    String createIntegrityCheckValue(final InputStream inputStream) throws KmcCryptoException;

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
