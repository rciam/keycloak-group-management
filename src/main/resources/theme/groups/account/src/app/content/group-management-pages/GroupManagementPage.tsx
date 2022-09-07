
import * as React from 'react';


import {
    Tabs, Tab, TabTitleText, Checkbox, Tooltip, Badge
} from '@patternfly/react-core';

import { MyGroups } from './MyGroups';
import { EnrollmentProgress } from './EnrollmentProgress';


interface State {
  activeTabKey: number,
  isBox: boolean
}


interface Props {
  children: React.ReactNode
}

export class GroupsManagementPage extends React.Component<Props, State> {

    constructor(props : Props){
        super(props);
        this.state = {
          activeTabKey: 0,
          isBox: false
        };
    }

    public componentDidMount(): void {

    }



    private handleTabClick = (event: any, tabIndex: number):void => {
      this.setState({
        activeTabKey: tabIndex
      });
    }



    public render(): React.ReactNode {


        return (
            <>

              <Tabs activeKey={this.state.activeTabKey} onSelect={this.handleTabClick} isBox={this.state.isBox} aria-label="Tabs in the default example" role="region">
                <Tab eventKey={0} title={<TabTitleText>My groups</TabTitleText>} aria-label="Show my groups">
                  <MyGroups></MyGroups>
                </Tab>
                <Tab eventKey={1} title={<TabTitleText>Enrollment progress<Badge key={1}>7 new</Badge> </TabTitleText>}>
                  <EnrollmentProgress></EnrollmentProgress>
                </Tab>
                <Tab eventKey={2} title={<TabTitleText>Disabled</TabTitleText>} isDisabled>
                  Disabled
                </Tab>

              </Tabs>

            </>
        );
    }
};
