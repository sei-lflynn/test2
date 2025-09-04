package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;

import ch.ntb.inf.kmip.attributes.ContactInformation;
import ch.ntb.inf.kmip.attributes.CryptographicUsageMask;
import ch.ntb.inf.kmip.attributes.InitialDate;
import ch.ntb.inf.kmip.attributes.KMIPAttributeValue;
import ch.ntb.inf.kmip.attributes.LastChangeDate;
import ch.ntb.inf.kmip.attributes.Name;
import ch.ntb.inf.kmip.attributes.ObjectType;
import ch.ntb.inf.kmip.attributes.State;
import ch.ntb.inf.kmip.attributes.UniqueIdentifier;
import ch.ntb.inf.kmip.container.KMIPBatch;
import ch.ntb.inf.kmip.container.KMIPContainer;
import ch.ntb.inf.kmip.kmipenum.EnumObjectType;
import ch.ntb.inf.kmip.kmipenum.EnumOperation;
import ch.ntb.inf.kmip.kmipenum.EnumResultStatus;
import ch.ntb.inf.kmip.objects.base.Attribute;
import ch.ntb.inf.kmip.objects.managed.CryptographicObject;
import ch.ntb.inf.kmip.objects.managed.ManagedObject;
import ch.ntb.inf.kmip.objects.managed.PrivateKey;
import ch.ntb.inf.kmip.objects.managed.PublicKey;
import ch.ntb.inf.kmip.objects.managed.SymmetricKey;
import ch.ntb.inf.kmip.stub.KMIPStub;

/**
 * The KmipKeyServiceClient connects to the Key Management Service (KMS) for retrieving keys using the KMIP protocol.
 *
 *
 */
public class KmipKeyServiceClient implements KeyServiceClient {
    public static final String PRIVATE_KEY_SUFFIX = "_:_private";

    private final KmcCryptoManager cryptoManager;
    private final KMIPStub kmipServer;
    private final KeyCache keyCache;

    private static final Logger logger = LoggerFactory.getLogger(KmipKeyServiceClient.class);

    /**
     * Constructor of KmipKeyServiceClient.
     *
     * @param cryptoManager The cryptoManger whose config parameters will be used to configure the connections to the KMIP server.
     * @throws KmcCryptoException if error in connecting to KMS.
     *
     */
    public KmipKeyServiceClient(final KmcCryptoManager cryptoManager) throws KmcCryptoException {
        this.cryptoManager = cryptoManager;
        Properties configParams = cryptoManager.getConfigParameters();
        Map<String, Object> configMap = new HashMap<String, Object>();
        for (final String name: configParams.stringPropertyNames()) {
            configMap.put(name, configParams.getProperty(name));
        }
        try {
            kmipServer = new KMIPStub(configMap);
        } catch (Exception e) {
            String msg = "Failed to create transport for KMIP server: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.KMS_CONNECTION_ERROR, msg, e);
        }
        keyCache = KeyCache.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getKmcKey(final String keyRef) throws KmcCryptoException {
        KmcKey key = keyCache.getKey(keyRef);
        if (key != null) {
            return key;
        }
        String keyId = locateCryptographicKey(keyRef);
        if (keyId == null) {
            String msg = "Cryptographic key \"" + keyRef + "\" does not exist in KMS.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
        }
        CryptographicObject cryptoObject = getCryptographicObject(keyId);
        if (cryptoObject == null) {
            String msg = "The retrieved key is null.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
        }
        key = new KmcKey(keyId);
        key.setCryptographicObject(cryptoObject);
        List<Attribute> attributes = getAllAttributes(keyId);
        key.setAttributes(attributes);
        logger.info("Retrieved key {} from KMS at {}", keyRef, this.cryptoManager.getKeyManagementServiceURI());
        keyCache.putKey(keyRef, key);
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getKmcKey(final String keyRef, final String keyPass) throws KmcCryptoException {
        // This method is intended for retrieving key from keystore
        String msg = "This method is intended for retrieving key from keystore.";
        logger.error(msg);
        throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Key getCryptoKey(final String keyRef, final int keyUsage) throws KmcCryptoException {
        // When we create a key pair, the PublicKey has the keyRef as its name, so given the keyRef KMS
        // returns the PublicKey.  The PrivateKey has its name keyRef + PRIVATE_KEY_SUFFIX.
        KmcKey kmcKey = getKmcKey(keyRef);
        checkKeyState(kmcKey, keyUsage);
        Key key = kmcKey.getJavaKey();
        if (key instanceof java.security.PublicKey) {
            if ((keyUsage & USAGE_MASK_ENCRYPT) == USAGE_MASK_ENCRYPT) {
                return key;
            } else if ((keyUsage & USAGE_MASK_DECRYPT) == USAGE_MASK_DECRYPT) {
                // get the PrivateKey
                return getPrivateKey(keyRef);
            } else if ((keyUsage & USAGE_MASK_SIGN) == USAGE_MASK_SIGN) {
                return getPrivateKey(keyRef);
            } else if ((keyUsage & USAGE_MASK_VERIFY) == USAGE_MASK_VERIFY) {
                return key;
            } else {
                String msg = "Unsupported usage (" + keyUsage + ") of key pair: " + keyRef;
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
            }
        } else {
            return key;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Key getCryptoKey(final String keyRef, final String keyPass, final int keyUsage)
            throws KmcCryptoException {
        // This method is intended for keystore
        String msg = "Retrieving keys from KMS does not need the keyPass argument.";
        logger.error(msg);
        throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
    }

    /**
     * Check if the state of the key is appropriate for the key usage,
     * e.g. deactivated key cannot be used for encryption but can be used for decryption.
     * @param kmcKey The KmcKey object that contains the key state.
     * @param keyUsage How the key is intended to be used.
     * @throws KmcCryptoException if the key state is not appropriate for the usage.
     */
    private void checkKeyState(final KmcKey kmcKey, final int keyUsage)
            throws KmcCryptoException {
        String keyState = kmcKey.getState();
        if ((keyUsage & USAGE_MASK_ENCRYPT) == USAGE_MASK_ENCRYPT
                || (keyUsage & USAGE_MASK_MAC_GENERATE) == USAGE_MASK_MAC_GENERATE
                || (keyUsage & USAGE_MASK_SIGN) == USAGE_MASK_SIGN) {
            if (!ACTIVE.equals(keyState)) {
                String msg = "The key state of " + kmcKey.getKeyRef() + " is " + keyState
                        + ".  The operation requires an Active key.";
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
            }
        } else if ((keyUsage & USAGE_MASK_DECRYPT) == USAGE_MASK_DECRYPT
            || (keyUsage & USAGE_MASK_MAC_VERIFY) == USAGE_MASK_MAC_VERIFY
            || (keyUsage & USAGE_MASK_VERIFY) == USAGE_MASK_VERIFY) {
            if (!(ACTIVE.equals(keyState)
                    || DEACTIVATED.equals(keyState)
                    || COMPROMISED.equals(keyState))) {
                String msg = "The key state of " + kmcKey.getKeyRef() + " is " + keyState
                        + ".  The operation requires an Active, Deactivated, or Compromised key.";
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
            }
        }
    }

    /**
     * Returns the private key of a key pair.
     * @param keyRef The keyRef of the key pair.
     * @return the Java PrivateKey object of a key pair.
     */
    private Key getPrivateKey(final String keyRef) throws KmcCryptoException {
        KmcKey kmcKey = getKmcKey(keyRef + PRIVATE_KEY_SUFFIX);
        return kmcKey.getJavaKey();
    }

    /**
     * Returns the KMIP unique identifier a key.
     * @param keyRef The KMC KeyRef of the key.
     * @return The KMIP unique identifier of the key.
     * @throws KmcCryptoException if error locating the key.
     */
    private String locateCryptographicKey(final String keyRef) throws KmcCryptoException {
        KMIPContainer request = createLocateKeyRequest(keyRef);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception during locating key from KMS: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        }
        if (response == null) {
            String msg = "KMIP request returns null.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.KMS_CONNECTION_ERROR, msg, null);
        }
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                for (Attribute a : b.getAttributes()) {
                    if (a instanceof UniqueIdentifier) {
                        KMIPAttributeValue[] values = a.getValues();
                        if (values.length > 0) {
                            logger.debug(a.getAttributeName() + ": " + values[0].getValueString());
                            return values[0].getValueString();
                        }
                    }
                }
            } else {
                logger.error("Operation Locate key \"" + keyRef + " failed.");
                return null;
            }
        }
        logger.info("Cannot locate key \"" + keyRef);
        return null;
    }

    /**
     * Creates a KMIP request for locating a key with its keyRef.
     * @param keyRef The KMC KeyRef of the key.
     * @param keyType The key type.
     * @return The KMIPContainer of the locate request.
     */
    private KMIPContainer createLocateKeyRequest(final String keyRef) {
        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set operation
        batch.setOperation(EnumOperation.Locate);

        // Set attribute: Name
        Name nameAttribute = new Name();
        nameAttribute.setValue(keyRef, "namevalue");
        nameAttribute.setValue("UninterpretedTextString", "nametype");
        batch.addAttribute(nameAttribute);

        return container;
    }

    private CryptographicObject getCryptographicObject(final String keyId)
            throws KmcCryptoException {
        KMIPContainer request = createGetCryptographicObjectRequest(keyId);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in Get cryptographic object from KMS: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        }
        if (response == null) {
            String msg = "KMIP request returns null.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.KMS_CONNECTION_ERROR, msg, null);
        }
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                ManagedObject object = b.getManagedObject();
                if (object instanceof SymmetricKey) {
                    return (CryptographicObject) object;
                } else if (object instanceof PublicKey) {
                    return (CryptographicObject) object;
                } else if (object instanceof PrivateKey) {
                    return (CryptographicObject) object;
                }
            } else {
                String msg = "Failed to get cryptographic object for keyId: " + keyId;
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
            }
        }
        String msg = "KMS returns null to get cryptographic object for KeyId: " + keyId;
        logger.error(msg);
        throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
    }

    private KMIPContainer createGetCryptographicObjectRequest(final String keyId) {
        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set Operation and Attribute
        batch.setOperation(EnumOperation.Get);

        // Set Key Unique Identifier
        UniqueIdentifier a = new UniqueIdentifier();
        a.setValue(keyId, null);
        batch.addAttribute(a);

        return container;
    }

    /**
     * Retrieves the attributes of the key from its keyId.
     * @param keyId The KMIP unique identifier of the key.
     * @return The list of key attributes.
     * @throws KmcKeyMgtException if error in retrieving the key.
     */
    private List<Attribute> getAllAttributes(final String keyId) throws KmcCryptoException {
        List<Attribute> attributes = new ArrayList<Attribute>();
        KMIPContainer request = createGetAllAttributesRequest(keyId);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in creating key from KMS: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        }
        if (response == null) {
            String msg = "KMIP request returns null.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.KMS_CONNECTION_ERROR, msg, null);
        }
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                logger.debug("getAllAttributes() for \"" + keyId + "\" done.");
                logger.trace(response.toString());
                for (Attribute a : b.getAttributes()) {
                    logger.debug("Attribute: " + a);
                    attributes.add(a);
                }
            } else {
                String msg = "Operation Get Attributes for key \"" + keyId + "\" failed."
                                + "\nResponse: " + response.toString();
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
            }
        }
        return attributes;
    }

    private KMIPContainer createGetAllAttributesRequest(final String keyId) {
        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set Operation
        batch.setOperation(EnumOperation.GetAttributes);

        // Set keyId
        UniqueIdentifier a = new UniqueIdentifier();
        a.setValue(keyId, null);
        batch.addAttribute(a);

        // list of basic attributes
        batch.addAttribute(new ContactInformation());
        batch.addAttribute(new Name());
        batch.addAttribute(new ObjectType());
        batch.addAttribute(new State());
        batch.addAttribute(new ObjectType(EnumObjectType.Default));
        batch.addAttribute(new CryptographicUsageMask());
        batch.addAttribute(new InitialDate());
        batch.addAttribute(new LastChangeDate());

        return container;
    }

}
