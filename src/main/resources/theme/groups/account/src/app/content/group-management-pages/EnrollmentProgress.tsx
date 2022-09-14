
import * as React from 'react';

import { GroupsServiceClient } from '../../groups-mngnt-service/groups.service';

import { DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell, Divider, Label } from '@patternfly/react-core';



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
            data: []
        };
        this.fetchData();
    }

    public componentDidMount(): void {

    }

    private fetchData(){
      this.groupsService!.doGet("/groups/user/enroll/request")
        .then((resp: any) => {
          if(resp.ok)
            this.setState({data: resp.data});
        }).catch((err: any) => {
          console.log(err);
        });
    }
    

    private toDate(timestamp: number){
      let dateTimeFormat = new Intl.DateTimeFormat('en-US', { year: 'numeric', month: 'long', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' });
      return dateTimeFormat.format(timestamp);
    }

    public render(): React.ReactNode {

      return (
        <>
          <DataList aria-label="Simple data list example" isCompact>
          {
            this.state.data.map( (d: any) => (
              <DataListItem>
                <DataListItemRow>
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell>
                        <span>{d.group.name}</span>
                      </DataListCell>,
                      <DataListCell>
                        <DataList aria-label="internal" isCompact>
                        {
                          d.enrollmentStates.map((es: any) => (
                          <DataListItem>
                            <DataListItemRow>
                              <DataListItemCells
                                dataListCells={[
                                  <DataListCell>
                                    <Label>{es.state}</Label>
                                  </DataListCell>,
                                  <DataListCell>
                                    <span>{this.toDate(es.timestamp)}</span>
                                  </DataListCell>,
                                  <DataListCell>
                                    <span>{es.justification}</span>
                                  </DataListCell>
                                ]}
                              />
                            </DataListItemRow>
                          </DataListItem>
                          ))
                        }
                        </DataList>
                      </DataListCell>
                    ]}
                  />
                </DataListItemRow>
              </DataListItem>

            ))
          }
          </DataList>
        </>

      );

/*
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
*/

    }
};
