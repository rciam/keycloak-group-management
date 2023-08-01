# keycloak-group-management
A keycloak plugin to perform advanced group management 

<br>
<br>

<h1>THIS PLUGIN IS CURRENTLY UNDER CONSTRUCTION</h1>

## General configuration options 

For general group management configuarion options execute following web service (necessary during first time deployed):

`curl --request PUT \
--url {server_url}/realms/{realmName}/agm/admin/configuration \
--header 'Accept: application/json' \
--header 'Authorization: Bearer {admin_access_token}' \
--header 'Content-Type: application/json' \
--data '{
"invitation-expiration-period":"72",
"expiration-notification-period": "21"
}'`

Parameter explanation:
- invitation-expiration-period = After how many hours the invitation will be expired.
- expiration-notification-period = How many days before Group Membership expiration (or aup expiration) notification email will be sent to user. Can be overridden per Group.

You should define realm attribute 'keycloakUrl' (Keycloak main url)

## REST API

Main url : {server_url}/realms/{realm}/agm

**User web services ( Any Keycloak User)**

Path | Method | Description                                                                          | Classes 
------------ |--------|--------------------------------------------------------------------------------------|--------
/account/user/groups | GET    | get all user groups                                                                  | UserGroups
/account/user/invitation/{id} | GET    | get invitation by id                                                                 | UserGroups
/account/user/invitation/{id}/accept | POST   | accept invitation and become group member or admin                                   | UserGroups
/account/user/invitation/{id}/reject | POST   | reject invitation for becoming group member  or admin                                | UserGroups
/account/user/groups/configurations | GET    | get all available group configurations (active and visibleToNotMembers) by groupPath | UserGroups
/account/user/group/{groupId}/configurations | GET    | get all available group configurations (active and visibleToNotMembers)              | UserGroup 
/account/user/group/{groupId}/member | GET    | get user group membership                                                            | UserGroupMember
/account/user/group/{groupId}/member | DELETE | leave user group membership                                                          | UserGroupMember
/account/user/enroll-requests | GET    | get all user ongoing enrollment requests                                             | UserGroups
/account/user/enroll-request | POST   | create new enrollment request                                                        | UserGroups
/account/user/enroll-request/{id} | GET    | get enrollment request by id                                                         | UserGroupEnrollmentRequestAction
/account/user/enroll-request/{id}/respond | POST   | respond t enrollment request by id                                                   | UserGroupEnrollmentRequestAction

**Group admin web services ( for group specific web services user must have admin rights to this group)**

Path | Method | Description                                                                   | Classes 
------------ |--|-------------------------------------------------------------------------------|--------- 
/account/group-admin/groups | GET | get all groups that this user has admin rights                                | GroupAdminService
/account/group-admin/configuration-rules | GET | get group enrollment configuration rules based on group type         | GroupAdminService
/account/group-admin/groupids/all | GET | get all groups ids that this user has admin rights                            | GroupAdminService
/account/group-admin/groups/members | GET | get all groups members given the groupids as comma-separated string           | GroupAdminService
/account/group-admin/group/{groupId} | DELETE | delete group                                                | GroupAdminGroup
/account/group-admin/group/{groupId}/all | GET | get all group information                                                     | GroupAdminGroup
/account/group-admin/group/{groupId}/children| POST | create child group                           | GroupAdminGroup
/account/group-admin/group/{groupId}/configuration/all | GET | get all group enrollment configurations                                       | GroupAdminGroup
/account/group-admin/group/{groupId}/configuration/{id} | GET | get group enrollment configuration                                            | GroupAdminGroup
/account/group-admin/group/{groupId}/configuration | POST | create/ update group enrollment configuration                                 | GroupAdminGroup
/account/group-admin/group/{groupId}/configuration/{id} | DELETE | delete group enrollment configuration                                         | GroupAdminGroup
/account/group-admin/group/{groupId}/roles | GET | get all group roles                                                           | GroupAdminGroup
/account/group-admin/group/{groupId}/roles | POST | create group role                                                             | GroupAdminGroup
/account/group-admin/group/{groupId}/role/{name} | DELETE | delete group role                                                             | GroupAdminGroup
/account/group-admin/group/{groupId}/members | GET | get all group members pager, being able to search and get by type (fe active) | GroupAdminGroupMembers
/account/group-admin/group/{groupId}/members/invitation | POST | send invitation to a user based on email  
/account/group-admin/group/{groupId}/member/{memberId} | PUT | update specific fields of group member                                    | GroupAdminGroupMembers
/account/group-admin/group/{groupId}/member/{memberId} | DELETE | delete group member                                                 | GroupAdminGroupMember
/account/group-admin/group/{groupId}/member/{memberId}/role | POST | add role to group member                                                           | GroupAdminGroupMember
/account/group-admin/group/{groupId}/member/{memberId}/role/{name} | DELETE | delete role from group member                                                 | GroupAdminGroupMember
/account/group-admin/group/{groupId}/member/{memberId}/suspend | POST | suspend group member                                                          | GroupAdminGroupMember
/account/group-admin/group/{groupId}/member/{memberId}/activate | POST | activate group member                                                         | GroupAdminGroupMember
/account/group-admin/group/{groupId}/admin/{userId} | POST | add user as admin                                                             | GroupAdminGroup
/account/group-admin/group/{groupId}/admin/invite | POST | invite user as group admin for this groupId group                             | GroupAdminGroup
/account/group-admin/group/{groupId}/admin/{userId} | DELETE | delete group admin                                                            | GroupAdminService
/account/group-admin/enroll-requests | GET | get all group admin enrollment requests                                       | GroupAdminService
/account/group-admin/enroll-request/{enrollId} | GET | get enrollment request                                                        | GroupAdminEnrollementRequest
/account/group-admin/enroll-request/{enrollId}/extra-info | POST | request extra infrormation from user                                          | GroupAdminEnrollementRequest
/account/group-admin/enroll-request/{enrollId}/accept | POST | accept group enrollment request                                               | GroupAdminEnrollementRequest
/account/group-admin/enroll-request/{enrollId}/reject | POST | reject group enrollment request                                               | GroupAdminEnrollementRequest

**Admin web services**

Path | Method | Description                                  | Classes |
------------ |--|----------------------------------------------|---------| 
/admin/group | POST | create top level group                       | AdminService
/admin/configuration | PUT | change realm settings (realm attributes)                  | AdminService
/admin/member-user-attribute/configuration | GET | get member user attribute configuration          | AdminService
/admin/member-user-attribute/configuration | POST |update member user attribute configuration | AdminService
/admin/configuration-rules | GET | get group enrollment configuration rules          | AdminEnrollmentConfigurationRules
/admin/configuration-rules | POST |create group enrollment configuration rule | AdminEnrollmentConfigurationRules
/admin/configuration-rules/{id}| GET | get group enrollment configuration rule by id       | AdminEnrollmentConfigurationRules
/admin/configuration-rules/{id} | PUT |update group enrollment configuration rule | AdminEnrollmentConfigurationRules
/admin/configuration-rules/{id} | DELETE |delete group enrollment configuration rule | AdminEnrollmentConfigurationRules
/admin/memberUserAttribute/calculation | POST |update member user attribute value for all users | AdminService
/admin/user/{id} | DELETE | delete user          | AdminService
/admin/group/{groupId} | DELETE | delete group          | AdminGroups
/admin/group/{groupId}/configuration/{id} | GET | get  group enrollment configuration          | AdminGroups
/admin/group/{groupId}/configuration | POST | create/update group enrollment configuration | AdminGroups
/admin/group/{groupId}/admin/{userId} | POST | create group admin                           | AdminGroups
/admin/group/{groupId}/admin/{userId} | DELETE | delete group admin                           | AdminGroups
/admin/group/{groupId}/children| POST | create child group                           | AdminGroups
