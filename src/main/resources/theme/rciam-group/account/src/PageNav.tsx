import {
  Nav,
  NavExpandable,
  NavItem,
  NavList,
  PageSidebar,
  PageSidebarBody,
} from "@patternfly/react-core";
import {
  PropsWithChildren,
  MouseEvent as ReactMouseEvent,
  useMemo,
} from "react";
import { useTranslation } from "react-i18next";
import {
  matchPath,
  useHref,
  useLinkClickHandler,
  useLocation,
} from "react-router-dom";
import { routes, NavRouteObject } from "./routes";      // 🔴 import NavRouteObject
import { environment } from "./environment";

// --------- Types derived from routes ------------------------------------

type LeafItem = {
  path: string;
  labelKey: string;
};

type GroupItem = {
  id: string;
  labelKey: string;
  children: LeafItem[];
};

// --------- URL + match helpers ------------------------------------------

function getFullUrl(path: string) {
  return `${new URL(environment.baseUrl).pathname}${path}`;
}

function isPathActive(currentPath: string, routePath: string): boolean {
  const pattern = getFullUrl(routePath);

  return !!matchPath(
    {
      path: pattern,
      // index route ("") should only match exactly baseUrl,
      // everything else can match as a prefix (for child URLs)
      end: routePath === "",
    },
    currentPath,
  );
}

// --------- Label helpers -------------------------------------------------

function kebabToCamel(str: string) {
  return str.replace(/-([a-z])/g, (_, char) => char.toUpperCase());
}

function labelForNav(groupId: string): string {
  // e.g. "group-management" -> "groupManagementSidebarTitle"
  return kebabToCamel(groupId) + "SidebarTitle";
}

function labelForLeaf(path: string): string {
  const last = (path || "").split("/").filter(Boolean).pop() || "personal-info";
  return kebabToCamel(last) + "SidebarTitle";
}

// --------- Build menu structure from routes -----------------------------

function buildMenuFromRoutes(
  childRoutes: NavRouteObject[]
): { singles: LeafItem[]; groups: GroupItem[] } {
  const singlesMap = new Map<string, LeafItem>();
  const groupsMap = new Map<string, GroupItem>();

  for (const route of childRoutes) {
    // optional: allow routes to be hidden from nav
    if (route.handle?.hideFromNav) {
      continue;
    }

    const rawPath =
      (route.path as string | undefined) ??
      (route.index ? "" : undefined);

    if (rawPath === undefined) {
      continue;
    }

    // de-dupe paths
    if (singlesMap.has(rawPath)) {
      continue;
    }

    const path = rawPath;
    const segments = path.split("/").filter(Boolean);

    // ---- Top-level (no expandable group) ----
    if (segments.length <= 1) {
      const item: LeafItem = {
        path,
        labelKey: route.handle?.navItemLabelKey ?? labelForLeaf(path),
      };
      singlesMap.set(path, item);
      continue;
    }

    const defaultGroupId = segments[0];
    const groupId = route.handle?.navGroupId ?? defaultGroupId;

    let group = groupsMap.get(groupId);
    if (!group) {
      group = {
        id: groupId,
        labelKey:
          route.handle?.navGroupLabelKey ?? labelForNav(groupId),
        children: [],
      };
      groupsMap.set(groupId, group);
    }

    group.children.push({
      path,
      labelKey: route.handle?.navItemLabelKey ?? labelForLeaf(path),
    });
  }

  return {
    singles: Array.from(singlesMap.values()),
    groups: Array.from(groupsMap.values()),
  };
}

// --------- NavLink ------------------------------------------------------

type NavLinkProps = {
  path: string;
  isActive: boolean;
};

const NavLink = ({
  path,
  isActive,
  children,
}: PropsWithChildren<NavLinkProps>) => {
  const menuItemPath = getFullUrl(path) + window.location.search;
  const href = useHref(menuItemPath);
  const handleClick = useLinkClickHandler(menuItemPath);

  return (
    <NavItem
      data-testid={path}
      to={href}
      isActive={isActive}
      onClick={(event) =>
        handleClick(
          event as unknown as ReactMouseEvent<HTMLAnchorElement>
        )
      }
    >
      {children}
    </NavItem>
  );
};

// --------- PageNav ------------------------------------------------------

export const PageNav = () => {
  const { t } = useTranslation();
  const { pathname } = useLocation();

  const rootRoute = routes[0];
  const childRoutes = (rootRoute.children ?? []) as NavRouteObject[];

  const { singles, groups } = useMemo(
    () => buildMenuFromRoutes(childRoutes),
    [childRoutes]
  );

  return (
    <PageSidebar>
      <PageSidebarBody>
        <Nav>
          <NavList>
            {/* Top-level items (no group) */}
            {singles.map((item) => {
              const active = isPathActive(pathname, item.path);
              return (
                <NavLink
                  key={item.path || "_index"}
                  path={item.path}
                  isActive={active}
                >
                  {t(item.labelKey)}
                </NavLink>
              );
            })}

            {/* Expandable groups */}
            {groups.map((group) => {
              const anyActive = group.children.some((child) =>
                isPathActive(pathname, child.path)
              );

              return (
                <NavExpandable
                  key={group.id}
                  data-testid={group.id}
                  title={t(group.labelKey)}
                  isActive={anyActive}
                  isExpanded={anyActive}
                >
                  {group.children.map((child) => {
                    const active = isPathActive(pathname, child.path);
                    return (
                      <NavLink
                        key={child.path}
                        path={child.path}
                        isActive={active}
                      >
                        {t(child.labelKey)}
                      </NavLink>
                    );
                  })}
                </NavExpandable>
              );
            })}
          </NavList>
        </Nav>
      </PageSidebarBody>
    </PageSidebar>
  );
};
