# keycloak-group-management
A keycloak plugin to perform advanced group management 

<br>
<br>

<h1>THIS PLUGIN IS CURRENTLY UNDER CONSTRUCTION</h1>

## REST API

Main url : {server_url}/realms/{realm}/agm

**User web services ( Any Keycloak User)**

Path | Method | Description                              | Classes 
------------ |--------|------------------------------------------|--------
/account/user/groups | GET    | get all user groups                      | UserGroups
/account/user/group/{groupId}/admin | POST   | accept invitation and become group admin | UserGroup 
/account/user/enroll-requests | GET    | get all user ongoing enrollment requests | UserGroups
/account/user/enroll-request | POST   | create new enrollment request            | UserGroups
/account/user/enroll-request/{id} | GET    | get enrollment request by id             | UserGroupEnrollmentAction
/account/user/enroll-request/{id}/respond | POST   | respond t enrollment request by id       | UserGroupEnrollmentAction

**Group admin web services ( for group specific web services user must have admin rights to this group)**

Path | Method | Description                                                                   | Classes 
------------ | ------------- |-------------------------------------------------------------------------------|--------- 
/account/group-admin/groups | GET | get all groups that this user has admin rights                                | GroupAdminService 
/account/group-admin/group/{groupId}/configuration/all | GET | get all group enrollment configurations                                       | GroupAdminGroup
/account/group-admin/group/{groupId}/configuration/{id} | GET | get group enrollment configuration                                            | GroupAdminGroup
/account/group-admin/group/{groupId}/configuration | POST | create/ update group enrollment configuration                                 | GroupAdminGroup
/account/group-admin/group/{groupId}/members | GET | get all group members pager, being able to search and get by type (fe active) | GroupAdminGroupMembers
/account/group-admin/group/{groupId}/member/{memberId}/suspend | POST | suspend group member                                                          | GroupAdminGroupMember
/account/group-admin/group/{groupId}/member/{memberId}/activate | POST | activate group member                                                         | GroupAdminGroupMember
/account/group-admin/group/{groupId}/admin | POST | invite user as group admin for this groupId group                             | GroupAdminGroup
/account/group-admin/group/{groupId}/admin/{userId} | DELETE | delete group admin                                                            | GroupAdminService
/account/group-admin/enroll-requests | GET | get all group admin enrollment requests  | GroupAdminEnrollement
/account/group-admin/enroll-request/{enrollId}/extra-info | POST | request extra infrormation from user  | GroupAdminEnrollement
/account/group-admin/enroll-request/{enrollId}/accept | POST | accept group enrollment  | GroupAdminEnrollement
/account/group-admin/enroll-request/{enrollId}/reject | POST | reject group enrollment  | GroupAdminEnrollement

**Admin web services**

Path | Method | Description                                  | Classes |
------------ |--------|----------------------------------------------|---------| 
/admin/group | POST   | create top level group                       | ResourcesProvider
/admin/group/{groupId}/configuration/{id} | GET    | get  group enrollment configuration          | AdminGroups
/admin/group/{groupId}/configuration | POST   | create/update group enrollment configuration | AdminGroups
/admin/group/{groupId}/admin/{userId} | POST   | create group admin                           | AdminGroups
/admin/group/{groupId}/admin/{userId} | DELETE | delete group admin                           | AdminGroups
/admin/group/{groupId}/children| POST   | create child group                           | AdminGroups
