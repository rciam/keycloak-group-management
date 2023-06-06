
import * as React from 'react';

import { GroupsServiceClient } from '../../groups-mngnt-service/groups.service';

import { } from '@patternfly/react-core';

import { CustomTableComposable } from '../../group-widgets/CustomTableComposable';


interface State {
  data: any
}


interface Props {

}



export class MyGroups extends React.Component<Props, State> {

    groupsService = new GroupsServiceClient();


    constructor(props : Props){
        super(props);
        this.state = {
            data: null
        };
        this.fetchData();
    }

    public componentDidMount(): void {

    }

    private fetchData(){
      this.groupsService!.doGet("/groups/user/test/get-all")
        .then((resp: any) => {
          if(resp.ok)
            this.showData(resp.data);
        }).catch((err: any) => {
          console.log(err);
        });
    }

    private showData(data: any){
      console.log(data);
      //transform data here (if needed before fitting them into the state)

      this.setState({data: data});
    }


    public render(): React.ReactNode {

        return (
            <>
              <CustomTableComposable></CustomTableComposable>
            </>
        );
    }
};
