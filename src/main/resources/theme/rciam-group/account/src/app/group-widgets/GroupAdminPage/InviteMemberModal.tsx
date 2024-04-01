import * as React from 'react';
import {useState,useEffect,useRef} from 'react';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '
import {ValidateEmail} from '../../js/utils.js'
import { Alert, Button, Checkbox, DataList, DataListCell, DataListItem, DataListItemCells, DataListItemRow, FormAlert, Modal, ModalVariant, Select, SelectOption, SelectVariant, Spinner, Wizard, WizardStep } from '@patternfly/react-core';
import { Msg } from '../../widgets/Msg';




export const InviteMemberModal: React.FC<any> = (props) => {


    let groupsService = new GroupsServiceClient();
    const [stepIdReached, setStepIdReached] = React.useState(1);
    const [isStep1Complete,setIsStep1Complete] = useState(false);
    const [isStep2Complete,setIsStep2Complete] = useState(false);
    const [invitationData,setInvitationData] = useState({
      groupEnrollmentConfiguration: {},
      groupRoles: [],
      withoutAcceptance:true
    });
    const [invitationResult,setInvitationResult] = useState<any>("");
    const [isModalOpen,setIsModalOpen] = useState(false);
    const [loading,setLoading] = useState(false);

    useEffect(()=>{
      setIsModalOpen(props.active)
    },[props.active])

    const onNext = ({ id }: WizardStep) => {
      if (id) {
        if (typeof id === 'string') {
          const [, orderIndex] = id.split('-');
          id = parseInt(orderIndex);
        }

        setStepIdReached(stepIdReached < id ? id : stepIdReached);
        
      }
    };

    const sendInvitation = ()=>{
      setLoading(true);
      groupsService!.doPost<any>("/group-admin/group/"+props.groupId+"/members/invitation",{...invitationData})
      .then((response: HttpResponse<any>) => {
        if(response.status===200||response.status===204){
          setInvitationResult('success');
          // setGroupMembers(response.data.results);
        }
        else{
          setInvitationResult('error');
        }
        setLoading(false);
      }).catch((err)=>{console.log(err)})
    }
  
    const closeWizard = () => {
      // eslint-disable-next-line no-console
      props.setActive(false);
    };
  
  
   


    const steps = [
        { 
            id: 'incrementallyEnabled-1', 
            name: (Msg.localize('invitationStep1')),
            component: <EnrollmentStep groupId={props.groupId} invitationData={invitationData} setInvitationData={setInvitationData} isStep1Complete={isStep1Complete} setIsStep1Complete={setIsStep1Complete}/>, 
            enableNext: isStep1Complete
        },
        {
          id: 'incrementallyEnabled-2',
          name: (Msg.localize('invitationStep2')),
          component: <EmailStep groupId={props.groupId} invitationData={invitationData} setInvitationData={setInvitationData} isStep2Complete={isStep2Complete} setIsStep2Complete={setIsStep2Complete}/>,
          enableNext: isStep2Complete,
          nextButtonText: 'Send Invitation',
          canJumpTo: stepIdReached >= 2
        }
      ];
  
      const title = Msg.localize('invitationSend');
  
  

  
    return (
      <React.Fragment>
         <Modal
            variant={ModalVariant.medium}
            title={Msg.localize('invitationTitle')}
            isOpen={isModalOpen}
            onClose={()=>{
              props.setActive(false);}}
            actions={[]}
            onEscapePress={()=>{
              if(!(loading&&!invitationResult)){
                props.setActive(false);
              }
            }}
            >
            <ResponseModal invitationResult={invitationResult}  close={()=>{closeWizard(); setInvitationResult("");}}/>
            <Loading active={loading&&!invitationResult}/>
            <Wizard
                navAriaLabel={`${title} steps`}
                mainAriaLabel={`${title} content`}
                onClose={closeWizard}
                steps={steps}
                onNext={onNext}
                height={400}
                onSave={sendInvitation}
            />
        </Modal>



        
        

      </React.Fragment>
    );
  }
  

  const EnrollmentStep: React.FC<any> = (props) => {
    let groupsService = new GroupsServiceClient();
    const toggleRef= useRef<any>(null);
    const [groupEnrollments,setGroupEnrollments] = useState<any>([]);
    const [isOpen,setIsOpen] = useState(false);
    const [selected,setSelected] = useState('');
    const [enrollment,setEnrollment] = useState<any>({});

    useEffect(()=>{
      fetchGroupEnrollments();
    },[]);

    useEffect(()=>{      
      props.setIsStep1Complete(props.invitationData.groupEnrollmentConfiguration?.id&&props.invitationData.groupRoles.length>0)
    },[props.invitationData]);


    useEffect(()=>{
      if(enrollment.id){
        props.invitationData.groupEnrollmentConfiguration = {id:enrollment.id};
        props.invitationData.groupRoles= [];
      }
      else{
        props.invitationData.groupEnrollmentConfiguration = {};
        props.invitationData.groupRoles= [];
      }
      props.setInvitationData({...props.invitationData});
    },[enrollment])


    const onToggle = isOpen => {
        setIsOpen(isOpen);
      };

     const clearSelection = () => {
        setSelected("");
        setIsOpen(false);
      };

    
    

    const onSelect = (event, selection, isPlaceholder) => {
        if (isPlaceholder) clearSelection();
        else {
            setSelected(selection);
            setIsOpen(false);  
            toggleRef.current.focus();
        }
      };

    let fetchGroupEnrollments = ()=>{
        groupsService!.doGet<any>("/group-admin/group/"+props.groupId+"/configuration/all")
        .then((response: HttpResponse<any>) => {
          if(response.status===200&&response.data){
            setGroupEnrollments(response.data);
          }
        })
      }

    let roleHandler = (role)=>{
      if(props.invitationData.groupRoles.includes(role)){
        const index = props.invitationData.groupRoles.indexOf(role);
        if (index > -1) { // only splice array when item is found
          props.invitationData.groupRoles.splice(index, 1); // 2nd parameter means remove one item only
        }
      }
      else{
        props.invitationData.groupRoles.push(role);
      }
      props.setInvitationData({...props.invitationData});
    }

    return(
        <React.Fragment>
             <Select
                variant={SelectVariant.single}
                aria-label="Select Input"
                onToggle={onToggle}
                onSelect={onSelect}
                selections={selected}
                isOpen={isOpen}
                aria-labelledby={"Test"}
                >
                  <SelectOption key="placeholder" value={Msg.localize('invitationEnrollmentSelectPlaceholder')} onClick={()=>{
                    props.setIsStep1Complete(false);
                    setEnrollment({});
                    }} 
                    isPlaceholder
                  />
                  {groupEnrollments?groupEnrollments.map((enrollment,index)=>{
                    return <SelectOption key={index} value={enrollment?.name} isDisabled={!enrollment.active} onClick={()=>{
                      
                      setEnrollment(enrollment)
                      }} />
                  }):[]}
            </Select>
            {enrollment?.id&&
              <React.Fragment>
                <DataList aria-label="Compact data list example" isCompact wrapModifier={"breakWord"}>
                  <DataListItem aria-labelledby="compact-item1">
                      <DataListItemRow>
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell key="primary content">
                              <span id="compact-item1"><strong><Msg msgKey='invitationMemberhipDuration' /></strong></span>
                            </DataListCell>,
                            <DataListCell width={3} key="secondary content ">
                              <span>{enrollment?.membershipExpirationDays?enrollment?.membershipExpirationDays +" "+Msg.localize('Days'):Msg.localize('Permanent')} </span>  
                            </DataListCell>
                          ]}
                        />
                      </DataListItemRow>
                  </DataListItem>
                  <DataListItem aria-labelledby="compact-item2">
                    <DataListItemRow className="gm_role_row">
                      <DataListItemCells
                        dataListCells={[
                            <DataListCell key="primary content">
                                <span id="compact-item1"><strong><Msg msgKey='invitationRoleSelection' /></strong></span>
                            </DataListCell>,
                            <DataListCell width={3} key="roles">
                              <table className="gm_roles-table">
                                <tbody>
                                  {enrollment&&enrollment?.groupRoles?.map((role,index)=>{
                                      return <tr onClick={()=>{roleHandler(role);}}>
                                          <td>
                                              {role}
                                          </td>
                                          <td>
                                            <Checkbox id="standalone-check" name="standlone-check" checked={props.invitationData?.groupRoles.includes(role)} aria-label="Standalone input" />
                                          </td>   
                                      </tr>                    
                                  })}
                                </tbody>
                              </table>
                            </DataListCell>
                          ]}
                      />
                    </DataListItemRow>
                  </DataListItem>                  
              </DataList>
              </React.Fragment>  
            }


        </React.Fragment>
    )
}



const EmailStep: React.FC<any> = (props) => {
    let groupsService = new GroupsServiceClient();

    const [inviteAddress,setInviteAddress] = useState("")
    const [emailError,setEmailError] = useState(true);
    const [showEmailError,setShowEmailError] = useState(false);
    const [isOpen,setIsOpen] = useState(false);
    const [selected,setSelected] = useState<any>(null);
    const [options,setOptions] = useState<any>([])
    const [groupIds,setGroupIds] = useState([]);



    useEffect(()=>{
      fetchGroupAdminIds();
    },[]);


    useEffect(()=>{
      if(groupIds.length>0){
        fetchGroupMembers();
      }
    },[groupIds])


    useEffect(()=>{
        props.setIsStep2Complete(inviteAddress&&!emailError);
    },[inviteAddress,emailError])

    useEffect(()=>{
      props.invitationData.email= inviteAddress;
      props.setInvitationData({...props.invitationData});
    },[inviteAddress])

    const onSelect = (event, selection, isPlaceholder) => {
      if (isPlaceholder) clearSelection();
      else if (!selectUser(selection)&&selection){
        if(ValidateEmail(selection)){
          setInviteAddress(selection);
          
        }
        else{
          setInviteAddress(selection);
          setEmailError(true);
        }
        setShowEmailError(true);
      }
      else {
        setShowEmailError(false);
        setIsOpen(false);
      }
      setSelected(selection);
    };

    const selectUser = (username)=>{
      let email;
      options.forEach(user=>{
        if(user.value===username){
          email=user.description;
        }
      })
      if(email){
        setInviteAddress(email);
      }
      return email;
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
          response.data.results.forEach((user)=>{
            members.push({value:getUserIdentifier(user),description:user.email,id:user.id});
          })
          setOptions(members);
          // setGroupMembers(response.data.results);
        }
      }).catch((err)=>{console.log(err)})
    }



    let getUserIdentifier = (user) => {
      return   (user.firstName || user.lastName?(user.firstName&&user.firstName+" ")+ user.lastName:user.username?user.username:user.email?user.email:user.id?user.id:"Info Not Available")
    }



    return(
        <React.Fragment>
            <strong>Select user or enter new email</strong>
            <div className="gm_invitation-email-input">
              <Select
                variant={SelectVariant.typeahead}
                typeAheadAriaLabel="Select a state"
                onToggle={onToggle}
                onSelect={()=>{}}
                onClear={clearSelection}
                selections={selected}
                createText={Msg.localize('invitationEmailInputTypeahead')}
                onCreateOption={(value)=>{
                  setInviteAddress(value)
                  setEmailError(!ValidateEmail(value));
                  setShowEmailError(true)
                  setSelected(value);
                  setIsOpen(false);
                }}
                onFilter={(e,searchString)=>{
                  setInviteAddress("");
                  setEmailError(false);
                  setShowEmailError(false);
                  let filterOptions :any = [];
                  fetchGroupMembers(searchString);
                  options.forEach((option, index) => (
                    filterOptions.push(
                    <SelectOption
                    isDisabled={option.disabled}
                    key={index}
                    value={option.value}
                    onClick={()=>{
                      setInviteAddress("");
                      if(option.description){
                        setSelected(option.description);
                        setInviteAddress(option.description);
                      }
                      setIsOpen(false);
                    }}

                    {...(option.description && { description: option.description })}
                    />)
                  ))
                  return filterOptions;
                }}
                isOpen={isOpen}
                aria-labelledby={"titleId"}
                isInputValuePersisted={true}
                placeholderText="Start typing a user's name or email address"
                isCreatable={true}
              >
              {options.map((option, index) => (
                  <SelectOption
                  isDisabled={option.disabled}
                  key={index}
                  value={option.value}
                  onClick={()=>{
                    setInviteAddress("");
                    if(option.description){
                      setSelected(option.description);
                      setInviteAddress(option.description);
                    }
                    setIsOpen(false);
                  }}
                  {...(option.description && { description: option.description })}
                  />
              ))}
              </Select>
              {emailError&&showEmailError?<FormAlert>
                <Alert variant="danger" title={!inviteAddress?Msg.localize('invitationEmailRequired'):Msg.localize('invitationEmailError')} aria-live="polite" isInline />
              </FormAlert>:null}
              </div>
            
        </React.Fragment>
    )
}



const ResponseModal: React.FC<any> = (props) => {
  const [isModalOpen,setIsModalOpen] = useState(false);
  useEffect(()=>{    
    setIsModalOpen(!!props.invitationResult)
  },[props.invitationResult])

  return(
    <Modal
      variant={ModalVariant.small}
      title={Msg.localize('Invitation')+" " + (props.invitationResult==='success'?Msg.localize('invitationSuccess'):Msg.localize('invitationFailed'))}
      isOpen={isModalOpen}
      onClose={()=>{props.close()}}
      actions={[         <Button key="confirm" variant="primary" onClick={()=>{props.close()}}>
      OK
    </Button>]}
    ><></></Modal>
  )
}


const Loading: React.FC<any> = (props) => {
  const [isModalOpen,setIsModalOpen] = useState(false);
  useEffect(()=>{    
    setIsModalOpen(props.active)
  },[props.active])

  return(
    <Modal
    variant={ModalVariant.large}
    width="19rem"
    isOpen={isModalOpen}
    header=""
    showClose={false}
    onEscapePress={()=>{}}
    aria-labelledby="modal-custom-header-label"
    aria-describedby="modal-custom-header-description"
    footer=""
  >
    <div tabIndex={0} id="modal-no-header-description" className="gm_loader-modal-container">
      <Spinner isSVG diameter="100px" aria-label="Contents of the custom size example" />
    </div>
  </Modal>
  )
}