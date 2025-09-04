package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;

/**
 * The ASEC KMC CryptoMetadata class.  This is the top-level metadata class which contains
 * attributes applied to all KMC cryptography APIs.  Its subclasses contain attributes
 * specific to the particular APIs.
 *
 */
class CryptoMetadata {
    static final String METADATA_TYPE_ATTR = "metadataType";
    static final String CRYPTO_ALGORITHM_ATTR = "cryptoAlgorithm";
    static final String KEY_REF_ATTR = "keyRef";
    static final String MAC_LENGTH_ATTR = "macLength";

    private static final String ATTRIBUTE_VALUE_DELIMITER = ":";
    private static final String ATTRIBUTE_DELIMITER = ",";

    private Map<String, String> metadata = new HashMap<String, String>();

    private static final Logger logger = LoggerFactory.getLogger(CryptoMetadata.class);

    /**
     * Constructor at package level.
     */
    CryptoMetadata() {
        // empty body
    }

    /**
     * Returns the set of attribute names.
     * @return The set of attribute names.
     */
    Set<String> getAttributes() {
        return metadata.keySet();
    }

    /**
     * Returns the value of the specified attribute.
     * @param attribute The name of the attribute.
     * @return The value of the attribute.
     */
    String getValue(final String attribute) {
        return metadata.get(attribute);
    }

    /**
     * Adds an attribute and its value to the metadata.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    void addAttribute(final String name, final String value) {
        metadata.put(name, value);
    }

    /**
     * Returns the metadata type.
     * @return the metadata type.
     */
    String getMetadataType() {
        return getValue(METADATA_TYPE_ATTR);
    }

    /**
     * Returns the cryptographic algorithm in the metadata.
     * @return the cryptographic algorithm in the metadata.
     */
    String getCryptoAlgorithm() {
        return getValue(CRYPTO_ALGORITHM_ATTR);
    }

    /**
     * Returns the keyRef of the key in the metadata.
     * @return the keyRef of the key in the metadata.
     */
    String getKeyRef() {
        return getValue(KEY_REF_ATTR);
    }

    /**
     * Returns the MAC length for ICV or Tag length for AE in the metadata.
     * @return the MAC or Tag length in the metadata.
     */
    int getMacLength() {
        String macLength = getValue(MAC_LENGTH_ATTR);
        if (macLength == null || macLength.isEmpty()) {
            // -1 means no truncation, i.e. create full MAC length
            return -1;
        }
        try {
            return Integer.parseInt(macLength);
        } catch (NumberFormatException e) {
            // bad macLength should have caught when it was set, return -1 just in case.
            logger.error("getMacLength() bad MAC length attribute in metadata: {}", macLength);
            return -1;
        }
    }

    /**
     * Parses the metadata string value to a Metadata object.
     * @param metadata The string representation of the metadata.
     * @throws KmcCryptoException if error occurs during parsing the metadata string value.
     */
    void parseMetadata(final String metadata) throws KmcCryptoException {
        this.metadata = new HashMap<String, String>();
        String[] pairs = metadata.split(ATTRIBUTE_DELIMITER);
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            String[] attributeValue = pair.split(ATTRIBUTE_VALUE_DELIMITER);
            if (attributeValue.length != 2) {
                String message = "Invalid metadata: " + metadata;
                logger.error(message);
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_METADATA_ERROR, message, null);
            }
            this.metadata.put(attributeValue[0], attributeValue[1]);
        }
        checkMetadataAttributes();
    }

    /**
     * Checks if there is any missing attributes in the metadata.
     * @throws KmcCryptoException if there is any missing attributes in the metadata.
     */
    private void checkMetadataAttributes() throws KmcCryptoException {
        if (getMetadataType() == null) {
            String error = "Missing metadata type in metadata.";
            logger.error(error);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_METADATA_ERROR, error, null);
        }
        if (getValue(MAC_LENGTH_ATTR) != null) {
            try {
                Integer.parseInt(getValue(MAC_LENGTH_ATTR));
            } catch (NumberFormatException e) {
                String error = "The " + MAC_LENGTH_ATTR + " attribute in the metadata is an invalid integer.";
                logger.error(error);
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_METADATA_ERROR, error, null);
            }
        }
    }

    /**
     * The string representation of the metadata.
     * @return The string representation of the metadata.
     */
    @Override
    public String toString() {
        if (metadata.isEmpty()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        for (String attribute : getAttributes()) {
            buffer.append(attribute).append(ATTRIBUTE_VALUE_DELIMITER)
                  .append(getValue(attribute)).append(ATTRIBUTE_DELIMITER);
        }
        return buffer.substring(0, buffer.length() - 1);
    }

}
