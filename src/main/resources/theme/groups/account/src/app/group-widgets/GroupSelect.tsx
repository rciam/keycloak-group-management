import * as React from 'react';

import { GroupsServiceClient } from '../groups-mngnt-service/groups.service';

import {
  Divider,
  Label,
  DualListSelector,
  DualListSelectorTreeItemData
} from '@patternfly/react-core';


interface State {
  reloadTrigger: boolean
}

interface Props {
  vo_id: any
  getGroupSelection: any
}



export class GroupSelect extends React.Component<Props, State> {

  groupsService = new GroupsServiceClient();



  availableOptions: any[] = [];
  chosenOptions : any[] = [];


  constructor(props : Props){
      super(props);

      this.availableOptions = [
        {id: "1", text: "Option 1", isChecked: false},
        {id: "2", text: "Option 2", isChecked: false}
      ];

      this.state = {
        reloadTrigger: false
      }
      this.fetchData();
  }


  private fetchData(){
    this.groupsService!.doGet("/groups/user/vo/" + this.props.vo_id + "/groups")
      .then((resp: any) => {
        if(resp.ok){


          //this.availableOptions = this.toTreeListData(resp.data);

          this.setState({
            reloadTrigger: !this.state.reloadTrigger
          });
        }

      }).catch((err: any) => {
        console.log(err);
      });
  }

  toTreeListData = (data: any) : DualListSelectorTreeItemData[] => {
    return data.subGroups.map(group => {
      let translated = {
        id: group.id,
        text: group.name,
        checkProps: { 'aria-label': group.name },
        isChecked: false
      }
      let children = this.toTreeListData(group);
      if(children.length != 0)
        translated['children'] = children;
      return translated;
    });
  }


  onListChange = (newAvailableOptions: DualListSelectorTreeItemData[], newChosenOptions: DualListSelectorTreeItemData[]) => {
    this.availableOptions = newAvailableOptions;
    this.chosenOptions = newChosenOptions;
  };

  public render(): React.ReactNode {

    return (
      <>
        <p>Please select the groups you would like to join</p>
        <br/>
        <Divider />

        <DualListSelector
          isSearchable
          isTree
          availableOptions={this.availableOptions}
          chosenOptions={this.chosenOptions}
          onListChange={this.onListChange}
          id="dual-list-selector-tree"
        />
        <div>{this.state.reloadTrigger}</div>
      </>
    );
  }


}
