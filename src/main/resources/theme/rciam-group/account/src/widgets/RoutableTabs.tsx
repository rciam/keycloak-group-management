import {
  Tabs,
  TabsComponent,
  TabsProps,
} from "@patternfly/react-core";
import { JSXElementConstructor, ReactElement, Children, isValidElement } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";

// TODO: Remove once PF issue resolved
type ChildElement = ReactElement<any, JSXElementConstructor<any>>;
type Child = ChildElement | boolean | null | undefined;

type RoutableTabsProps = {
  children: Child | Child[];
  defaultTab?: string;
} & Omit<TabsProps, "ref" | "activeKey" | "defaultActiveKey" | "component" | "children" | "onSelect">;

export const RoutableTabs = ({ children, defaultTab, ...otherProps }: RoutableTabsProps) => {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const currentTab = searchParams.get("tab") ?? defaultTab ?? "";

  // Collect allowed event keys from children, so we can fall back safely
  const eventKeys = Children.toArray(children)
    .filter((c): c is ChildElement => isValidElement(c))
    .map((c) => String((c.props as any).eventKey ?? ""));

  const activeKey = eventKeys.includes(currentTab) ? currentTab : (eventKeys[0] ?? "");

  return (
    <Tabs
      activeKey={activeKey}
      component={TabsComponent.nav}
      onSelect={(_, key) => {
        const tab = String(key ?? "");
        const next = new URLSearchParams(searchParams);

        if (tab) next.set("tab", tab);
        else next.delete("tab");

        // IMPORTANT: use navigate so React Router handles it (no full refresh)
        const search = next.toString();
        navigate({ pathname, search: search ? `?${search}` : "" }, { replace: true });
      }}
      {...otherProps}
    >
      {children as any}
    </Tabs>
  );
};

// Convenience helper for your tabs
export const useRoutableTab = (tab: string) => ({
  eventKey: tab,
});
