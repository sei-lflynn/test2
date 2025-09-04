package ch.ntb.inf.kmip.stub.transport;
/**
 * KMIPStubTransportLayerHTTP.java
 * -----------------------------------------------------------------
 *     __ __ __  ___________
 *    / //_//  |/  /  _/ __ \	  .--.
 *   / ,<  / /|_/ // // /_/ /	 /.-. '----------.
 *  / /| |/ /  / // // ____/ 	 \'-' .--"--""-"-'
 * /_/ |_/_/  /_/___/_/      	  '--'
 *
 * -----------------------------------------------------------------
 * Description for class
 * The KMIPStubTransportLayerHTTP provides the communication between a server and a client via HTTP,
 * using a HttpUrlConnection.
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ntb.inf.kmip.utils.KMIPUtils;

/**
 * The KMIPStubTransportLayerHTTP provides the communication between a server and a client via HTTP,
 * using a HttpUrlConnection.
 */
public class KMIPStubTransportLayerHTTP implements KMIPStubTransportLayerInterface{

	private static final Logger logger = LoggerFactory.getLogger(KMIPStubTransportLayerHTTP.class);
	private String url;
	private String ssoCookie = null;

	public KMIPStubTransportLayerHTTP() {
		logger.info("KMIPStubTransportLayerHTTP initialized...");
	}

	@Override
    public ArrayList<Byte> send(final ArrayList<Byte> al) throws Exception {
		String kmipRequest = KMIPUtils.convertArrayListToHexString(al);
		try {
			String parameter = "KMIPRequest="+URLEncoder.encode(kmipRequest,"UTF-8");
			logger.debug("kmip-service request = " + parameter);
			String responseString = executePost(url, ssoCookie, parameter);
			logger.debug("kmip-service response = " + responseString);
			return KMIPUtils.convertHexStringToArrayList(responseString);
		} catch (Exception e) {
			logger.error("send(): " + e);
			throw e;
		}
	}

	private static String executePost(final String targetURL, final String ssoCookie, final String urlParameters)
		throws Exception {
		URL url;
		HttpURLConnection connection = null;
		try { // Create connection
			url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type","./.");


			if (ssoCookie != null) {
				logger.debug("executePost() set ssoCookie");
				connection.setRequestProperty("Cookie", ssoCookie);
			}
			connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();
		} catch (Exception e) {
			logger.error("executePost(): " + e);
			throw e;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

    /**
     * Sets the configuration parameters of the transport layer.
     *
     * @param configParams :  the configuration parameters to be set.
     */
    @Override
    public void setConfigParameters(final Map<String, Object> configParams) {
        url = (String) configParams.get(CFG_KMS_URI);
        ssoCookie = (String) configParams.get(CFG_SSO_COOKIE);
        logger.info("KMIP Service URI = " + url);
        if (ssoCookie == null) {
          logger.info("ssoCookie is null");
        } else {
          logger.debug("ssoCookie length = " + ssoCookie.length());
        }
    }

}
