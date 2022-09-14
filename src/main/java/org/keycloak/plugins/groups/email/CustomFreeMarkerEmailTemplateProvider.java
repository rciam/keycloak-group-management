package org.keycloak.plugins.groups.email;

import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.FreeMarkerUtil;

public class CustomFreeMarkerEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

    public CustomFreeMarkerEmailTemplateProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        super(session, freeMarker);
    }
    public void sendVoAdminEmail(String groupName, boolean isAdded) throws EmailException {
        String title = isAdded ? "addVoAdminSubject" : "removeVoAdminSubject";
        String text1 = isAdded ? "added" : "removed";
        String text2 = isAdded ? "to" : "from";
        attributes.put("text1", text1);
        attributes.put("text2", text2);
        attributes.put("groupname", groupName);
        send(title, "add-remove-vo-admin.ftl", attributes);
    }

    public void sendSuspensionEmail(String groupName,String justification) throws EmailException, EmailException {
        attributes.put("justification", justification);
        attributes.put("groupname", groupName);
        send("suspendMemberSubject", "suspend-member.ftl", attributes);
    }

    public void sendActivationEmail(String groupName,String justification) throws EmailException, EmailException {
        attributes.put("justification", justification);
        attributes.put("groupname", groupName);
        send("activateMemberSubject", "activate-member.ftl", attributes);
    }
}
