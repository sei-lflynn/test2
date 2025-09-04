package gov.nasa.jpl.ammos.kmc.crypto;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * The Encrypter performs encryption using the key retrieved from KMS or from a keystore.
 * The key determines the encryption algorithm, key length, and encryption type
 * (symmetric or asymmetric) to be performed. The key to be used, and the use of
 * a KMS or keystore to get the key, is set when the Encrypter instance is created.
 *
 */
public interface Encrypter {

    /**
     * Encrypts the data read from input stream with the resulting encrypted data written to the output stream.
     *
     * @param inputStream input stream attached to the data source to be encrypted.
     *          The inputStream will be closed upon completion.
     * @param outputStream output stream attached to the data sink for encrypted data.
     *          The outputStream will be closed upon completion.  If outputStream is empty,
     *          it means error that cannot be caught had occurred during encryption.
     * @return String containing the metadata to be used for decrypting the encrypted data.
     * @exception KmcCryptoException if any error occurs during encryption.
     */
    String encrypt(final InputStream inputStream, final OutputStream outputStream) throws KmcCryptoException;

    /**
     * Encrypts the data read from input stream with the resulting encrypted data written to the output stream.
     * This method allows specifying the encryption offset for authenticated encryption, and the IV for encryption.
     *
     * @param inputStream input stream attached to the data source to be encrypted.
     *          The inputStream will be closed upon completion.
     * @param encryptOffset The byte from which encryption is applied.
     *          Only used for authenticated encryption.
     * @param iv URL-safe Base64 encoded String for the initial vector in encryption.
     *          Input null for randomly generated IV.
     * @param outputStream output stream attached to the data sink for encrypted data.
     *          The outputStream will be closed upon completion.  If outputStream is empty,
     *          it means error that cannot be caught had occurred during encryption.
     * @return String containing the metadata to be used for decrypting the encrypted data.
     * @exception KmcCryptoException if any error occurs during encryption.
     */
    String encrypt(final InputStream inputStream, final int encryptOffset,
                   final String iv, final OutputStream outputStream) throws KmcCryptoException;

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
