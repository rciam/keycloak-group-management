package org.keycloak.plugins.groups.email;

import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.FreeMarkerUtil;

public class CustomFreeMarkerEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

    public CustomFreeMarkerEmailTemplateProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        super(session, freeMarker);
    }
    public void sendGroupAdminEmail(String groupName, boolean isAdded) throws EmailException {
        String title = isAdded ? "addGroupAdminSubject" : "removeGroupAdminSubject";
        String text1 = isAdded ? "added" : "removed";
        String text2 = isAdded ? "to" : "from";
        attributes.put("text1", text1);
        attributes.put("text2", text2);
        attributes.put("groupname", groupName);
        send(title, "add-remove-group-admin.ftl", attributes);
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

    public void sendInviteGroupAdminEmail(String groupadmin, String groupname, String url) throws EmailException, EmailException {
        attributes.put("fullname", user.getFirstName()+" "+user.getLastName());
        attributes.put("groupadmin", groupadmin);
        attributes.put("groupname", groupname);
        attributes.put("url", url);
        send("inviteGroupAdminSubject", "invite-group-admin.ftl", attributes);
    }

    public void sendAcceptRejectEnrollmentEmail(boolean isAccepted, String groupname, String justification) throws EmailException, EmailException {
        attributes.put("fullname", user.getFirstName()+" "+user.getLastName());
        attributes.put("groupname", groupname);
        attributes.put("action", isAccepted ? "accepted" : "rejected");
        attributes.put("justification", justification != null ? justification : "");
        send(isAccepted ? "acceptEnrollmentSubject" : "rejectEnrollmentSubject", "accept-reject-enrollment.ftl", attributes);
    }
}
