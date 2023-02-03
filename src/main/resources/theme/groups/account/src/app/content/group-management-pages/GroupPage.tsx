
import * as React from 'react';

import { GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
import { DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell, Divider, Label } from '@patternfly/react-core';



interface State {
  data: any,
  group_id: any
}


interface Props {
  match:any
}



export class GroupPage extends React.Component<Props, State> {

    groupsService = new GroupsServiceClient();

    constructor(props : Props){

      super(props);
      console.log(props);
      this.state = {
            group_id: props.match.params.id,
            data: []
        };
        
    }

    public componentDidMount(): void {
    }


    public render(): React.ReactNode {
      return (
        <>
          <h1>This is the Show Group Page</h1>
          <h3>The group Id is {this.state.group_id}</h3>
        </>
      );
    }
};
