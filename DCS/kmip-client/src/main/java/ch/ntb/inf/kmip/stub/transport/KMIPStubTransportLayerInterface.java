/**
 * KMIPStubTransportLayerInterface.java
 * -----------------------------------------------------------------
 *     __ __ __  ___________
 *    / //_//  |/  /  _/ __ \	  .--.
 *   / ,<  / /|_/ // // /_/ /	 /.-. '----------.
 *  / /| |/ /  / // // ____/ 	 \'-' .--"--""-"-'
 * /_/ |_/_/  /_/___/_/      	  '--'
 *
 * -----------------------------------------------------------------
 * Description:
 * The KMIPStubTransportLayerInterface provides the needful
 * flexibility for the interchangeability of the Transport Layer on
 * the client side. It offers one method to send a message and three
 * methods to set dynamically loaded parameters.
 *
 * @author     Stefanie Meile <stefaniemeile@gmail.com>
 * @author     Michael Guster <michael.guster@gmail.com>
 * @org.       NTB - University of Applied Sciences Buchs, (CH)
 * @copyright  Copyright � 2013, Stefanie Meile, Michael Guster
 * @license    Simplified BSD License (see LICENSE.TXT)
 * @version    1.0, 2013/08/09
 * @since      Class available since Release 1.0
 *
 *
 */

package ch.ntb.inf.kmip.stub.transport;

import java.util.ArrayList;
import java.util.Map;

/**
 * The KMIPStubTransportLayerInterface provides the needful
 * flexibility for the interchangeability of the Transport Layer on
 * the client side. It offers one method to send a message and three
 * methods to set dynamically loaded parameters.
 */
public interface KMIPStubTransportLayerInterface {

    // transport layer config parameters in config file
    public static final String CFG_KMS_URI = "key_management_service_uri";
    public static final String CFG_KEYSTORE_FILE = "keystore_file";
    public static final String CFG_KEYSTORE_PASSWORD = "keystore_password";
    public static final String CFG_SSO_COOKIE = "sso_cookie";
    public static final String CFG_ADDITIONAL_CIPHERS = "additional_cipher_suites";
    public static final String CFG_CONNECTION_TIMEOUT_CONNECT = "connection_timeout_connect";
    public static final String CFG_CONNECTION_TIMEOUT_READ = "connection_timeout_read";

    // generated config parameters
    public static final String CFG_SOCKET_HOST = "socket_host";
    public static final String CFG_SOCKET_PORT = "socket_port";
    public static final String CFG_SOCKET_PROTOCOLS = "socket_protocols";
    public static final String CFG_SOCKET_CIPHERS = "socket_ciphers";

	/**
	 * Sends a KMIP-Request-Message as a TTLV-encoded hexadecimal string stored in an
	 * <code>ArrayList{@literal <}Byte{@literal >}</code> to a defined target and returns
	 * a corresponding KMIP-Response-Message.
	 *
	 * @param al :     	the <code>ArrayList{@literal <}Byte{@literal >}</code> to be sent.
	 * @return			<code>ArrayList{@literal <}Byte{@literal >}</code>: the response message.
	 */
	public ArrayList<Byte> send(ArrayList<Byte> al) throws Exception;

    /**
     * Sets the configuration parameters for the transport layer.
     *
     * @param configParams: a <code>Map</code> that contains configuration parameters for the transport layer.
     * @exception IllegalArgumentException if any config parameter is invalid.
     */
    public void setConfigParameters(Map<String, Object> configParams) throws IllegalArgumentException;

}
