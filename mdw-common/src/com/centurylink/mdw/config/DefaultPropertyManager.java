/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.constant.PaaSConstants;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.startup.StartupException;

public class DefaultPropertyManager extends PropertyManager {

    protected Properties properties = new Properties();
    private String containerName;
    private String servletRealPath;
    private String mainPropertyFileName;
    public static final String APP_CONFIG_NAME = "mdw.application.config.name";

    /**
     * This method loads mdw.properties, application.properties and any property file defined for {@link #APP_CONFIG_NAME}
     * The property mdw.application.config.name takes property file names seperated by , without the .properties extension
     * @param containerName
     */
    public DefaultPropertyManager(String containerName, String servletRealPath) throws StartupException {
        this.containerName = containerName;
        this.servletRealPath = servletRealPath;
        mainPropertyFileName = getMainPropertyFileName();
        if (mainPropertyFileName == null) {
            if ("standalone".equals(ApplicationContext.getRuntimeEnvironment())) {
                return; // no props load
            }
            else {
                throw new StartupException(StartupException.NO_PROPERTY_FILE_FOUND,
                        "No mdw.properties configuration file is found");
            }
        }

        loadPropertiesFromFile(null, mainPropertyFileName, true, true);
        loadPropertiesFromFile(null, APPLICATION_PROPERTIES_FILE_NAME, true, false);

        // Application config files should be in same place as mdw.properties.
        String appPropertyFiles = this.getStringProperty(APP_CONFIG_NAME);
        if (appPropertyFiles != null) {
            for (String fileName : appPropertyFiles.split(",")) {
                loadPropertiesFromFile(null, fileName + ".properties", true, false);
            }
        }
    }

    public void loadPropertiesFromFile(Properties tempProperties, String fileName,
            boolean verboseSuccess, boolean verboseFailure) {
        try {
            loadFromFile(tempProperties == null ? properties : tempProperties, fileName);
            if (verboseSuccess)
                System.out.println("Loaded properties from file " + fileName);
        }
        catch (PropertyException e) {
            if (verboseFailure) {
                System.out.println("Cannot load properties from " + fileName);
                e.printStackTrace();
            }
        }
    }

    public void refreshCache() throws PropertyException {
        refreshProperties(true);
    }

    public void clearCache() {
        properties.clear();
    }

    /**
     * Null means not found.
     */
    private String getMainPropertyFileName() throws StartupException {
        String configLoc = getPropertyFileLocation();
        File file = new File(configLoc == null ? MDW_PROPERTIES_FILE_NAME : (configLoc+MDW_PROPERTIES_FILE_NAME));
        if (file.exists())
            return MDW_PROPERTIES_FILE_NAME;
        URL url = this.getClass().getClassLoader().getResource(MDW_PROPERTIES_FILE_NAME);
        if (url != null)
            return MDW_PROPERTIES_FILE_NAME;
        if (containerName.equals(NamingProvider.TOMCAT)) {
            file = new File(servletRealPath + "/../../conf/" + MDW_PROPERTIES_FILE_NAME);
            if (file.exists())
                return MDW_PROPERTIES_FILE_NAME;
        }
        return null;
    }

    private void refreshProperties(boolean printStackTraceWhenError) throws PropertyException {

        Properties tempProperties = new Properties();
        getSources().clear();

        // 1. load properties from mdw.properties or ApplicationProperties.xml from configuration directory
        loadPropertiesFromFile(tempProperties, mainPropertyFileName, true, true);

        // 2. load properties from application.properties
        loadPropertiesFromFile(tempProperties, APPLICATION_PROPERTIES_FILE_NAME, true, false);

        // Application config files should be in same place as mdw.properties.
        String appPropertyFiles = this.getStringProperty(APP_CONFIG_NAME);
        if (appPropertyFiles != null) {
            for (String fileName : appPropertyFiles.split(",")) {
                loadPropertiesFromFile(tempProperties, fileName + ".properties", true, false);
            }
        }

        // update the cache
        updateCache(tempProperties);
    }

    final protected void updateCache(Properties tempProperties) {
        synchronized (properties) {
            properties.clear();
            properties.putAll(tempProperties);
        }
    }

    /**
     * Returns the handle to the property based on the passed in GroupName and the property Name
     * @param group
     * @param name
     * @return Value defined for the property as String
     */
    public String getStringProperty(String group, String name) {
        if (group != null)
            return this.getStringProperty(group + "/" + name);
        else
            return this.getStringProperty(name);
    }

     /**
         * returns the properties for group.
         * When the group is specified with ApplicationProperties.xml
         * (old style), the names of the properties returned
         * will not contain the group names and '/', for backward
         * compatibility. When the group is in the new style,
         * where a group is any property name prefix terminated by a '.',
         * the property names returned will contain the
         * whole property names including the group names.
         * @param group
         * @return Properties for the group
         */
    public Properties getProperties(String group) {
        Properties props = new Properties();
        int k = group.length();
        for (Object key : properties.keySet()) {
            String propname = (String)key;
            int l = propname.length();
            char ch = l>k?propname.charAt(k):' ';
            if ((ch=='.'||ch=='/') && propname.startsWith(group)) {
                if (ch=='.') props.put(propname, properties.get(propname));
                else props.put(propname.substring(k+1), properties.get(propname));
            }
        }
        return props;
    }

    public String getStringProperty(String name) {
        return (String)properties.get(name);
    }


    final protected void loadFromFile(Properties properties, String filename)
    throws PropertyException {
        String configLoc = getPropertyFileLocation();
        InputStream stream = null;
        try {
            File file = new File(configLoc==null?filename:(configLoc+filename));
            if (file.exists()) {
                stream = new FileInputStream(file);
            }
            else if (containerName.equals(NamingProvider.TOMCAT) && PaaSConstants.PAAS_VCAP_APPLICATION == null) {
                stream = new FileInputStream(servletRealPath + "/../../conf/" + filename);
            }
            else {
                stream = this.getClass().getClassLoader().getResourceAsStream(filename);
                if (stream == null)  // try context classloader
                    stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
            }
            if (stream==null)
                throw new PropertyException("Property file does not exist: " + filename);
            else
                loadFromStream(properties, stream, filename);
        }
        catch (Exception ex) {
            throw new PropertyException(-1, "Failed to load properties from " + filename, ex);
        }
    }

    public Properties getAllProperties() {
        return properties;
    }

    public void setStringProperty(String name, String value) {
        if (value==null || value.length() == 0)
            properties.remove(name);
        else
            properties.put(name, value);
    }
}