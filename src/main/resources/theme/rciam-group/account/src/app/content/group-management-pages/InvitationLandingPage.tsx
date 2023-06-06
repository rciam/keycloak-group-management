import * as React from 'react';
import {FC,useState,useEffect} from 'react';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
import { Button, Checkbox, DataList, DataListCell, DataListItem, DataListItemCells, DataListItemRow, HelperText, HelperTextItem, Hint, HintBody, Modal, ModalVariant, Tooltip } from '@patternfly/react-core';
import { Loading } from '../../group-widgets/LoadingModal';
//import { ContentPage } from '../ContentPage';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';




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
          props.history.push('/groups/showgroups');
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
          props.history.push('/groups/showgroups');

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
          props.history.push('/groups/showgroups');
        }} active={actionBlocked}/>
        {Object.keys(invitationData).length  >0 ?
          <>
            <span className="gm_invitation-landing-title">Welcome to {invitationData?.groupEnrollmentConfiguration?.group?.name||invitationData?.group?.name}</span>
            <div className="gm_invitation-content-container">
              <Hint>
                <HintBody>
                  You have been invited to join as a{invitationData?.forMember?invitationData?.groupRoles.map((role,index)=>{return <strong> {role}{index !== invitationData.groupRoles.length - 1&&','}</strong>}):'n admin'}.
                </HintBody>
              </Hint>
              {(invitationData?.groupEnrollmentConfiguration?.group?.attributes?.description||invitationData?.group?.attributes?.description)&&<div className="gm_invitation-purpuse">
                <h1>Description</h1>
                {invitationData?.groupEnrollmentConfiguration?.group?.attributes?.description[0]||invitationData?.group?.attributes?.description[0]}
              </div>}
              {invitationData?.forMember&&invitationData?.groupEnrollmentConfiguration?.membershipExpirationDays&&
                    <HelperText>
                    <HelperTextItem variant="warning" hasIcon>
                      <p>This membership expires in <strong>{invitationData?.groupEnrollmentConfiguration?.membershipExpirationDays} days</strong> after enrollment.</p></HelperTextItem>
                    </HelperText>
              }
              {invitationData?.groupEnrollmentConfiguration?.aup?.url?
                <>
                  <p>
                    Before joining, you must review the <a href={invitationData?.groupEnrollmentConfiguration?.aup?.url} target="_blank" rel="noreferrer">acceptable use policy (AUP)</a> and accept it.
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
                <Tooltip  {...(!(invitationData?.groupEnrollmentConfiguration?.aup?.url&&!acceptAup) ? { trigger:'manual', isVisible:false }:{trigger:'mouseenter'})} content={<div>First accept the terms and conditions</div>}>
                  <div className="gm_invitation-response-button-container">
                    <Button isDisabled={invitationData?.groupEnrollmentConfiguration?.aup?.url&&!acceptAup} onClick={acceptInvitation}>Accept</Button>
                  </div>
                </Tooltip>
                <Button variant="danger" onClick={rejectInvitation}>Reject</Button>
              </div>
            </div>
          </>
          :
          <span className="gm_invitation-landing-title">Invitation Could not be found</span>
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
      title={"Invitation Could not be accepted"}
      isOpen={isModalOpen}
      
      onClose={()=>{props.close()}}
      actions={[         <Button key="confirm" variant="primary" onClick={()=>{props.close()}}>
      OK
    </Button>]}
    ><>
      Please make sure you are not already a group{props.type?" member":" admin"}.
    </></Modal>
  )
}