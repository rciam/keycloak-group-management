# keycloak-group-management
A keycloak plugin to perform advanced group management 

<br>
<br>

<h1>THIS PLUGIN IS CURRENTLY UNDER CONSTRUCTION</h1>

## REST API

Main url : {server_url}/realms/{realm}/agm

**Group admin web services ( for group specific web services user must have admin rights to this group)**

Path | Method | Description                                                                  | Classes |
------------ | ------------- |------------------------------------------------------------------------------|---------| 
/account/group-admin/groups | GET | get all groups that this user has admin rights                          | VoAdminService 
/account/group-admin/group/{groupId}/configuration/all | GET | get all group enrollment configurations                                      | VoAdminGroup
/account/group-admin/group/{groupId}/configuration/{id} | GET | get group enrollment configuration                                           | VoAdminGroup
/account/group-admin/group/{groupId}/configuration | POST | create/ update group enrollment configuration                                | VoAdminGroup
/account/group-admin/group/{groupId}/members | GET | get all group members pager, being able to search and get by type (fe active) | VoAdminGroupMembers
/account/group-admin/group/{groupId}/member/{memberId}/suspend | POST | suspend group member                                                         | VoAdminGroupMember
/account/group-admin/group/{groupId}/member/{memberId}/activate | POST | activate group member                                                        | VoAdminGroupMember
/account/group-admin/group/{groupId}/admin/{userId} | POST | create group admin                                                        | VoAdminGroup
/account/group-admin/group/{groupId}/admin/{userId} | DELETE | delete group admin                                                       | VoAdminGroup


**Admin web services**

Path | Method | Description                                  | Classes |
------------ |--------|----------------------------------------------|---------| 
/admin/group | POST   | create top level group                       | ResourcesProvider
/admin/group/{groupId}/configuration/{id} | GET    | get  group enrollment configuration          | AdminGroups
/admin/group/{groupId}/configuration | POST   | create/update group enrollment configuration | AdminGroups
/admin/group/{groupId}/admin/{userId} | POST   | create group admin                           | AdminGroups
/admin/group/{groupId}/admin/{userId} | DELETE | delete group admin                           | AdminGroups
/admin/group/{groupId}/children| POST   | create child group                           | AdminGroups
