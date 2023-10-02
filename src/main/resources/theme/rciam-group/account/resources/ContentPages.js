/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import * as React from "../../common/keycloak/web_modules/react.js";
import { Route, Switch, matchPath } from "../../common/keycloak/web_modules/react-router-dom.js";
import { NavItem, NavExpandable } from "../../common/keycloak/web_modules/@patternfly/react-core.js"; // @ts-ignore

import { Msg } from "./widgets/Msg.js"; // @ts-ignore

import { PageNotFound } from "./content/page-not-found/PageNotFound.js"; // @ts-ignore

import { ForbiddenPage } from "./content/forbidden-page/ForbiddenPage.js";
import { GroupPage } from "./content/group-management-pages/GroupPage.js";
import { AdminGroupPage } from "./content/group-management-pages/AdminGroupPage.js";
import { InvitationLandingPage } from "./content/group-management-pages/InvitationLandingPage.js";
import { CreateEnrollment } from "./group-widgets/GroupEnrollment/CreateEnrollment.js";
import { EnrollmentRequests } from "./content/group-management-pages/EnrollmentRequests.js";
;
let customPages = [{
  path: "/groups/showgroups/:id",
  expandId: "groups",
  parentId: "showgroups",
  componentName: "GroupPage"
}, {
  path: "/groups/admingroups/:id",
  expandId: "groups",
  parentId: "admingroups",
  componentName: "AdminGroupPage"
}, {
  path: "/enroll",
  expandId: "groups",
  parentId: "showgroups",
  componentName: "CreateEnrollment"
}];
export function isModulePageDef(item) {
  return item.modulePath !== undefined;
}
export function isExpansion(contentItem) {
  return contentItem.content !== undefined;
}

function groupId(group) {
  return 'grp-' + group;
}

function itemId(group, item) {
  return 'grp-' + group + '_itm-' + item;
}

function isChildOf(parent, child) {
  for (var item of parent.content) {
    if (isExpansion(item) && isChildOf(item, child)) return true;
    if (parent.groupId === child.groupId) return true;
  }

  return false;
}

function removeQueryParamsFromString(inputString) {
  // Check if the input string contains a query parameter
  if (inputString.includes('?')) {
    // Split the string at the '?' character and take the part before it
    const parts = inputString.split('?');
    const newPath = parts[0]; // Return the path without query parameters

    return newPath;
  } // If there are no query parameters, return the original string


  return inputString;
}

function createNavItems(activePage, contentParam, groupNum) {
  if (typeof content === 'undefined') return /*#__PURE__*/React.createElement(React.Fragment, null);
  let current_path = window.location.hash.substring(1);
  let customPage = {
    path: "",
    parentId: "",
    expandId: "",
    componentName: ""
  };
  customPages.forEach(page => {
    matchPath(removeQueryParamsFromString(current_path), {
      path: page.path,
      exact: true,
      strict: false
    }) && (customPage = page) && (activePage = {
      path: "",
      label: "",
      groupId: "",
      itemId: ""
    });
  });
  const links = contentParam.map(item => {
    const navLinkId = `nav-link-${item.id}`;

    if (isExpansion(item)) {
      return /*#__PURE__*/React.createElement(NavExpandable, {
        id: navLinkId,
        groupId: item.groupId,
        key: item.groupId,
        title: Msg.localize(item.label, item.labelParams),
        isExpanded: isChildOf(item, activePage) || customPage.expandId === item.id
      }, createNavItems(activePage, item.content, groupNum + 1));
    } else {
      const page = item;
      return /*#__PURE__*/React.createElement(NavItem, {
        id: navLinkId,
        groupId: item.groupId,
        itemId: item.itemId,
        key: item.itemId,
        to: '#/' + page.path,
        isActive: activePage.itemId === item.itemId || customPage.parentId === item.id,
        type: "button"
      }, Msg.localize(page.label, page.labelParams));
    }
  });
  return /*#__PURE__*/React.createElement(React.Fragment, null, links);
}

export function makeNavItems(activePage) {
  return createNavItems(activePage, content, 0);
}

function setIds(contentParam, groupNum) {
  if (typeof contentParam === 'undefined') return groupNum;
  let expansionGroupNum = groupNum;

  for (let i = 0; i < contentParam.length; i++) {
    const item = contentParam[i];

    if (isExpansion(item)) {
      item.itemId = itemId(groupNum, i);
      expansionGroupNum = expansionGroupNum + 1;
      item.groupId = groupId(expansionGroupNum);
      expansionGroupNum = setIds(item.content, expansionGroupNum);
    } else {
      item.groupId = groupId(groupNum);
      item.itemId = itemId(groupNum, i);
    }
  }

  ;
  return expansionGroupNum;
}

export function initGroupAndItemIds() {
  setIds(content, 0);
} // get rid of Expansions and put all PageDef items into a single array

export function flattenContent(pageDefs) {
  const flat = [];

  for (let item of pageDefs) {
    if (isExpansion(item)) {
      flat.push(...flattenContent(item.content));
    } else {
      flat.push(item);
    }
  }

  return flat;
}
export function makeRoutes() {
  if (typeof content === 'undefined') return /*#__PURE__*/React.createElement("span", null);
  const customComponents = {
    GroupPage: GroupPage,
    AdminGroupPage: AdminGroupPage,
    CreateEnrollment: CreateEnrollment,
    EnrollmentRequests: EnrollmentRequests
  };
  const pageDefs = flattenContent(content);
  const routes = pageDefs.map(page => {
    if (isModulePageDef(page)) {
      const node = React.createElement(page.module[page.componentName], {
        'pageDef': page
      });
      return /*#__PURE__*/React.createElement(Route, {
        key: page.itemId,
        path: '/' + page.path,
        exact: true,
        render: () => node
      });
    } else {
      const pageDef = page;
      return /*#__PURE__*/React.createElement(Route, {
        key: page.itemId,
        path: '/' + page.path,
        exact: true,
        component: pageDef.component
      });
    }
  });
  return /*#__PURE__*/React.createElement(Switch, null, /*#__PURE__*/React.createElement(Route, {
    path: "/groups/groupenrollments",
    component: EnrollmentRequests
  }), routes, customPages.map((item, index) => {
    return /*#__PURE__*/React.createElement(Route, {
      path: item.path,
      component: customComponents[item.componentName]
    });
  }), /*#__PURE__*/React.createElement(Route, {
    path: "/invitation/:invitation_id",
    component: InvitationLandingPage
  }), /*#__PURE__*/React.createElement(Route, {
    path: "/forbidden",
    component: ForbiddenPage
  }), /*#__PURE__*/React.createElement(Route, {
    component: PageNotFound
  }));
}
//# sourceMappingURL=ContentPages.js.map