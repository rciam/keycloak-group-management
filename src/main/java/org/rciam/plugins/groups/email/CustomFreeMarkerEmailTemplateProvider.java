package org.rciam.plugins.groups.email;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.UserModel;
import org.rciam.plugins.groups.helpers.Utils;

public class CustomFreeMarkerEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

    //must be changed to a ui ( account console url)
    private static final String enrollmentUrl = "realms/{realmName}/account/#/groups/groupenrollments?id={id}";
    private static final String enrollmentStartUrl = "realms/{realmName}/account/#/enroll?groupPath={path}";
    private static final String finishGroupInvitation = "realms/{realmName}/account/#/invitation/{id}";
    private static final String adminGroupPageUrl = "realms/{realmName}/account/#/groups/admingroups/{id}?tab=admins";
    private static final String membersGroupPageUrl = "realms/{realmName}/account/#/groups/admingroups/{id}?tab=members";

    private static final String subgroupsStr = " and its subgroups: ";
    private static final String subgroupsHtmlStr = " and its subgroups:<br>";
    private static final String comma = ",";
    private static final String JUSTIFICATION = " with the following justification: ";
    private static final String USER_REACTIVATION_JUSTIFICATION = "The reactivation was issued with the following justification: ";

    private String signatureMessage;

    public CustomFreeMarkerEmailTemplateProvider(KeycloakSession session) {
        super(session);
    }

    public CustomFreeMarkerEmailTemplateProvider setSignatureMessage(String signatureMessage) {
        this.signatureMessage = signatureMessage;
        return this;
    }

    public void sendGroupAdminEmail(boolean isAdded, String groupPath, String groupId, UserModel groupadmin) throws EmailException {
        String title = isAdded ? "addGroupAdminSubject" : "removeGroupAdminSubject";
        String text1 = isAdded ? "added" : "removed";
        String text2 = isAdded ? "to" : "from";
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        String text3 = isAdded ? "For more information about the group, please visit the following link: " + uriInfo.getBaseUri().toString() + adminGroupPageUrl.replace("{realmName}", realm.getName()).replace("{id}", groupId) : "";
        attributes.put("text1", text1);
        attributes.put("text2", text2);
        attributes.put("text3", text3);
        attributes.put("groupPath", groupPath);
        attributes.put("groupadmin", groupadmin.getFirstName() + " " + groupadmin.getLastName());
        attributes.put("signatureMessage", signatureMessage);
        send(title, Stream.of(groupPath).collect(Collectors.toList()), "add-remove-group-admin.ftl", attributes);
    }

    public void sendSuspensionEmail(String groupPath, List<String> subgroupPaths, String justification) throws EmailException {
        attributes.put("justification", justification != null ? JUSTIFICATION + justification :"");
        attributes.put("groupPath", groupPath);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupPaths));
        attributes.put("signatureMessage", signatureMessage);
        send("suspendMemberSubject", "suspend-member.ftl", attributes);
    }

    public void sendSuspensionEmailToAdmins(String groupPath, List<String> subgroupPaths, String justification, UserModel member, UserModel groupadmin) throws EmailException {
        attributes.put("userName", user.getFirstName() + " " + user.getLastName());
        attributes.put("justification", justification != null ? JUSTIFICATION + justification :"");
        attributes.put("groupPath", groupPath);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupPaths));
        attributes.put("subgroupsStrHtml", subgroupsHtmlStrCalculation(subgroupPaths));
        attributes.put("memberName", member.getFirstName() + " " + member.getLastName());
        attributes.put("groupadmin", groupadmin.getFirstName() + " " + groupadmin.getLastName());
        attributes.put("signatureMessage", signatureMessage);
        send("suspendMemberGroupAdminsSubject", "suspend-member-group-admins.ftl", attributes);
    }

    public void sendActivationEmail(String groupPath, List<String> subgroupPaths, String justification) throws EmailException {
        attributes.put("userName", user.getFirstName() + " " + user.getLastName());
        attributes.put("justification", justification != null ? USER_REACTIVATION_JUSTIFICATION + justification :"");
        attributes.put("groupPath", groupPath);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupPaths));
        attributes.put("subgroupsStrHtml", subgroupsHtmlStrCalculation(subgroupPaths));
        attributes.put("signatureMessage", signatureMessage);
        send("activateMemberSubject", "activate-member.ftl", attributes);
    }

    public void sendActivationEmailToAdmins(String groupPath, List<String> subgroupPaths, String justification, UserModel member, UserModel groupadmin) throws EmailException {
        attributes.put("userName", user.getFirstName() + " " + user.getLastName());
        attributes.put("justification", justification != null ? JUSTIFICATION + justification :"");
        attributes.put("groupPath", groupPath);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupPaths));
        attributes.put("subgroupsStrHtml", subgroupsHtmlStrCalculation(subgroupPaths));
        attributes.put("memberName", member.getFirstName() + " " + member.getLastName());
        attributes.put("groupadmin", groupadmin.getFirstName() + " " + groupadmin.getLastName());
        attributes.put("signatureMessage", signatureMessage);
        send("activateMemberGroupAdminsSubject", "activate-member-group-admins.ftl", attributes);
    }


    public void sendInviteGroupAdminEmail(String invitationId, UserModel groupadmin, String groupName, String groupPath, String description, long invitationExpirationHour) throws EmailException {
        attributes.put("groupadmin", groupadmin.getFirstName() + " " + groupadmin.getLastName());
        attributes.put("groupName", groupName);
        attributes.put("groupPath", groupPath);
        attributes.put("description", description !=  null ? description : "");
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        URI baseUri = uriInfo.getBaseUri();
        attributes.put("urlLink", baseUri.toString() + finishGroupInvitation.replace("{realmName}", realm.getName()).replace("{id}", invitationId));
        attributes.put("invitationExpirationHour", invitationExpirationHour);
        attributes.put("signatureMessage", signatureMessage);
        send("inviteGroupAdminSubject", "invite-group-admin.ftl", attributes);
    }

    public void sendAcceptRejectEnrollmentEmail(boolean isAccepted, String groupPath, String justification) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("action", isAccepted ? "accepted" : "rejected");
        attributes.put("justification", justification != null ? "Comment from reviewer: " + justification : "");
        attributes.put("signatureMessage", signatureMessage);
        send(isAccepted ? "acceptEnrollmentSubject" : "rejectEnrollmentSubject", Stream.of(groupPath).collect(Collectors.toList()), "accept-reject-enrollment.ftl", attributes);
    }

    public void sendGroupAdminEnrollmentCreationEmail(UserModel userRequest, String groupPath, List<String> groupRoles, String reason, String enrollmentId) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("userName", userRequest.getFirstName() + " " + userRequest.getLastName());
        String groupPathStr = groupPath;
        if (groupRoles != null && !groupRoles.isEmpty()) {
            StringBuilder sb = new StringBuilder(groupPathStr).append(" group with roles: ");
            groupRoles.stream().forEach(role -> sb.append(role).append(", "));
            groupPathStr = StringUtils.removeEnd(sb.toString(), ", ");
        }
        attributes.put("groupname", groupPathStr);
        attributes.put("reason", reason != null ?  "Justification: " + reason : "");
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        URI baseUri = uriInfo.getBaseUri();
        attributes.put("urlLink", baseUri.toString() + enrollmentUrl.replace("{realmName}", realm.getName()).replace("{id}", enrollmentId));
        attributes.put("signatureMessage", signatureMessage);
        send("groupadminEnrollmentRequestCreationSubject", Stream.of(groupPath).collect(Collectors.toList()), "groupadmin-enrollment-creation.ftl", attributes);
    }

    public void sendExpiredGroupMemberEmailToAdmin(UserModel userRequest, String groupname, List<String> subgroupsPaths) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("userName", userRequest.getFirstName() + " " + userRequest.getLastName());
        attributes.put("groupname", groupname);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupsPaths));
        attributes.put("signatureMessage", signatureMessage);
        send("adminGroupUserRemovalSubject", "expired-group-membership-admin.ftl", attributes);
    }

    public void sendExpiredGroupMemberEmailToUser(String groupPath, String groupId, List<String> subgroupsPaths, String serverUrl) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupsPaths));
        attributes.put("urlLink", (serverUrl != null ? serverUrl : "localhost:8080") + enrollmentStartUrl.replace("{realmName}", realm.getName()).replace("{path}", groupPath));
        attributes.put("signatureMessage", signatureMessage);
        send("userRemovalSubject", "expired-group-membership-user.ftl", attributes);
    }

    public void sendExpiredGroupMembershipNotification(String groupPath, String date, String groupId, String serverUrl) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupname", groupPath);
        attributes.put("date", date);
        attributes.put("urlLink", (serverUrl != null ? serverUrl : "localhost:8080") + enrollmentStartUrl.replace("{realmName}", realm.getName()).replace("{path}", groupPath));
        attributes.put("signatureMessage", signatureMessage);
        session.getContext().setRealm(this.realm);
        send("groupMembershipExpirationNotificationSubject", "group-membership-expiration-notification.ftl", attributes);
    }

    public void sendGroupInvitationEmail(UserModel groupadmin, String groupName, String groupPath, String description, boolean withoutAcceptance, List<String> groupRoles, String id, Long  invitationExpirationHour) throws EmailException {
        attributes.put("groupadmin", groupadmin.getFirstName() + " " + groupadmin.getLastName());
        attributes.put("groupName", groupName);
        attributes.put("groupPath", groupPath);
        attributes.put("description", description !=  null ? description : "");
        if (groupRoles != null && !groupRoles.isEmpty()) {
            StringBuilder sb = new StringBuilder("Your roles in the group:<br><strong>");
            StringBuilder sbText = new StringBuilder("\nYour roles in the group:\n");
            groupRoles.stream().forEach(role -> {
                sb.append("•").append(role).append("<br>");
                sbText.append("•").append(role).append("\n");
            });
            sb.append("</strong>");
            attributes.put("groupRoles", sb.toString());
            attributes.put("groupRolesText", sbText.toString());
        } else  {
            attributes.put("groupRoles", "");
            attributes.put("groupRolesText", "");
        }
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        URI baseUri = uriInfo.getBaseUri();
        attributes.put("urlLink", baseUri.toString() + (withoutAcceptance ? finishGroupInvitation : enrollmentStartUrl).replace("{realmName}", realm.getName()).replace("{id}", id).replace("{path}", groupPath));
        attributes.put("invitationExpirationHour", invitationExpirationHour != null ? "expire after "+invitationExpirationHour+" hours" :"not expire");
        attributes.put("signatureMessage", signatureMessage);
        send("groupInvitationSubject", "user-group-invitation.ftl", attributes);
    }

    public void sendAcceptInvitationEmail(UserModel userModel, String groupPath, String groupId, boolean forMember, List<String> groupRoles) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("userfullname", userModel.getFirstName() + " " + userModel.getLastName());
        attributes.put("email", userModel.getEmail());
        //attributes.put("type", forMember ? "member" : "admin");
        String groupPathStr = groupPath;
        if (forMember && groupRoles != null && !groupRoles.isEmpty()) {
            StringBuilder sb2 = new StringBuilder(groupPathStr).append(" with roles : ");
            groupRoles.stream().forEach(role -> sb2.append(role).append(", "));
            groupPathStr = StringUtils.removeEnd(sb2.toString(), ", ");
        }
        attributes.put("groupPath", groupPathStr);
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        String groupUrl = forMember ? uriInfo.getBaseUri().toString() + membersGroupPageUrl : uriInfo.getBaseUri().toString() + adminGroupPageUrl;
        attributes.put("groupUrl", groupUrl.replace("{realmName}", realm.getName()).replace("{id}", groupId));
        attributes.put("signatureMessage", signatureMessage);
        send("groupAcceptInvitationSubject", Stream.of(groupPath).collect(Collectors.toList()), "accept-invitation.ftl", attributes);
    }

    public void sendRejectionInvitationEmail(UserModel userModel, String groupPath, String groupId, boolean forMember, List<String> groupRoles) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("userfullname", userModel.getFirstName() + " " + userModel.getLastName());
        attributes.put("email", userModel.getEmail());
       // attributes.put("type", forMember ? "member" : "admin");
        String groupPathStr = groupPath;
        if (forMember && groupRoles != null && !groupRoles.isEmpty()) {
            StringBuilder sb2 = new StringBuilder(groupPathStr).append(" with roles : ");
            groupRoles.stream().forEach(role -> sb2.append(role).append(", "));
            groupPathStr = StringUtils.removeEnd(sb2.toString(), ", ");
        }
        attributes.put("groupPath", groupPathStr);
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        String groupUrl = forMember ? uriInfo.getBaseUri().toString() + membersGroupPageUrl : uriInfo.getBaseUri().toString() + adminGroupPageUrl;
        attributes.put("groupUrl", groupUrl.replace("{realmName}", realm.getName()).replace("{id}", groupId));
        attributes.put("signatureMessage", signatureMessage);
        send("groupRejectionInvitationSubject", Stream.of(groupPath).collect(Collectors.toList()), "reject-invitation.ftl", attributes);
    }

    public void sendInvitionAdminInformationEmail(String email, boolean forMember, String groupname, UserModel admin, List<String> groupRoles) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("email", email);
        attributes.put("type", forMember ? "member" : "admin");
        if (forMember && groupRoles != null && !groupRoles.isEmpty()) {
            StringBuilder sb2 = new StringBuilder(groupname).append(" with roles : ");
            groupRoles.stream().forEach(role -> sb2.append(role).append(", "));
            groupname = StringUtils.removeEnd(sb2.toString(), ", ");
        }
        attributes.put("groupname", groupname);
        attributes.put("adminFullName", admin.getFirstName() + " " + admin.getLastName());
        attributes.put("signatureMessage", signatureMessage);
        send(forMember ? "groupInvitationSubject" : "groupInvitationAdminInformSubject", "invitation-admin-inform.ftl", attributes);
    }

    public void sendAddRemoveAdminAdminInformationEmail(boolean added, String groupPath, String groupId, UserModel adminAdded, UserModel adminAction) throws EmailException {
        attributes.put("text", added ? "added as" : "removed from");
        attributes.put("groupPath", groupPath);
        attributes.put("adminAdded", adminAdded.getFirstName() + " " + adminAdded.getLastName());
        attributes.put("adminAction", adminAction.getFirstName() + " " + adminAction.getLastName());
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        attributes.put("groupUrl",  uriInfo.getBaseUri().toString() + adminGroupPageUrl.replace("{realmName}", realm.getName()).replace("{id}", groupId));
        attributes.put("signatureMessage", signatureMessage);
        send(added ? "addGroupAdminAdminInformationSubject" : "removeGroupAdminAdminInformationSubject", Stream.of(groupPath).collect(Collectors.toList()), "add-remove-groupadmin-admin-inform.ftl", attributes);
    }

    public void sendMemberUpdateAdminInformEmail(String groupPath, UserModel userChanged, UserModel admin, LocalDate validFrom, LocalDate membershipExpiresAt, List<String> roles) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("userFullName", userChanged.getFirstName() + " " + userChanged.getLastName());
        attributes.put("adminFullName", admin.getFirstName() + " " + admin.getLastName());
        attributes.put("validFrom", validFrom.format(Utils.dateFormatter));
        attributes.put("membershipExpiresAt",  membershipExpiresAt != null ? membershipExpiresAt.format(Utils.dateFormatter) : "N/A");
        attributes.put("roles", roles.stream().collect(Collectors.joining(",")));
        attributes.put("signatureMessage", signatureMessage);
        send("memberUpdateAdminInformSubject", Stream.of(groupPath).collect(Collectors.toList()),"member-update-admin-inform.ftl", attributes);
    }

    public void sendMemberUpdateUserInformEmail(String groupPath, UserModel admin, LocalDate validFrom, LocalDate membershipExpiresAt, List<String> roles) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("adminFullName", admin.getFirstName() + " " + admin.getLastName());
        attributes.put("validFrom", validFrom.format(Utils.dateFormatter));
        attributes.put("membershipExpiresAt", membershipExpiresAt != null ? membershipExpiresAt.format(Utils.dateFormatter) : "N/A");
        attributes.put("roles", roles.stream().collect(Collectors.joining(",")));
        attributes.put("signatureMessage", signatureMessage);
        send("memberUpdateUserInformSubject", Stream.of(groupPath).collect(Collectors.toList()), "member-update-user-inform.ftl", attributes);
    }

    public void sendDeleteGroupAdminInformationEmail(String groupPath, UserModel admin) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("adminFullName", admin.getFirstName() + " " + admin.getLastName());
        attributes.put("signatureMessage", signatureMessage);
        send("deleteGroupAdminInformationSubject", "delete-group-admin-inform.ftl", attributes);
    }

    public void sendRolesChangesUserEmail(String groupPath, List<String> roles) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("roles", roles.stream().collect(Collectors.joining(",")));
        attributes.put("rolesHtml", rolesHtmlStrCalculation(roles));
        attributes.put("signatureMessage", signatureMessage);
        send("rolesChangesUserSubject", "roles-changes-user.ftl", attributes);
    }

    public void sendRolesChangesGroupAdminEmail(String groupPath, List<String> roles, UserModel admin, UserModel userChanged) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("roles", roles.stream().collect(Collectors.joining(",")));
        attributes.put("rolesHtml", rolesHtmlStrCalculation(roles));
        attributes.put("adminFullName", admin.getFirstName() + " " + admin.getLastName());
        attributes.put("userFullName", userChanged.getFirstName() + " " + userChanged.getLastName());
        attributes.put("signatureMessage", signatureMessage);
        send("rolesChangesGroupAdminSubject", "roles-changes-group-admin.ftl", attributes);
    }

    private String subgroupsHtmlStrCalculation(List<String> subgroupsPaths) {
        if (subgroupsPaths.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder(subgroupsHtmlStr);
        subgroupsPaths.stream().forEach(x -> sb.append("- ").append(x).append("<br>"));
        return sb.toString();
    }

    private String subgroupsStrCalculation(List<String> subgroupsPaths) {
        if (subgroupsPaths.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder(subgroupsStr);
        subgroupsPaths.stream().forEach(x -> sb.append(x).append(comma));
        return StringUtils.removeEnd(sb.toString(), comma);
    }

    private String rolesHtmlStrCalculation(List<String> roles) {
        if (roles.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        roles.stream().forEach(x -> sb.append("<br>- ").append(x));
        return sb.toString();
    }


}
