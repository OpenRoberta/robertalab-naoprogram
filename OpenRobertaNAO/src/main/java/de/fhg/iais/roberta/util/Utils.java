package de.fhg.iais.roberta.util;

import java.io.FileReader;
import java.util.Properties;

public class Utils {

    private static final String PROPERTY_DEFAULT_PATH = "openRobertaUSB.properties";
    private static Properties robertaProperties;
    String result = "";
    static {
        robertaProperties = loadProperties(null);
    }

    public static void setSystemDefaultProperty(String propertyURI) {
        robertaProperties = loadProperties(propertyURI);
    }

    public static Properties loadProperties(String propertyURI) {
        Properties properties = new Properties();
        try {
            if ( propertyURI == null || propertyURI.trim().equals("") ) {
                properties.load(Utils.class.getClassLoader().getResourceAsStream(Utils.PROPERTY_DEFAULT_PATH));
            } else if ( propertyURI.startsWith("file:") ) {
                String filesystemPathName = propertyURI.substring(5);
                properties.load(new FileReader(filesystemPathName));
            } else if ( propertyURI.startsWith("classpath:") ) {
                String classPathName = propertyURI.substring(10);

                properties.load(Utils.class.getClassLoader().getResourceAsStream(classPathName));
                robertaProperties = properties;
            } else {
                return null;
            }
            return properties;
        } catch ( Exception e ) {
            return null;
        }
    }

    public static String getRobertaProperty(String propertyName) {
        return robertaProperties.getProperty(propertyName);
    }

    public static Properties getRobertaProperties() {
        return robertaProperties;
    }
}
