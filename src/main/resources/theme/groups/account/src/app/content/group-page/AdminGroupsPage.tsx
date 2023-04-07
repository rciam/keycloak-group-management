import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import {DataListContent,Tooltip, DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell,Breadcrumb, BreadcrumbItem, InputGroup,TextInput,Button,Pagination} from '@patternfly/react-core';
import {Link} from 'react-router-dom';
//import { fa-search } from '@patternfly/react-icons';
//import { faSearch } from '@fortawesome/free-solid-svg-icons';

// @ts-ignore
import { ContentPage } from '../ContentPage';
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { Msg } from '../../widgets/Msg';

export interface AdminGroupsPageProps {
  match :any;
}

export interface AdminGroupsPageState {
  groups: AdminGroup[];
  directGroups: AdminGroup[];
  isDirectMembership: boolean;
}

interface AdminGroup{
  id? : string;
  name: string;
  path: string;
  extraSubGroups: AdminGroup[];
}

interface Response {
  results: AdminGroup[],
  count: BigInteger;
}

export const AdminGroupsPage: FC<AdminGroupsPageProps> = (props) =>{
// export class AdminGroupsPage extends React.Component<AdminGroupsPageProps, AdminGroupsPageState> {

  let groupsService = new GroupsServiceClient();
 

  const [groups,setGroups] = useState([] as AdminGroup[]);
  const [searchString,setSearchString] = useState<string>("");
  const [searchResult,setSearchResult] = useState<boolean>(false);
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const [totalItems,setTotalItems] = useState<number>(0);

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

  useEffect(()=>{
    fetchAdminGroups({});
  },[perPage,page]);
  


  useEffect(()=>{
    fetchAdminGroups({});
  },[]);


  let fetchAdminGroups= (options)=> {
    let params = [] as string[];
    options?.search&&params.push("search="+options?.search);
    setSearchResult(options?.search);
    groupsService!.doGet<Response>("/group-admin/groups?first="+ (perPage*(page-1))+ "&max=" + perPage + (params.length>0?"&"+params[0]:""))
      .then((response: HttpResponse<Response>) => {
        let count = response?.data?.count||0;
        setTotalItems(count as number);
        setGroups(response?.data?.results||[] as AdminGroup[]);
        //setExpandedIds([]);        
      });
  }




  const emptyGroup= ()=>{
    return (
      <DataListItem key='emptyItem' aria-labelledby="empty-item">
        <DataListItemRow key='emptyRow'>
          <DataListItemCells dataListCells={[
            <DataListCell key='empty'><strong>{searchResult?"No groups match your search":<Msg msgKey='noGroupsText' />}</strong></DataListCell>
          ]} />
        </DataListItemRow>
      </DataListItem>
    )
  }


  return (
    
      <div className="gm_content">
        <Breadcrumb className="gm_breadcumb">
            <BreadcrumbItem to="#">
              Account Console
            </BreadcrumbItem>
            <BreadcrumbItem isActive>
              {Msg.localize('adminGroupLabel')}
            </BreadcrumbItem>
        </Breadcrumb> 
        <ContentPage title={Msg.localize('adminGroupLabel')}>
          <div className="gm_search-input-container">
            <InputGroup className="gm_search-input">
              <TextInput
                name="searchInput"
                id="searchInput1"
                type="text"
                onChange={(e)=>{setSearchString(e)}}
                placeholder="Search..."
                aria-label="Search Input from admin groups"
                onKeyDown={(e)=>{e.key=== 'Enter'&&fetchAdminGroups({search:searchString});setPage(1);}}
              />
              <Tooltip
                content={
                  <div>
                    Search based on Group Name
                  </div>
                }
              >
                <Button variant="control" aria-label="popover for input" onClick={()=>{fetchAdminGroups({search:searchString});setPage(1);}}>
                  <div className='gm_search-icon-container'></div>
              </Button>
              </Tooltip>
              <Tooltip
                content={
                  <div>
                    View All Groups
                  </div>
                }
              >
                <Button variant="control" aria-label="popover for input" onClick={()=>{fetchAdminGroups({});setPage(1);}}>
                  <div className='gm_cancel-icon-container'></div>
                </Button>
              </Tooltip>
            </InputGroup>
          </div>
  
          <DataList id="groups-list" aria-label={Msg.localize('groupLabel')} isCompact>
            
            <DataListItem id="groups-list-header" aria-labelledby="Columns names">
            
              <DataListItemRow className="gm_datalist-header">
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key='group-name-header' width={2}>
                      <strong><Msg msgKey='Name' /></strong>
                    </DataListCell>,
                    <DataListCell key='group-path-header' width={2}>
                      <strong><Msg msgKey='Path' /></strong>
                    </DataListCell>
                  ]}
                />
              </DataListItemRow>
            </DataListItem>
            {groups.length===0 ?
              emptyGroup():
              groups.map((group:AdminGroup,appIndex:number)=>{
                return(
                <GroupListItem group={group as AdminGroup} appIndex={appIndex} depth={0} />
                )
              })
              }
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
      </div>
    );
  }


  export interface GroupListItemProps {
    group: AdminGroup,
    appIndex: number;
    depth:number;
  }
  
 
  const GroupListItem: FC<GroupListItemProps> = ({group,appIndex,depth}) =>{
    useEffect(()=>{
      setExpanded(false);
    },[group]);
    const [expanded,setExpanded]= useState<boolean>(false);

    return(     
        <DataListItem id={`${appIndex}-group`} key={'group-' + appIndex} className={"gm_expandable-list" + (group?.extraSubGroups.length>0?" gm_expandable-list-item":"")} aria-labelledby="groups-list" isExpanded={expanded}>
          <DataListItemRow style={{"paddingLeft": ((depth===0?2:(3+depth-1))+ (group?.extraSubGroups.length>0?0:0.4))+"rem"}}>
            {group?.extraSubGroups.length>0?
              <div className={"gm_epxand-toggle"} onClick={() => {setExpanded(!expanded)}}>
                <div className={expanded?"gm_epxand-toggle-expanded":"gm_epxand-toggle-hidden"}></div>
              </div>
            :null}
            <Link to={"/groups/showgroups/"+group.id}>
            <DataListItemCells
              dataListCells={[
                <DataListCell id={`${appIndex}-group-name`} width={2} key={'name-' + appIndex}>
                  {group.name}
                </DataListCell>,
                <DataListCell id={`${appIndex}-group-path`} width={2} key={'path-' + appIndex}>
                  {group.path}
                </DataListCell>
              ]}
            />
            </Link>
          </DataListItemRow>
          <DataListContent
            aria-label="First expandable content details"
            id="ex-expand1"
            isHidden={!expanded}
          >
            {group?.extraSubGroups.length>0?group?.extraSubGroups.map((subGroup:AdminGroup,appSubIndex:number)=>{
                return(
                  <GroupListItem group={subGroup as AdminGroup} appIndex={appSubIndex} depth={depth+1} />
                )
              }):null}
          </DataListContent>
        </DataListItem>
    )
  }
