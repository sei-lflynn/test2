/**
 * KMIPSkeletonTransportLayerInterface.java
 * -----------------------------------------------------------------
 *     __ __ __  ___________
 *    / //_//  |/  /  _/ __ \	  .--.
 *   / ,<  / /|_/ // // /_/ /	 /.-. '----------.
 *  / /| |/ /  / // // ____/ 	 \'-' .--"--""-"-'
 * /_/ |_/_/  /_/___/_/      	  '--'
 *
 * -----------------------------------------------------------------
 * Description:
 * The KMIPSkeletonTransportLayerInterface provides the needful
 * flexibility for the interchangeability of the Transport Layer on
 * the server side. It offers one method to set the dynamically
 * loaded port-number of the server-socket.
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


package ch.ntb.inf.kmip.skeleton.transport;

/**
 * The KMIPSkeletonTransportLayerInterface provides the needful
 * flexibility for the interchangeability of the Transport Layer on
 * the server side. It offers one method to dynamically set the port
 * of the server-socket.
 * It is only used if no middle-ware is used.
 */
public interface KMIPSkeletonTransportLayerInterface {

	/**
	 * Sets the communication URI of the server.
	 * Encodes a <code>KMIPContainer</code> and returns a KMIP-Request-Message as a TTLV-encoded
	 * hexadecimal string stored in an <code>ArrayList{@literal <}Byte{@literal >}</code>.
	 *
	 * @param uri                the communication URI of the server.
     * @param keystoreFile       the path to the keystore file.
     * @param keystorePassword   the password of the keystore.
     * @return true if the uri is valid, otherwise false.
	 */
	boolean setTransportLayerInfo(String uri, String keystoreFile, String keystorePassword);

}


