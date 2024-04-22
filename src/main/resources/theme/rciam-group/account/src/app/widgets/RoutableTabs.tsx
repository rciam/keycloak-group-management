import {
  TabProps,
  Tabs,
  TabsComponent,
  TabsProps,
} from "@patternfly/react-core";
import {
  Children,
  isValidElement,
  JSXElementConstructor,
  ReactElement,useEffect,useState
} from "react";
import * as React from 'react';
import { NavLink } from "react-router-dom";

// TODO: Remove the custom 'children' props and type once the following issue has been resolved:
// https://github.com/patternfly/patternfly-react/issues/6766
type ChildElement = ReactElement<TabProps, JSXElementConstructor<TabProps>>;
type Child = ChildElement | boolean | null | undefined;

// TODO: Figure out why we need to omit 'ref' from the props.
type RoutableTabsProps = {
  children: Child | Child[];
  defaultTab?: string;
} & Omit<
  TabsProps,
  "ref" | "activeKey" | "defaultActiveKey" | "component" | "children"
>;

export const RoutableTabs = ({
  children,
  defaultTab,
  ...otherProps
}: RoutableTabsProps) => {

    const [tab,setTab] = useState(defaultTab)

    useEffect(() => {
        const searchParams = new URLSearchParams(location.hash.split('?')[1]);
        let tab_param = searchParams.get('tab');
        if(tab_param){
            setTab(tab_param);
        }
    }, [location.hash]); 

  return (
    <Tabs
      activeKey={
        tab
      }
      component={TabsComponent.nav}
      {...otherProps}
    >
      {children}
    </Tabs>
  );
};

export const useRoutableTab = (tab: string) => ({
  eventKey: tab,
  href: location.hash.split('?')[0]+ (tab?"?tab="+tab:""),
  component: NavLink,
});
