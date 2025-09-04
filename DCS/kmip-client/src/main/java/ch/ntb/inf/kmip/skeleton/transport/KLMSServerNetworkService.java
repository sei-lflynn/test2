/**
 * KLMSServerNetworkService.java
 * -----------------------------------------------------------------
 *     __ __ __  ___________
 *    / //_//  |/  /  _/ __ \	  .--.
 *   / ,<  / /|_/ // // /_/ /	 /.-. '----------.
 *  / /| |/ /  / // // ____/ 	 \'-' .--"--""-"-'
 * /_/ |_/_/  /_/___/_/      	  '--'
 *
 * -----------------------------------------------------------------
 * Description:
 * A KLMSServerNetworkService provides a Thread, which waits until a
 * connection is made to the ServerSocket. An incoming request is
 * going to be executed in a KLMSServerHandler and via the
 * ExecutorService.
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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ntb.inf.kmip.skeleton.KMIPSkeleton;

class KLMSServerNetworkService implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(KLMSServerNetworkService.class);
	private final SSLServerSocket serverSocket;
	private final ExecutorService pool;
	private final KMIPSkeleton skeleton;
	private boolean firstConnection = true;

	public KLMSServerNetworkService(ExecutorService pool, SSLServerSocket serverSocket, KMIPSkeleton skeleton) {
		this.serverSocket = serverSocket;
		this.pool = pool;
		this.skeleton = skeleton;
	}

	@Override
    public void run() {
		try {
			while ( true ) {
				// Wait until a connection is made
			    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
				// Start the client-thread via the ExecutorService
			    if (firstConnection) {
			        firstConnection = false;
			        String socketInfo = getSocketInfo(clientSocket);
			        logger.debug("Client socket information: \n" + socketInfo);
			    }
				pool.execute(new KLMSServerHandler(clientSocket, skeleton));
			}
		} catch (IOException e) {
			e.printStackTrace();
	    }
	    finally {
	    	logger.info("Finish KLMSServerNetworkService and disable new tasks from being submitted. ExecutorService.shutdown()...");
	    	pool.shutdown();
	    	try {
	    		// Wait until all tasks have completed execution after a shutdown request, or the timeout occurs
	    		if (!pool.awaitTermination(5L, TimeUnit.SECONDS)) {
	    			// Cancel currently executing tasks
	    			pool.shutdownNow();
	    			// Wait a while for tasks to respond to being cancelled
	    			if (!pool.awaitTermination(5L, TimeUnit.SECONDS)){
	    				logger.error("Pool / ExecutorService did not terminate");
	    			}
	    		}
	    		// Close ServerSocket if it is not closed already
	    		if (!serverSocket.isClosed()) {
	    			logger.info("End of KLMSServerNetworkService. ServerSocket.close()...");
	    			serverSocket.close();
	    		}
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    }
	}

    private String getSocketInfo(SSLSocket s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Client Socket class: " + s.getClass() + "\n");
        sb.append("   Remote address = " + s.getInetAddress().toString() + "\n");
        sb.append("   Remote port = " + s.getPort() + "\n");
        sb.append("   Local socket address = " + s.getLocalSocketAddress().toString() + "\n");
        sb.append("   Local address = " + s.getLocalAddress().toString() + "\n");
        sb.append("   Local port = " + s.getLocalPort() + "\n");
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
