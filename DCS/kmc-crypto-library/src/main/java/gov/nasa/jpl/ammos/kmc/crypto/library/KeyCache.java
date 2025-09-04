package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache keys retrieved from Key Management Service (KMS).
 *
 *
 */
public class KeyCache {
    private static KeyCache instance;
    private final ConcurrentHashMap<String, KmcKey> keyCache;

    private static final Logger logger = LoggerFactory.getLogger(KeyCache.class);
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    private KeyCache() {
        keyCache = new ConcurrentHashMap<String, KmcKey>();
    }

    public synchronized static KeyCache getInstance() {
        if (instance == null) {
            instance = new KeyCache();
        }
        return instance;
    }

    public KmcKey getKey(String keyRef) {
        KmcKey key = keyCache.get(keyRef);
        if (key == null) {
            logger.debug("Key {} is not in KMS key cache.", keyRef);
        } else {
            logger.info("Retrieved key {} from KMS key cache.", keyRef);
            audit.info("KeyCache: Retrieved key {} from KMS key cache.", keyRef);
        }
        return key;
    }

    public void putKey(String keyRef, KmcKey kmcKey) {
        keyCache.put(keyRef, kmcKey);
        logger.info("Saved key {} to KMS key cache.", keyRef);
        audit.info("KeyCache: Saved key {} to KMS key cache.", keyRef);
    }

}
