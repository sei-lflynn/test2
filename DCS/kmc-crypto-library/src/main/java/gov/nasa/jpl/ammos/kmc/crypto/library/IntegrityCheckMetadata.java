package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;

/**
 * This is a subclass of CryptoMetadata for Integrity Check.
 *
 *
 */
class IntegrityCheckMetadata extends CryptoMetadata {

    protected static final String INTEGRITY_CHECK_VALUE_ATTR = "integrityCheckValue";

    private static final Logger logger = LoggerFactory.getLogger(IntegrityCheckMetadata.class);

    /**
     * Construction of the IntegrityCheckMetadata.
     */
    IntegrityCheckMetadata() {
    }

    /**
     * IntegrityCheckMetadata for Message Digest.  ICV is truncated to the macLength.
     *
     * @param cryptoAlgorithm Algorithm used for Message Digest.
     * @param macLength The length of the MAC.
     * @param icvBytes The ICV produced from Message Digest.
     */
    IntegrityCheckMetadata(final String cryptoAlgorithm, final int macLength, final byte[] icvBytes) {
        addAttribute(METADATA_TYPE_ATTR, getClass().getSimpleName());
        addAttribute(CRYPTO_ALGORITHM_ATTR, cryptoAlgorithm);
        byte[] macBytes;
        if (macLength == -1) {
            macBytes = icvBytes;
        } else {
            // truncate the ICV
            int nBytes = macLength / 8;
            macBytes = new byte[nBytes];
            System.arraycopy(icvBytes, 0, macBytes, 0, nBytes);
            logger.info("ICV is truncated from {} bits to {} bits", icvBytes.length * 8, macLength);
            addAttribute(MAC_LENGTH_ATTR, String.valueOf(macLength));
        }
        String icv = Base64.getUrlEncoder().encodeToString(macBytes);
        addAttribute(INTEGRITY_CHECK_VALUE_ATTR, icv);
    }

    /**
     * IntegrityCheckMetadata for Message Digest.  ICV is truncated to the macLength.
     *
     * @param cryptoAlgorithm Algorithm used for Message Digest.
     * @param icvBytes The ICV produced from Message Digest.
     */
    IntegrityCheckMetadata(final String cryptoAlgorithm, final byte[] icvBytes) {
        this(cryptoAlgorithm, -1, icvBytes);
    }

    /**
     * IntegrityCheckMetadata for HMAC or Digital Signature.  ICV is truncated to the macLength.
     *
     * @param keyRef The keyRef of the key used for HMAC or Digital Signature.
     * @param cryptoAlgorithm The algorithm used for HMAC or Digital Signature.
     * @param macLength The length of the MAC.
     * @param icvBytes The ICV produced from HMAC or Digital Signature.
     */
    IntegrityCheckMetadata(final String keyRef, final String cryptoAlgorithm,
            final int macLength, final byte[] icvBytes) {
        this(cryptoAlgorithm, macLength, icvBytes);
        addAttribute(KEY_REF_ATTR, keyRef);
    }

    /**
     * IntegrityCheckMetadata for HMAC or Digital Signature.
     *
     * @param keyRef The keyRef of the key used for HMAC or Digital Signature.
     * @param cryptoAlgorithm The algorithm used for HMAC or Digital Signature.
     * @param integrityCheckValue The ICV produced from HMAC or Digital Signature.
     */
    IntegrityCheckMetadata(final String keyRef, final String cryptoAlgorithm, final byte[] icvBytes) {
        this(cryptoAlgorithm, -1, icvBytes);
        addAttribute(KEY_REF_ATTR, keyRef);
    }

    @Override
    String getKeyRef() {
        return getValue(KEY_REF_ATTR);
    }

    /**
     * Returns the ICV in the metadata.
     * @return The ICV in the metadata.
     * @exception KmcCryptoException if ICV is invalid.
     */
    byte[] getIntegrityCheckValue() throws KmcCryptoException {
        String icv = getValue(INTEGRITY_CHECK_VALUE_ATTR);
        if (icv == null) {
            logger.error("getIntegrityCheckValue() ICV is null");
            return null;
        }
        try {
            byte[] icvBytes = Base64.getUrlDecoder().decode(icv);
            return icvBytes;
        } catch (IllegalArgumentException e) {
            String error = "Invalid ICV (" + icv + "): " + e;
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
        if (getCryptoAlgorithm() == null) {
            error = "Missing crypto algorithm in metadata.";
        } else if (getIntegrityCheckValue() == null) {
            error = "Missing integrity check value in metadata.";
        } else if (getKeyRef() == null && getCryptoAlgorithm().contains("Hmac")) {
            error = "Missing keyRef in metadata.";
        }
        if (error != null) {
            logger.error(error);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_METADATA_ERROR, error, null);
        }
    }

}
