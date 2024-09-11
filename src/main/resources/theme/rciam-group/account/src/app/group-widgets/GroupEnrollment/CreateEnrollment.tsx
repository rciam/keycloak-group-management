import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import {  Button, Tooltip, SelectVariant, Checkbox,Select,SelectOption, Alert, Form, FormGroup, Breadcrumb, BreadcrumbItem, TextArea} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import {dateParse,formatDateToString,isFutureDate} from '../../js/utils.js'
import { Loading } from '../LoadingModal';
import { Msg } from '../../widgets/Msg';
// @ts-ignore
import { ContentPage } from '../../content/ContentPage';
import { GroupRolesTable } from '../GroupRolesTable';
import { Link } from 'react-router-dom';


const reg_url = /^(https?|chrome):\/\/[^\s$.?#].[^\s]*$/

export const CreateEnrollment: FC<any> = (props) => {
    const touchDefault = {
      comments:false,
      groupRoles:false
    };
    const [errors,setErrors] = useState<any>({});
    const [loading,setLoading] = useState(false)
    const [modalInfo,setModalInfo] = useState({});
    const [touched,setTouched] = useState<any>(touchDefault);
    const [enrollments,setEnrollments] = useState<any>([]);
    const [selected,setSelected]= useState('');
    const [group,setGroup] = useState<any>({});
    const [isOpen,setIsOpen] = useState(false);
    const [enrollment,setEnrollment] = useState<any>({});
    const [acceptAup,setAcceptAup] = useState(false);
    const [openRequest,setOpenRequest] = useState(false);
    const [defaultId,setDefaultId] = useState("");
    const [enrollmentRequest,setEnrollmentRequest] = useState({
      groupEnrollmentConfiguration: {id:''},
      groupRoles:[],
      comments:""
    });

    let groupsService = new GroupsServiceClient();
    useEffect(()=>{   
      if(props.location.search){
        const query = new URLSearchParams(props.location.search);
        let groupPath = decodeURI(query.get('groupPath')||"");
        let id = decodeURI(query.get('id')||"");

        if(id){
          fetchGroupEnrollment(id);
        }
        else if(groupPath){
          fetchGroupEnrollments(groupPath);
        }
       
      }
    },[]);


    useEffect(()=>{
      if(group.name){
        fetchGroupEnrollmentRequests();
      }      
    },[group])

    useEffect(()=>{
      if(enrollments.length>0&&defaultId){
        enrollments.forEach(enrollment=>{
          if(enrollment.id===defaultId){
            setSelected(enrollment.name);
            enrollmentRequest.groupEnrollmentConfiguration.id=enrollment.id
            setEnrollmentRequest({...enrollmentRequest});
            setEnrollment(enrollment);
          }
        })
      }
    },[enrollments,defaultId])


    let fetchGroupEnrollment = (id)=>{
      groupsService!.doGet<any>("/user/configuration/"+id)
      .then((response: HttpResponse<any>) => {
        if(response.status===200&&response.data){
            setGroup(response.data.group);
            setEnrollments([response.data]);
        }
      })
  }


    let fetchGroupEnrollments = (groupPath)=>{
        groupsService!.doGet<any>("/user/groups/configurations",{params:{groupPath:groupPath}})
        .then((response: HttpResponse<any>) => {
          if(response.status===200&&response.data){
            if(response.data.length>0){
              if(response.data[0].group?.attributes?.defaultConfiguration){
                setDefaultId(response.data[0].group?.attributes?.defaultConfiguration[0]);  
              }
              setGroup(response.data[0].group);
            }          
            setEnrollments(response.data);
          }
        })
    }



    let fetchGroupEnrollmentRequests = ()=>{
      groupsService!.doGet<any>("/user/enroll-requests",{params:{groupId:group.id}})
      .then((response: HttpResponse<any>) => {
        if(response.status===200&&response.data){
          let requests = response.data.results;
          let openRequest = false;
          requests.forEach(request=>{
            if(request.status==='PENDING_APPROVAL'||request.status==='WAITING_FOR_REPLY'){
              openRequest= true;              
            }

          })
          setOpenRequest(openRequest);
        }
      })
    }

    useEffect(()=>{
      if(enrollments.length===1){
        enrollmentRequest.groupEnrollmentConfiguration.id=enrollments[0].id
        setEnrollmentRequest({...enrollmentRequest});
        setEnrollment(enrollments[0]);
      }
    },[enrollments])

    useEffect(()=>{
      validateEnrollmentRequest();
    },[enrollmentRequest])

    const createEnrollmentRequest = (requiresApproval) => {
      setLoading(true);
      groupsService!.doPost<any>("/user/enroll-request",{...enrollmentRequest})
      .then((response: HttpResponse<any>) => {
        setLoading(false);
        if(response.status===200||response.status===204){
          if(requiresApproval){
            props.history.push('/groups/mygroupenrollments');
          }
          else{
            props.history.push('/groups/showgroups');
          }
        }
      }).catch((err)=>{
        setLoading(false);
        console.log(err)})
    }


    const validateEnrollmentRequest = () => {
        let errors: Record<string, string> = {};
        (!enrollmentRequest?.comments&&enrollment.commentsNeeded) && (errors.comments = Msg.localize('requredFormError'));
        !(enrollmentRequest?.groupRoles?.length>0) && (errors.groupRoles=Msg.localize('groupRolesFormError'));
        (!enrollment.multiselectRoles&&enrollmentRequest.groupRoles.length>1) && (errors.groupRoles=Msg.localize('groupRolesFormErrorMulitple'));
        setErrors(errors);
        //!(enrollemtn?)
    }

    const touchFields = ()=> {
        for (const property in touched){
            touched[property]= true;
        }            
        setTouched({...touched});
    }
      

    const onToggle = (open) => {
      setIsOpen(open);
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
      }
    };


    return (
      <React.Fragment>
        <div className="gm_content">
          <Breadcrumb className="gm_breadcumb">
            <BreadcrumbItem to="#">
              <Msg msgKey='accountConsole' />
            </BreadcrumbItem>
            <BreadcrumbItem to="#/groups/showgroups">
              <Msg msgKey='groupLabel' />
              </BreadcrumbItem>
            <BreadcrumbItem isActive>
              {group?.name}
            </BreadcrumbItem>
          </Breadcrumb>
          <ConfirmationModal modalInfo={modalInfo}/>
          <Loading active={loading}/>
          <ContentPage title={group?.name||""}>
            <p className="gm_group_desc">
              {(group?.attributes?.description&&group?.attributes?.description[0])||Msg.localize('noDescription')}
            </p>
            <div className="gm_enrollment_container">
            <Form> 
              {!openRequest?
                <React.Fragment>              
                  {enrollments&&enrollments.length>0?
                    <React.Fragment>
                      <FormGroup
                      label= {Msg.localize('Group Enrollment')}
                      isRequired
                      fieldId="simple-form-name-01"
                      >
                        <Select
                          variant={SelectVariant.single}
                          aria-label="Select Input"
                          className="gm_form-input"
                          onToggle={onToggle}
                          onSelect={onSelect}
                          isDisabled={enrollments.length===1}
                          selections={selected}
                          isOpen={isOpen}
                          aria-labelledby={"Test"}
                        >
                          <SelectOption {...(enrollments.length!==1&& {...{key:"placeholder",isPlaceholder:true}})} value={Msg.localize('invitationEnrollmentSelectPlaceholder')} onClick={()=>{
                            setEnrollment({});
                            }} 
                            
                          />
                            {enrollments.map((enrollment,index)=>{
                              return <SelectOption {...(enrollments.length===1&& {...{key:"placeholder",isPlaceholder:true}})} key={index} value={enrollment?.name} isDisabled={!enrollment.active} onClick={()=>{
                                enrollmentRequest.groupEnrollmentConfiguration.id=enrollment.id
                                setEnrollmentRequest({...enrollmentRequest});
                                setEnrollment(enrollment);
                                }} />
                            })}
                      </Select>
                    </FormGroup>
                </React.Fragment>
                  :
                  <Alert  className='gm_content-width' variant="warning" title="This group has no available enrollments" />  
              }

        
        
        {Object.keys(enrollment).length !== 0?
          <React.Fragment>
            <Alert variant="warning" className='gm_content-width' title={
              ("The membership ")+ 
              (enrollment.validFrom &&isFutureDate(dateParse(enrollment.validFrom))? "will take effect at " +formatDateToString(dateParse(enrollment.validFrom)):"")+
              (enrollment.validFrom &&isFutureDate(dateParse(enrollment.validFrom))&&parseInt(enrollment.membershipExpirationDays)>0 ?" and it ": "")+
              (parseInt(enrollment.membershipExpirationDays)>0? "will expire on " + enrollment.membershipExpirationDays+ " days after activation":" does not have an expiration date.")} 
            />  

            
            {enrollment.commentsNeeded&&
              <FormGroup
              label= {enrollment.commentsLabel}
              isRequired
              
              fieldId="simple-form-name-01"
              helperTextInvalid={touched.comments&&errors.comments}
              validated={errors.comments&&touched.comments?'error':'default'}
              >
                <TextArea
                  className="gm_form-input"
                  isRequired
                  type="text"
                  id="simple-form-name-01"
                  onBlur={()=>{touched.comments=true; setTouched({...touched});}}
                  name="simple-form-name-01"
                  aria-describedby="simple-form-name-01-helper"
                  value={enrollmentRequest.comments}
                  validated={errors.comments&&touched.comments?'error':'default'}
                  onChange={(value)=>{enrollmentRequest.comments=value; setEnrollmentRequest({...enrollmentRequest}); }}
                />
                 <div className="gm_description-text">{enrollment.commentsDescription}</div>
              </FormGroup>  
            }

            <FormGroup
              label= {Msg.localize('Select Your Group Role')}
              isRequired
              fieldId="simple-form-name-01"
              helperTextInvalid={touched.groupRoles&&errors.groupRoles}
              onBlur={()=>{
                  touched.groupRoles = true;
                  setTouched({...touched});
              }}
              validated={errors.groupRoles&&touched.groupRoles?'error':'default'}
              >
              <GroupRolesTable groupRoles={enrollment.groupRoles} selectedRoles={enrollmentRequest.groupRoles} setSelectedRoles={(roles)=>{
                enrollmentRequest.groupRoles=roles;
                setEnrollmentRequest({...enrollmentRequest});
              }} />
            </FormGroup>  
            {enrollment?.aup?.url?
              <>
                <p>
                <Msg msgKey='enrollmentFlowAupMessage1' /> <a href={enrollment?.aup?.url} target="_blank" rel="noreferrer"><Msg msgKey='invitationAUPMessage2' /></a> <Msg msgKey='invitationAUPMessage3' />
                </p>
                <div className="gm_checkbox-container gm_content-width">
                  <Checkbox
                    onClick={()=>{setAcceptAup(!acceptAup)}}
                    checked={acceptAup}
                    id="description-check-1"
                    label={Msg.localize('enrollmentConfigurationAupMessage')}
                  />
                </div>
              </> 
            :""}
            <Alert variant="info" className='gm_content-width' title={enrollment?.requireApproval?Msg.localize('enrollmentRequiresApprovalAlert'):Msg.localize('enrollmentNoApprovalAlert')} />
            <div>
            <Tooltip  {...(!(enrollment?.aup?.url&&!acceptAup) ? { trigger:'manual', isVisible:false }:{trigger:'mouseenter'})} content={<div><Msg msgKey='invitationAUPErrorMessage' /></div>}>
                  <div className="gm_invitation-response-button-container">
                  <Button isDisabled={enrollment?.aup?.url&&!acceptAup} onClick={()=>{
                    touchFields();
                    if(Object.keys(errors).length !== 0){
                        setModalInfo({
                            message:Msg.localize('enrollmentConfigurationModalSubmitError'),
                            accept_message: Msg.localize('OK'),
                            accept: function(){
                                setModalInfo({})},
                            cancel: function(){
                                setModalInfo({})}
                          });
                    }
                    else{
                        createEnrollmentRequest(enrollment?.requireApproval);
                    }
                  }}>Submit</Button>
                  </div>
                </Tooltip>
            
              </div>
                          
          </React.Fragment>
        :null}
        </React.Fragment>
        :
          <Alert className='gm_content-width' variant="warning" title={Msg.localize('enrollmentRequestExistsTitle')}>
            <p>
              <Msg msgKey='enrollmentRequestExistsMessage'/>{' '}
              <Link to={"/groups/mygroupenrollments"}>"View My Enrollment Requests”</Link>
            </p>
          </Alert>
       }  
            </Form> 
            </div>
          </ContentPage>
          
          
        </div>






          
      </React.Fragment>     
   
    )
}

