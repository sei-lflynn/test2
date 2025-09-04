/**
 * KMIPClientHandler.java
 * ------------------------------------------------------------------
 *     __ __ __  ___________
 *    / //_//  |/  /  _/ __ \	  .--.
 *   / ,<  / /|_/ // // /_/ /	 /.-. '----------.
 *  / /| |/ /  / // // ____/ 	 \'-' .--"--""-"-'
 * /_/ |_/_/  /_/___/_/      	  '--'
 *
 * ------------------------------------------------------------------
 * Description:
 * The KMIPClientHandler provides a Thread, which handles the client-
 * requests to the server, as well the read and write service to the
 * server via TCP-Sockets.
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

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class KMIPClientHandler implements Callable<ArrayList<Byte>> {

	private static final Logger logger = LoggerFactory.getLogger(KMIPClientHandler.class);

    private final ArrayList<Byte> al;
	private final int port;
	private final String host;
    private final String keystoreFile;
    private final String keystorePassword;
    private final String[] protocols;
    private final String[] ciphers;
    private final int connectTimeout;
    private final int readTimeout;

	private SSLSocket clientSocket;
	private static boolean firstConnection = true;

	public KMIPClientHandler(final Map<String, Object> configParams, final ArrayList<Byte> al) {
        this.host = (String) configParams.get(KMIPStubTransportLayerInterface.CFG_SOCKET_HOST);
		this.port = (Integer) configParams.get(KMIPStubTransportLayerInterface.CFG_SOCKET_PORT);
        logger.debug("KMIPClientHandler: hostname = " + host + ", port = " + port);
        this.protocols = (String[]) configParams.get(KMIPStubTransportLayerInterface.CFG_SOCKET_PROTOCOLS);
        this.ciphers = (String[]) configParams.get(KMIPStubTransportLayerInterface.CFG_SOCKET_CIPHERS);
		this.keystoreFile = (String) configParams.get(KMIPStubTransportLayerInterface.CFG_KEYSTORE_FILE);
        this.keystorePassword = (String) configParams.get(KMIPStubTransportLayerInterface.CFG_KEYSTORE_PASSWORD);
        this.connectTimeout = (Integer) configParams.get(KMIPStubTransportLayerInterface.CFG_CONNECTION_TIMEOUT_CONNECT);
        this.readTimeout = (Integer) configParams.get(KMIPStubTransportLayerInterface.CFG_CONNECTION_TIMEOUT_READ);
		this.al = al;
	}

	// Call method for the FutureTask (similar to run() of a Thread)
	@Override
    public ArrayList<Byte> call() throws Exception {
		logger.debug("KMIPClientHandler:" + Thread.currentThread());
		// Start a server-request
		// Create a Socket for the TCP Client and build up the communication to the corresponding server.
		try {
			//clientSocket = new Socket(targetHostname, port);
		    clientSocket = createClientSocket();
		    if (firstConnection) {
		        String socketInfo = getSocketInfo(clientSocket);
		        logger.debug("Client socket information: \n" + socketInfo);
                firstConnection = false;
		    }
			// Send to server
			logger.debug("KMIPClientHandler: Write data to server...");
			writeData(clientSocket);
			logger.debug("KMIPClientHandler: Data transmitted!");

			// Close output signalize EOF
			//clientSocket.shutdownOutput(); // not supported by SSLSocket

			// Read from server
			ArrayList<Byte> responseFromServer = readData();

			// Close connection
			clientSocket.close();

			return responseFromServer;

		} catch (Exception e) {
			logger.error("Exception in call(): " + e);
			throw e;
		}
	}

	private void writeData(final Socket clientSocket) throws Exception {
		try {
			// Get OutputStream from Socket
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			// Prepare data to send
			byte[] b = new byte[al.size()];
			for(int i=0; i<al.size();i++){
				b[i]=al.get(i);
			}
			if (logger.isDebugEnabled()) {
	            // ignore response, just wants to log the request.
	            toArrayList(b);
			}
			// Send data
			outToServer.write(b);
			outToServer.flush();
		} catch (IOException e) {
            logger.info("writeData() IOException: " + e);
            throw e;
		}
	}

	private ArrayList<Byte> readData() throws Exception {
    	byte[] resultBuff = new byte[0];
        byte[] buff = new byte[1024];
        int k = -1;

        logger.debug("KMIPClientHandler: Read data from server...");
        try {
    		InputStream is = clientSocket.getInputStream();
			while((k = is.read(buff, 0, buff.length)) > -1) {
			    byte[] tbuff = new byte[resultBuff.length + k]; // temp buffer size = bytes already read + bytes last read
			    System.arraycopy(resultBuff, 0, tbuff, 0, resultBuff.length); // copy previous bytes
			    System.arraycopy(buff, 0, tbuff, resultBuff.length, k);  // copy current lot
			    resultBuff = tbuff; // call the temp buffer as your result buff
			    int available = is.available();
			    logger.debug("readData() available bytes to read = {}", available);
			    if (available == 0) {
			        break;
			    }
			}
        } catch (SocketException e) {
            logger.error("readData() SocketException: " + e);
            throw e;
		} catch (IOException e) {
		    logger.error("readData() IOException: " + e);
		    throw e;
		}
/*
        ArrayList<Byte> response = new ArrayList<Byte>();
        for(byte b:resultBuff){
        	response.add(b);
        }
*/
        ArrayList<Byte> response = toArrayList(resultBuff);
        getTTLVlength(resultBuff);

        logger.debug("KMIPClientHandler: Response received, bytes " + response.size());
        return response;
	}

	// The TTLV length is the length of the value field in bytes
    private int getTTLVlength(byte[] ttlv) throws UnsupportedEncodingException {
        // first bye of all KMIP tag has value 0x42 or 0x54
        if (ttlv[0] != 0x42 && ttlv[0] != 0x54) {
            logger.warn("Invalid reponse tag");
            logger.info("Invalid reponse: " + new String(ttlv, "UTF-8"));
        }
        ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(ttlv, 4, 8));
        int length = wrapped.getInt();
        if (length != ttlv.length - 8) {
            logger.warn("TTLV length (" + length + ") does not equal to TTLV value length (" + (ttlv.length - 8) + ")");
        } else {
            logger.debug("TTLV length = " + length);
        }
        return length;
	}

    private ArrayList<Byte> toArrayList(final byte[] resultBuff) {
        ArrayList<Byte> request = new ArrayList<Byte>();
        StringBuilder sb = new StringBuilder();
        for (byte b : resultBuff){
            sb.append(String.format("%02X", b));
            request.add(b);
        }
        logger.debug(sb.toString());
        return request;
    }

    public SSLSocket createClientSocket() throws Exception {
        logger.debug("createClientSocket() Keystore = {}", keystoreFile);
        KeyStore keyStore;
        if (keystoreFile.endsWith("bcfks")) {
          Provider provider = new BouncyCastleFipsProvider();
          Security.addProvider(provider);
          logger.debug("createClientSocket() add provider = {}", provider.getName());
          keyStore = KeyStore.getInstance("BCFKS", provider.getName());
        } else if (keystoreFile.endsWith("p12")) {
          keyStore = KeyStore.getInstance("PKCS12");
        } else if (keystoreFile.endsWith("jks")) {
          keyStore = KeyStore.getInstance("JKS");
        } else {
          logger.error("Only .bcfks, .p12, and .jks keystore are supported.  Unsupported keystore type for {}", keystoreFile);
          return null;
        }
        try {
            keyStore.load(new FileInputStream(keystoreFile), keystorePassword.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, keystorePassword.toCharArray());
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(kmf.getKeyManagers(), null, null);

            SSLSocketFactory f = sc.getSocketFactory();
            logger.debug("connecting to hostname = {}, port = {}", host, port);
            SSLSocket c = (SSLSocket) f.createSocket(host, port);
            logger.debug("socket isConnected() = " + c.isConnected());
            c.setUseClientMode(true);
            SSLParameters params = new SSLParameters();
            params.setProtocols(protocols);
            params.setCipherSuites(ciphers);
            params.setEndpointIdentificationAlgorithm("HTTPS");
            c.setSSLParameters(params);
            c.setNeedClientAuth(true);
            logger.debug("connectTimeout = {}, readTimeout = ", connectTimeout, readTimeout);
            c.setSoTimeout(readTimeout);
            c.startHandshake();
            return c;
        } catch (Exception e) {
            logger.error("Exception in createClientSocket(): " + e);
            throw e;
        }
    }

    private String getSocketInfo(final SSLSocket s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Client Socket class: " + s.getClass() + "\n");
        sb.append("   Remote address = " + s.getInetAddress().toString() + "\n");
        sb.append("   Remote port = " + s.getPort() + "\n");
        sb.append("   Local socket address = " + s.getLocalSocketAddress().toString() + "\n");
        //sb.append("   Local address = " + s.getLocalAddress().toString() + "\n");
        //sb.append("   Local port = " + s.getLocalPort() + "\n");
        sb.append("   Need client authentication = " + s.getNeedClientAuth() + "\n");
        SSLSession ss = s.getSession();
        try {
            sb.append("Client Session class: " + ss.getClass() + "\n");
            sb.append("   Cipher suite = " + ss.getCipherSuite() + "\n");
            sb.append("   Protocol = " + ss.getProtocol() + "\n");
            sb.append("   PeerPrincipal = " + ss.getPeerPrincipal().getName() + "\n");
            sb.append("   LocalPrincipal = " + ss.getLocalPrincipal().getName());
        } catch (Exception e) {
            sb.append("Exception getting session info: " + e);
        }
        return sb.toString();
    }

}
