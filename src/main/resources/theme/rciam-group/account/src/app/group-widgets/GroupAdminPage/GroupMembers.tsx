import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, Tooltip, DataListAction, Pagination, Dropdown, BadgeToggle, DropdownItem, Badge, Modal, Checkbox} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import { SearchInput } from './SearchInput';
import {CheckIcon } from '@patternfly/react-icons';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '
import { InviteMemberModal } from './InviteMemberModal';
import { Msg } from '../../widgets/Msg';



interface FederatedIdentity {
    identityProvider: string;
}

interface User {
    id?: string;
    username: string;
    emailVerified: boolean;
    email: string;
    federatedIdentities: FederatedIdentity[];
    firstName: string;
    lastName: string;
    attributes: any;
}

interface Memberships {
    id?: string;
    user: User;
    status: string;
    membershipExpiresAt: string;
    groupRoles: string[];
  }



export const GroupMembers: FC<any> = (props) => {
    const [groupMembers,setGroupMembers] = useState<Memberships[]>([]);
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(10);
    const [totalItems,setTotalItems] = useState<number>(0);
    const [modalInfo,setModalInfo] = useState({});
    const [statusSelection,setStatusSelection] = useState("");
    const [roleSelection,setRoleSelection] = useState("")
    const [editMemberRoles,setEditMemberRoles] = useState({});
    const [inviteModalActive,setInviteModalActive] = useState(false);


    let groupsService = new GroupsServiceClient();
    useEffect(()=>{
      fetchGroupMembers();
    },[])

    useEffect(()=>{
      setPage(1);
      fetchGroupMembers();
    },[props.groupId])

    useEffect(()=>{
      setPage(1);
      fetchGroupMembers();
    },[statusSelection]);

    useEffect(()=>{
      setPage(1);
      fetchGroupMembers();
    },[roleSelection]);


    useEffect(()=>{
      fetchGroupMembers();
    },[perPage,page]);


    const [isOpen, setIsOpen] = React.useState({status:false,roles:false});

    const onToggle = (open: boolean,type) => {
      isOpen[type] = open;
      setIsOpen({...isOpen});
    };

    const onFocus = (type) => {
      const element = document.getElementById('toggle-badge-'+type);
      element?.focus();
    };

    const onSelect = (type) => {
      isOpen[type] = false;
      setIsOpen({...isOpen});
      onFocus(type);
    };



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


  
    
    let fetchGroupMembers = (searchString = undefined)=>{
      groupsService!.doGet<any>("/group-admin/group/"+props.groupId+"/members?first="+ (perPage*(page-1))+ "&max=" + perPage + (searchString?"&search="+searchString:""),{params: { ...(statusSelection.length > 0 ? {status:statusSelection}:{}),...(roleSelection.length > 0 ? {role:roleSelection}:{})}
    })
      .then((response: HttpResponse<any>) => {
        if(response.status===200&&response.data){
          setTotalItems(response.data.count);
          setGroupMembers(response.data.results);
        }
      })
    }

    let deleteGroupMember = (memberId) => {
      groupsService!.doDelete<any>("/group-admin/group/"+props.groupId+"/member/"+ memberId)
      .then((response: HttpResponse<any>) => {
        if(response.status===200||response.status===204){
          fetchGroupMembers();
        }
      })
    }
    let suspendGroupMember = (memberId) => {
      groupsService!.doPost<any>("/group-admin/group/"+props.groupId+"/member/"+ memberId+"/suspend",{})
      .then((response: HttpResponse<any>) => {
        if(response.status===200||response.status===204){
          fetchGroupMembers();
        }
      })
    }

    let activateGroupMember = (memberId) => {
      groupsService!.doPost<any>("/group-admin/group/"+props.groupId+"/member/"+ memberId+"/activate",{})
      .then((response: HttpResponse<any>) => {
        if(response.status===200||response.status===204){
          fetchGroupMembers();
        }
      })
    }



    const noMembers= ()=>{
        return (
          <DataListItem key='emptyItem' aria-labelledby="empty-item">
            <DataListItemRow key='emptyRow'>
              <DataListItemCells dataListCells={[
                <DataListCell key='empty'><strong><Msg msgKey='adminGroupNoMembers' /></strong></DataListCell>
              ]} />
            </DataListItemRow>
          </DataListItem>
        )
      }
    
  

  
  
    return (
      <React.Fragment>
        <ConfirmationModal modalInfo={modalInfo}/>
        <EditRolesModal member={editMemberRoles} setMember={setEditMemberRoles} groupRoles={props.groupConfiguration?.groupRoles} groupId={props.groupId} fetchGroupMembers={fetchGroupMembers} />
        <SearchInput
          childComponent={<Button className="gm_invite-member-button" onClick={()=>{setInviteModalActive(true)}}><Msg msgKey='adminGroupInviteMemberButton' /></Button>}
          searchText={Msg.localize('adminGroupSearchMember')} cancelText={Msg.localize('adminGroupSearchCancel')}search={(searchString)=>{
            fetchGroupMembers(searchString);
            setPage(1);
          }} cancel={()=>{
            fetchGroupMembers();
            setPage(1);
          }} />
        <DataList aria-label="Group Member Datalist" isCompact>
            <DataListItem aria-labelledby="compact-item1">
              <DataListItemRow>
                <DataListItemCells dataListCells={[
                  <DataListCell className="gm_vertical_center_cell" width={3} key="id-hd">
                    <strong><Msg msgKey='adminGroupMemberCellId' /></strong>
                  </DataListCell>,
                  <DataListCell className="gm_vertical_center_cell" width={3} key="username-hd">
                    <strong><Msg msgKey='Username' /></strong>
                  </DataListCell>,
                  <DataListCell className="gm_vertical_center_cell" width={3} key="email-hd">
                    <strong><Msg msgKey='adminGroupMemberCellNameEmail' /></strong>
                  </DataListCell>,
                  <DataListCell className="gm_vertical_center_cell" width={3} key="email-hd">
                    <strong><Msg msgKey='Roles' /></strong>
                    <Dropdown
                        onSelect={()=>{onSelect('roles')}}
                        toggle={
                          <BadgeToggle id="toggle-badge-roles" onToggle={(e)=>{onToggle(e,'roles')}}>
                            {roleSelection?roleSelection:"all"}
                          </BadgeToggle>
                        }
                        className="gm_badge_dropdown"
                        isOpen={isOpen.roles}
                        dropdownItems={[
                          <DropdownItem key="all" component="button" onClick={()=>{setRoleSelection('')}} icon={!roleSelection&&<CheckIcon />}>
                                <Msg msgKey='all' />
                          </DropdownItem>,
                          ...(props.groupConfiguration && props.groupConfiguration.groupRoles ? props.groupConfiguration.groupRoles.map((role)=>{
                            return (
                              <DropdownItem key={role} component="button" onClick={()=>{setRoleSelection(role)}} icon={roleSelection===role&&<CheckIcon />}>
                                {role}
                              </DropdownItem>
                            )
                          }):[])
                        ]}
                      />
                  </DataListCell>,
                  <DataListCell className="gm_vertical_center_cell" width={3} key="expiration-hd">
                    <strong><Msg msgKey='adminGroupMemberCellMembershipExp' /></strong>
                  </DataListCell>,
                  <DataListCell className="gm_vertical_center_cell" width={2} key="status-hd">
                    <strong><Msg msgKey='Status' />
                      <Dropdown
                        onSelect={()=>{onSelect('status')}}
                        toggle={
                          <BadgeToggle id="toggle-badge-status" onToggle={(e)=>{onToggle(e,'status')}}>
                            {statusSelection==="ENABLED"?"active":statusSelection?statusSelection.toLowerCase():"all"}
                          </BadgeToggle>
                        }
                        className="gm_badge_dropdown"
                        isOpen={isOpen.status}
                        dropdownItems={[                            
                          <DropdownItem key="All" component="button" onClick={()=>{setStatusSelection("")}} icon={!statusSelection&&<CheckIcon />}>
                            <Msg msgKey='All' />
                          </DropdownItem>,
                          <DropdownItem key="Enabled" component="button" onClick={()=>{setStatusSelection("ENABLED")}} icon={statusSelection==="ENABLED"&&<CheckIcon />}>
                            <Msg msgKey='Active' />
                          </DropdownItem>,
                          <DropdownItem key="Suspended" component="button" onClick={()=>{setStatusSelection("SUSPENDED")}} icon={statusSelection==="SUSPENDED"&&<CheckIcon />}>
                            <Msg msgKey='Suspended' />
                          </DropdownItem>,
                          <DropdownItem key="Pending" component="button" onClick={()=>{setStatusSelection("PENDING")}} icon={statusSelection==="PENDING"&&<CheckIcon />}>
                            <Msg msgKey='Pending' />
                          </DropdownItem>
                        ]}
                      />
                    </strong>
                  </DataListCell>
                ]}>
                </DataListItemCells>
                <DataListAction
                      className="gm_cell-center"
                      aria-labelledby="check-action-item1 check-action-action2"
                      id="check-action-action1"
                      aria-label="Actions"
                      isPlainButtonAction
                    ><div className="gm_cell-placeholder"></div></DataListAction>
              </DataListItemRow>
            </DataListItem>
            {groupMembers.length>0?groupMembers.map((member,index)=>{
              return <DataListItem aria-labelledby={"member-"+index}>
                <DataListItemRow>
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell width={3} key="primary content">
                        {member.user?.attributes?.voPersonID||Msg.localize('notAvailable')}
                      </DataListCell>,
                      <DataListCell width={3} key="secondary content ">
                        {member.user.username}
                      </DataListCell>,
                      <DataListCell width={3} key="secondary content ">
                        <span className="gm_fullname_datalist pf-c-select__menu-item-main">{member.user.firstName && member.user.lastName?member.user.firstName + " " + member.user.lastName:Msg.localize('notAvailable')}</span>
                        <span className="gm_email_datalist pf-c-select__menu-item-description">{member.user.email}</span>
                      </DataListCell>,
                      <DataListCell width={3} key="secondary content ">
                      {member.groupRoles.map((role,index)=>{
                        return <Badge key={index} className="gm_role_badge" isRead>{role}</Badge>
                      })}
                      <Tooltip
                        content={
                          <div>
                            <Msg msgKey='adminGroupMemberCellRolesTooltip' />
                          </div>
                        }
                      >
                        <div className="gm_edit-member-roles" onClick={()=>{setEditMemberRoles(member);}}><div></div></div>
                      </Tooltip>
                      </DataListCell>,
                      <DataListCell width={3} key="secondary content ">
                      {member.membershipExpiresAt||<Msg msgKey='Never' />}
                      </DataListCell>,
                      <DataListCell width={2} key="secondary content ">
                        <Tooltip
                          content={
                            <div>
                              {member.status==='ENABLED'?Msg.localize('adminGroupMemberUserActiveTooltip'):member.status==="SUSPENDED"?Msg.localize('adminGroupMemberUserSuspendedTooltip'):""}
                            </div>
                          }
                        >
                        <div className="gm_user-status-container">
                          <div className={member.status==='ENABLED'?"gm_icon gm_icon-active-user":member.status==="SUSPENDED"?"gm_icon gm_icon-suspended-user":""}></div>
                        </div>
                        </Tooltip>
                      </DataListCell>
                    
                    ]}
                  />
                  <DataListAction
                        className="gm_cell-center"
                        aria-labelledby="check-action-item1 check-action-action1"
                        id="check-action-action1"
                        aria-label="Actions"
                        isPlainButtonAction
                      >
                        <Tooltip
                          content={
                            <div>
                              {member.user.id===props.user.userId?Msg.localize('adminGroupMemberLeave'):Msg.localize('adminGroupMemberRemove')}
                            </div>
                          }
                        >
                          <Button className={"gm_x-button-small"} onClick={()=>{
                              setModalInfo({
                                title:(Msg.localize('Confirmation')),
                                accept_message: (Msg.localize('YES')),
                                cancel_message: (Msg.localize('NO')),
                                message: (Msg.localize('adminGroupMemberRemoveConfirmation')),
                                accept: function(){
                                    deleteGroupMember(member.id);
                                    setModalInfo({})},
                                cancel: function(){
                                    setModalInfo({})}
                              });
                             
                            }}>
                              <div className={"gm_x-button"}></div>
                          </Button>
                        </Tooltip>
                        <Tooltip
                          content={
                            <div>
                              {member.status==='ENABLED'?Msg.localize('adminGroupMemberSuspendTooltip'):member.status==="SUSPENDED"?Msg.localize('adminGroupMemberActivateTooltip'):""}
                            </div>
                          }
                        >
                          <Button variant="danger" className={member.status==='ENABLED'?"gm_ban-button-small":"gm_activate-button-small"} onClick={()=>{
                              setModalInfo({
                                title:"Confirmation",
                                accept_message: "YES",
                                cancel_message: "NO",
                                message: (member.status==="ENABLED"?Msg.localize('adminGroupMemberSuspendConfirmation'):Msg.localize('adminGroupMemberRevokeSuspendConfirmation')),
                                accept: function(){
                                    if(member.status==="ENABLED"){
                                      suspendGroupMember(member.id);
                                    }
                                    else{
                                      activateGroupMember(member.id);
                                    }
                                    setModalInfo({})},
                                cancel: function(){
                                    setModalInfo({})}
                              });
                             
                            }}>
                              <div className={member.status==="ENABLED"?"gm_lock-button":"gm_activate-button"}></div>
                          </Button>
                        </Tooltip>
                    </DataListAction>
                </DataListItemRow>
              </DataListItem>
            }):noMembers()}
          </DataList>
          <Pagination
            itemCount={totalItems}
            perPage={perPage}
            page={page}
            onSetPage={onSetPage}
            widgetId="top-example"
            onPerPageSelect={onPerPageSelect}
          /> 
          <InviteMemberModal active={inviteModalActive} setActive={setInviteModalActive} groupId={props.groupId}/>
         
        </React.Fragment>         
   
    )
  }


  interface EditRolesModalProps {
    member: any;
    setMember: any;
    groupRoles:any;
    groupId:any;
    fetchGroupMembers:any;
};




const EditRolesModal: React.FC<EditRolesModalProps> = (props) =>{
    let groupsService = new GroupsServiceClient();

    useEffect(()=>{
      setIsModalOpen(Object.keys(props.member).length > 0);
  },[props.member])

    const [isModalOpen, setIsModalOpen] = React.useState(false);

    const handleModalToggle = () => {
        props?.setMember({});
    };


    let deleteGroupMemberRole = (role) => {
      groupsService!.doDelete<any>("/group-admin/group/"+props.groupId+"/member/"+ props.member?.id+"/role/" + role)
      .then((response: HttpResponse<any>) => {
        if(response.status===200||response.status===204){
          const index = props.member.groupRoles.indexOf(role);
          if (index > -1) { // only splice array when item is found
            props.member.groupRoles.splice(index, 1); // 2nd parameter means remove one item only
          }
          props.setMember({...props.member});
          props.fetchGroupMembers();
        }
      })
    }


    let addGroupMemberRole = (role) => {
      groupsService!.doPost<any>("/group-admin/group/"+props.groupId+"/member/"+ props.member?.id+ "/role?name="+role,{params:{name:role}})
      .then((response: HttpResponse<any>) => {
        if(response.status===200||response.status===204){
          props.member.groupRoles.push(role);
          props.setMember({...props.member});
          props.fetchGroupMembers();
        }
      })
    }
    
    return (
        <React.Fragment>
            <Modal
            variant={"small"}
            title={Msg.localize('adminGroupMemberEditRole')}
            isOpen={isModalOpen}
            onClose={handleModalToggle}
            actions={[
                <Button key="confirm" variant="primary" onClick={()=>{props.setMember({})}}>
                    Ok
                </Button>
            ]}
            >
                 <table className="gm_roles-table gm_table-center">
                    <tbody>
                      {props.groupRoles?.map((role,index)=>{
                          return <tr>
                              <td>
                                  {role}
                              </td>
                              <td>
                                <Checkbox id="standalone-check" name="standlone-check" checked={props.member?.groupRoles?.includes(role)} onClick={()=>{
                                  if(props.member?.groupRoles?.includes(role)){
                                    deleteGroupMemberRole(role);
                                  }
                                  else{
                                    addGroupMemberRole(role);
                                  }
                                }} aria-label="Standalone input" />
                              </td>   
                          </tr>                    
                      })}
                  </tbody>
                  </table>
            </Modal>
        </React.Fragment>   );
}
  
