package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.lang.reflect.InvocationTargetException;
import java.security.Provider;
import java.security.Security;


import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;

/**
 * An utility class for Crypto Service.
 *
 *
 */
public final class CryptoLibraryUtilities {

    private CryptoLibraryUtilities() {
    }

    /**
     * Add the crypto algorithm provider class so that the provider can be used for cryptographic functions.
     * @param providerClassName the class name of the provider.
     * @throws KmcCryptoException if provider class not found.
     */
    public static void addCryptoProvider(final String providerClassName) throws KmcCryptoException {
        Class<?> providerClass;
        try {
            providerClass = Class.forName(providerClassName);
            Provider provider = (Provider) providerClass.getDeclaredConstructor().newInstance();
            Security.addProvider(provider);
        } catch (ClassNotFoundException e) {
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR,
                    "Crypto algorithm provider class not found: " + providerClassName, e);
        } catch (InstantiationException e) {
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR,
                    "Crypto algorithm provider class cannot be instantiated: " + providerClassName, e);
        } catch (IllegalAccessException e) {
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR,
                    "Crypto algorithm provider class cannot be accessed: " + providerClassName, e);
        } catch (IllegalArgumentException|InvocationTargetException|NoSuchMethodException|SecurityException e) {
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR,
                    "Failed to get instance of Crypto algorithm provider class: " + providerClassName, e);    
        }
    }

}
