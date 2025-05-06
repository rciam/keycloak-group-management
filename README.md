# keycloak-group-management
A keycloak plugin to support advanced group management features:

* User-driven group enrollment flows:
  * Users can request membership in groups:
    * Accept group Terms & Conditions
    * Provide comment/justification
  * Membership requests are reviewed by group managers

* Time-based group membership: 
  * Automatic expiration of group membership beyond a configurable time period after joining the group
  * Membership renewal process

* Roles within groups

## Keycloak compatibility matrix
| Group management version | Keycloak version |
|--------------------------|------------------|
| 0.9.0                    | 18.0.1-2.17      |
| 0.10.0                   | 22.0.5-1.1 +     |
| 0.13.0                   | 22.0.5-1.2 +     |
| 0.18.0                   | 22.0.10-1.4 +    |
| 0.19.0                   | 22.0.10-1.8 +    |
| 0.19.0                   | 22.0.10-1.8 +    |
| 1.7.0                    | 22.0.13-1.17 +   |


## General configuration options 
All web services to be executed needs realm management rights role.

1. You should define realm attributes :
 - 'keycloakUrl' = Keycloak main url
 - 'userAssuranceForEnrollment' = User assurance (default 'assurance')
 - 'userIdentifierForEnrollment' = User identifier (default 'username')
2. You could create account roles 'manage-groups' and 'manage-groups-extended' for managing groups.
   'manage-groups' is a special role that can manage all groups in a same way as group admins. 
   Actions that can not be done with this role are:
     - delete group
     - delete role
     - invite a user
     - add group member role
     - delete group member role
   Among others users with this role can also: 
     - create top level group
     - create group configuration
     - can view all realm users
   'manage-groups-extended' is a role for comanage to Keycloak migration. 
   User with this role has the same rigths plus some rules in creating a group member does not take into account with this role.
3. (optional) For general group management configuration options execute following web service (necessary during first time deployed):

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
- invitation-expiration-period = After how many hours the invitation will be expired. (default value is 72)
- expiration-notification-period = How many days before Group Membership expiration (or aup expiration) notification email will be sent to user. Can be overridden per Group. (default value is 21)

4. For configuring entitlements user attribute you must execute the following web service :
   `curl --request POST \
   --url {server_url}/realms/{realmName}/agm/admin/member-user-attribute/configuration \
   --header 'Accept: application/json' \
   --header 'Authorization: Bearer {admin_access_token}' \
   --header 'Content-Type: application/json' \
   --data '{
   "userAttribute" : "entitlements",
   "urnNamespace" : "urn%3Amace%3Aexample.org",
   "authority" : "rciam.example.org" // Optional. It will be omitted from the group entitlements if not specified
   }'`

Only authority is optional.

5.  Configuration rules exists for group configuration options. Web service example:
   `curl --request POST \
   --url {server_url}/realms/{realmName}/agm/admin/configuration-rules \
   --header 'Accept: application/json' \
   --header 'Authorization: Bearer {admin_access_token}' \
   --header 'Content-Type: application/json' \
   --data '{
   "field" : "membershipExpirationDays" ,
   "type" : "TOP_LEVEL" ,
   "required" : true,
   "defaultValue" : "30",
   "max" : "45"
   }'`

Fields explanation :
- *field* : field of group management (required)
- *type* : "TOP_LEVEL" or "SUBGROUP" (required)
- *required* : required field (required)
- *defaultValue* : default value
- *max* : max value

With PUT *{server_url}/realms/{realmName}/agm/admin/configuration-rules/{id}* you could update a configuration rule.
With GET *{server_url}/realms/{realmName}/agm/admin/configuration-rules* you could get all configuration rules.

When a group is created, a default configuration is created. Group admin can change it/ create a new one.
Configuration rules determines the default group configuration and applies rules in group configuration creation/ update.

Default group configuration values without any configuration rules:

| Field                    | Label            | Default value       |
|--------------------------|------------------|---------------------|
| name                     | name             | Join + <group name> |
| requireApproval          | Requires approval| True                |
| commentsNeeded           | Comments         | True                |
| visibleToNotMembers      | Visible          | False               |
| active                   | Active           | True                |
| membershipExpirationDays | Expiration date  | null                |
| aup.url                  | Aup url          | null                |

Default group role is member (default group role during creation)


## REST API

Main url : {server_url}/realms/{realm}/agm

**User web services ( Any Keycloak User)**

| Path                                         | Method | Description                                                                          | Classes                         |
|----------------------------------------------|--------|--------------------------------------------------------------------------------------|---------------------------------|
| /account/user/groups                         | GET    | get all user groups                                                                  | UserGroups                      |
| /account/user/invitation/{id}                | GET    | get invitation by id                                                                 | UserGroups                      |                      
| /account/user/invitation/{id}/accept         | POST   | accept invitation and become group member or admin                                   | UserGroups                      |                      
| /account/user/invitation/{id}/reject         | POST   | reject invitation for becoming group member  or admin                                | UserGroups                      |                      
| /account/user/groups/configurations          | GET    | get all available group configurations (active and visibleToNotMembers) by groupPath | UserGroups                      |                     
| /account/user/groups/configuration/{id}      | GET    | get group configuration by id                                                        | UserGroups                      |                    
| /account/user/group/{groupId}/configurations | GET    | get all available group configurations (active and visibleToNotMembers)              | UserGroup                       |                   
| /account/user/group/{groupId}/member         | GET    | get user group membership                                                            | UserGroupMember                 |              
| /account/user/group/{groupId}/member         | DELETE | leave user group membership                                                          | UserGroupMember                 |             
| /account/user/enroll-requests                | GET    | get all user ongoing enrollment requests                                             | UserGroups                      |            
| /account/user/enroll-request                 | POST   | create new enrollment request                                                        | UserGroups                      |           
| /account/user/enroll-request/{id}            | GET    | get enrollment request by id                                                         | UserGroupEnrollmentRequestAction | 
| /account/user/enroll-request/{id}/respond    | POST   | respond t enrollment request by id                                                   | UserGroupEnrollmentRequestAction |

**Group admin web services ( for group specific web services user must have admin rights to this group)**

| Path                                                               | Method | Description                                                                   | Classes                      |
|--------------------------------------------------------------------|--------|-------------------------------------------------------------------------------|------------------------------|
| /account/group-admin/groups                                        | GET    | get all groups that this user has admin rights                                | GroupAdminService            |
| /account/group-admin/configuration-rules                           | GET    | get group enrollment configuration rules based on group type                  | GroupAdminService            |
| /account/group-admin/groupids/all                                  | GET    | get all groups ids that this user has admin rights                            | GroupAdminService            |
| /account/group-admin/groups/members                                | GET    | get all groups members given the groupids as comma-separated string           | GroupAdminService            |
| /account/group-admin/group/{groupId}                               | DELETE | delete group                                                                  | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/all                           | GET    | get all group information                                                     | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/children                      | POST   | create child group                                                            | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/configuration/all             | GET    | get all group enrollment configurations                                       | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/configuration/{id}            | GET    | get group enrollment configuration                                            | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/configuration                 | POST   | create/ update group enrollment configuration                                 | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/configuration/{id}            | DELETE | delete group enrollment configuration                                         | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/default-configuration         | POST   | change default group enrollment configuration                                 | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/roles                         | GET    | get all group roles                                                           | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/roles                         | POST   | create group role                                                             | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/role/{name}                   | DELETE | delete group role                                                             | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/members                       | GET    | get all group members pager, being able to search and get by type (fe active) | GroupAdminGroupMembers       |
| /account/group-admin/group/{groupId}/members                       | POST   | create a user group member based on username                                  | GroupAdminGroupMembers       |
| /account/group-admin/group/{groupId}/members/invitation            | POST   | send invitation to a user based on email                                      | GroupAdminGroupMembers       |
| /account/group-admin/group/{groupId}/member/{memberId}             | PUT    | update specific fields of group member                                        | GroupAdminGroupMembers       |
| /account/group-admin/group/{groupId}/member/{memberId}             | DELETE | delete group member                                                           | GroupAdminGroupMember        |
| /account/group-admin/group/{groupId}/member/{memberId}/role        | POST   | add role to group member                                                      | GroupAdminGroupMember        |
| /account/group-admin/group/{groupId}/member/{memberId}/role/{name} | DELETE | delete role from group member                                                 | GroupAdminGroupMember        |
| /account/group-admin/group/{groupId}/member/{memberId}/suspend     | POST   | suspend group member                                                          | GroupAdminGroupMember        |
| /account/group-admin/group/{groupId}/member/{memberId}/activate    | POST   | activate group member                                                         | GroupAdminGroupMember        |
| /account/group-admin/group/{groupId}/admin                         | POST   | add user as admin using user id or username                                   | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/admin/invite                  | POST   | invite user as group admin for this groupId group                             | GroupAdminGroup              |
| /account/group-admin/group/{groupId}/admin                         | DELETE | delete group admin using user id or username                                  | GroupAdminService            |
| /account/group-admin/enroll-requests                               | GET    | get all group admin enrollment requests                                       | GroupAdminService            |
| /account/group-admin/enroll-request/{enrollId}                     | GET    | get enrollment request                                                        | GroupAdminEnrollementRequest |
| /account/group-admin/enroll-request/{enrollId}/extra-info          | POST   | request extra infrormation from user                                          | GroupAdminEnrollementRequest |
| /account/group-admin/enroll-request/{enrollId}/accept              | POST   | accept group enrollment request                                               | GroupAdminEnrollementRequest |
| /account/group-admin/enroll-request/{enrollId}/reject              | POST   | reject group enrollment request                                               | GroupAdminEnrollementRequest |

**manage-groups account role**

Role name can be changed in database( column GROUP_ROLE_NAME of table GROUP_MANAGEMENT_EVENT)

| Path                       | Method    | Description                                  | Classes            |
|----------------------------|-----------|----------------------------------------------|--------------------| 
| /account/group-admin/group | POST      | create top level group                       | GroupAdminService  |

**Admin web services**

| Path                                       | Method  | Description                                                           | Classes                            |
---------------------------------------------|---------|-----------------------------------------------------------------------|------------------------------------| 
 /admin/group                                | POST    | create top level group                                                | AdminService                       |     
 /admin/configuration                        | PUT     | change realm settings (realm attributes)                              | AdminService                       | 
 /admin/member-user-attribute/configuration  | GET     | get member user attribute configuration                               | AdminService                       |
 /admin/member-user-attribute/configuration  | POST    | update member user attribute configuration                            | AdminService                       |
 /admin/configuration-rules                  | GET     | get group enrollment configuration rules                              | AdminEnrollmentConfigurationRules  |
 /admin/configuration-rules                  | POST    | create group enrollment configuration rule                            | AdminEnrollmentConfigurationRules  |
 /admin/configuration-rules/{id}             | GET     | get group enrollment configuration rule by id                         | AdminEnrollmentConfigurationRules  |
 /admin/configuration-rules/{id}             | PUT     | update group enrollment configuration rule                            | AdminEnrollmentConfigurationRules  |
 /admin/configuration-rules/{id}             | DELETE  | delete group enrollment configuration rule                            | AdminEnrollmentConfigurationRules  |
 /admin/memberUserAttribute/calculation      | POST    | update member user attribute value for all users                      | AdminService                       |
 /admin/effective-expiration-date/calculation| POST    | update user group membership effective expiration date for all realms | AdminService                       | 
 /admin/user/{id}                            | DELETE  | delete user                                                           | AdminService                       |
 /admin/group/{groupId}                      | DELETE  | delete group                                                          | AdminGroups                        |
 /admin/group/{groupId}/configuration/{id}   | GET     | get  group enrollment configuration                                   | AdminGroups                        |
 /admin/group/{groupId}/configuration        | POST    | create/update group enrollment configuration                          | AdminGroups                        |
 /admin/group/{groupId}/admin/{userId}       | POST    | create group admin                                                    | AdminGroups                        |
 /admin/group/{groupId}/admin/{userId}       | DELETE  | delete group admin                                                    | AdminGroups                        |
 /admin/group/{groupId}/children             | POST    | create child group                                                    | AdminGroups                        |
