# Changelog
All notable changes in keycloak-group-management will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

For Keycloak upstream changelog please see https://www.keycloak.org/docs/latest/release_notes/index.html.
  Full Keycloak upstream jira issue can be shown if filtered by Fix version. For example [Keycloak jira issue for 15.0.2 version](https://issues.redhat.com/browse/KEYCLOAK-19161?jql=project%20%3D%20keycloak%20and%20fixVersion%20%3D%2015.0.2)

## [Unreleased]

### Added
- Expand invite group member search to include group admins [RCIAM-1343](https://jira.argo.grnet.gr/browse/RCIAM-1343)
- Requires Approval - Create Enrollment Page [RCIAM-1345](https://jira.argo.grnet.gr/browse/RCIAM-1345)

### Changed
- Improve group member/ admin email [Improve group member/ admin email invitation](https://trello.com/c/uDaQ5usH/2581-improve-group-member-admin-email-invitation)

### Fixed
- Correct notifications for submitting enrollment rrequest

## [0.14.0] - 2024-04-08

### Added

- Support advance group management entity
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
- Support for eduperson_entitlment user attribute based on group memberships
- Group admin can delete a group
- Support for manage-groups users
- manage-groups users can create top-level group
- Group admin can create subgroups
- Group admin can view/ update group attributes












