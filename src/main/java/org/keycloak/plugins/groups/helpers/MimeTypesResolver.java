package org.keycloak.plugins.groups.helpers;

import org.apache.commons.io.FilenameUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MimeTypesResolver {

    private static final Logger logger = Logger.getLogger(MimeTypesResolver.class);

    private static Properties mimeTypes = new Properties();

    static {
        try {
            InputStream resourceStream = MimeTypesResolver.class.getClassLoader().getResourceAsStream("mimetypes.properties");
            mimeTypes.load(resourceStream);
        }
        catch(IOException ex){
            logger.error("Could not initiate the mimetypes resolver, expect serious problems.");
            ex.printStackTrace();
        }
    }

    public static String getMimeType(String file){
        return mimeTypes.getProperty(FilenameUtils.getExtension(file));
    }

}
