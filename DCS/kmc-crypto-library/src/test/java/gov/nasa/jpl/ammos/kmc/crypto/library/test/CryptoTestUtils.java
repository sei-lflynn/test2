package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;

/**
 * Utility class for creating KMIP keys.
 *
 *
 */
public final class CryptoTestUtils {

    private static SecureRandom random = null;

    private CryptoTestUtils() { }

    public static synchronized byte[] createTestData(final int nBytes) {
        if (random == null) {
            random = new SecureRandom();
        }
        byte[] testData = new byte[nBytes];
        random.nextBytes(testData);
        return testData;
    }

    public static void addCryptoProvider(final String providerName)
            throws KmcCryptoManagerException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?>  providerClass = Class.forName(providerName);
        Provider provider = (Provider) providerClass.newInstance();
        Security.addProvider(provider);
    }

    /**
     * Modify the value of the specified parameter in the metadata.  If value is null, remove the parameter.
     * @param metadata The metadata to be changed.
     * @param param Name of the parameter to be changed.
     * @param value New value of the parameter.
     * @return The changed metadata.
     */
    public static String modifyMetadataValue(final String metadata, final String param, final String value) {
        int paramIndex = metadata.indexOf(param);
        int valueIndex = metadata.indexOf(":", paramIndex + 1) + 1;
        int nextIndex = metadata.indexOf(",", valueIndex);
        String changed;
        if (value == null) {
            if (nextIndex == -1) {
                changed = metadata.substring(0, paramIndex);
            } else {
                changed = metadata.substring(0, paramIndex) + metadata.substring(nextIndex);
            }
        } else {
            if (nextIndex == -1) {
                changed = metadata.substring(0, valueIndex) + value;
            } else {
                changed = metadata.substring(0, valueIndex) + value + metadata.substring(nextIndex);
            }
        }
        return changed;
    }

}
