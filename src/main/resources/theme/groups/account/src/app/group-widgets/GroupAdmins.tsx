import * as React from 'react';
import {FC,useState,useEffect,useRef} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, Tooltip, DataListAction, SelectVariant, Checkbox,Select,SelectOption, FormAlert, Alert} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from './Modal';
import {ValidateEmail} from '../js/utils.js'

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

    useEffect(()=>{
      fetchGroupMembers()
    },[]);


 

    const noAdmins= ()=>{
        return (
          <DataListItem key='emptyItem' aria-labelledby="empty-item">
            <DataListItemRow key='emptyRow'>
              <DataListItemCells dataListCells={[
                <DataListCell key='empty'><strong>This group has no admins</strong></DataListCell>
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

    const onSelect = (event, selection, isPlaceholder) => {
      setInviteAddress("");
      if (isPlaceholder) clearSelection();
      else if (!selectUser(selection)&&selection){
        if(ValidateEmail(selection)){
          setInviteAddress(selection)
        }
        else{
          setInviteAddress("");
          setEmailError(true);
        }
      }
      else {
          setIsOpen(false);
      }
      setSelected(selection);
    };
    
    const selectUser = (username)=>{
      let userId;
      options.forEach(user=>{
        if(user.value===username){
          userId=user.id;
        }
      })
      setSelectedUserId(userId);

      return userId;
    }
    
    const makeAdmin = (userId) =>{
      groupsService!.doPost<any>("/group-admin/group/"+props.groupId+"/admin/"+userId,{})
      .then((response: HttpResponse<any>) => {
        if(response.status===200||response.status===204){
          props.fetchGroupConfiguration();
          disapearingMessage("Admin Succesfully Added.")
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
      groupsService!.doPost<any>("/group-admin/group/"+props.groupId+"/admin/invite",{"email":email})
      .then((response: HttpResponse<any>) => {
        if(response.status===200||response.status===204){
          disapearingMessage("Invitation was succesfully sent to the email address.")
          props.fetchGroupConfiguration();
          // setGroupMembers(response.data.results);
        }
      }).catch((err)=>{console.log(err)})
    }

    let  fetchGroupMembers = async (searchString = "")=>{
      groupsService!.doGet<any>("/group-admin/group/"+props.groupId+"/members",{params:{max:20,search:searchString}})
      .then((response: HttpResponse<any>) => {
        if(response.status===200&&response.data){
          let members: any = [];
          response.data.results.forEach((membership)=>{
            members.push({value:membership.user.username,description:membership.user.email,id:membership.user.id});
          })
          setOptions(members);
          // setGroupMembers(response.data.results);
        }
      }).catch((err)=>{console.log(err)})
    }

    const clearSelection = () => {

        setSelected(null);
        setIsOpen(false);
        setOptions([]);
    };

    const onToggle = (open) => {
      setIsOpen(open);
      };



    


  

  
  
    return (
      <React.Fragment>
        <ConfirmationModal modalInfo={modalInfo}/>
        <DataList aria-label="Group Member Datalist" isCompact>
            <DataListItem aria-labelledby="compact-item1">
              <DataListItemRow>
                <DataListItemCells dataListCells={[
                  <DataListCell width={1} key="id-hd">
                    <strong>Id</strong>
                  </DataListCell>,
                  <DataListCell width={1} key="username-hd">
                    <strong>Username</strong>
                  </DataListCell>,
                  <DataListCell width={1} key="email-hd">
                  <strong>Email</strong>
                  </DataListCell>,
                  <DataListCell width={1} key="email-hd">
                  <strong>Direct Admin</strong>
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
                        <Tooltip content={<div>{admin.direct?"This user is a direct admin in this group":"This user is not a direct admin in this group"}</div>}>
                            <Checkbox id="disabled-check-1" className="gm_direct-checkbox" defaultChecked={admin.direct?true:false} isDisabled />
                        </Tooltip>
                      </DataListCell>
                    ]}
                  />
                  {admin.direct?
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
                            {admin.user.id===props.user.userId?"Revoke Admin Rights for this group":"Revoke Admin Rights for this group"}
                            </div>
                        }
                        >
                            <Button variant="danger" className={"gm_x-button-small"} onClick={()=>{
                                setModalInfo({
                                  title:"Confirmation",
                                  accept_message: "YES",
                                  cancel_message: "NO",
                                  message: ("Are you sure you want to remove this user as an admin to this group."),
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
                    </DataListAction>
                  :""}
                </DataListItemRow>
              </DataListItem>
            }):noAdmins()}
          </DataList> 
          <div className="gm_add-admin-container">
            <h1>Add New Group Admin</h1>
            <p>Use the input to search for a user to add as a group admin, or type a valid email address to send an invitation.</p>

            <div className="gm_add-admin-input">
              <div>
              <Select
                variant={SelectVariant.typeahead}
                typeAheadAriaLabel="Select a state"
                onToggle={onToggle}
                onSelect={onSelect}
                onClear={clearSelection}
                selections={selected}
                createText="Invite with email"
                onCreateOption={(value)=>{
                  if(ValidateEmail(value)){
                    setInviteAddress(value)
                  }
                  else{
                    setInviteAddress("");
                    setEmailError(true);
                  }

                }}
                onFilter={(e,searchString)=>{
                  setInviteAddress("");
                  setSelectedUserId("");
                  setEmailError(false);
                  let filterOptions :any = []
                  fetchGroupMembers(searchString);
                  options.forEach((option, index) => (
                    filterOptions.push(
                    <SelectOption
                    isDisabled={option.disabled}
                    key={index}
                    value={option.value}
                    {...(option.description && { description: option.description })}
                    />)
                  ))
                  return filterOptions;
                }}
                isOpen={isOpen}
                aria-labelledby={titleId}
                isInputValuePersisted={true}
                placeholderText="Select a user"
                isCreatable={true}
              >
              {options.map((option, index) => (
                  <SelectOption
                  isDisabled={option.disabled}
                  key={index}
                  value={option.value}
                  {...(option.description && { description: option.description })}
                  />
              ))}
              </Select>
              {successMessage?<FormAlert>
                <Alert variant="success" title={successMessage} aria-live="polite" isInline />
              </FormAlert>:null}
              {emailError?<FormAlert>
                <Alert variant="danger" title="Invalid Email" aria-live="polite" isInline />
              </FormAlert>:null}
              </div>
              <Tooltip content={<div>{selectedUserId?"Add selected user as an admin to this group.":emailError?"To send an invitation please provide a valid email address.":inviteAddress?"Send invitation to become a group admin.":"Select a user or provide a valid a valid email address to add/invite a user to become a group admin."}</div>}>
                <Button isDisabled={!(selectedUserId||(!emailError&&inviteAddress))}  className={"gm_admin-button "+(inviteAddress||emailError?"gm_invitation-button":"gm_add-admin-button")} onClick={()=>{
                    if(selectedUserId){
                      setModalInfo({
                        title:"Confirmation",
                        accept_message: "YES",
                        cancel_message: "NO",
                        message: ("Are you sure you want to add this user ("+  selected +") as an admin to this group."),
                        accept: function(){
                          makeAdmin(selectedUserId);
                          setModalInfo({})},
                        cancel: function(){
                          setModalInfo({})}
                      });
                    }
                    if(inviteAddress){
                      setModalInfo({
                        title:"Confirmation",
                        accept_message: "YES",
                        cancel_message: "NO",
                        message: ("Are you sure you want to send an invitation to this address ("+  selected +")."),
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
