import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, Tooltip, ModalVariant,DataListAction, Pagination,Badge, Modal, Checkbox, Form, FormGroup, Popover, TextArea} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import { TableActionBar } from './TableActionBar';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '
import { InviteMemberModal } from './InviteMemberModal';
import { Msg } from '../../widgets/Msg';
import { DatalistFilterSelect } from '../DatalistFilterSelect';
import { HelpIcon } from '@patternfly/react-icons';
import { Loading } from '../LoadingModal';
import { Link } from 'react-router-dom';



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
    direct:boolean;
    membershipExpiresAt: string;
    group: any;
    groupRoles: string[];
  }

const UserActionModal:FC<any> = (props) => {
  const [justification,setJustification] = useState("");
  let groupsService = new GroupsServiceClient();
  const [user,setUser] = useState<any>({});
  const [isOpen,setIsOpen] = useState(false);
  const [loading,setLoading] = useState(false);
  useEffect(()=>{
    setUser(props.user);
    setIsOpen(Object.keys(props.user).length > 0);
  },[props.user])

  let suspendGroupMember = () => {
    setLoading(true);
    groupsService!.doPost<any>("/group-admin/group/"+user.group.id+"/member/"+ user.id+"/suspend" + (justification?"?justification="+justification:""),{})
    .then((response: HttpResponse<any>) => {
      if(response.status===200||response.status===204){
        props.fetchGroupMembers();
      }
      setLoading(false);
      close();
    })
  }

  let activateGroupMember = () => {
    setLoading(true);
    groupsService!.doPost<any>("/group-admin/group/"+user.group.id+"/member/"+ user.id+"/activate" + (justification?"?justification="+justification:""),{})
    .then((response: HttpResponse<any>) => {
      if(response.status===200||response.status===204){
        props.fetchGroupMembers();
      }
      setLoading(false);
      close();
    })
  }

  const close = ()=> {
    props.setUser({}); 
    setJustification("");
  }
  
  return (
    <Modal
            variant={ModalVariant.medium}
            title={Msg.localize('Confirmation')}
            isOpen={isOpen}
            onClose={()=>{close()}}
            actions={[
              <Button key="confirm" variant="primary" onClick={()=>{
                if(user.status==="ENABLED"){
                  suspendGroupMember();
                }
                else{
                  activateGroupMember();
                }
              }}>
                  {Msg.localize('YES')}
              </Button>,
              <Button key="cancel" variant="link" onClick={()=>{close();}}>
                  {Msg.localize('NO')}
              </Button>
              
            ]}
          >
            <div>
                <Loading active={loading}/>
                <p>{user.status==="ENABLED"?Msg.localize('adminGroupMemberSuspendConfirmation'):Msg.localize('adminGroupMemberRevokeSuspendConfirmation')}</p>
                <Form>
                  <FormGroup
                    labelIcon={
                      <Popover
                        bodyContent={
                          <div>
                            {user.status==="ENABLED"?Msg.localize('adminGroupMemberSuspendJustification'):Msg.localize('adminGroupMemberRevokeSuspendJustification')}
                          </div>
                        }
                      >
                        <button
                          type="button"
                          aria-label="More info for name field"
                          onClick={e => e.preventDefault()}
                          aria-describedby="simple-form-name-01"
                          className="pf-c-form__group-label-help"
                        >
                          <HelpIcon noVerticalAlign />
                        </button>
                      </Popover>
                    }
                    className="gm_suspend-justification-formgroup"
                    label={Msg.localize('justificationLabel')} fieldId={''}                                    >
                      <TextArea
                        type="text"
                        id="simple-form-name-01"
                        name="simple-form-name-01"
                        aria-describedby="simple-form-name-01-helper"
                        value={justification}
                        onChange={(value)=>{setJustification(value)}}
                      />
                  </FormGroup>
                </Form>
              </div>          
              </Modal>
  )
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
    const [initialRender,setInitialRender] = useState(true);
    const [selectedUser,setSelectedUser] = useState<any>({});
    const [directMembers,setDirectMembers] = useState<boolean>(true);
    const [searchParams,setSearchParams] = useState<any>(new URLSearchParams(location.hash.split('?')[1]));

    let groupsService = new GroupsServiceClient();
    useEffect(()=>{
      fetchGroupMembers();
    },[])

    useEffect(()=>{
      if(initialRender){
        setInitialRender(false);
        return;
      }
      setPage(1);
      fetchGroupMembers();
    },[statusSelection,roleSelection,page,perPage,props.groupId,directMembers]);



    useEffect(() => {
      const searchParams = new URLSearchParams(location.hash.split('?')[1]);
      setSearchParams(searchParams);
  }, [location.hash]); 


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

    let preselectedMembershipHandler = (memberships) =>{
      let preselectedMembership = searchParams.get('membership');
      if(preselectedMembership){
        memberships.forEach((membership)=>{
          if(membership.id=preselectedMembership){
            setEditMemberRoles(membership)
          }
        })
      }
      const newSearchParams = new URLSearchParams(searchParams);
      newSearchParams.delete('membership');
      props.history.push({
        hash: newSearchParams.toString() ? `#${newSearchParams.toString()}` : '',
      });
    } 
  
    
    let fetchGroupMembers = (searchString = undefined)=>{
      groupsService!.doGet<any>("/group-admin/group/"+props.groupId+"/members?first="+ (perPage*(page-1))+ "&max=" + perPage + (searchString?"&search="+searchString:""),{params: {...(statusSelection ? {status:statusSelection}:{}),...(roleSelection ? {role:roleSelection}:{}),...(!directMembers?{direct:'false'}:{})}
    })
      .then((response: HttpResponse<any>) => {
        if(response.status===200&&response.data){
          setTotalItems(response.data.count);  
          setGroupMembers(response.data.results);
          preselectedMembershipHandler(response.data.results);
        }
      })
    }

    let deleteGroupMember = (memberId,groupId) => {
      groupsService!.doDelete<any>("/group-admin/group/"+groupId+"/member/"+ memberId)
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
        <UserActionModal user={selectedUser} setUser={setSelectedUser} groupId={props.groupId}  fetchGroupMembers={fetchGroupMembers}/>
        <EditRolesModal member={editMemberRoles} setMember={setEditMemberRoles} groupRoles={props.groupConfiguration?.groupRoles} groupId={props.groupId} fetchGroupMembers={fetchGroupMembers} />
        <TableActionBar
          childComponent={
            <React.Fragment>
              <Checkbox className="gm_direct-member-checkbox" label={Msg.localize('adminGroupViewAllMembersButton')} checked={directMembers} onClick={()=>{setDirectMembers(!directMembers);}} id="required-check" name="required-check" />
              {props.isGroupAdmin&&
                <Button className="gm_invite-member-button" onClick={()=>{setInviteModalActive(true)}}>
                  <Msg msgKey='adminGroupInviteMemberButton' />
                </Button>}
            </React.Fragment>
          }
          searchText={Msg.localize('adminGroupSearchMember')} cancelText={Msg.localize('adminGroupSearchCancel')} search={(searchString)=>{
            fetchGroupMembers(searchString);
            setPage(1);
          }} cancel={()=>{
            fetchGroupMembers();
            setPage(1);
          }}
       />
      <DataList aria-label="Group Member Datalist" isCompact wrapModifier={"breakWord"}>
        <DataListItem aria-labelledby="compact-item1">
          <DataListItemRow>
            <DataListItemCells dataListCells={[
              <DataListCell className="gm_vertical_center_cell" width={3} key="id-hd">
                <strong><Msg msgKey='UniqueIdentifier' /></strong>
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={3} key="email-hd">
                <strong><Msg msgKey='adminGroupMemberCellNameEmail' /></strong>
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={3} key="email-hd">
                <strong><Msg msgKey='Roles' /></strong>
                {props.groupConfiguration?.groupRoles &&
                  <DatalistFilterSelect default={roleSelection} name="group-roles"  options={Object.keys(props.groupConfiguration.groupRoles)} optionsType="raw" action={(selection)=>{setRoleSelection(selection)}}/>
                }       
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={3} key="expiration-hd">
                <strong><Msg msgKey='adminGroupMemberCellMembershipExp' /></strong>
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={2} key="status-hd">
                <strong><Msg msgKey='Status' />
                  <DatalistFilterSelect default={statusSelection} name="group-status"  options={['ENABLED','SUSPENDED','PENDING']} action={(selection)=>{setStatusSelection(selection)}}/>
                </strong>
              </DataListCell>,
              ...(!directMembers?[
                <DataListCell className="gm_vertical_center_cell" width={3} key="group-path-hd">
                    <strong><Msg msgKey='adminGroupPathCell' /></strong>
                </DataListCell>,
                <DataListCell className="gm_vertical_center_cell" width={2} key="direct-hd">
                  <strong><Msg msgKey='adminGroupDirectMembership' /></strong>
                </DataListCell>
              ]:[])

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
                    {member.user?.attributes?.voPersonID||member.user.username}
                  </DataListCell>,
                  <DataListCell width={3} key="secondary content1">
                    <span className="gm_fullname_datalist pf-c-select__menu-item-main">{member.user.firstName && member.user.lastName?member.user.firstName + " " + member.user.lastName:Msg.localize('notAvailable')}</span>
                    <span className="gm_email_datalist pf-c-select__menu-item-description">{member.user.email}</span>
                  </DataListCell>,
                  <DataListCell width={3} key="secondary content2">
                    {member.groupRoles.map((role,index)=>{
                      return <Badge key={index} className="gm_role_badge" isRead>{role}</Badge>
                    })}
                    {props.isGroupAdmin&&
                      <Tooltip
                        content={
                          <div>
                            <Msg msgKey='adminGroupMemberCellRolesTooltip' />
                          </div>
                        }
                      >
                        <div className="gm_edit-member-roles" onClick={()=>{if(directMembers){setEditMemberRoles(member);}else{props.history.push({pathname: '/groups/admingroups/'+member.group.id,search: '?tab=members&membership=' + member.id})} }}><div></div></div>
                      </Tooltip> 
                    }
                  </DataListCell>,
                  <DataListCell width={3} key="secondary content3">
                  {member.membershipExpiresAt||<Msg msgKey='Never' />}
                  </DataListCell>,
                  <DataListCell width={2} key="secondary content4">
                    <Tooltip
                      content={
                        <div>
                          {member.status==='ENABLED'?Msg.localize('adminGroupMemberUserActiveTooltip'):member.status==="SUSPENDED"?Msg.localize('adminGroupMemberUserSuspendedTooltip'):member.status==="PENDING"?Msg.localize('adminGroupMemberUserPendingTooltip'):""}
                        </div>
                      }
                    >
                    <div className="gm_user-status-container">
                      <div className={member.status==='ENABLED'?"gm_icon gm_icon-active-user":member.status==="SUSPENDED"?"gm_icon gm_icon-suspended-user":member.status==="PENDING"?"gm_icon gm_icon-pending-user":""}></div>
                    </div>
                    </Tooltip>
                  </DataListCell>,
                  ...(!directMembers?[
                    <DataListCell width={3} key="secondary content5">
                      <Link to={{pathname:"/groups/admingroups/"+member.group.id,search:"?tab=members"}}  onClick={()=> setDirectMembers(true)}>
                        {member.group.path}
                      </Link>
                    </DataListCell>,
                    <DataListCell width={2} key="secondary content6">
                      <Tooltip content={<div>{member.direct?Msg.localize('adminGroupIsDirect'):Msg.localize('adminGroupIsNotDirect')}</div>}>
                          <Checkbox id="disabled-check-1" className="gm_direct-checkbox" checked={member.direct?true:false} isDisabled />
                      </Tooltip>
                    </DataListCell>
                  ]:[])
                ]}
              />
              <DataListAction
                className="gm_cell-center"
                aria-labelledby="check-action-item1 check-action-action1"
                id="check-action-action1"
                aria-label="Actions"
                isPlainButtonAction
              >
                {props.isGroupAdmin&&
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
                              deleteGroupMember(member.id,member.group.id);
                              setModalInfo({})},
                          cancel: function(){
                              setModalInfo({})}
                        });
                      
                      }}>
                        <div className={"gm_x-button"}></div>
                    </Button>
                  </Tooltip>
                }
                <Tooltip
                  content={
                    <div>
                      {member.status==='ENABLED'?Msg.localize('adminGroupMemberSuspendTooltip'):member.status==="SUSPENDED"||member.status==="PENDING"?Msg.localize('adminGroupMemberActivateTooltip'):""}
                    </div>
                  }
                >
                  <Button variant="danger" className={member.status==='ENABLED'?"gm_ban-button-small":"gm_activate-button-small"} onClick={()=>{setSelectedUser(member);}}>
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
      {props.isGroupAdmin&&<InviteMemberModal active={inviteModalActive} setActive={setInviteModalActive} groupId={props.groupId}/>}        
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
                      {props?.groupRoles&&Object.keys(props.groupRoles).map((role,index)=>{
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
  
