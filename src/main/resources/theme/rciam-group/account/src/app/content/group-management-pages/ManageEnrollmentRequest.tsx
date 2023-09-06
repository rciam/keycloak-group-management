import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, Tooltip, DataListAction, SelectVariant, Checkbox,Select,SelectOption, FormAlert, Alert, Form, FormGroup, TextInput, Modal, ModalVariant, Switch, FormFieldGroupHeader, FormFieldGroup, DatePicker, Popover, NumberInput, HelperTextItem, TextArea, Badge} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import {isIntegerOrNumericString,getCurrentDate} from '../../js/utils.js'
import { Loading } from '../../group-widgets/LoadingModal';
import { Msg } from '../../widgets/Msg';
import { CopyIcon, ExternalLinkSquareAltIcon, HelpIcon, ShareSquareIcon } from '@patternfly/react-icons';


const reg_url = /^(https?|chrome):\/\/[^\s$.?#].[^\s]*$/

export const EnrollmentRequest: FC<any> = (props) => {
    
    const [modalInfo,setModalInfo] = useState({});
    const [loading,setLoading] = useState(false); 
    const [enrollmentRequest,setEnrollmentRequest] = useState<any>({});
    const [copyTooltip,setCopyTooltip] = useState(false);
    const [isModalOpen,setIsModalOpen] = useState<boolean>(false)
    let groupsService = new GroupsServiceClient();
    const [reviewerComment,setReviewerComment] = useState("")

    useEffect(()=>{
      if(Object.keys(props.enrollmentRequest).length !== 0) {
            
            setIsModalOpen(true);
            setEnrollmentRequest({...props.enrollmentRequest});
        }
        else{
            setIsModalOpen(false);
            setEnrollmentRequest({});
        }
    },[props.enrollmentRequest]);

    const disapearingTooltip = () => {
      setCopyTooltip(true);
      setTimeout(() => {
        setCopyTooltip(false);
      }, 2000);
      
    }



    let reviewEnrollmentRequest = (action)=>{
      setLoading(true);
      groupsService!.doPost<any>("/group-admin/enroll-request/"+ enrollmentRequest.id +"/" + action,{},{params:{...(reviewerComment?{adminJustification:reviewerComment}:{})}})
      .then((response: HttpResponse<any>) => {
        setLoading(false);
        if(response.status===200||response.status===204){
          props.close();
        }
      }).catch((err)=>{console.log(err)})
     
    }
   
    return (
      <React.Fragment>
        
        <Modal
                variant={ModalVariant.large}
                header={
                  <React.Fragment >
                    <h1 className="pf-c-modal-box__title gm_flex-center">
                      {enrollmentRequest?.status==='PENDING_APPROVAL'?<Msg msgKey='reviewRequestTitle'/>:<Msg msgKey='viewRequestTitle'/>}
                      <Tooltip {...(!!(copyTooltip) ? { trigger:'manual', isVisible:true }:{trigger:'mouseenter'})}
                            content={
                                <div>
                                    {copyTooltip?<Msg msgKey='copiedTooltip'/>:<Msg msgKey='copyTooltip'/>}
                                </div>
                            }
                        >  
                        <Button isSmall className={'gm_grey-button gm_title-button'} onClick={()=>{
                        disapearingTooltip();
                        let link = groupsService.getBaseUrl() + '/account/#/groups/groupenrollments?id=' + encodeURI(enrollmentRequest?.id);
                        navigator.clipboard.writeText(link)}} ><CopyIcon/> </Button>
                      </Tooltip>              
                    </h1>
                    
                  </React.Fragment>
                }
                isOpen={isModalOpen}
                onClose={()=>{props.close()}}
                actions={[
                  ...(enrollmentRequest?.status==='PENDING_APPROVAL'?
                    [<Tooltip 
                        content={
                            <div>
                                <Msg msgKey={'approveRequestTooltip'}/>
                            </div>
                        }
                    >
                    <div>
                        <Button key="confirm"  className="gm_green-button gm_button-spacer"  onClick={()=>{
                          reviewEnrollmentRequest('accept'); 
                            }}>
                            <Msg msgKey={"Approve"} />
                        </Button>
                    </div>

                    </Tooltip>
                    ,
                    
                    <Button key="cancel" variant="danger" onClick={()=>{
                      reviewEnrollmentRequest('reject');
                    }}>
                        <Msg msgKey='Deny' />
                    </Button>
                    ]:[
                      <Button key="back" className="gm_grey-button" variant="primary" onClick={()=>
                        props.close()
                        }
                      >
                        <Msg msgKey="Back"/>
                      </Button>
                    ]
                  ) 
                ]}
                >
                  <Loading active={loading}/>

                  <Alert variant={enrollmentRequest?.status==='ACCEPTED'?"success":enrollmentRequest?.status==='REJECTED'?"danger":"info"} title={
                    <React.Fragment>
                      <p><Msg msgKey='reviewAlertSubmitted'/><span className="gm_normal-text">{enrollmentRequest?.submittedDate}</span></p>
                      {enrollmentRequest?.approvedDate&&
                        <p className="gm_margin-top-1rem"><Msg msgKey={enrollmentRequest?.status}/>: <span className="gm_normal-text">{enrollmentRequest?.approvedDate}</span></p>
                      }
                      {!enrollmentRequest?.approvedDate&&<p className="gm_margin-top-1rem"><Msg msgKey='reviewAlertStatus'/> <span className="gm_normal-text"><Msg msgKey={enrollmentRequest?.status}/></span></p>}
                      {enrollmentRequest?.checkAdmin && <p className="gm_margin-top-1rem"><Msg msgKey='reviewAlertReviewer'/><span className="gm_normal-text">{enrollmentRequest?.checkAdmin?.firstName + " " + enrollmentRequest?.checkAdmin?.lastName + " (" + enrollmentRequest?.checkAdmin?.email +")"}</span></p> }
                      {enrollmentRequest?.adminJustification &&<p className="gm_margin-top-1rem"><Msg msgKey='reviewAlertComment'/> <span className="gm_normal-text">{enrollmentRequest?.adminJustification}</span></p>}
                    </React.Fragment>
                  } />                  
                    <Form className="gm_enrollment-request-view-form" isHorizontal>
                      <FormFieldGroup
                          header={
                          <FormFieldGroupHeader
                              titleText={{ text: "User Details", id: 'field-group4-non-expandable-titleText-id' }}
                          />
                          }                          
                        >
                          <FormGroup
                              label={Msg.localize('enrollmentFullNameLabel')}
                              fieldId="simple-form-name-01" 
                              // helperText=""
                          >
                            <div>
                              {enrollmentRequest?.user?.firstName + " " + enrollmentRequest?.user?.lastName}
                            </div>
                          </FormGroup>
                          <FormGroup
                            label={Msg.localize('enrollmentEmailLabel')}
                            fieldId="simple-form-name-02"
                          >
                            <div>{enrollmentRequest?.user?.email}</div>
                          </FormGroup>
                          <FormGroup
                            label={Msg.localize('enrollmentAssuranceLabel')}
                            fieldId="simple-form-name-03"
                          >
                            <div>
                              {enrollmentRequest?.user?.attributes?.eduPersonAssurance && Array.isArray(enrollmentRequest?.user?.attributes?.eduPersonAssurance) ?
                                  enrollmentRequest?.user?.attributes?.eduPersonAssurance.map((value,index)=>{
                                    return <Badge key={index} className="gm_role_badge" isRead>{value}</Badge>
                                  })
                                : enrollmentRequest?.user?.attributes?.eduPersonAssurance?
                                  <Badge key={'single'} className="gm_role_badge" isRead>{enrollmentRequest?.user?.attributes?.eduPersonAssurance}</Badge>
                                :<Msg msgKey='notAvailable'/>
                              }
                            </div>
                          </FormGroup>
                          <FormGroup
                            label={Msg.localize('enrollmentIdentityProvidersLabel')}
                            fieldId="simple-form-name-04"
                          >
                            <div>{enrollmentRequest?.user?.federatedIdentities &&enrollmentRequest?.user?.federatedIdentities.length>0 ? enrollmentRequest?.user?.federatedIdentities.map((federatedIdentity,index)=>{
                                return <Badge key={'single'} className="gm_role_badge" isRead>{federatedIdentity.identityProvider}</Badge>
                              }):
                                <Msg msgKey='none'/>}</div>
                          </FormGroup>
                        </FormFieldGroup>
                        <FormFieldGroup
                          header={
                          <FormFieldGroupHeader
                              titleText={{ text: Msg.localize('enrollmentMembershipTitle'), id: 'field-group4-non-expandable-titleText-id' }}
                          />
                          }                          
                        >
                          <FormGroup
                            label={Msg.localize('enrollmentGroupNameLabel')}
                            fieldId="simple-form-name-05"
                          >
                            <div>{enrollmentRequest?.groupEnrollmentConfiguration?.group?.name}</div>
                          </FormGroup>
                          <FormGroup
                            label={Msg.localize('enrollmentEnrollmentNameLabel')}
                            fieldId="simple-form-name-06"
                          >
                            <div>{enrollmentRequest?.groupEnrollmentConfiguration?.name}</div>
                          </FormGroup>
                          <FormGroup
                            label={Msg.localize('enrollmentGroupRolesLabel')}
                            fieldId="simple-form-name-07"
                          >
                            {enrollmentRequest?.groupEnrollmentConfiguration?.groupRoles.map((role,index)=>{
                              return <Badge key={index} className="gm_role_badge" isRead>{role}</Badge>
                            })}
                          </FormGroup>
                          <FormGroup
                            label={Msg.localize('enrollmentAUPLabel')}
                            fieldId="simple-form-name-08"
                            >
                            <div>{enrollmentRequest?.groupEnrollmentConfiguration?.aup?.url?
                              <Button variant="link" className="gm_button-enrollment-link" icon={<ExternalLinkSquareAltIcon />} iconPosition="right">
                                <a href={enrollmentRequest?.groupEnrollmentConfiguration?.aup?.url} target="_blank" rel="noreferrer">link</a>
                              </Button>
                            :<Msg msgKey='notAvailable'/>} </div>
                          </FormGroup>
                          <FormGroup
                            label={Msg.localize('enrollmentExpirationLabel')}
                            fieldId="simple-form-name-09"
                          >
                            <div>{enrollmentRequest?.groupEnrollmentConfiguration?.membershipExpirationDays}</div>
                          </FormGroup>
                          {
                            enrollmentRequest?.groupEnrollmentConfiguration?.commentsNeeded &&
                            <FormGroup
                              label={(enrollmentRequest?.groupEnrollmentConfiguration?.commentsLabel  || Msg.localize('enrollmentUserCommentLabel')) + ":" }
                              fieldId="simple-form-name-10"
                            >
                              <div>{enrollmentRequest?.comments}</div>
                            </FormGroup>
                          }
                        </FormFieldGroup>
                        {enrollmentRequest?.status==='PENDING_APPROVAL'&&
                          <FormFieldGroup
                            header={
                            <FormFieldGroupHeader
                                titleText={{ text: Msg.localize('enrollmentReviewResponseLabel'), id: 'field-group4-non-expandable-titleText-id' }}
                            />
                            }                          
                          >
                            <FormGroup
                              label={Msg.localize('enrollmentUserCommentLabel')}
                              fieldId="simple-form-name-11"
                            >
                              <TextArea
                                  className="gm_form-input"
                                  type="text"
                                  id="simple-form-name-12"
                                  name="simple-form-name-12"
                                  aria-describedby="simple-form-name-01-helper"
                                  value={reviewerComment}
                                  onChange={(value)=>{setReviewerComment(value); }}
                                />
                            </FormGroup>
                          </FormFieldGroup>
                        }
                    </Form>
        </Modal>
        
        </React.Fragment>         
   
    )
}

