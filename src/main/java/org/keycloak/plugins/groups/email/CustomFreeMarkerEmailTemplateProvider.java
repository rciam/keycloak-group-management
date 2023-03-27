package org.keycloak.plugins.groups.email;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.UserModel;
import org.keycloak.theme.FreeMarkerUtil;

public class CustomFreeMarkerEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

    //must be changed to a ui ( account console url)
    private static final String enrollmentUrl = "/realms/{realmName}/agm/account/group-admin/enroll-request/{id}";
    private static final String enrollmentStartUrl = "/realms/{realmName}/agm/account/user/group/{id}";
    private static final String finishGroupInvitation = "/realms/{realmName}/agm/account/user/groups/invitation/{id}/accept";

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

    public void sendSuspensionEmail(String groupName,String justification) throws EmailException {
        attributes.put("justification", justification);
        attributes.put("groupname", groupName);
        send("suspendMemberSubject", "suspend-member.ftl", attributes);
    }

    public void sendActivationEmail(String groupName,String justification) throws EmailException {
        attributes.put("justification", justification);
        attributes.put("groupname", groupName);
        send("activateMemberSubject", "activate-member.ftl", attributes);
    }

    public void sendInviteGroupAdminEmail(String invitationId, UserModel groupadmin, String groupname) throws EmailException {
        StringBuilder sb = new StringBuilder();
        if (user.getFirstName() != null){
            sb.append(user.getFirstName()).append(" ");
        }
        if (user.getLastName() != null){
            sb.append(user.getLastName());
        } else  if (user.getFirstName() == null){
            sb.append("Sir/Madam");
        }
        attributes.put("fullname", sb.toString());
        attributes.put("groupadmin", groupadmin.getFirstName()+" "+groupadmin.getLastName());
        attributes.put("groupname", groupname);
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        URI baseUri = uriInfo.getBaseUri();
        attributes.put("url",baseUri.toString() + finishGroupInvitation.replace("{realmName}",realm.getName()).replace("{id}",invitationId));
        send("inviteGroupAdminSubject", "invite-group-admin.ftl", attributes);
    }

    public void sendAcceptRejectEnrollmentEmail(boolean isAccepted, String groupname, String justification) throws EmailException{
        attributes.put("fullname", user.getFirstName()+" "+user.getLastName());
        attributes.put("groupname", groupname);
        attributes.put("action", isAccepted ? "accepted" : "rejected");
        attributes.put("justification", justification != null ? justification : "");
        send(isAccepted ? "acceptEnrollmentSubject" : "rejectEnrollmentSubject", "accept-reject-enrollment.ftl", attributes);
    }

    public void sendGroupAdminEnrollmentCreationEmail(UserModel userRequest, String groupname, List<String> groupRoles, String reason, String enrollmentId) throws EmailException {
        attributes.put("fullname", user.getFirstName()+" "+user.getLastName());
        attributes.put("user", userRequest.getFirstName()+" "+userRequest.getLastName());
        if (groupRoles != null && !groupRoles.isEmpty()) {
            StringBuilder sb = new StringBuilder(groupname).append(" with roles : ");
            groupRoles.stream().forEach(role -> sb.append(role).append(", "));
            groupname= StringUtils.removeEnd(sb.toString(),", ")+" and ";
        }
        attributes.put("groupname", groupname);
        attributes.put("reason", reason != null ? reason : "");
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        URI baseUri = uriInfo.getBaseUri();
        attributes.put("url",baseUri.toString() + enrollmentUrl.replace("{realmName}",realm.getName()).replace("{id}",enrollmentId));
        send( "groupadminEnrollmentRequestCreationSubject", "groupadmin-enrollment-creation.ftl", attributes);
    }

    public void sendExpiredGroupMemberEmailToAdmin(UserModel userRequest, String groupname) throws EmailException {
        attributes.put("fullname", user.getFirstName()+" "+user.getLastName());
        attributes.put("user", userRequest.getFirstName()+" "+userRequest.getLastName());
        attributes.put("groupname", groupname);
        send( "adminGroupUserRemovalSubject", "expired-group-membership-admin.ftl", attributes);
    }

    public void sendExpiredGroupMemberEmailToUser(String groupname, String groupId, String serverUrl) throws EmailException {
        attributes.put("fullname", user.getFirstName()+" "+user.getLastName());
        attributes.put("groupname", groupname);
        attributes.put("url",(serverUrl != null ? serverUrl : "localhost:8080") + enrollmentUrl.replace("{realmName}",realm.getName()).replace("{id}",groupId));
        send( "userRemovalSubject", "expired-group-membership-user.ftl", attributes);
    }

    public void sendExpiredGroupMembershipNotification(String groupname, String date, String groupId, String serverUrl) throws EmailException {
        attributes.put("fullname", user.getFirstName()+" "+user.getLastName());
        attributes.put("groupname", groupname);
        attributes.put("date", date);
        attributes.put("url",(serverUrl != null ? serverUrl : "localhost:8080") + enrollmentUrl.replace("{realmName}",realm.getName()).replace("{id}",groupId));
        session.getContext().setRealm(this.realm);
        send( "groupMembershipExpirationNotificationSubject", "group-membership-expiration-notification.ftl", attributes);
    }

    public void sendGroupInvitationEmail(UserModel groupadmin, String groupname, boolean withoutAcceptance, List<String> groupRoles, String id) throws EmailException {
        StringBuilder sb = new StringBuilder();
        if (user.getFirstName() != null){
            sb.append(user.getFirstName()).append(" ");
        }
        if (user.getLastName() != null){
            sb.append(user.getLastName());
        } else  if (user.getFirstName() == null){
            sb.append("Sir/Madam");
        }

        attributes.put("fullname", sb.toString());
        attributes.put("groupadmin", groupadmin.getFirstName()+" "+groupadmin.getLastName());
        if (withoutAcceptance && groupRoles != null && !groupRoles.isEmpty()) {
            StringBuilder sb2 = new StringBuilder(groupname).append(" with roles : ");
            groupRoles.stream().forEach(role -> sb2.append(role).append(", "));
            groupname= StringUtils.removeEnd(sb2.toString(),", ");
        }
        attributes.put("groupname", groupname);
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        URI baseUri = uriInfo.getBaseUri();
        attributes.put("url",baseUri.toString() + (withoutAcceptance ? finishGroupInvitation:enrollmentStartUrl).replace("{realmName}",realm.getName()).replace("{id}",id));
        send( "groupInvitationSubject", "user-group-invitation.ftl", attributes);
    }

    public void sendAcceptInvitationEmail(UserModel userModel, String groupname, boolean forMember) throws EmailException {
        attributes.put("fullname", user.getFirstName()+" "+user.getLastName());
        attributes.put("userfullname", userModel.getFirstName()+" "+userModel.getLastName());
        attributes.put("email", userModel.getEmail());
        attributes.put("groupname", groupname);
        attributes.put("type", forMember ? "member" : "group admin");
        send("groupAcceptInvitationSubject", "accept-invitation.ftl", attributes);
    }
}
