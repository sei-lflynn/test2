/**
 * KMIPStubTransportLayer.java
 * -----------------------------------------------------------------
 *     __ __ __  ___________
 *    / //_//  |/  /  _/ __ \	  .--.
 *   / ,<  / /|_/ // // /_/ /	 /.-. '----------.
 *  / /| |/ /  / // // ____/ 	 \'-' .--"--""-"-'
 * /_/ |_/_/  /_/___/_/      	  '--'
 *
 * -----------------------------------------------------------------
 * Description:
 * The KMIPStubTransportLayer provides a Thread, which handles the
 * client requests to the server via TCP-Sockets. The whole read and
 * write functionality is encapsulated in the KMIPClientHandler.
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

package ch.ntb.inf.kmip.stub.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The KMIPStubTransportLayer provides the communication between a server and a client via TCP-Sockets.
 */
public class KMIPStubTransportLayer implements KMIPStubTransportLayerInterface {

    private static final String[] BASIC_CIPHERS = { "TLS_RSA_WITH_AES_128_CBC_SHA" };
    private static final String[] V12_CIPHERS = { "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384" };
    private static final Integer DEFAULT_CONNECT_TIMEOUT = 10000;   // ms
    private static final Integer DEFAULT_READ_TIMEOUT = 30000;

	private Map<String, Object> configParams;
	private KMIPClientHandler clientHandler;

    private static final Logger logger = LoggerFactory.getLogger(KMIPStubTransportLayer.class);

	public KMIPStubTransportLayer(){
		logger.debug("KMIPTransportLayer initialized...");
	}

	/**
	 * Sends a KMIP-Request-Message as a TTLV-encoded hexadecimal string stored in an
	 * <code>ArrayList{@literal <}Byte{@literal >}</code> to a defined target and returns
	 * a corresponding KMIP-Response-Message.
	 *
	 * @param al :     	the <code>ArrayList{@literal <}Byte{@literal >}</code> to be sent.
	 * @return			<code>ArrayList{@literal <}Byte{@literal >}</code>: the response message.
	 */
	@Override
    public ArrayList<Byte> send(final ArrayList<Byte> al) throws Exception {
		logger.debug("KMIP client send request thread: " + Thread.currentThread());
		clientHandler = new KMIPClientHandler(configParams, al);
		/* Process the call-Method from the clientHandler asynchronous with FutureTask
		 * A Future represents the result of an asynchronous computation
		 */
		FutureTask<ArrayList<Byte>> ft = new FutureTask<ArrayList<Byte>>(clientHandler);
		Thread tft = new Thread(ft);
		tft.start();

		// While the clientHandler is in process, other Threads can run
		while (!ft.isDone()) {	// while FutureTask is busy
			Thread.yield();
		} 						// FutureTask isDone

		logger.debug("KMIP client send request done.");
		try {
			return ft.get();
		} catch (Exception e) {
			logger.error("send(): " + e);
			throw e;
		}
	}

    /**
     * Sets the configuration parameters of the transport layer.
     *
     * @param configParams :  the configuration parameters to be set.
     */
    @Override
    public void setConfigParameters(final Map<String, Object> configParams) throws IllegalArgumentException {
        this.configParams = configParams;
        String msg;

        String uri = (String) configParams.get(CFG_KMS_URI);
        logger.debug("Kmip Service URI = " + uri);

        // hostname
        int begin = "tls10://".length();
        int end = uri.indexOf(":", begin);
        if (end == -1) {
            msg = "Invalid " + CFG_KMS_URI + ": " + uri;
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
        String host = uri.substring(begin, end);
        configParams.put(CFG_SOCKET_HOST, host);

        // port
        String portString = uri.substring(end + 1);
        try {
            Integer port = Integer.parseInt(portString);
            configParams.put(CFG_SOCKET_PORT, port);
        } catch (NumberFormatException e) {
            msg = "Socket port in URI is not an integer: " + uri;
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // TLS protocols and ciphers
        List<String> ciphers = new ArrayList<String>();
        String additionalCiphers = (String) configParams.get(CFG_ADDITIONAL_CIPHERS);
        if (additionalCiphers != null) {
            String[] additions = additionalCiphers.split("[ :;\"]");
            for (int i = 0; i < additions.length; i++) {
                if (additions[i].isEmpty()) {
                    continue;
                }
                logger.trace("additional cipher = " + additions[i]);
                ciphers.add(additions[i]);
            }
        }
        if (uri.startsWith("tls10://")) {
            String[] protocols = { "TLSv1", "TLSv1.1", "TLSv1.2" };
            configParams.put(CFG_SOCKET_PROTOCOLS, protocols);
            for (int i = 0; i < BASIC_CIPHERS.length; i++) {
                ciphers.add(BASIC_CIPHERS[i]);
            }
        } else {
            String[] protocols = { "TLSv1.2" };
            configParams.put(CFG_SOCKET_PROTOCOLS, protocols);
            for (int i = 0; i < V12_CIPHERS.length; i++) {
                ciphers.add(V12_CIPHERS[i]);
            }
        }
        logger.debug("Ciphers suites = " + ciphers);
        configParams.put(CFG_SOCKET_CIPHERS, ciphers.toArray(new String[]{}));

        // keystore
        Object keystoreFile = configParams.get(CFG_KEYSTORE_FILE);
        if (keystoreFile == null) {
            msg = "Keystore file not found in config file.";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
        Object keystorePassword = configParams.get(CFG_KEYSTORE_PASSWORD);
        if (keystorePassword == null) {
            msg = "Keystore password not found in config file.";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // timeout
        String connectTimeout = (String) configParams.get(CFG_CONNECTION_TIMEOUT_CONNECT);
        Integer timeout;
        if (connectTimeout == null) {
            timeout = DEFAULT_CONNECT_TIMEOUT;
        } else {
            try {
                timeout = Integer.parseInt(connectTimeout);
            } catch (NumberFormatException e) {
                msg = "Invalid " + CFG_CONNECTION_TIMEOUT_CONNECT + ": " + connectTimeout;
                logger.error(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        logger.debug("Connection timeout = " + timeout);
        configParams.put(CFG_CONNECTION_TIMEOUT_CONNECT, timeout);

        String readTimeout = (String) configParams.get(CFG_CONNECTION_TIMEOUT_READ);
        if (readTimeout == null) {
            timeout = DEFAULT_READ_TIMEOUT;
        } else {
            try {
                timeout = Integer.parseInt(readTimeout);
            } catch (NumberFormatException e) {
                msg = "Invalid " + CFG_CONNECTION_TIMEOUT_READ + ": " + readTimeout;
                logger.error(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        logger.debug("Read timeout = " + timeout);
        configParams.put(CFG_CONNECTION_TIMEOUT_READ, timeout);
    }

}
