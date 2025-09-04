/**
 * KMIPSkeletonTransportLayer.java
 * -------------------------------------------------------------------
 *     __ __ __  ___________
 *    / //_//  |/  /  _/ __ \	  .--.
 *   / ,<  / /|_/ // // /_/ /	 /.-. '----------.
 *  / /| |/ /  / // // ____/ 	 \'-' .--"--""-"-'
 * /_/ |_/_/  /_/___/_/      	  '--'
 *
 * -------------------------------------------------------------------
 * Description:
 * The Skeleton Transport Layer provides a server thread for the
 * client-server-communication via sockets. Each communication is
 * executed in an own thread, handled in the KLMSServerNetworkService.
 *
 * @author     Stefanie Meile <stefaniemeile@gmail.com>
 * @author     Michael Guster <michael.guster@gmail.com>
 * @org.       NTB - University of Applied Sciences Buchs, (CH)
 * @copyright  Copyright ��� 2013, Stefanie Meile, Michael Guster
 * @license    Simplified BSD License (see LICENSE.TXT)
 * @version    1.0, 2013/08/09
 * @since      Class available since Release 1.0
 *
 *
 */

package ch.ntb.inf.kmip.skeleton.transport;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ntb.inf.kmip.skeleton.KMIPSkeleton;


public class KMIPSkeletonTransportLayer implements KMIPSkeletonTransportLayerInterface {

	private static final Logger logger = LoggerFactory.getLogger(KMIPSkeletonTransportLayer.class);

	public KMIPSkeletonTransportLayer(final KMIPSkeleton skeleton,
	        final String uri, final String keystoreFile, final String keystorePassword) {
		logger.info("--KLMS-Server is starting...");
		// open communication ServerSide
		logger.info("server URI = " + uri);
		logger.info("keystore = " + keystoreFile);
		try {
	        int start = uri.indexOf(":", "tls10://".length());
	        String portString = uri.substring(start + 1);
	        int port = Integer.parseInt(portString);
			// Create a ServerSocket. It waits for requests to come in over the network
			final SSLServerSocket serverSocket = createServerSocket(port, keystoreFile, keystorePassword);
			// Creates a thread pool that creates new threads as needed. An Executor is normally used instead of explicitly creating threads.
			final ExecutorService pool = Executors.newCachedThreadPool();
			// Create and start the server thread for the Client-Server-Communication
			Thread ts = new Thread(new KLMSServerNetworkService(pool, serverSocket, skeleton));
		    ts.start();
			logger.info("KLMS-Server is ready to receive requests. Listening on port: " + port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// unused
	@Override
    public boolean setTransportLayerInfo(final String uri, final String keystoreFile, final String keystorePassword) {
	    return true;
	}

    public SSLServerSocket createServerSocket(final int port, final String keystoreFile, final String keystorePassword)
            throws KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException,
            CertificateException, FileNotFoundException, IOException, UnrecoverableKeyException, KeyManagementException {
        KeyStore ks = null;
        if (keystoreFile.endsWith("bcfks")) {
          Provider provider = new BouncyCastleFipsProvider();
          Security.addProvider(provider);
          logger.debug("createClientSocket() add provider = {}", provider.getName());
          ks = KeyStore.getInstance("BCFKS", provider.getName());
        } else if (keystoreFile.endsWith("p12")) {
          ks = KeyStore.getInstance("PKCS12");
        } else if (keystoreFile.endsWith("jks")) {
          ks = KeyStore.getInstance("JKS");
        } else {
          logger.error("Only .bcfks, .p12, and .jks keystore are supported.  Unsupported keystore type for {}", keystoreFile);
          return null;
        }
        ks.load(new FileInputStream(keystoreFile), keystorePassword.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keystorePassword.toCharArray());
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(kmf.getKeyManagers(), null, null);
        SSLServerSocketFactory ssf = sc.getServerSocketFactory();
        SSLServerSocket s = (SSLServerSocket) ssf.createServerSocket(port);
        s.setNeedClientAuth(true);
        String socketInfo = getServerSocketInfo(s);
        logger.info("Server socket information: \n" + socketInfo);
        return s;
    }

    private String getServerSocketInfo(final SSLServerSocket s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Server socket class: " + s.getClass() + "\n");
        sb.append("   Socket address = " + s.getInetAddress().toString() + "\n");
        sb.append("   Socket port = " + s.getLocalPort() + "\n");
        sb.append("   Need client authentication = " + s.getNeedClientAuth() + "\n");
        sb.append("   Want client authentication = " + s.getWantClientAuth() + "\n");
        sb.append("   Use client mode = " + s.getUseClientMode());
        return sb.toString();
    }

}
