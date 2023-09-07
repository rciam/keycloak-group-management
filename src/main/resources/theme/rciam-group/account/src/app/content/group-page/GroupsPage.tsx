import * as React from 'react';
import {Link} from 'react-router-dom';
import {FC,useState,useEffect} from 'react';
import {LongArrowAltDownIcon,LongArrowAltUpIcon,AngleDownIcon } from '@patternfly/react-icons';


import {
  Checkbox,
  DataList,
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListItemCells,
  Pagination,
  Tooltip,
  Badge,
} from '@patternfly/react-core';

// @ts-ignore
import { ContentPage } from '../ContentPage';
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { Msg } from '../../widgets/Msg';

export interface GroupsPageProps {
}

export interface GroupsPageState {
  groups: Group[];
  directGroups: Group[];
  isDirectMembership: boolean;
}

interface Group {
  id?: string;
  name: string;
  path: string;
}

interface Response {
  results:Group[];
  count: BigInteger;
}
export const GroupsPage: FC<GroupsPageProps> = (props) => {

  let groupsService = new GroupsServiceClient();
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const [groups,setGroups] = useState([]as Group[]);
  const [totalItems,setTotalItems] = useState<number>(0);
  const [orderBy,setOrderBy] = useState<string>('');
  const [asc,setAsc] = useState<boolean>(true);

  useEffect(()=>{
    fetchGroups();
  },[]);



  useEffect(()=>{
    fetchGroups();
  },[perPage,page,orderBy,asc]);

  const onSetPage = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => {
    setPage(newPage);
  };

  const onPerPageSelect = (
    _event: React.MouseEvent | React.KeyboardEvent | MouseEvent,
    newPerPage: number,
    newPage: number
  ) => {
    setPerPage(newPerPage);
    setPage(newPage);
  };




  const fetchGroups = () =>  {
    groupsService!.doGet<Response>("/user/groups",{params:{first:(perPage*(page-1)),max:perPage,...(orderBy?{order:orderBy}:{}),asc:asc?"true":"false"}})
      .then((response: HttpResponse<Response>) => {
        let count = response?.data?.count||0;
        setTotalItems(count as number);
        setGroups(response?.data?.results||[] as Group[]);
      });
  }

  
  const emptyGroup= ()=> {

    return (
      <DataListItem key='emptyItem' aria-labelledby="empty-item">
        <DataListItemRow key='emptyRow'>
          <DataListItemCells dataListCells={[
            <DataListCell key='empty'><strong><Msg msgKey='noGroupsText' /></strong></DataListCell>
          ]} />
        </DataListItemRow>
      </DataListItem>
    )
  }

  const renderGroupList = (membership, appIndex: number) => {
    return (
      <Link to={"/groups/showgroups/"+membership.group.id}>        
        <DataListItem id={`${appIndex}-group`} key={'group-' + appIndex} aria-labelledby="groups-list" >
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[
                <DataListCell id={`${appIndex}-group-name`} width={2} key={'name-' + appIndex}>
                  {membership.group.name} 
                </DataListCell>,
                <DataListCell id={`${appIndex}-group-roles`} width={2} key={'directMembership-' + appIndex}>
                  {membership.groupRoles.map((role,index)=>{
                        return <Badge key={index} className="gm_role_badge" isRead>{role}</Badge>
                      })}
                </DataListCell>,
                <DataListCell id={`${appIndex}-group-membershipExpiration`} width={2} key={'directMembership-' + appIndex}>
                {membership.membershipExpiresAt||<Msg msgKey='Never'/>}
              </DataListCell>
              ]}
            />
          </DataListItemRow>
        </DataListItem>
      </Link>
    )
  }

  const orderResults = (type) => {
    if(orderBy!==type){
      setOrderBy(type); setAsc(true);
    }
    else if(asc){
      setAsc(false);
    }
    else{
      setAsc(true);
    }
  }
  
  
    return (
      <ContentPage title={Msg.localize('groupLabel')}>
        <DataList id="groups-list" aria-label={Msg.localize('groupLabel')} isCompact>
          <DataListItem id="groups-list-header" aria-labelledby="Columns names">
            <DataListItemRow className="gm_view-groups-header">
              <DataListItemCells
                dataListCells={[
                  <DataListCell key='group-name-header' width={2} onClick={()=>{orderResults('')}}>
                    <strong><Msg msgKey='nameDatalistTitle' /></strong>{!orderBy?<AngleDownIcon/>:asc?<LongArrowAltDownIcon />:<LongArrowAltUpIcon/>}
                  </DataListCell>,
                  <DataListCell key='group-roles' width={2}>
                    <strong><Msg msgKey='rolesDatalistTitle' /></strong>
                  </DataListCell>,
                  <DataListCell key='group-membership-expiration-header' width={2} onClick={()=>{orderResults('membershipExpiresAt')}}>
                  <strong><Msg msgKey='membershipDatalistTitle'/></strong> {orderBy!=='membershipExpiresAt'?<AngleDownIcon/>:asc?<LongArrowAltDownIcon/>:<LongArrowAltUpIcon/>}
                </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          {groups.length === 0
            ? emptyGroup()
            : groups.map((group: Group, appIndex: number) =>
              renderGroupList(group, appIndex))}
        </DataList>
        <Pagination
            itemCount={totalItems}
            perPage={perPage}
            page={page}
            onSetPage={onSetPage}
            widgetId="top-example"
            onPerPageSelect={onPerPageSelect}
          />
      </ContentPage>
    );
  
};
