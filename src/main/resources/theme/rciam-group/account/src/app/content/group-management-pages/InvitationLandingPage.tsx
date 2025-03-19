import * as React from 'react';
import { FC, useState, useEffect } from 'react';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
import { Button, Checkbox, HelperText, HelperTextItem, Hint, HintBody, Modal, ModalVariant, Tooltip } from '@patternfly/react-core';
import { isPastDate, dateParse } from '../../widgets/Date';

//import { ContentPage } from '../ContentPage';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { Msg } from '../../widgets/Msg';
import { useLoader } from '../../group-widgets/LoaderContext';




export interface InvitationLandingPageProps {
  match: any;
  history: any;
}






// export class GroupPage extends React.Component<GroupsPageProps, GroupsPageState> {
export const InvitationLandingPage: FC<InvitationLandingPageProps> = (props) => {

  let groupsService = new GroupsServiceClient();

  const [invitationId] = useState(props.match.params.invitation_id);
  const [invitationData, setInvitationData] = useState<any>({});
  const [acceptAup, setAcceptAup] = useState(false);
  const [actionBlocked, setActionBlocked] = useState(false);
  const [isParentGroup,setIsParentGroup] = useState(false);
  const { startLoader, stopLoader } = useLoader();

  useEffect(() => {
    getInvitation();
  }, [])



  let getInvitation = () => {
    startLoader();
    groupsService!.doGet<any>("/user/invitation/" + invitationId)
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 && response.data) {
          setInvitationData(response.data);
          // Check if group is a parent group
          if(response.data?.groupEnrollmentConfiguration?.group?.path && response.data.groupEnrollmentConfiguration.group.path.split("/").length ===2){
            setIsParentGroup(true);
          } 
          
        }

      }).catch((err) => {
        console.log(err);
        stopLoader();
      })
  }


  const acceptInvitation = () => {
    startLoader();
    groupsService!.doPost<any>("/user/invitation/" + invitationId + "/accept", {})
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          if (invitationData?.forMember) {
            props.history.push('/groups/showgroups');
          }
          else {
            props.history.push('/groups/admingroups')
          }
        }
        else {
          setActionBlocked(true);
        }
      }).catch((err) => {
        setActionBlocked(true);
        stopLoader();
      })
  }

  const rejectInvitation = () => {
    startLoader();
    groupsService!.doPost<any>("/user/invitation/" + invitationId + "/reject", {})
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          if (invitationData?.forMember) {
            props.history.push('/groups/showgroups');
          }
          else {
            props.history.push('/groups/admingroups')
          }
        }
        else {
          setActionBlocked(true);
        }
      }).catch((err) => {
        setActionBlocked(true);

        stopLoader();
      })
  }


  return (

    <>
      <div className="gm_invitation-container">

        <ResponseModal type={invitationData?.forMember} close={() => {
          setActionBlocked(false);
          if (invitationData?.forMember) {
            props.history.push('/groups/showgroups');
          }
          else {
            props.history.push('/groups/admingroups')
          }
        }} active={actionBlocked} />
        {Object.keys(invitationData).length > 0 ?
          <>
            <span className="gm_invitation-landing-title"><Msg msgKey='invitationGreetings' /> {invitationData?.groupEnrollmentConfiguration?.group?.name || invitationData?.group?.name}</span>
            <div className="gm_invitation-content-container">
              {(invitationData?.groupEnrollmentConfiguration?.group?.attributes?.description || invitationData?.group?.attributes?.description) && <div className="gm_invitation-purpuse">
                <h1><Msg msgKey='Description' /></h1>
                {invitationData?.groupEnrollmentConfiguration?.group?.attributes?.description[0] || invitationData?.group?.attributes?.description[0]}
              </div>}
              <div className="gm_invitation-purpuse">
                <h1><Msg msgKey='groupPath' /></h1>
                {invitationData?.groupEnrollmentConfiguration?.group?.path || "Group/Path/test"}
              </div>
              <Hint>
                <HintBody>
                  <Msg msgKey='invitationMessage' />{invitationData?.forMember ? invitationData?.groupRoles.map((role, index) => { return <strong> {role}{index !== invitationData.groupRoles.length - 1 && ','}</strong> }) : ' admin'}.
                </HintBody>
              </Hint>
              {invitationData?.forMember &&
                <>
                  <HelperText>
                    <HelperTextItem variant="warning" hasIcon>
                      <p>
                        {
                          invitationData?.groupEnrollmentConfiguration?.membershipExpirationDays ?
                            <React.Fragment>
                              <div dangerouslySetInnerHTML={{ 
                                __html: 
                                  (invitationData?.groupEnrollmentConfiguration?.validFrom?
                                    Msg.localize("invitationExpirationMeddageValidFrom", [invitationData.groupEnrollmentConfiguration.validFrom,JSON.stringify(invitationData?.groupEnrollmentConfiguration?.membershipExpirationDays)])
                                    :Msg.localize("invitationExpirationMessage", [JSON.stringify(invitationData?.groupEnrollmentConfiguration?.membershipExpirationDays)])) 
                                  }} /></React.Fragment>
                            :
                            
                            (invitationData?.groupEnrollmentConfiguration?.validFrom ?
                              <div dangerouslySetInnerHTML={{ 
                                __html: Msg.localize("invitationExpirationMeddageInfiniteValidFrom", [invitationData.groupEnrollmentConfiguration.validFrom])
                              }}/>
                              :
                              <Msg msgKey='invitationExpirationMessageInfinite' />
                            )
                        }
                      </p></HelperTextItem>
                  </HelperText>
                  {!isParentGroup &&
                    <HelperText>
                      <HelperTextItem variant="warning" hasIcon>
                        <p>
                          <Msg msgKey='invitationExpirationInfo' /> <a onClick={() => { props.history.push('/groups/showgroups'); }}>My Groups</a> page.
                        </p>
                      </HelperTextItem>
                    </HelperText>            
                  }
                </>
              }

              {invitationData?.groupEnrollmentConfiguration?.aup?.url ?
                <>
                  <p>
                    <Msg msgKey='invitationAUPMessage1' /> <a href={invitationData?.groupEnrollmentConfiguration?.aup?.url} target="_blank" rel="noreferrer"><Msg msgKey='invitationAUPMessage2' /></a> <Msg msgKey='invitationAUPMessage3' />
                  </p>
                  <div className="gm_checkbox-container">
                    <Checkbox
                      onClick={() => { setAcceptAup(!acceptAup) }}
                      checked={acceptAup}
                      id="description-check-1"
                      label="I have read the terms and accept them"
                    />
                  </div>
                </>
                : ""}

              <div className="gm_invitation-response-container">
                <Tooltip  {...(!(invitationData?.groupEnrollmentConfiguration?.aup?.url && !acceptAup) ? { trigger: 'manual', isVisible: false } : { trigger: 'mouseenter' })} content={<div><Msg msgKey='invitationAUPErrorMessage' /></div>}>
                  <div className="gm_invitation-response-button-container">
                    <Button isDisabled={invitationData?.groupEnrollmentConfiguration?.aup?.url && !acceptAup} onClick={acceptInvitation}><Msg msgKey='Accept' /></Button>
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
  const [isModalOpen, setIsModalOpen] = useState(false);
  useEffect(() => {
    setIsModalOpen(!!props.active)
  }, [props.active])

  return (
    <Modal
      variant={ModalVariant.small}
      title={Msg.localize('invitationErrorResponseTitle')}
      isOpen={isModalOpen}

      onClose={() => { props.close() }}
      actions={[<Button key="confirm" variant="primary" onClick={() => { props.close() }}>
        <Msg msgKey='OK' />
      </Button>]}
    ><>
        <Msg msgKey='invitationErrorResponseMessage' />{props.type ? " member" : " admin"}.
      </></Modal>
  )
}