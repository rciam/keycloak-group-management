import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import { Tabs, Tab, TabTitleText, DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell,Breadcrumb, BreadcrumbItem, } from '@patternfly/react-core';
// @ts-ignore
import { ContentPage } from '../ContentPage';
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { Msg } from '../../widgets/Msg';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';

export interface GroupsPageProps {
  match:any;
}

export interface GroupsPageState {
  group_id: any;
  group_membership: GroupMembership;
}
interface User {
  id: string;
  username: string;
  emailVerified: boolean;
  email: string;
  federatedIdentities: object;
}

interface Group {
  id: string;
  name: string;
}

interface GroupMembership {
  id?: string;
  group: Group;
  user: User;
  status: string;
  membershipExpiresAt: string;
  aupExpiresAt: string;
  validFrom: string;
  groupRoles: string[];
}




// export class GroupPage extends React.Component<GroupsPageProps, GroupsPageState> {
export const GroupPage: FC<GroupsPageProps> = (props)=> {

  let groupsService = new GroupsServiceClient();
  useEffect(()=>{
    fetchGroups();
  },[]);
  const [groupMembership,setGroupMembership] = useState({} as GroupMembership);
  const [groupId] = useState(props.match.params.id);
  const [activeTabKey, setActiveTabKey] = React.useState<string | number>(0);

  const handleTabClick = (
    event: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent,
    tabIndex: string | number
  ) => {
    setActiveTabKey(tabIndex);
  };


  let fetchGroups = ()=>{
    groupsService!.doGet<GroupMembership>("/user/group/"+groupId+"/member")
    .then((response: HttpResponse<GroupMembership>) => {
      if(response.status===200&&response.data){
        setGroupMembership(response.data);
        console.log(response.data);
      }
    })
  }
    return (
      <>
        <div className="gm_content">
          <Breadcrumb className="gm_breadcumb">
            <BreadcrumbItem to="#">
              Account Console
            </BreadcrumbItem>
            <BreadcrumbItem to="#/group-managment/showgroups">
              Group Managment
            </BreadcrumbItem>
            <BreadcrumbItem isActive>
              {groupMembership?.group?.name}
            </BreadcrumbItem>
          </Breadcrumb>
          <ContentPage title={groupMembership?.group?.name||""}>
            <Tabs
            className="gm_tabs"
            activeKey={activeTabKey}
            onSelect={handleTabClick}
            isBox={false}
            aria-label="Tabs in the default example"
            role="region"
            >
              <Tab eventKey={0} title={<TabTitleText>Membership Details</TabTitleText>} aria-label="Default content - users">
                <DataList className="gm_datalist" aria-label="Compact data list example" isCompact>
                <DataListItem aria-labelledby="compact-item2">
                    <DataListItemRow>
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="primary content">
                            <span id="compact-item2"><strong>Member Since</strong></span>
                          </DataListCell>,
                          <DataListCell key="secondary content ">
                             <span>{groupMembership?.validFrom||"Not Available"}</span>  
                          </DataListCell>
                        ]}
                      />
                    </DataListItemRow>
                  </DataListItem>
                  <DataListItem aria-labelledby="compact-item1">
                    <DataListItemRow>
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="primary content">
                            <span id="compact-item1"><strong>Memberhip Expiration</strong></span>
                          </DataListCell>,
                          <DataListCell key="secondary content">{groupMembership?.membershipExpiresAt||"Never"}</DataListCell>
                        ]}
                      />
                    </DataListItemRow>
                  </DataListItem>
                  <DataListItem aria-labelledby="compact-item2">
                    <DataListItemRow>
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="primary content">
                            <span id="compact-item2"><strong>AUP Expiration</strong></span>
                          </DataListCell>,
                          <DataListCell key="secondary content ">
                            <span>{groupMembership?.aupExpiresAt||"Never"}</span>
                          </DataListCell>
                        ]}
                      />
                    </DataListItemRow>
                  </DataListItem>
                  <DataListItem aria-labelledby="compact-item2">
                    <DataListItemRow>
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="primary content">
                            <span id="compact-item2"><strong>Group Roles</strong></span>
                          </DataListCell>,
                          <DataListCell key="secondary content ">
                            {groupMembership?.groupRoles&&groupMembership?.groupRoles.join(', ')||"No Roles"}  
                          </DataListCell>
                        ]}
                      />
                    </DataListItemRow>
                  </DataListItem>
                  
                </DataList>
              </Tab>           
            </Tabs>
          </ContentPage>
        </div>
      </>  
    )
  
};
