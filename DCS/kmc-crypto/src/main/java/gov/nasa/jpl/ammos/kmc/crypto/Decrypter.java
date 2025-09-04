package gov.nasa.jpl.ammos.kmc.crypto;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * The Decrypter performs decryption of the cipher text based on its associated metadata.
 * The cryptographic key to be used to perform decryption is obtained either from a KMS or a keystore,
 * which is set when the Decrypter instance is created.
 *
 */
public interface Decrypter {

    /**
     * Decrypts the encrypted data read from the input stream with the resulting decrypted data written to the output stream.
     *
     * @param inputStream input stream attached to the data source to be decrypted.
     *          The inputStream will be closed upon completion.
     * @param outputStream output stream attached to the data sink for decrypted data.
     *          The outputStream will be closed upon completion.
     * @param metadata String containing the metadata to be used for decrypting the encrypted data.
     * @exception KmcCryptoException if any error occurs during encryption.
     * @throws KmcCryptoException if any error occurs.
     */
    void decrypt(final InputStream inputStream, final OutputStream outputStream, final String metadata)
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
