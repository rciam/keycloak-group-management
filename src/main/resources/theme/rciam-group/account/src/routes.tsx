import type { IndexRouteObject, RouteObject } from "react-router-dom";
import App from "./App";
import { environment } from "./environment";
import {
  Applications,
  DeviceActivity,
  LinkedAccounts,
  Oid4Vci,
  Resources,
  SigningIn,
  PersonalInfo,
} from "@keycloak/keycloak-account-ui";
import { GroupsPage } from "./groups/GroupsPage";
import { AdminGroupsPage } from "./group-management/AdminGroupsPage";
import { AdminGroupPage } from "./group-management/AdminGroupPage";
import { GroupPage } from "./groups/GroupPage";
import { EnrollmentDiscovery } from "./enrolments/EnrollmentDiscovery";
import { EnrollmentRequests } from "./enrolments/EnrollmentRequests";
import { InvitationLandingPage } from "./enrolments/InvitationLandingPage";

// We can define a small extension type for convenience
export type NavRouteObject = RouteObject & {
  handle?: {
    navGroupId?: string; // which sidebar group this route belongs to
    navGroupLabelKey?: string; // optional custom i18n key for the group
    navItemLabelKey?: string; // optional custom i18n key for this item
    hideFromNav?: boolean; // optional: don't show in sidebar at all
  };
};

export const DeviceActivityRoute: NavRouteObject = {
  path: "account-security/device-activity",
  element: <DeviceActivity />,
};

export const LinkedAccountsRoute: NavRouteObject = {
  path: "account-security/linked-accounts",
  element: <LinkedAccounts />,
};

export const SigningInRoute: NavRouteObject = {
  path: "account-security/signing-in",
  element: <SigningIn />,
};

export const ApplicationsRoute: NavRouteObject = {
  path: "applications",
  element: <Applications />,
};

export const MyGroupsRoute: NavRouteObject = {
  path: "groups/showgroups",
  element: <GroupsPage />,
  // Sidebar group will default to "groups" (first segment), so no handle needed here
};

export const MyGroupRoute: NavRouteObject = {
  path: "groups/showgroups/:groupId",
  element: <GroupPage />,
  handle: {
    hideFromNav: true,
  },
  // Sidebar group will default to "groups" (first segment), so no handle needed here
};

export const EnrollmentDiscoveryRoute: NavRouteObject = {
  path: "enroll",
  element: <EnrollmentDiscovery />,
  handle: {
    hideFromNav: true,
  },
};

export const MyGroupEnrollmentsRoute: NavRouteObject = {
  path: "groups/mygroupenrollments",
  element: <EnrollmentRequests />,
};

export const GroupManagementRoute: NavRouteObject = {
  path: "groups/admingroups",
  element: <AdminGroupsPage />,
  handle: {
    navGroupId: "group-management",
    navGroupLabelKey: "groupManagementSidebarTitle",
    navItemLabelKey: "groupManagementSidebarTitle",
  },
};

export const GroupAdminPageRoute: RouteObject = {
  path: "groups/admingroups/:groupId",
  element: <AdminGroupPage />,
  handle: {
    hideFromNav: true,
  },
};


export const ResourcesRoute: NavRouteObject = {
  path: "resources",
  element: <Resources />,
};

export const PersonalInfoRoute: IndexRouteObject & NavRouteObject = {
  index: true,
  element: <PersonalInfo />,
  path: "",
};

export const Oid4VciRoute: NavRouteObject = {
  path: "oid4vci",
  element: <Oid4Vci />,
};
export const GroupEnrollmentsRoute: NavRouteObject = {
  path: "groups/groupenrollments",
  element: <EnrollmentRequests manage={true} />,
  handle: {
    navGroupId: "group-management",
    navGroupLabelKey: "groupManageEnrollmentsLabel",
    navItemLabelKey: "groupManageEnrollmentsLabel",
  },
};

export const InvitationLandingRoute: NavRouteObject = {
  path: "invitation/:invitation_id",
  element: <InvitationLandingPage />,
  handle: {
    navGroupId: "groups",
    hideFromNav: true,
  },
};

export const RootRoute: NavRouteObject = {
  path: decodeURIComponent(new URL(environment.baseUrl).pathname),
  element: <App />,
  errorElement: <>Error</>,
  children: [
    PersonalInfoRoute,
    DeviceActivityRoute,
    LinkedAccountsRoute,
    SigningInRoute,
    ApplicationsRoute,
    MyGroupsRoute,
    MyGroupRoute,
    InvitationLandingRoute,
    MyGroupEnrollmentsRoute,
    GroupAdminPageRoute,
    GroupManagementRoute,
    EnrollmentDiscoveryRoute,
    GroupEnrollmentsRoute,
    PersonalInfoRoute, // duplicated index is a bit odd but leaving as you had
    ResourcesRoute,
    ...(environment.features.isOid4VciEnabled ? [Oid4VciRoute] : []),
  ],
};

export const routes: NavRouteObject[] = [RootRoute];
