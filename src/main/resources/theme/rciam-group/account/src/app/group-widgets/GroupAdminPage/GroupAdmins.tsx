import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, Tooltip, DataListAction, SelectVariant, Checkbox,Select,SelectOption, FormAlert, Alert} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import {ValidateEmail} from '../../js/utils.js'
import { Loading } from '../LoadingModal';
import { Msg } from '../../widgets/Msg';


export const GroupAdmins: FC<any> = (props) => {

    const titleId = 'typeahead-select-id-1';

    let groupsService = new GroupsServiceClient();
    const [isOpen,setIsOpen] = useState<boolean>(false);
    const [selected,setSelected] = useState<any>(null);
    const [options,setOptions] = useState<any>([]);
    const [emailError,setEmailError] = useState<boolean>(false);
    const [inviteAddress,setInviteAddress] = useState<string>("");
    const [selectedUserId,setSelectedUserId] = useState<string>("");
    const [modalInfo,setModalInfo] = useState({});
    const [successMessage,setSuccessMessage] = useState("");
    const [loading,setLoading] = useState(false);
    const [groupIds,setGroupIds] = useState([]);
    const [groupAdminIds,setGroupAdminIds] = useState<any>([]);
    const [initialRender,setInitialRender] = useState(true);

    useEffect(()=>{
      fetchGroupAdminIds();
    },[]);

    useEffect(()=>{
      if(initialRender){
        setInitialRender(false);
        return;
      }
      fetchGroupAdminIds();
    },[props.groupId]);

    useEffect(()=>{      
      if(groupIds.length>0){
        fetchGroupMembers();
      }
    },[groupIds])

    useEffect(()=>{
      if(initialRender){
        setInitialRender(false);
        return;
      }
      let groupadminids = [] as any;
      props.groupConfiguration?.admins?.length>0&&props.groupConfiguration?.admins.map((admin)=> {
        groupadminids.push(admin.user.id);
        // groupadminids.push(admin.user.id);
        })
      setGroupAdminIds(groupadminids);      
    },[props.groupConfiguration]);

 

    const noAdmins= ()=>{
        return (
          <DataListItem key='emptyItem' aria-labelledby="empty-item">
            <DataListItemRow key='emptyRow'>
              <DataListItemCells dataListCells={[
                <DataListCell key='empty'><strong><Msg msgKey='adminGroupNoAdmins' /></strong></DataListCell>
              ]} />
            </DataListItemRow>
          </DataListItem>
        )
      }

    const disapearingMessage = (message) => {
      setSuccessMessage(message);
      setTimeout(() => {
        setSuccessMessage("");
      }, 2000);
      
    }

    
    const makeAdmin = (userId) =>{
      groupsService!.doPost<any>("/group-admin/group/"+props.groupId+"/admin/"+userId,{})
      .then((response: HttpResponse<any>) => {
        if(response.status===200||response.status===204){
          props.fetchGroupConfiguration();
          disapearingMessage(Msg.localize('adminGroupAdded'))
          // setGroupMembers(response.data.results);
        }
      }).catch((err)=>{console.log(err)})
    } 

    const removeAdmin = (userId) => {
      groupsService!.doDelete<any>("/group-admin/group/"+props.groupId+"/admin/"+userId,{})
      .then((response: HttpResponse<any>) => {
        if(response.status===200||response.status===204){
          props.fetchGroupConfiguration();
          // setGroupMembers(response.data.results);
        }
      }).catch((err)=>{console.log(err)})
    }

    const sendInvitation = (email) => {
      setLoading(true);
      groupsService!.doPost<any>("/group-admin/group/"+props.groupId+"/admin/invite",{"email":email})
      .then((response: HttpResponse<any>) => {
        setLoading(false);
        if(response.status===200||response.status===204){
          disapearingMessage(Msg.localize('adminGroupInvitationSent'))
          props.fetchGroupConfiguration();
          // setGroupMembers(response.data.results);
        }
      }).catch((err)=>{
        setLoading(false);
        console.log(err)})
    }

    let fetchGroupAdminIds = () => {
        groupsService!.doGet<any>("/group-admin/groupids/all")
        .then((response: HttpResponse<any>) => {
          if(response.status===200&&response.data){
            setGroupIds(response.data)
            // setGroupMembers(response.data.results);
          }
        }).catch((err)=>{console.log(err)})
      
    } 


    let  fetchGroupMembers = async (searchString = "")=>{
      groupsService!.doGet<any>("/group-admin/groups/members",{params:{max:20,search:searchString,groups:groupIds.join(',')}})
      .then((response: HttpResponse<any>) => {
        if(response.status===200&&response.data){
          let members: any = [];

          response.data.results.forEach((membership)=>{
            members.push({value:getUserIdentifier(membership),description:membership.email,id:membership.id,disabled:groupAdminIds.includes(membership.id)});
          })
          setOptions(members);
          // setGroupMembers(response.data.results);
        }
      }).catch((err)=>{console.log(err)})
    }

    let getUserIdentifier = (user) => {
      return   (user.firstName || user.lastName?(user.firstName&&user.firstName+" ")+ user.lastName:user.username?user.username:user.email?user.email:user.id?user.id:Msg.localize('infoNotAvailable'))
    }

    const clearSelection = () => {
        setInviteAddress("");
        setSelected(null);
        setIsOpen(false);
        setEmailError(false);
        fetchGroupMembers();
    };

    const onToggle = (open) => {
      setIsOpen(open);
      };


      
    


  

  
  
    return (
      <React.Fragment>
        <Loading active={loading}/>
        <ConfirmationModal modalInfo={modalInfo}/>
        <DataList aria-label="Group Member Datalist" isCompact>
            <DataListItem aria-labelledby="compact-item1">
              <DataListItemRow>
                <DataListItemCells dataListCells={[
                  <DataListCell width={1} key="id-hd">
                    <strong><Msg msgKey='Id' /></strong>
                  </DataListCell>,
                  <DataListCell width={1} key="username-hd">
                    <strong><Msg msgKey='Username' /></strong>
                  </DataListCell>,
                  <DataListCell width={1} key="email-hd">
                  <strong><Msg msgKey='Email' /></strong>
                  </DataListCell>,
                  <DataListCell width={1} key="email-hd">
                  <strong><Msg msgKey='directAdmin' /></strong>
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
            {props.groupConfiguration?.admins?.length>0?props.groupConfiguration.admins.map((admin,index)=>{
              return <DataListItem aria-labelledby={"member-"+index}>
                <DataListItemRow>
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell width={1} key="primary content">
                        {admin.user.id}
                      </DataListCell>,
                      <DataListCell width={1} key="secondary content ">
                        {admin.user.username}
                      </DataListCell>,
                      <DataListCell width={1} key="secondary content ">
                        {admin.user.email}
                      </DataListCell>,
                      <DataListCell width={1} key="secondary content ">
                        <Tooltip content={<div>{admin.direct?Msg.localize('adminGroupIsDirect'):Msg.localize('adminGroupIsNotDirect')}</div>}>
                            <Checkbox id="disabled-check-1" className="gm_direct-checkbox" defaultChecked={admin.direct?true:false} isDisabled />
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
                      {admin.direct?
                        <Tooltip
                        content={
                            <div>
                            {admin.user.id===props.user.userId?Msg.localize('adminGroupRevokeAdminTooltip'):Msg.localize('adminGroupRevokeAdminTooltip')}
                            </div>
                        }
                        >
                            <Button variant="danger" className={"gm_x-button-small"} onClick={()=>{
                                setModalInfo({
                                  title:"Confirmation",
                                  accept_message: "YES",
                                  cancel_message: "NO",
                                  message: (Msg.localize('adminGroupRevokeAdminConfirmation')),
                                  accept: function(){
                                    removeAdmin(admin.user.id);
                                    setModalInfo({})},
                                  cancel: function(){
                                    setModalInfo({})}
                                });                                
                            }}>
                                <div className={"gm_x-button"}></div>
                            </Button>
                        </Tooltip>
                         :<div className="gm_cell-placeholder"></div>}
                    </DataListAction>
                 
                </DataListItemRow>
              </DataListItem>
            }):noAdmins()}
          </DataList> 
          <div className="gm_add-admin-container">
            <h1><Msg msgKey='adminGroupAddNewTitle' /></h1>
            <p><Msg msgKey='adminGroupAddNewDescription' /></p>

            <div className="gm_add-admin-input">
              <div>
              <Select
                variant={SelectVariant.typeahead}
                typeAheadAriaLabel="Select a state"
                onToggle={onToggle}
                onSelect={()=>{}}
                onClear={clearSelection}
                selections={selected}
                createText={Msg.localize('adminGroupInviteTypeahead')}
                onCreateOption={(value)=>{
                  if(ValidateEmail(value)){
                    setInviteAddress(value)
                  }
                  else{
                    setInviteAddress("");
                    setEmailError(true);
                  }
                  setSelected(value);
                  setIsOpen(false);

                }}
                onFilter={(e,searchString)=>{
                  setInviteAddress("");
                  setSelectedUserId("");
                  setEmailError(false);
                  let filterOptions :any = [];
                  fetchGroupMembers(searchString);
                  options.forEach((option, index) => (
                    filterOptions.push(
                    <SelectOption
                    isDisabled={option.disabled}
                    key={index}
                    onClick={()=>{
                      setInviteAddress("");
                      if(option.id){
                        setSelectedUserId(option.id);
                        if(option.value===Msg.localize('adminGroupNameNotAvailable')){
                          setSelected(option.description);
                        }
                        else{ 
                          setSelected(option.value);
                        }
                      }
                      setIsOpen(false);
                    }}
                    value={option.value+ (option.disabled?' ' +Msg.localize('adminGroupAlreadyAdmin'):"")}
                    {...(option.description && { description: option.description })}
                    />)
                  ));
                  return filterOptions;
                }}
                isOpen={isOpen}
                aria-labelledby={titleId}
                isInputValuePersisted={true}
                placeholderText={Msg.localize('adminGroupSelectUser')}
                isCreatable={true}
              >
              {options.map((option, index) => (
                  <SelectOption
                  isDisabled={option.disabled}
                  key={index}
                  onClick={()=>{
                    setInviteAddress("");
                    if(option.id){
                      setSelectedUserId(option.id);
                      if(option.value===Msg.localize('adminGroupNameNotAvailable')){
                        setSelected(option.description);
                      }
                      else{ 
                        setSelected(option.value);
                      }
                    }
                    setIsOpen(false);
                  }}
                  value={option.value + (option.disabled?' '+Msg.localize('adminGroupAlreadyAdmin'):"")}
                  {...(option.description && { description: option.description })}
                  />
              ))}
              </Select>
              {successMessage?<FormAlert>
                <Alert variant="success" title={successMessage} aria-live="polite" isInline />
              </FormAlert>:null}
              {emailError?<FormAlert>
                <Alert variant="danger" title={Msg.localize('adminGroupInvalidEmail')} aria-live="polite" isInline />
              </FormAlert>:null}
              </div>
              <Tooltip content={<div>{selectedUserId?Msg.localize('adminGroupAddTooltip'):emailError?Msg.localize('adminGroupEmailErrorTooltip'):inviteAddress?Msg.localize('adminGroupSendEmailTooltip'):Msg.localize('adminGroupAddDescriptionTooltip')}</div>}>
                <Button isDisabled={!(selectedUserId||(!emailError&&inviteAddress))}  className={"gm_admin-button "+(inviteAddress||emailError?"gm_invitation-button":"gm_add-admin-button")} onClick={()=>{
                    if(selectedUserId){
                      setModalInfo({
                        title:Msg.localize('Confirmation'),
                        accept_message: "YES",
                        cancel_message: "NO",
                        message: (Msg.localize('adminGroupAddConfirmation1')+  selected +Msg.localize('adminGroupAddConfirmation2')),
                        accept: function(){
                          makeAdmin(selectedUserId);
                          setModalInfo({})},
                        cancel: function(){
                          setModalInfo({})}
                      });
                    }
                    if(inviteAddress){
                      setModalInfo({
                        title:Msg.localize('Confirmation'),
                        accept_message: "YES",
                        cancel_message: "NO",
                        message: (Msg.localize('adminGroupInviteConfirmation') +" ("+  selected +")."),
                        accept: function(){
                          sendInvitation(inviteAddress);
                          setModalInfo({})},
                        cancel: function(){
                          setModalInfo({})}
                      });
                      
                    }
                  }}>
                    <div></div>
                </Button>
              </Tooltip>
            </div>  
            

          </div>
        </React.Fragment>         
   
    )
}
