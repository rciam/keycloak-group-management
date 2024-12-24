package org.rciam.plugins.groups.email;

import java.net.URI;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.UserModel;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.rciam.plugins.groups.helpers.Utils;

public class CustomFreeMarkerEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

    //must be changed to a ui ( account console url)
    private static final String enrollmentUrl = "realms/{realmName}/account/#/groups/groupenrollments?id={id}";
    private static final String SHOW_GROUPS_URL = "realms/{realmName}/account/#/groups/showgroups";
    private static final String MEMBER_URL = "realms/{realmName}/account/#/groups/showgroups/{id}";
    private static final String enrollmentStartUrl = "/realms/{realmName}/account/#/enroll?groupPath={path}";
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
        String text1 = isAdded ? "added as a" : "removed from being";
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        String text3 = isAdded ? "For more information about the group, please visit the following link:\n " + uriInfo.getBaseUri().toString() + adminGroupPageUrl.replace("{realmName}", realm.getName()).replace("{id}", groupId) : "";
        attributes.put("text1", text1);
        attributes.put("text3", text3);
        attributes.put("groupPath", groupPath);
        attributes.put("groupadmin", groupadmin.getFirstName() + " " + groupadmin.getLastName());
        attributes.put("signatureMessage", signatureMessage);
        send(title, Stream.of(groupPath).collect(Collectors.toList()), "add-remove-group-admin.ftl", attributes);
    }

    public void sendSuspensionEmail(String groupPath, List<String> subgroupPaths, String justification) throws EmailException {
        attributes.put("userName", user.getFirstName() + " " + user.getLastName());
        if (justification != null) {
            attributes.put("justification", new StringBuilder("\n\nJustification:\n").append(justification));
            attributes.put("justificationHtml", new StringBuilder("<br><br>Justification:<br>").append(justification).append("</p>"));
        } else{
            attributes.put("justification", "");
            attributes.put("justificationHtml", "");
        }
        attributes.put("groupPath", groupPath);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupPaths));
        attributes.put("signatureMessage", signatureMessage);
        //, Stream.of(groupPath).collect(Collectors.toList())
        send("suspendMemberSubject", Stream.of(groupPath).collect(Collectors.toList()), "suspend-member.ftl", attributes);
    }

    public void sendSuspensionEmailToAdmins(String groupPath, List<String> subgroupPaths, String justification, UserModel member, UserModel groupadmin) throws EmailException {
        attributes.put("userName", user.getFirstName() + " " + user.getLastName());
        if (justification != null) {
            attributes.put("justification", JUSTIFICATION + "\n"+justification);
            attributes.put("justificationHtml", JUSTIFICATION +"<br>" +justification);
        } else{
            attributes.put("justification", "");
            attributes.put("justificationHtml", "");
        }
        attributes.put("groupPath", groupPath);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupPaths));
        attributes.put("subgroupsStrHtml", subgroupsHtmlStrCalculation(subgroupPaths));
        attributes.put("memberName", member.getFirstName() + " " + member.getLastName());
        attributes.put("groupadmin", groupadmin.getFirstName() + " " + groupadmin.getLastName());
        attributes.put("signatureMessage", signatureMessage);
        //, Stream.of(groupPath).collect(Collectors.toList())
        send("suspendMemberGroupAdminsSubject", Stream.of(groupPath).collect(Collectors.toList()), "suspend-member-group-admins.ftl", attributes);
    }

    public void sendActivationEmail(String groupPath, List<String> subgroupPaths, String justification) throws EmailException {
        attributes.put("userName", user.getFirstName() + " " + user.getLastName());
        if (justification != null) {
            attributes.put("justification", USER_REACTIVATION_JUSTIFICATION + "\n"+justification);
            attributes.put("justificationHtml", USER_REACTIVATION_JUSTIFICATION +"<br>" +justification);
        } else{
            attributes.put("justification", "");
            attributes.put("justificationHtml", "");
        }
        attributes.put("groupPath", groupPath);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupPaths));
        attributes.put("subgroupsStrHtml", subgroupsHtmlStrCalculation(subgroupPaths));
        attributes.put("signatureMessage", signatureMessage);
        send("activateMemberSubject", Stream.of(groupPath).collect(Collectors.toList()), "activate-member.ftl", attributes);
    }

    public void sendActivationEmailToAdmins(String groupPath, List<String> subgroupPaths, String justification, UserModel member, UserModel groupadmin) throws EmailException {
        attributes.put("userName", user.getFirstName() + " " + user.getLastName());
        if (justification != null) {
            attributes.put("justification", JUSTIFICATION + "\n"+justification);
            attributes.put("justificationHtml", JUSTIFICATION +"<br>" +justification);
        } else{
            attributes.put("justification", "");
            attributes.put("justificationHtml", "");
        }
        attributes.put("groupPath", groupPath);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupPaths));
        attributes.put("subgroupsStrHtml", subgroupsHtmlStrCalculation(subgroupPaths));
        attributes.put("memberName", member.getFirstName() + " " + member.getLastName());
        attributes.put("groupadmin", groupadmin.getFirstName() + " " + groupadmin.getLastName());
        attributes.put("signatureMessage", signatureMessage);
        send("activateMemberGroupAdminsSubject", Stream.of(groupPath).collect(Collectors.toList()), "activate-member-group-admins.ftl", attributes);
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
        send("inviteGroupAdminSubject", Stream.of(groupPath).collect(Collectors.toList()), "invite-group-admin.ftl", attributes);
    }

    public void sendAcceptRejectEnrollmentEmail(boolean isAccepted, String groupPath, String justification) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("action", isAccepted ? "accepted" : "rejected");
        if (justification != null) {
            attributes.put("justification", "Comment from reviewer: "+justification);
            attributes.put("justificationHtml", "Comment from reviewer: <b>" +justification+"</b>");
        } else{
            attributes.put("justification", "");
            attributes.put("justificationHtml", "");
        }
        attributes.put("signatureMessage", signatureMessage);
        send(isAccepted ? "acceptEnrollmentSubject" : "rejectEnrollmentSubject", Stream.of(groupPath).collect(Collectors.toList()), "accept-reject-enrollment.ftl", attributes);
    }

    public void sendAcceptRejectEnrollmentAdminInfoEmail(boolean isAccepted, UserModel admin, UserModel memberUser, String groupPath, String groupId, String justification) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("adminFullname", admin.getFirstName() + " " + admin.getLastName());
        attributes.put("memberFullname", memberUser.getFirstName() + " " + memberUser.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("action", isAccepted ? "accepted" : "rejected");
        if (justification != null) {
            attributes.put("justification", "Comment from reviewer: "+justification);
            attributes.put("justificationHtml", "Comment from reviewer: <b>" +justification+"</b>");
        } else{
            attributes.put("justification", "");
            attributes.put("justificationHtml", "");
        }
        String groupUrl = session.getContext().getUri().getBaseUri().toString() + membersGroupPageUrl;
        attributes.put("groupUrl", groupUrl.replace("{realmName}", realm.getName()).replace("{id}", groupId));
        attributes.put("signatureMessage", signatureMessage);
        send(isAccepted ? "acceptEnrollmentAdminInfoSubject" : "rejectEnrollmentAdminInfoSubject", Stream.of(groupPath).collect(Collectors.toList()), "accept-reject-enrollment-admin-info.ftl", attributes);
    }

    public void sendGroupAdminEnrollmentCreationEmail(UserModel userRequest, String groupPath, List<String> groupRoles, String reason, String enrollmentId) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("userName", userRequest.getFirstName() + " " + userRequest.getLastName());
        if (groupRoles != null && !groupRoles.isEmpty()) {
            StringBuilder sb = new StringBuilder("");
            StringBuilder sbText = new StringBuilder("");
            groupRoles.stream().forEach(role -> {
                sb.append("• ").append(role).append("<br>");
                sbText.append("• ").append(role).append("\n");
            });
            attributes.put("groupRoles", sb.toString());
            attributes.put("groupRolesText", sbText.toString());
        } else  {
            attributes.put("groupRoles", "");
            attributes.put("groupRolesText", "");
        }
        attributes.put("groupPath", groupPath);
        attributes.put("reason", reason != null ?  "Justification: " + reason : "");
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        URI baseUri = uriInfo.getBaseUri();
        attributes.put("urlLink", baseUri.toString() + enrollmentUrl.replace("{realmName}", realm.getName()).replace("{id}", enrollmentId));
        attributes.put("signatureMessage", signatureMessage);
        send("groupadminEnrollmentRequestCreationSubject", Stream.of(groupPath).collect(Collectors.toList()), "groupadmin-enrollment-creation.ftl", attributes);
    }

    public void sendExpiredGroupMemberEmailToAdmin(UserModel userRequest, String groupPath, List<String> subgroupsPaths) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("userName", userRequest.getFirstName() + " " + userRequest.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupsPaths));
        attributes.put("signatureMessage", signatureMessage);
        send("adminGroupUserRemovalSubject", Stream.of(groupPath).collect(Collectors.toList()), "expired-group-membership-admin.ftl", attributes);
    }

    public void sendExpiredGroupMemberEmailToUser(String groupPath, List<String> subgroupsPaths, String serverUrl) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("subgroupsStr", subgroupsStrCalculation(subgroupsPaths));
        attributes.put("urlLink", (serverUrl != null ? serverUrl : "localhost:8080") + enrollmentStartUrl.replace("{realmName}", realm.getName()).replace("{path}", groupPath));
        attributes.put("signatureMessage", signatureMessage);
        send("userRemovalSubject", Stream.of(groupPath).collect(Collectors.toList()), "expired-group-membership-user.ftl", attributes);
    }

    public void sendExpiredGroupMembershipNotification(String groupPath, String date, String serverUrl) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("date", date);
        attributes.put("urlLink", (serverUrl != null ? serverUrl : "localhost:8080") + enrollmentStartUrl.replace("{realmName}", realm.getName()).replace("{path}", groupPath));
        attributes.put("signatureMessage", signatureMessage);
        send("groupMembershipExpirationNotificationSubject", Stream.of(groupPath).collect(Collectors.toList()), "group-membership-expiration-notification.ftl", attributes);
    }

    public void sendGroupInvitationEmail(UserModel groupadmin, String groupName, String groupPath, String description, List<String> groupRoles, String id, Long  invitationExpirationHour) throws EmailException {
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
        attributes.put("urlLink", baseUri.toString() + finishGroupInvitation.replace("{realmName}", realm.getName()).replace("{id}", id).replace("{path}", groupPath));
        attributes.put("invitationExpirationHour", invitationExpirationHour != null ? "expire after "+invitationExpirationHour+" hours" :"not expire");
        attributes.put("signatureMessage", signatureMessage);
        send("groupInvitationSubject", Stream.of(groupPath).collect(Collectors.toList()), "user-group-invitation.ftl", attributes);
    }

    public void sendAcceptInvitationEmail(UserModel userModel, String groupPath, String groupId, boolean forMember, List<String> groupRoles) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("userfullname", userModel.getFirstName() + " " + userModel.getLastName());
        attributes.put("email", userModel.getEmail());
        attributes.put("type", forMember ? "join" : "become an administrator of");
        String groupPathStr = groupPath+" group";
        String groupPathStrText = groupPath+" group";
        if (forMember && groupRoles != null && !groupRoles.isEmpty()) {
            StringBuilder sb = new StringBuilder(groupPathStr).append(" with the following roles:");
            StringBuilder sbText = new StringBuilder(groupPathStr).append(" with the following roles: ");
            groupRoles.stream().forEach(role -> {
                sb.append("<br>•").append(role);
                sbText.append(role).append(", ");
            });
            groupPathStrText = StringUtils.removeEnd(sbText.toString(), ", ");
            groupPathStr = sb.toString();
        }
        attributes.put("groupPath", groupPathStr);
        attributes.put("groupPathText", groupPathStrText);
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        String groupUrl = forMember ? uriInfo.getBaseUri().toString() + membersGroupPageUrl : uriInfo.getBaseUri().toString() + adminGroupPageUrl;
        attributes.put("groupUrl", groupUrl.replace("{realmName}", realm.getName()).replace("{id}", groupId));
        attributes.put("signatureMessage", signatureMessage);
        String titleType = forMember ? "joining" : "becoming an administrator of";
        send("groupAcceptInvitationSubject", Stream.of(titleType, groupPath).collect(Collectors.toList()), "accept-invitation.ftl", attributes);
    }

    public void sendRejectionInvitationEmail(UserModel userModel, String groupPath, String groupId, boolean forMember, List<String> groupRoles) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("userfullname", userModel.getFirstName() + " " + userModel.getLastName());
        attributes.put("email", userModel.getEmail());
        attributes.put("type", forMember ? "join" : "become an administrator of");
        String groupPathStr = groupPath+" group";
        String groupPathStrText = groupPath+" group";
        if (forMember && groupRoles != null && !groupRoles.isEmpty()) {
            StringBuilder sb = new StringBuilder(groupPathStr).append(" with the following roles:");
            StringBuilder sbText = new StringBuilder(groupPathStr).append(" with the following roles: ");
            groupRoles.stream().forEach(role -> {
                sb.append("<br>•").append(role);
                sbText.append(role).append(", ");
            });
            groupPathStrText = StringUtils.removeEnd(sbText.toString(), ", ");
            groupPathStr = sb.toString();
        }
        attributes.put("groupPath", groupPathStr);
        attributes.put("groupPathText", groupPathStrText);
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        String groupUrl = forMember ? uriInfo.getBaseUri().toString() + membersGroupPageUrl : uriInfo.getBaseUri().toString() + adminGroupPageUrl;
        attributes.put("groupUrl", groupUrl.replace("{realmName}", realm.getName()).replace("{id}", groupId));
        attributes.put("signatureMessage", signatureMessage);
        String titleType = forMember ? "joining" : "becoming an administrator of";
        send("groupRejectionInvitationSubject", Stream.of(titleType, groupPath).collect(Collectors.toList()), "reject-invitation.ftl", attributes);
    }

    public void sendInvitionAdminInformationEmail(String email, boolean forMember, String groupPath, UserModel admin, List<String> groupRoles) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("email", email);
        attributes.put("type", forMember ? "a member" : "an administrator");
        if (forMember && groupRoles != null && !groupRoles.isEmpty()) {
            StringBuilder sb2 = new StringBuilder(groupPath).append(" with roles: ");
            groupRoles.stream().forEach(role -> sb2.append(role).append(", "));
            String groupPathEnchance = StringUtils.removeEnd(sb2.toString(), ", ");
            attributes.put("groupPath", groupPathEnchance);
        } else {
            attributes.put("groupPath", groupPath);
        }
        attributes.put("adminFullName", admin.getFirstName() + " " + admin.getLastName());
        attributes.put("signatureMessage", signatureMessage);
        send(forMember ? "groupInvitationSubject" : "groupInvitationAdminInformSubject", Stream.of(groupPath).collect(Collectors.toList()), "invitation-admin-inform.ftl", attributes);
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

    public void sendMemberCreateAdminInformEmail(String groupId, String groupPath, UserModel userChanged, UserModel admin) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("userFullName", userChanged.getFirstName() + " " + userChanged.getLastName());
        attributes.put("adminFullName", admin.getFirstName() + " " + admin.getLastName());
        String groupUrl = session.getContext().getUri().getBaseUri().toString() + membersGroupPageUrl ;
        attributes.put("groupUrl", groupUrl.replace("{realmName}", realm.getName()).replace("{id}", groupId));
        attributes.put("signatureMessage", signatureMessage);
        send("memberCreateAdminInformSubject", Stream.of(groupPath).collect(Collectors.toList()),"member-create-admin-inform.ftl", attributes);
    }

    public void sendMemberCreateUserInformEmail(String groupId, String groupPath, UserModel admin, LocalDate validFrom, LocalDate membershipExpiresAt, List<String> roles) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("adminFullName", admin.getFirstName() + " " + admin.getLastName());
        attributes.put("validFrom", validFrom.format(Utils.dateFormatter));
        attributes.put("membershipExpiresAt", membershipExpiresAt != null ? membershipExpiresAt.format(Utils.dateFormatter) : "N/A");
        attributes.put("roles", roles.stream().collect(Collectors.joining(",")));
        String memberUrl = session.getContext().getUri().getBaseUri().toString() + MEMBER_URL ;
        attributes.put("memberUrl", memberUrl.replace("{realmName}", realm.getName()).replace("{id}", groupId));
        attributes.put("signatureMessage", signatureMessage);
        send("memberCreateUserInformSubject", Stream.of(groupPath).collect(Collectors.toList()), "member-create-user-inform.ftl", attributes);
    }

    public void sendDeleteGroupAdminInformationEmail(String groupPath, UserModel admin) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("adminFullName", admin.getFirstName() + " " + admin.getLastName());
        attributes.put("signatureMessage", signatureMessage);
        send("deleteGroupAdminInformationSubject", Stream.of(groupPath).collect(Collectors.toList()), "delete-group-admin-inform.ftl", attributes);
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
        send("rolesChangesGroupAdminSubject", Stream.of(groupPath).collect(Collectors.toList()), "roles-changes-group-admin.ftl", attributes);
    }

    public void sendRemoveMemberEmail(String groupPath, UserModel admin) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("adminFullName", admin.getFirstName() + " " + admin.getLastName());
        String groupUrl = session.getContext().getUri().getBaseUri().toString() + SHOW_GROUPS_URL ;
        attributes.put("groupsUrl", groupUrl.replace("{realmName}", realm.getName()));
        attributes.put("signatureMessage", signatureMessage);
        send("removeMemberSubject", Stream.of(groupPath).collect(Collectors.toList()),"remove-member-inform.ftl", attributes);
    }

    public void sendRemoveMemberAdminInformationEmail(String groupId, String groupPath, UserModel admin, UserModel userChanged) throws EmailException {
        attributes.put("fullname", user.getFirstName() + " " + user.getLastName());
        attributes.put("groupPath", groupPath);
        attributes.put("userFullName", userChanged.getFirstName() + " " + userChanged.getLastName());
        attributes.put("adminFullName", admin.getFirstName() + " " + admin.getLastName());
        String memberUrl = session.getContext().getUri().getBaseUri().toString() + membersGroupPageUrl ;
        attributes.put("groupUrl", memberUrl.replace("{realmName}", realm.getName()).replace("{id}", groupId));
        attributes.put("signatureMessage", signatureMessage);
        send("removeMemberAdminInformationSubject", Stream.of(groupPath).collect(Collectors.toList()), "remove-member-admin-inform.ftl", attributes);
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

    //override method in order not to have problem with finding Keycloak url from request
    //This does not work with background process like notification and membership expiration emails
    @Override
    protected EmailTemplate processTemplate(String subjectKey, List<Object> subjectAttributes, String template, Map<String, Object> attributes) throws EmailException {
        try {
            Theme theme = getTheme();
            Locale locale = session.getContext().resolveLocale(user);
            attributes.put("locale", locale);

            Properties messages = theme.getEnhancedMessages(realm, locale);
            attributes.put("msg", new MessageFormatterMethod(locale, messages));

            attributes.put("properties", theme.getProperties());
            attributes.put("realmName", getRealmName());
            attributes.put("user", new ProfileBean(user));

            String subject = new MessageFormat(messages.getProperty(subjectKey, subjectKey), locale).format(subjectAttributes.toArray());
            String textTemplate = String.format("text/%s", template);
            String textBody;
            try {
                textBody = freeMarker.processTemplate(attributes, textTemplate, theme);
            } catch (final FreeMarkerException e) {
                throw new EmailException("Failed to template plain text email.", e);
            }
            String htmlTemplate = String.format("html/%s", template);
            String htmlBody;
            try {
                htmlBody = freeMarker.processTemplate(attributes, htmlTemplate, theme);
            } catch (final FreeMarkerException e) {
                throw new EmailException("Failed to template html email.", e);
            }

            return new EmailTemplate(subject, textBody, htmlBody);
        } catch (Exception e) {
            throw new EmailException("Failed to template email", e);
        }
    }


}
