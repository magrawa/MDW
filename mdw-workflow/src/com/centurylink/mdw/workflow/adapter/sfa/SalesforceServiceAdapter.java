/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter.sfa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.AdapterActivityBase;
import com.centurylink.mdw.xml.DomHelper;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.bind.TypeMapper;
import com.sforce.ws.parser.XmlOutputStream;

/**
 * This adapter allows the developer or workflow designer to connect to either
 * <p>
 * <li>An internal SFA sandbox</li>
 * or
 * <li>and external SFA sandbox</li>
 * </p>
 * <p>
 * The internal SFA sandbox assumes that we only need an endpoint defined, and
 * perhaps a user/pass combination. The external SFA sandbox requires that a
 * certificate be exported from the sandbox and imported into a keystore which
 * is available to the container (usually ServiceMix)
 *
 * @author aa70413
 *
 */
@Tracked(LogLevel.TRACE)
public class SalesforceServiceAdapter extends AdapterActivityBase {

    // Username resident on the SFA sandbox
    public static final String SFA_USER = "MDW.SFA.USER";
    // Password on the SFA sandbox. This is usually the password followed by
// token generated by SFA
    public static final String SFA_PASS = "MDW.SFA.PASS";
    // Keystore in the container that contains the imported certificate
    public static final String KEYSTORE = "SFA_KEYSTORE";
    // Keystore password
    public static final String KEYSTORE_PASSWORD = "SFA_KEYSTORE_PASSWORD";
    // Truststore in the container
    public static final String TRUSTSTORE = "SFA_TRUSTSTORE";
    // Truststore password
    public static final String TRUSTSTORE_PASSWORD = "SFA_TRUSTSTORE_PASSWORD";
    // Truststore type, usually JKS
    public static final String TRUSTSTORE_TYPE = "SFA_TRUSTSTORE_TYPE";
    // Endpoint for Salesforce Sandbox
    public static final String ENDPOINT_URI = "SFA_ENDPOINTURI";
    // CREATE UPDATE or QUERY
    public static final String SFA_FUNCTION = "SFA_FUNCTION";

    /**
     * Private enum for SFA function type
     *
     * @author aa70413
     *
     */
    private enum SFA_FUNCTION_TYPE {
        QUERY, CREATE, UPDATE
    }

    @Override
    public final boolean isSynchronous() {
        return true;
    }

    /**
     * Returns an EnterpriseConnection Salesforce object based on the configured
     * keystores/passwords etc
     * <p>
     * The steps to do are as follows for the situation where SFA is outside the
     * firewall and the container is ServiceMix:
     * <li>Export the certificate from the SFA sandbox</li>
     * <li>Either create a keystore that is available to the container
     * (ServiceMix), or use an existing one, like cacerts.</li>
     * <li>Import the certificate from the SFA sandbox into the cacerts keystore
     * on ServiceMix, e.g $JAVA_HOME/bin/keytool -import -v -alias salesforce
     * -file /tmp/iain/force/QXGSFA.cer -keystore cacerts.jks</li>
     * <li>Specific implementations should override this method</li>
     * </p>
     */
    @Override
    protected Object openConnection() throws ConnectionException {
        EnterpriseConnection enterpriseConnection = null;
        try {
            /**
             * For testing and debugging you can use the properties below
             */
            // System.setProperty("javax.net.ssl.keyStore", "etc/cacerts");
            // System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
            // System.setProperty("javax.net.ssl.trustStore", "etc/cacerts");
            // System.setProperty("javax.net.ssl.trustStorePassword","changeit");
            // System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            // System.setProperty("javax.net.debug","all");
            /**
             * Set up the authentication details
             */
            String keyStore = getValueFor(KEYSTORE);
            String keyStorePassword = getValueFor(KEYSTORE_PASSWORD);
            String trustStore = getValueFor(TRUSTSTORE);
            String trustStorePassword = getValueFor(TRUSTSTORE_PASSWORD);
            String trustStoreType = getValueFor(TRUSTSTORE_TYPE);

            if (!StringUtils.isEmpty(keyStore))
                System.setProperty("javax.net.ssl.keyStore", keyStore);
            if (!StringUtils.isEmpty(keyStorePassword))
                System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
            if (!StringUtils.isEmpty(trustStore))
                System.setProperty("javax.net.ssl.trustStore", trustStore);
            if (!StringUtils.isEmpty(trustStorePassword))
                System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            if (!StringUtils.isEmpty(trustStoreType))
                System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);

            ConnectorConfig config = new ConnectorConfig();
            String user = getValueFor(SFA_USER);
            String passw = getValueFor(SFA_PASS);
            if (!StringUtils.isEmpty(user))
                config.setUsername(user);
            if (!StringUtils.isEmpty(passw))
                config.setPassword(passw);
            // config.setAuthEndpoint("http://test.salesforce.com/services/Soap/c/28.0");
            config.setTraceMessage(true);
            // config.setAuthEndpoint(PropertyManager.getProperty(""));

            // try {
            if (logger.isMdwDebugEnabled()) {
                logger.debug("keystore=" + keyStore);
                logger.debug("keystorePassword=" + keyStorePassword);
                logger.debug("truststore=" + trustStore);
                logger.debug("truststorePassword=" + trustStorePassword);
                logger.debug("truststoreType=" + trustStoreType);
                logger.debug("user=" + user);
                logger.debug("pass=" + passw);
                logger.debug("config=" + config);
            }
            // create a connection to Enterprise API -- authentication occurs
            enterpriseConnection = com.sforce.soap.enterprise.Connector.newConnection(config);
            // display some current settings
            if (logger.isInfoEnabled()) {
                logger.info("Auth EndPoint: " + config.getAuthEndpoint());
                logger.info("Service EndPoint: " + config.getServiceEndpoint());
                logger.info("Username: " + config.getUsername());
                logger.info("SessionId: " + config.getSessionId());
            }
        }
        catch (Exception ex) {
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
        }
        return enterpriseConnection;
    }

    /**
     * Delegates and closes the Salesforce connection
     */
    @Override
    protected void closeConnection(Object connection) {
        if (connection != null) {
            try {
                ((EnterpriseConnection) connection).logout();
            }
            catch (com.sforce.ws.ConnectionException ex) {
                logger.warn("Unable to close Salesforce connection, connection=" + connection+"exception ");
            }
            connection = null;
        }
    }

    /**
     * Get the user-configured endpoint
     *
     * @return
     * @throws PropertyException
     */
    protected String getEndpointUri() throws PropertyException {
        return getAttributeValueSmart(ENDPOINT_URI);
    }

    /**
     *
     * @return SFA user as string
     * @throws PropertyException
     */
    protected String getSFAUser() throws PropertyException {
        return getValueFor(SFA_USER);
    }

    /**
     *
     * @return SFA password as string
     * @throws PropertyException
     */
    protected String getSFAPass() throws PropertyException {
        return getValueFor(SFA_PASS);
    }

    /**
     *
     * @return Keystore as string
     * @throws PropertyException
     */
    protected String getKeystore() throws PropertyException {
        return getValueFor(KEYSTORE);
    }

    /**
     *
     * @return keystore password as string
     * @throws PropertyException
     */
    protected String getKeystorePassword() throws PropertyException {
        return getValueFor(KEYSTORE_PASSWORD);
    }

    /**
     *
     * @return Truststore as string
     * @throws PropertyException
     */
    protected String getTruststore() throws PropertyException {
        return getValueFor(TRUSTSTORE);
    }

    /**
     *
     * @return Truststore password as string
     * @throws PropertyException
     */
    protected String getTruststorePassword() throws PropertyException {
        return getValueFor(TRUSTSTORE_PASSWORD);
    }

    /**
     * Utility method to return the value of an attribute by first trying the
     * attribute value and, if unsuccessful, then checking the property file.
     *
     * @param attributeName
     * @return
     * @throws PropertyException
     */
    protected String getValueFor(String attributeName) throws PropertyException {
        String attrValue = getAttributeValueSmart(attributeName);
        if (StringUtils.isEmpty(attrValue)) {
            return PropertyManager.getProperty(attributeName);
        }
        else {
            return attrValue;
        }
    }

    /**
     * For <b>CREATEs</b> and <b>UPDATEs</b>, the requestData parameter must be
     * of the form:
     * <p>
     * MefLocation__c loc = new MefLocation__c();
     * loc.setAddress1__c("Address1"); loc.setZipCode__c(1234.0); SObject[]
     * requestData = new SObject[] { loc };
     * </p>
     * <p>
     * For <b>QUERYs</b>, the requestData parameter must be of the form of an
     * Object query string, e.g.:
     * <p>
     * String requestData =
     * "SELECT Name,Id FROM mefServiceInterfaceCfgTable__c WHERE Name = 'Iain'";
     * </p>
     * </p>
     *
     * @return For CREATE and UPDATE returns SaveResult[] , for QUERY returns
     *         QueryResult
     */
    public Object invoke(Object conn, Object requestData) throws AdapterException {
        if (logger.isDebugEnabled()) {
            logger.debug("Salesforce Adapter - invoke: start");
        }
        EnterpriseConnection enterpriseConnection = (EnterpriseConnection) conn;

        SFA_FUNCTION_TYPE functionType;
        try {
            functionType = SFA_FUNCTION_TYPE.valueOf(getSFAFunctionType());
            if (logger.isDebugEnabled()) {
                logger.debug("Salesforce Adapter - invoke: functiontype=" + functionType);
            }
            if (SFA_FUNCTION_TYPE.CREATE.equals(functionType)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Salesforce Adapter - invoking create ");
                }
                return enterpriseConnection.create((SObject[]) requestData);
            }
            else if (SFA_FUNCTION_TYPE.UPDATE.equals(functionType)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Salesforce Adapter - invoking update ");
                }
                return enterpriseConnection.update((SObject[]) requestData);
            }
            else if (SFA_FUNCTION_TYPE.QUERY.equals(functionType)) {
                if (logger.isDebugEnabled()) {

                    logger.debug("Salesforce Adapter - invoking query using "
                            + (String) requestData);
                }
                return DomHelper.toDomDocument((toXML(enterpriseConnection
                        .query((String) requestData))));
            }
            else
                throw new AdapterException("Unsupported SFA Function: " + functionType.toString());
        }
        catch (Exception ex) {
            int responseCode = -1;
            throw new AdapterException(responseCode, ex.getMessage(), ex);
        }

    }

    private String toXML(QueryResult queryResult) throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        XmlOutputStream stream = new XmlOutputStream(bao, false);
        javax.xml.namespace.QName qname = new javax.xml.namespace.QName("mdw-sforce");
        queryResult.write(qname, stream, new TypeMapper());
        bao.flush();
        bao.close();
        stream.close();
        if (logger.isDebugEnabled()) {
            logger.debug("Returning " + bao.toString());
        }
        return bao.toString();
    }

    /**
     * Gets the attribute for function type (should be CREATE/UPDATE or QUERY)
     *
     * @return Either CREATE, UPDATE or QUERY
     * @throws AdapterException
     */
    protected String getSFAFunctionType() throws AdapterException {
        try {
            String sfaFunctionType = getAttributeValueSmart(SFA_FUNCTION);
            if (sfaFunctionType == null)
                throw new AdapterException("SFA adapter required attribute missing: "
                        + SFA_FUNCTION);
            return sfaFunctionType;
        }
        catch (PropertyException ex) {
            throw new AdapterException(-1, ex.getMessage(), ex);
        }
    }

}
