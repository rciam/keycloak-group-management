import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import {DataListContent, DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell,Breadcrumb, BreadcrumbItem,Pagination, DataListAction, Dropdown, KebabToggle, DropdownItem, Tooltip} from '@patternfly/react-core';
import {Link} from 'react-router-dom';
//import { fa-search } from '@patternfly/react-icons';
//import { faSearch } from '@fortawesome/free-solid-svg-icons';

// @ts-ignore
import { ContentPage } from '../ContentPage';
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { Msg } from '../../widgets/Msg';
import { SearchInput } from '../../group-widgets/GroupAdminPage/SearchInput';
import { CreateSubgroupModal, DeleteSubgroupModal } from '../../group-widgets/Modals';

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

interface User {

}

interface Response {
  results: AdminGroup[],
  count: BigInteger;
}

interface User {
  userId?: string;
  displayName: string;
}

export const AdminGroupsPage: FC<AdminGroupsPageProps> = (props) =>{
// export class AdminGroupsPage extends React.Component<AdminGroupsPageProps, AdminGroupsPageState> {

  let groupsService = new GroupsServiceClient();
 

  const [groups,setGroups] = useState([] as AdminGroup[]);
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const [totalItems,setTotalItems] = useState<number>(0);
  const [initialRender,setInitialRender] = useState(true);

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
    if(initialRender){
      setInitialRender(false);
      return;
    }
    fetchAdminGroups();
  },[perPage,page]);
  


  useEffect(()=>{
    fetchAdminGroups();
  },[]);



  let fetchAdminGroups= (searchString = undefined)=> {

    groupsService!.doGet<Response>("/group-admin/groups?first="+ (perPage*(page-1))+ "&max=" + perPage + (searchString?"&search="+searchString:""))
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
            <DataListCell key='empty'><strong><Msg msgKey='noGroups' /></strong></DataListCell>
          ]} />
        </DataListItemRow>
      </DataListItem>
    )
  }


  return (
    
      <div className="gm_content">
        <Breadcrumb className="gm_breadcumb">
            <BreadcrumbItem to="#">
            <Msg msgKey='Account Console' />
            </BreadcrumbItem>
            <BreadcrumbItem isActive>
              {Msg.localize('adminGroupLabel')}
            </BreadcrumbItem>
        </Breadcrumb> 
        <ContentPage title={Msg.localize('adminGroupLabel')}>
          <SearchInput searchText={Msg.localize('searchBoxPlaceholder')} cancelText={Msg.localize('searchBoxCancel')}  search={(searchString)=>{
            fetchAdminGroups(searchString);
            setPage(1);
          }} />
  
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
                <DataListAction
                      className="gm_cell-center"
                      aria-labelledby="check-action-item1 check-action-action2"
                      id="check-action-action1"
                      aria-label="Actions"
                      isPlainButtonAction
                ><div className="gm_cell-placeholder"></div></DataListAction>
              </DataListItemRow>
            </DataListItem>
            {groups.length===0 ?
              emptyGroup():
              groups.map((group:AdminGroup,appIndex:number)=>{
                return(
                <GroupListItem group={group as AdminGroup}  fetchAdminGroups={fetchAdminGroups} appIndex={appIndex} depth={0} />
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
    fetchAdminGroups: Function;
  }
  
 
  export const GroupListItem: FC<GroupListItemProps> = ({group,appIndex,depth,fetchAdminGroups}) =>{
    useEffect(()=>{
      setExpanded(false);
    },[group]);
    const [expanded,setExpanded]= useState<boolean>(false);
    const [isOpen,setIsOpen] = useState(false);
    const [createSubGroup,setCreateSubGroup] = useState(false);
    const [deleteGroup,setDeleteGroup] = useState(false);
    const [tooltip,setTooltip] = useState(false);

    let groupsService = new GroupsServiceClient();

    const disapearingTooltip = () => {
      setTooltip(true);
      setTimeout(() => {
        setTooltip(false);
      }, 2000);
      
    }

    const onToggle = (isOpen: boolean) => {
      setIsOpen(isOpen);
    };
  
    const onFocus = () => {
      const element = document.getElementById('toggle-kebab');
      element&&element.focus();
    };
  
    const onSelect = () => {
      setIsOpen(false);
      onFocus();
    };

    const onCopyLink = ()=>{
      disapearingTooltip();
      let link = groupsService.getBaseUrl() + '/account/#/enroll?groupPath='+encodeURI(group.path);
      navigator.clipboard.writeText(link)
    }

    const dropdownItems = [
      <DropdownItem key="link" onClick={()=>{setCreateSubGroup(true);}}><Msg msgKey='createSubGroup' /></DropdownItem>,
      <DropdownItem key="link" onClick={() => onCopyLink()}>
        <Msg msgKey='copyGroupEnrollmentLink' />
      </DropdownItem>,
      ...(('/'+group.name!==group.path)&& !(group?.extraSubGroups.length>0)?[<DropdownItem key="action" onClick={()=>{setDeleteGroup(true);}} component="button">
        <Msg msgKey='deleteGroup' /> 
      </DropdownItem>
      ]:[])
    ];
  

    return(  
      <React.Fragment>
        <CreateSubgroupModal groupId={group.id} active={createSubGroup} afterSuccess={()=>{fetchAdminGroups();}} close={()=>{setCreateSubGroup(false);}}/> 
        <DeleteSubgroupModal groupId={group.id} active={deleteGroup} afterSuccess={()=>{fetchAdminGroups();}} close={()=>{setDeleteGroup(false);}}/>  
        <DataListItem id={`${appIndex}-group`} key={'group-' + appIndex} className={"gm_expandable-list" + (group?.extraSubGroups.length>0?" gm_expandable-list-item":"")} aria-labelledby="groups-list" isExpanded={expanded}>
          <DataListItemRow style={{"paddingLeft": ((depth===0?2:(3+depth-1))+ (group?.extraSubGroups.length>0?0:0.4))+"rem"}}>
            {group?.extraSubGroups.length>0?
              <div className={"gm_epxand-toggle"} onClick={() => {setExpanded(!expanded)}}>
                <div className={expanded?"gm_epxand-toggle-expanded":"gm_epxand-toggle-hidden"}></div>
              </div>
            :null}
            <Link to={"/groups/admingroups/"+group.id}>
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
            <DataListAction
                      className="gm_cell-center gm_kebab-menu-cell"
                      aria-labelledby="check-action-item1 check-action-action2"
                      id="check-action-action1"
                      aria-label="Actions"
                      isPlainButtonAction
            >
              <Tooltip {...(!!(tooltip) ? { trigger:'manual', isVisible:true }:{ trigger:'manual', isVisible:false })}
                      content={
                          <div><Msg msgKey='copiedTooltip'/></div>
                      }
              >
                <Dropdown
                  alignments={{
                    sm: 'right',
                    md: 'right',
                    lg: 'right',
                    xl: 'right',
                    '2xl': 'right'
                  }}
                  onSelect={onSelect}
                  toggle={<KebabToggle id="toggle-kebab" onToggle={onToggle} />}
                  isOpen={isOpen}
                  isPlain
                  dropdownItems={dropdownItems}
                />
              </Tooltip>
              
              </DataListAction>
          </DataListItemRow>
          <DataListContent
            aria-label="First expandable content details"
            id="ex-expand1"
            isHidden={!expanded}
          >
            {group?.extraSubGroups.length>0?group?.extraSubGroups.map((subGroup:AdminGroup,appSubIndex:number)=>{
                return(
                  <GroupListItem group={subGroup as AdminGroup} appIndex={appSubIndex} depth={depth + 1} fetchAdminGroups={fetchAdminGroups} />
                )
              }):null}
          </DataListContent>
        </DataListItem>
      </React.Fragment>
    )
  }
