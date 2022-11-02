import * as React from 'react';

import { GroupsServiceClient } from '../groups-mngnt-service/groups.service';

import {
  DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell,
  Divider,
  Label,

} from '@patternfly/react-core';


interface State {
  selectedDataListItemId: any,
  data: any
}

interface Props {
  getVOSelection: any
}



export class VOSelect extends React.Component<Props, State> {

  groupsService = new GroupsServiceClient();

  onSelectDataListItem = (selectedDataListItemId: any) => {
    this.setState({
      selectedDataListItemId: selectedDataListItemId
    });
    this.props.getVOSelection(selectedDataListItemId);
  }

  onChange = (event: any) => {
    console.log("changed: ", event);
  }


  constructor(props : Props){
      super(props);
      this.state = {
        data: [] ,
        selectedDataListItemId: ''
      };
      this.fetchData();
  }


  private fetchData(){
    this.groupsService!.doGet("/groups/user/vo")
      .then((resp: any) => {
        if(resp.ok)
          this.setState({data: resp.data});
      }).catch((err: any) => {
        console.log(err);
      });
  }



  public render(): React.ReactNode {
    return (
      <>
        <p>Please select the virtual organisation you would like to join</p>
        <Divider />

        <DataList aria-label="Compact data list example" isCompact onSelectDataListItem={this.onSelectDataListItem} onChange={this.onChange}>
        {
        this.state.data && this.state.data.map(item => (
          <DataListItem aria-labelledby="compact-item1" id={item}>
            <DataListItemRow>
              <DataListItemCells
                dataListCells={[
                  <DataListCell key="primary content">
                    <span id="compact-item1">{item.group.name}</span>
                  </DataListCell>,
                  <DataListCell key="secondary content">{item.description}</DataListCell>
                ]}
              />
            </DataListItemRow>
          </DataListItem>
        ))}
        </DataList>

      </>
    );
  }


}
