# Changelog
All notable changes in keycloak-group-management will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.21.0]

### Added
- Support adding/ removing group admin by username

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












