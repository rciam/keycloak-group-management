import * as React from 'react';
import {FC,useState,useEffect} from 'react';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
import { Button, Checkbox, HelperText, HelperTextItem, Hint, HintBody, Modal, ModalVariant, Tooltip } from '@patternfly/react-core';
import { Loading } from '../../group-widgets/LoadingModal';
//import { ContentPage } from '../ContentPage';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { Msg } from '../../widgets/Msg';




export interface InvitationLandingPageProps {
  match:any;
  history:any;
}






// export class GroupPage extends React.Component<GroupsPageProps, GroupsPageState> {
export const InvitationLandingPage: FC<InvitationLandingPageProps> = (props)=> {

  let groupsService = new GroupsServiceClient();

  const [invitationId] = useState(props.match.params.invitation_id);
  const [invitationData,setInvitationData] =  useState<any>({});
  const [loading,setLoading] = useState(false);
  const [acceptAup,setAcceptAup] = useState(false);
  const [actionBlocked, setActionBlocked] = useState(false);


  useEffect(()=>{
    getInvitation();    
  },[])


  
  let getInvitation = () =>{
    setLoading(true);
    groupsService!.doGet<any>("/user/invitation/"+invitationId)
      .then((response: HttpResponse<any>) => {
        setLoading(false);
        if(response.status===200&&response.data){
          setInvitationData(response.data);
        }
        else{
          
        }
        
    }).catch((err)=>{
      console.log(err);
      setLoading(false);
    })
  }


  const acceptInvitation = () =>{
    setLoading(true);
    groupsService!.doPost<any>("/user/invitation/"+invitationId+ "/accept",{})
      .then((response: HttpResponse<any>) => {
        setLoading(false);
        if(response.status===200||response.status===204){
          if(invitationData?.forMember){
            props.history.push('/groups/showgroups');
          }
          else{
            props.history.push('/groups/admingroups')
          }
        }
        else{
          setActionBlocked(true);          
        }
    }).catch((err)=>{
      setActionBlocked(true);          
      setLoading(false);
    })
  }

  const rejectInvitation = () =>{
    setLoading(true);
    groupsService!.doPost<any>("/user/invitation/"+invitationId+ "/reject",{})
      .then((response: HttpResponse<any>) => {
        setLoading(false);
        if(response.status===200||response.status===204){
          if(invitationData?.forMember){
            props.history.push('/groups/showgroups');
          }
          else{
            props.history.push('/groups/admingroups')
          }
        }
        else{ 
          setActionBlocked(true);
        }
    }).catch((err)=>{
      setActionBlocked(true);          

      setLoading(false);
    })
  }

  
  return (

    <>
      <div className="gm_invitation-container">

        <Loading active={loading}/>
        <ResponseModal type={invitationData?.forMember} close={()=>{
          setActionBlocked(false);
          if(invitationData?.forMember){
            props.history.push('/groups/showgroups');
          }
          else{
            props.history.push('/groups/admingroups')
          }
        }} active={actionBlocked}/>
        {Object.keys(invitationData).length  >0 ?
          <>
            <span className="gm_invitation-landing-title"><Msg msgKey='invitationGreetings' /> {invitationData?.groupEnrollmentConfiguration?.group?.name||invitationData?.group?.name}</span>
            <div className="gm_invitation-content-container">
              <Hint>
                <HintBody>
                <Msg msgKey='invitationMessage' />{invitationData?.forMember?invitationData?.groupRoles.map((role,index)=>{return <strong> {role}{index !== invitationData.groupRoles.length - 1&&','}</strong>}):' admin'}.
                </HintBody>
              </Hint>
              {(invitationData?.groupEnrollmentConfiguration?.group?.attributes?.description||invitationData?.group?.attributes?.description)&&<div className="gm_invitation-purpuse">
                <h1><Msg msgKey='Description' /></h1>
                {invitationData?.groupEnrollmentConfiguration?.group?.attributes?.description[0]||invitationData?.group?.attributes?.description[0]}
              </div>}
              {invitationData?.forMember &&
                <HelperText>
                  <HelperTextItem variant="warning" hasIcon>
                    <p>{invitationData?.groupEnrollmentConfiguration?.membershipExpirationDays?<React.Fragment><Msg msgKey='invitationExpirationMessage1' /> <strong>{invitationData?.groupEnrollmentConfiguration?.membershipExpirationDays} <Msg msgKey='days' /></strong> <Msg msgKey='invitationExpirationMessage2' /></React.Fragment>:<Msg msgKey='invitationExpirationMessageInfinite'/>}</p></HelperTextItem>
                </HelperText>
              }
        
              {invitationData?.groupEnrollmentConfiguration?.aup?.url?
                <>
                  <p>
                  <Msg msgKey='invitationAUPMessage1' /> <a href={invitationData?.groupEnrollmentConfiguration?.aup?.url} target="_blank" rel="noreferrer"><Msg msgKey='invitationAUPMessage2' /></a> <Msg msgKey='invitationAUPMessage3' />
                  </p>
                  <div className="gm_checkbox-container">
                    <Checkbox
                      onClick={()=>{setAcceptAup(!acceptAup)}}
                      checked={acceptAup}
                      id="description-check-1"
                      label="I have read the terms and accept them"
                    />
                  </div>
                </> 
              :""}
        
              <div className="gm_invitation-response-container">
                <Tooltip  {...(!(invitationData?.groupEnrollmentConfiguration?.aup?.url&&!acceptAup) ? { trigger:'manual', isVisible:false }:{trigger:'mouseenter'})} content={<div><Msg msgKey='invitationAUPErrorMessage' /></div>}>
                  <div className="gm_invitation-response-button-container">
                    <Button isDisabled={invitationData?.groupEnrollmentConfiguration?.aup?.url&&!acceptAup} onClick={acceptInvitation}><Msg msgKey='Accept' /></Button>
                  </div>
                </Tooltip>
                <Button variant="danger" onClick={rejectInvitation}><Msg msgKey='Reject' /></Button>
              </div>
            </div>
          </>
          :
          <span className="gm_invitation-landing-title"><Msg msgKey='invitationNotFound' /></span>
        }
      
      </div>

    </>  
  )
};


const ResponseModal: React.FC<any> = (props) => {
  const [isModalOpen,setIsModalOpen] = useState(false);
  useEffect(()=>{    
    setIsModalOpen(!!props.active)
  },[props.active])

  return(
    <Modal
      variant={ModalVariant.small}
      title={Msg.localize('invitationErrorResponseTitle')}
      isOpen={isModalOpen}
      
      onClose={()=>{props.close()}}
      actions={[         <Button key="confirm" variant="primary" onClick={()=>{props.close()}}>
      <Msg msgKey='OK' />
    </Button>]}
    ><>
      <Msg msgKey='invitationErrorResponseMessage' />{props.type?" member":" admin"}.
    </></Modal>
  )
}