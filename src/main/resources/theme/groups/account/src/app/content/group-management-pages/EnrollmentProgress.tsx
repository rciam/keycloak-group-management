
import * as React from 'react';

import { GroupsServiceClient } from '../../groups-mngnt-service/groups.service';

import { DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell, Divider } from '@patternfly/react-core';



interface State {
  data: any
}


interface Props {

}



export class EnrollmentProgress extends React.Component<Props, State> {

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
      this.groupsService!.doGet("/groups/user/enroll/request")
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
              <DataList aria-label="Simple data list example" isCompact>
                <DataListItem aria-labelledby="compact-item1">
                  <DataListItemRow>
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell key="primary content">
                          <span id="simple-item1">Primary content</span>
                        </DataListCell>,
                        <DataListCell key="secondary content">Secondary content</DataListCell>
                      ]}
                    />
                  </DataListItemRow>
                </DataListItem>
                <DataListItem aria-labelledby="compact-item2">
                  <DataListItemRow>
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell isFilled={false} key="secondary content fill">
                          <span id="simple-item2">Secondary content (pf-m-no-fill)</span>
                        </DataListCell>,
                        <DataListCell isFilled={false} alignRight key="secondary content align">
                          Secondary content (pf-m-align-right pf-m-no-fill)
                        </DataListCell>
                      ]}
                    />
                  </DataListItemRow>
                </DataListItem>
              </DataList>
            </>
        );
    }
};
