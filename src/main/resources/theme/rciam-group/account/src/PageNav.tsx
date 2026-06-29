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
import { routes, NavRouteObject } from "./routes";

type LeafItem = {
  path: string;
  labelKey: string;
};

type GroupItem = {
  id: string;
  labelKey: string;
  children: LeafItem[];
};

function getRouteUrl(path: string) {
  return path === "" ? "/" : `/${path}`;
}

function isPathActive(currentPath: string, routePath: string): boolean {
  return !!matchPath(
    {
      path: getRouteUrl(routePath),
      end: routePath === "",
    },
    currentPath,
  );
}

function kebabToCamel(str: string) {
  return str.replace(/-([a-z])/g, (_, char) => char.toUpperCase());
}

function labelForNav(groupId: string): string {
  return kebabToCamel(groupId) + "SidebarTitle";
}

function labelForLeaf(path: string): string {
  const last = (path || "").split("/").filter(Boolean).pop() || "personal-info";
  return kebabToCamel(last) + "SidebarTitle";
}

function buildMenuFromRoutes(
  childRoutes: NavRouteObject[],
): { singles: LeafItem[]; groups: GroupItem[] } {
  const singlesMap = new Map<string, LeafItem>();
  const groupsMap = new Map<string, GroupItem>();

  for (const route of childRoutes) {
    if (route.handle?.hideFromNav) {
      continue;
    }

    const rawPath =
      (route.path as string | undefined) ?? (route.index ? "" : undefined);

    if (rawPath === undefined || singlesMap.has(rawPath)) {
      continue;
    }

    const segments = rawPath.split("/").filter(Boolean);

    if (segments.length <= 1) {
      singlesMap.set(rawPath, {
        path: rawPath,
        labelKey: route.handle?.navItemLabelKey ?? labelForLeaf(rawPath),
      });
      continue;
    }

    const defaultGroupId = segments[0];
    const groupId = route.handle?.navGroupId ?? defaultGroupId;

    let group = groupsMap.get(groupId);
    if (!group) {
      group = {
        id: groupId,
        labelKey: route.handle?.navGroupLabelKey ?? labelForNav(groupId),
        children: [],
      };
      groupsMap.set(groupId, group);
    }

    group.children.push({
      path: rawPath,
      labelKey: route.handle?.navItemLabelKey ?? labelForLeaf(rawPath),
    });
  }

  return {
    singles: Array.from(singlesMap.values()),
    groups: Array.from(groupsMap.values()),
  };
}

type NavLinkProps = {
  path: string;
  isActive: boolean;
};

const NavLink = ({
  path,
  isActive,
  children,
}: PropsWithChildren<NavLinkProps>) => {
  const routeUrl = getRouteUrl(path);
  const href = useHref(routeUrl);
  const handleClick = useLinkClickHandler(routeUrl);

  return (
    <NavItem
      data-testid={path || "_index"}
      to={href}
      isActive={isActive}
      onClick={(event) =>
        handleClick(event as unknown as ReactMouseEvent<HTMLAnchorElement>)
      }
    >
      {children}
    </NavItem>
  );
};

export const PageNav = () => {
  const { t } = useTranslation();
  const { pathname } = useLocation();

  const rootRoute = routes[0];
  const childRoutes = (rootRoute.children ?? []) as NavRouteObject[];

  const { singles, groups } = useMemo(
    () => buildMenuFromRoutes(childRoutes),
    [childRoutes],
  );

  return (
    <PageSidebar>
      <PageSidebarBody>
        <Nav>
          <NavList>
            {singles.map((item) => (
              <NavLink
                key={item.path || "_index"}
                path={item.path}
                isActive={isPathActive(pathname, item.path)}
              >
                {t(item.labelKey)}
              </NavLink>
            ))}

            {groups.map((group) => {
              const anyActive = group.children.some((child) =>
                isPathActive(pathname, child.path),
              );

              return (
                <NavExpandable
                  key={group.id}
                  data-testid={group.id}
                  title={t(group.labelKey)}
                  isActive={anyActive}
                  isExpanded={anyActive}
                >
                  {group.children.map((child) => (
                    <NavLink
                      key={child.path}
                      path={child.path}
                      isActive={isPathActive(pathname, child.path)}
                    >
                      {t(child.labelKey)}
                    </NavLink>
                  ))}
                </NavExpandable>
              );
            })}
          </NavList>
        </Nav>
      </PageSidebarBody>
    </PageSidebar>
  );
};