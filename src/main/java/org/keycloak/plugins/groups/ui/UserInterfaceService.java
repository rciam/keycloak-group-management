package org.keycloak.plugins.groups.ui;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.helpers.MimeTypesResolver;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.ClientsManagementService;
import org.keycloak.services.resources.RealmsResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Providers;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.List;
import java.util.stream.Collectors;

//PLEASE REMOVE THIS CLASS
@Deprecated
public class UserInterfaceService {

    private static final Logger logger = Logger.getLogger(UserInterfaceService.class);

    private static final String PATH_SEPARATOR = System.getProperty("file.separator");

    private static final String BASE_FOLDER = "webapp";

    protected KeycloakSession session;


    public UserInterfaceService(KeycloakSession session) {
        this.session = session;
    }

    @Path("{any: .*}")
    @GET
    public Response get(@PathParam("any") List<PathSegment> segments) {
        RealmModel realm = session.getContext().getRealm();
        String path = BASE_FOLDER + "/" + segments.stream().map(PathSegment::getPath).collect(Collectors.joining(PATH_SEPARATOR));
        try {
            return Response.ok().type(mimeType(path)).entity(getFileAsStream(path)).build();
        }
        catch(FileNotFoundException ex){
            return Response.status(Response.Status.NOT_FOUND).entity("Requested file not found!").build();
        }
    }


//    private static String purify(String str){
//        return str.replaceAll("[\\..\\~]", ""); //replace all . and ~
//    }


    private InputStream getFileAsStream(String filePath) throws FileNotFoundException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            throw new FileNotFoundException("File not found! " + filePath);
        } else {
            return inputStream;
        }
    }

    private String mimeType(String filePath){
        String mimeType = URLConnection.guessContentTypeFromName(filePath);
        if(mimeType == null)
            mimeType = MimeTypesResolver.getMimeType(filePath);
        return mimeType;
    }

}
