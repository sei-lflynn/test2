package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * This Java program prints the KMC Crypto API version information and can be run by the command:
 * <code>java -jar kmc-crypto-library.jar</code>.
 *
 *
 */
public final class KmcCryptoVersion {

    /**
     * This private constructor is used to silence the checkstyle warning:
     * Utility classes should not have a public or default constructor.
     */
    private KmcCryptoVersion() {
        // not called
     }

    /**
     * The main program prints the KMC Crypto API version information.
     * @param args command-line arguments are not used.
     */
    public static void main(final String[] args) {
        System.out.println("AMMOS Application Security (ASEC)");
        System.out.println("Key Management & Cryptography (KMC)");
        System.out.println("KMC Cryptography Library");
        try {
            URL res = KmcCryptoVersion.class.getResource(KmcCryptoVersion.class.getSimpleName() + ".class");
            JarURLConnection conn = (JarURLConnection) res.openConnection();
            Manifest manifest = conn.getManifest();
            Attributes attrs = manifest.getMainAttributes();
            System.out.println("Version: " + attrs.getValue("Version"));
            System.out.println("Built-Time: " + attrs.getValue("Built-Time"));
            String copyright = attrs.getValue("Copyright");
            if (copyright != null) {
                System.out.println(copyright);
            }
        } catch (IOException e) {
            System.err.println("Exception: " + e);
        }
    }

}
