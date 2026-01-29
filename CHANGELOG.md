# Changelog
All notable changes in keycloak-group-management will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [1.11.0] - 2026-01-29

### Added
- member REST API based on user id

## [1.10.0] - 2025-11-26

### Fixed
- Fix bug in effective date calculation [RCIAM-427](https://tts.grnet.gr/jira/browse/RCIAM-427)

## [1.9.11] - 2025-11-13

### Fixed
- Fix QuarkusKeycloakSession kept in memory for each timer [keycloak-43761](https://github.com/keycloak/keycloak/issues/43761)
- Remove whitespaces in line starting (text emails)

## [1.9.10] - 2025-10-24

## Fixed
- Fix group administrator text message for user expiration

## [1.9.9] - 2025-10-15

## Fixed
- Direct Enrollment Fix

## [1.9.8] - 2025-10-02

### Fixed 
- State Bug in Update Membership Page

## [1.9.7] - 2025-09-22

### Fixed State Bug in Update Membership Page

## [1.9.6] - 2025-09-18

### Fixed
- Improve daily task for members delete

## [1.9.5] - 2025-09-17

### Fixed
- Fix problem when daily task has to delete

## [1.9.4] - 2025-09-16

### Fixed
- Fix problem when daily task has to delete

## [1.9.3] - 2025-09-10

### Fixed
- Fix update enrolment in Keycloak Group Management

## [1.9.2] - 2025-08-25

### Fixed
- Fix email bug in accept enrollment request

## [1.9.1] - 2025-06-23

### Fixed
- Fix New Exp Date Bug

## [1.9.0] - 2025-06-18

### Added
- Show current state of user during enrollment request from existing group member

### Update
- Friendly Dates for end user
- Update Labels
- lock Pending Users from creating enrollment requests

## [1.8.3] - 2025-06-13

### Fixed
- Fix problems with deleted groups

## [1.8.2] - 2025-05-21

### Fixed
- Enrollment Request Group Roles Bug

## [1.8.1] - 2025-05-20

### Fixed
- Correct set of Membership Expiration for already membership of a group (Now + Membership Expiration (days))

## [1.8.0] - 2025-05-16

### Fixed
- Save appropriate admin event for group crud actions

## [1.7.1] - 2025-05-12

### Fixed
- Fix group admin pager

## [1.7.0] - 2025-05-05

### Added
- Added Membership Details Modal in Group Members Admin Tab

## [1.6.2] - 2025-04-17

### Fixed
- Group invitation email template fix
- Response loader for requests in Modals
- Group Roles Filter in Group Members Tab
- Updated Validation for Membership Expiration in Enrollment configuration to match Backend validation
- Changed enrollment to enrolment in messages
- Fixed Typos
- Use Keycloak.username in Group Management UI
- Tooltip Bug

## [1.6.1] - 2025-04-03

### Fixed
- Correct error return in REST API
- Group Description Update Bug Fix

### Changed
- Update group admin notification subject for member removed

## [1.6.0] - 2025-03-20

### Added
- Return username and optionally voPersonId in group event details

### Fixed
- Fix admin email for adding another admin to group

## [1.5.7] - 2025-03-19

### Fixed
- Alerts for Requests Response
- Loaders for Requests

## [1.5.6] - 2025-02-25

### Fixed
- Return 409 conflict for concurrent group membership update

## [1.5.5] - 2025-02-25

### Fixed
- Alerts for update membership requests 
- Return 409 conflict for concurrent group membership update

## [1.5.4] - 2025-02-24

### Fixed
- Throw Optimistic locking exception for concurrent updating roles in group member

## [1.5.3] - 2025-02-24

### Fixed
- Fix problem with updating roles in group member

## [1.5.2] - 2025-02-17

### Fixed
- Avoid dublicate roles
- Correct update user email

## [1.5.1] - 2025-02-14

### Fixed
- Being able to delete group role
- Correct User enrollment attribute update

## [1.5.0] - 2025-02-07

### Added 
- Admins can directly add new members to groups
- Being possible to return group instead of top level group in group pager
- Being possible to search with exact match in group pager

### Fixed
- Fix problem deleting group with aup in configuration
- Fix bug when aup entity was updated/ removed from enrollment configuration entity

## [1.4.0] - 2025-01-23

### Changed
- Change order of authnauthorities (current IdP last in the list)

## [1.3.1] - 2025-01-21

### Fixed
- Fix Leave Group Confirmation Window

## [1.3.0] - 2025-01-21

### Added
- Users can now leave the group
- Admin emails when user leaves group

## [1.2.0] - 2025-01-07

### Changed
- Rules for user manage-groups-extended for update membership

### Added
- Emails when removing user from group

## [1.1.1] - 2024-12-20

### Fixed
- Improve email templates

## [1.1.0] - 2024-12-18

### Added
- Group admin can create a user group member based on username (REST API)
- Add group admins emails for accept/ reject enrollment request

### Fixed
- Recalculate effective membership expires at when pending groups become active

## [1.0.4] - 2024-12-02

### Fixed
- Fix bug with updating group enrollment request with aup

## [1.0.3] - 2024-11-28

### Fixed
- Fix default comments needed based on rules
- Fix expired member email
- Fix membership expiration warning email

## [1.0.2] - 2024-11-21

### Fixed
- Fix problem sending email from background process


## [1.0.1] - 2024-11-21

### Changed
- email bolds
- Add/change logs for emails sent for notification and membership expiration

### Fixed
- Notification emails every week


## [1.0.0] - 2024-11-15

### Changed
- Change suspend and reactivate email
- Change group membership update email
- Change invitation emails for group admins

### Fixed
- General email fixes

## [1.0.0rc2] - 2024-11-13
### Added
- Spinner when loading groups for Admin and Group Members
- Group Path in Invitation Flow

### Changed
- Rename NO_APPROVAL group enrollment request status to SELF_APPROVED
- Edit Membership for Indirect Members Improvement

### Fixed
- Fix null in enrollment request without approval email
- Fix implementation for user attribute update
- Multiselect Roles in Enrollment Fix
- Removed Effective Expiration Warning for Parent Groups 
- Extend Membership Warning Action Bug

## [1.0.0rc1] - 2024-10-25

### Added 
- Effective Expiration
- Exposed Enrollment Discovery Page to Members
- Warning indications for Expiring Memberships
- Extended Edit Membership View

### Fixed
- Group Members Admin View Bug
- Correct geting rules for update member

## [0.23.1] - 2024-10-08

### Changed
- Improve accept/ reject member invitation email


## [0.23.0] - 2024-10-07

### Added
- Group Path in Members Group View

### Fixed
- Emails fixes

## [0.22.0]

### Changed
- Improve update membership emails
- Improve add/remove admin emails
- Added Valid From to Invitation Flow

### Fixed
- Correct calculation of membership expiration for enrollment request of an already member

## [0.21.1]

### Fixed
- Bug in Msg Params
- Bug in Enrollment Configuration Rules in Edit Enrollment Modal  

## [0.21.0]

### Added
- Support adding/ removing group admin by username
- Add support for search group members by username
- Validate group enrollment configuration based on rules (REST API)
- Can not reactivate member if a parent member is suspended
- Delete realm admin REST API
- Edit Membership in Group Admin View

### Fixed
- When search for subgroup (ids) in group management must return all tree hierarchy
- Default Value for Enrollment Expiration Days Bug
- Group Management Labels Update

## [0.20.0]

### Added
- User assurance for group enrollment

## [0.19.2]

### Fixed
- Removed Unused Fields and updated Labels in Enrollment Request Review Page 

## [0.19.1]

### Added
- Added AuthnAuthorityRepresentation default constructor

## [0.19.0]
This version is compatible with Keycloak version [22.0.10-1.8](https://github.com/eosc-kc/keycloak/releases/tag/22.0.10-1.7)

### Added
- User attributes for group enrollment
- Inform others group admins for suspension - reactivation

### Fixed
- Fix bug where removing a group will not remove group entitlement user attribute from group members

## [0.18.0] - 2024-05-23 
This version is compatible with Keycloak version [22.0.10-1.4](https://github.com/eosc-kc/keycloak/releases/tag/22.0.10-1.4)

### Added
- SSH Public Keys Page (UI)
- Error Handling Group Admin Actions

### Fixed
- Footer CSS
- Membership Expiration Input Bug

## [0.17.0] - 2024-05-16

### Fixed
- Enrollment Configuration Validation Bug Fix

## [0.16.0] - 2024-04-26

### Added
- Breadcrumb Links for Parents in Subgroup page
- Loading component in Admin Actions 

### Fixed
- Reset Enrollment Form when it closes

## [0.15.0] - 2024-04-19

### Added
- Routable Tabs in Group Managment
- Indirect Members in Group Admin view 
- Expand invite group member search to include group admins [RCIAM-1343](https://jira.argo.grnet.gr/browse/RCIAM-1343)
- Requires Approval - Create Enrollment Page [RCIAM-1345](https://jira.argo.grnet.gr/browse/RCIAM-1345)

### Changed
- Improve group member/ admin email [Improve group member/ admin email invitation](https://trello.com/c/uDaQ5usH/2581-improve-group-member-admin-email-invitation)

### Fixed
- Correct notifications for submitting enrollment request
- Redirects in UI


## [0.14.0] - 2024-04-08

### Added

- Support advanced group management entity
- Support for roles in group
- Support for group admins
- Support for group enrollement configuration
- Support for enrollement requests to join in a group
- Support for invitation to join in a group
- Support for invitation to become group admin
- Group admin can add another group admin
- Suspend and reactivate group member
- Group membership expiration
- Extra support for account events
- Group member being able to leave group
- Support for pending group members
- Support for [AARC-G069](https://aarc-community.org/guidelines/aarc-g069/) compatible entitlement user attribute URN value based on group membership
- Group admin can delete a group
- Support for manage-groups users
- manage-groups users can create top-level group
- Group admin can create subgroups
- Group admin can view/ update group attributes












