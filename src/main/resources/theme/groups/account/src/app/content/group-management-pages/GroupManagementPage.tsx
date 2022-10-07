
import * as React from 'react';

import {HashRouter, Route, Link, Switch} from 'react-router-dom';


import {
    Tabs, Tab, TabTitleText,
    Checkbox,
    Button,
    Tooltip,
    Badge,
    Breadcrumb, BreadcrumbItem,
    Flex, FlexItem,
    Card, CardTitle, CardBody, CardFooter,
    Divider,
    Grid, GridItem
} from '@patternfly/react-core';


import { MyGroups } from './MyGroups';
import { EnrollmentProgress } from './EnrollmentProgress';
import { EnrollmentRequest } from './EnrollmentRequest';


enum Menus {
  main,
  show_groups,
  join_groups,
  enrollment_progress
}

interface State {
  menu: Menus
}


interface Props {
  children: React.ReactNode
}

export class GroupsManagementPage extends React.Component<Props, State> {

    constructor(props : Props){
        super(props);
        this.state = {
          menu: Menus.main,
        };
    }

    public componentDidMount(): void {

      let navItem = document.getElementById('nav-link-group-management');
      if(navItem==null)
        return;
      navItem.onclick = (event) => {
        this.goToMainMenu();
      }

    }


    goToMainMenu = () =>{
        this.setState({menu:Menus.main});
    }


    public render(): React.ReactNode {

      return (
          <>
            {this.state.menu==Menus.main && this.renderMainMenu()}
            {this.state.menu==Menus.show_groups && this.renderGroupsMenu()}
            {this.state.menu==Menus.join_groups && this.renderJoinGroupMenu()}
            {this.state.menu==Menus.enrollment_progress && this.renderEnrollmentProgressMenu()}
          </>
      );

    }


    public renderMainMenu(): React.ReactNode {

      return (
        <>
          <Grid className="top-bottom-margin-10 centered-text">
            <GridItem span={12}>What would you like to do?</GridItem>
          </Grid>

          <Flex>
            <FlexItem>
              <Card
                id="show-groups"
                onClick={() => this.setState({menu:Menus.show_groups})}
                isRounded
                isSelectable
              >
                <CardTitle>Show my groups</CardTitle>
                <CardBody>Here you can see which groups you have already joined into</CardBody>
              </Card>
            </FlexItem>
            <FlexItem>
              <Card
                id="enroll-groups"
                onClick={() => this.setState({menu:Menus.join_groups})}
                isRounded
                isSelectable
              >
                <CardTitle>Join group(s)</CardTitle>
                <CardBody>Here you can ask to join a new group</CardBody>
              </Card>
            </FlexItem>
            <FlexItem>
              <Card
                id="enroll-groups"
                onClick={() => this.setState({menu:Menus.enrollment_progress})}
                isRounded
                isSelectable
              >
                <CardTitle>View enrollment progress</CardTitle>
                <CardBody>Here you can view the progress of your group enrollment requests</CardBody>
              </Card>
            </FlexItem>
          </Flex>
        </>
      )

    }


    public renderGroupsMenu(): React.ReactNode {

      return (
        <MyGroups></MyGroups>
      );

    }


    public renderJoinGroupMenu(): React.ReactNode {

      return (
        <EnrollmentRequest goToMainMenu = {this.goToMainMenu}></EnrollmentRequest>
      );

    }

    public renderEnrollmentProgressMenu(): React.ReactNode {

      return (
        <EnrollmentProgress></EnrollmentProgress>
      );

    }


};
