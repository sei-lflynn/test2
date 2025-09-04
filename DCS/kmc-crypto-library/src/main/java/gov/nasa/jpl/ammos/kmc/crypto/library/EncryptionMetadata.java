package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;

/**
 * This is a subclass of CryptoMetadata for Encryption.
 *
 *
 */
class EncryptionMetadata extends CryptoMetadata {

    static final String KEY_LENGTH_ATTR = "keyLength";
    static final String TRANSFORMATION_ATTR = "cipherTransformation";
    static final String INITIAL_VECTOR_ATTR = "initialVector";
    static final String ENCRYPT_OFFSET_ATTR = "encryptOffset";

    private static final Logger logger = LoggerFactory.getLogger(EncryptionMetadata.class);

    /**
     * Construction of the EncryptionMetadata.
     */
    EncryptionMetadata() {
    }

    /**
     * Construction of the EncryptionMetadata.
     *
     * @param keyRef The keyRef of the key used for encryption.
     * @param cryptoAlgorithm Algorithm used for encryption.
     */
    EncryptionMetadata(final String keyRef, final String cryptoAlgorithm) {
        addAttribute(METADATA_TYPE_ATTR, getClass().getSimpleName());
        addAttribute(KEY_REF_ATTR, keyRef);
        addAttribute(CRYPTO_ALGORITHM_ATTR, cryptoAlgorithm);
    }

    /**
     * Adds encryption specific attributes to the metadata.
     *
     * @param keyLength The length of the key in bits.
     * @param transformation The transformation used for symmetric encryption.
     * @param initialVector The initial vector used for symmetric encryption.
     * @param tagLength The length of the tag.
     */
    void addEncryptionAttributes(final int keyLength, final String transformation,
            final int encryptOffset, final byte[] initialVector, final int tagLength) {
        addAttribute(KEY_LENGTH_ATTR, String.valueOf(keyLength));
        addAttribute(TRANSFORMATION_ATTR, transformation);
        if (encryptOffset > 0) {
            addAttribute(ENCRYPT_OFFSET_ATTR, Integer.toString(encryptOffset));
        }
        if (initialVector != null) {
            addAttribute(INITIAL_VECTOR_ATTR, Base64.getUrlEncoder().encodeToString(initialVector));
        }
        if (tagLength != -1) {
            addAttribute(MAC_LENGTH_ATTR, String.valueOf(tagLength));
        }
    }

    /**
     * Returns the length of the key (number of bits) in the metadata.
     * @return the length of the key (number of bits) in the metadata.
     */
    int getKeyLength() {
        try {
            return Integer.parseInt(getValue(KEY_LENGTH_ATTR));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Returns the cipher transformation in the metadata.
     * @return The cipher transformation in the metadata.
     */
    String getTransformation() {
        return getValue(TRANSFORMATION_ATTR);
    }

    /**
     * Returns the encrypt offset in the metadata or 0 if not exist.
     * @return The encrypt offset in the metadata or 0 if not exist.
     * @exception KmcCryptoException if encrypt offset is not an integer.
     */
    int getEncryptOffset() throws KmcCryptoException {
        String encryptOffset = getValue(ENCRYPT_OFFSET_ATTR);
        if (encryptOffset == null) {
            return 0;
        }
        try {
            return Integer.parseInt(encryptOffset);
        } catch (NumberFormatException e) {
            String error = "Invalid encrypt offset (" + encryptOffset + "): " + e;
            logger.error(error);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_METADATA_ERROR, error, e);
        }
    }

    /**
     * Returns the initial vector in the metadata.
     * @return The initial vector in the metadata.
     * @exception KmcCryptoException if initial vector is invalid.
     */
    byte[] getInitialVector() throws KmcCryptoException {
        String iv = getValue(INITIAL_VECTOR_ATTR);
        try {
            byte[] ivBytes = Base64.getUrlDecoder().decode(iv);
            return ivBytes;
        } catch (IllegalArgumentException e) {
            String error = "Invalid initial vector (" + iv + "): " + e;
            logger.error(error);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_METADATA_ERROR, error, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void parseMetadata(final String metadata) throws KmcCryptoException {
        super.parseMetadata(metadata);
        checkMetadataAttributes();
    }

    /**
     * Checks if there is any missing attributes in the metadata.
     * @throws KmcCryptoException if there is any missing attributes in the metadata.
     */
    private void checkMetadataAttributes() throws KmcCryptoException {
        String error = null;
        if (getKeyRef() == null) {
            error = "Missing keyRef in metadata.";
        } else if (getCryptoAlgorithm() == null) {
            error = "Missing crypto algorithm in metadata.";
        } else if (getKeyLength() == -1) {
            error = "Missing or invalid key length in metadata.";
        } else if (getTransformation() == null) {
            error = "Missing cipher transformation in metadata.";
        }
        if (error != null) {
            logger.error(error);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_METADATA_ERROR, error, null);
        }
    }

}
