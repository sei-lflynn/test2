/**
 * KLMSServerHandler.java
 * -----------------------------------------------------------------
 *     __ __ __  ___________
 *    / //_//  |/  /  _/ __ \	  .--.
 *   / ,<  / /|_/ // // /_/ /	 /.-. '----------.
 *  / /| |/ /  / // // ____/ 	 \'-' .--"--""-"-'
 * /_/ |_/_/  /_/___/_/      	  '--'
 *
 * -----------------------------------------------------------------
 * Description:
 * A KLMSServerHandler provides a Thread, which handles the client-
 * requests, as well the read and write service to the client.
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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ntb.inf.kmip.skeleton.KMIPSkeleton;



class KLMSServerHandler implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(KLMSServerHandler.class);
	private final Socket clientSocket;
	private final KMIPSkeleton skeleton;

	KLMSServerHandler(final Socket clientSocket, final KMIPSkeleton skeleton) {
		this.clientSocket = clientSocket;
		this.skeleton = skeleton;
	}

    @Override
    public void run() {
		try {
	    	logger.info("Running KMIP service: " + Thread.currentThread());

	    	DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
	    	InputStream is = clientSocket.getInputStream();

	    	byte[] resultBuff = readBytes(is);
	    	ArrayList<Byte> request = toArrayList(resultBuff);

	    	//logger.debug("delay 10 sec to test read timeout");
	    	//try { Thread.sleep(10000); } catch (Exception e) {};

	    	byte[] res = processRequest(request);
	    	writeBytes(res, outToClient);
	    } catch (Exception e){
	    	logger.error("Error while processing Request!");
	    	e.printStackTrace();
	    }
	    finally {
    		try {
	    		if (!clientSocket.isClosed()){
	    		    System.out.println("close socket");
	    			logger.info("End of KLMSServerHandler. clientSocket.close()...");
	    			clientSocket.close();
	    		}
	    	} catch (IOException e){
	    		e.printStackTrace();
	    	}
	    }
    }

    /*
    // Old code in which the client sends EOF by calling shutdownOutput().
    private byte[] readBytes(InputStream is) throws IOException{
    	byte[] resultBuff = new byte[0];
        byte[] buff = new byte[1024];
        int k = -1;
        while((k = is.read(buff, 0, buff.length)) > -1) {
            byte[] tbuff = new byte[resultBuff.length + k]; // temp buffer size = bytes already read + bytes last read
            System.arraycopy(resultBuff, 0, tbuff, 0, resultBuff.length); // copy previous bytes
            System.arraycopy(buff, 0, tbuff, resultBuff.length, k);  // copy current lot
            resultBuff = tbuff; // call the temp buffer as your result buff
        }
        logger.info(resultBuff.length + " bytes read.");
        return resultBuff;
    }
    */

    // The SSL client no longer sends EOF by calling shutdownOutput().  Use available() to detect end of request.
    // Hopefully there won't be more data coming.
    private byte[] readBytes(final InputStream is) throws IOException {
        byte[] resultBuff = new byte[0];
        byte[] buff = new byte[1024];
        int k = -1;
        do {
            k = is.read(buff, 0, buff.length);
            if (k == -1) {
                logger.debug("read -1.");
                break;
            }
            byte[] tbuff = new byte[resultBuff.length + k]; // temp buffer size = bytes already read + bytes last read
            System.arraycopy(resultBuff, 0, tbuff, 0, resultBuff.length); // copy previous bytes
            System.arraycopy(buff, 0, tbuff, resultBuff.length, k);  // copy current lot
            resultBuff = tbuff; // call the temp buffer as your result buff
        } while (is.available() > 0);
        logger.debug(resultBuff.length + " bytes read.");
        return resultBuff;
    }

    private ArrayList<Byte> toArrayList(final byte[] resultBuff) {
        ArrayList<Byte> request = new ArrayList<Byte>();
        StringBuilder sb = new StringBuilder();
        for(byte b:resultBuff){
        	sb.append(String.format("%02X", b));
        	request.add(b);
        }
        logger.debug(sb.toString());
        return request;
    }

    private byte[] processRequest(final ArrayList<Byte> request) {
        ArrayList<Byte> response = skeleton.processRequest(request);
        byte[] res = new byte[response.size()];
        for(int i = 0; i < res.length; i++){
        	res[i] = response.get(i);
        }
        return res;
    }

    private void writeBytes(final byte[] res, final DataOutputStream outToClient) throws IOException {
        if (logger.isDebugEnabled()) {
            // ignore response, just wants to log the response.
            toArrayList(res);
        }
        logger.debug("Write Data to Client...");
        outToClient.write(res);
        outToClient.flush();
        outToClient.close();
    }

}
