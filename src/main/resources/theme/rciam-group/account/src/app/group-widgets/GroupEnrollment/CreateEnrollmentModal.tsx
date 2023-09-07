import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, Tooltip, DataListAction, SelectVariant, Checkbox,Select,SelectOption, FormAlert, Alert, Form, FormGroup, TextInput, Modal, ModalVariant, Switch, FormFieldGroupHeader, FormFieldGroup, DatePicker, Popover, NumberInput, HelperTextItem, TextArea} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import {isIntegerOrNumericString,getCurrentDate} from '../../js/utils.js'
import { Loading } from '../LoadingModal';
import { Msg } from '../../widgets/Msg';
import { HelpIcon, TrashIcon } from '@patternfly/react-icons';


const reg_url = /^(https?|chrome):\/\/[^\s$.?#].[^\s]*$/

export const EnrollmentModal: FC<any> = (props) => {
    
    let currentDate = getCurrentDate();


    const touchDefault = {
      name: false,
      groupRoles: false,
      aup_url:false,
      membershipExpirationDays:false,
      validFrom:false,
      commentsLabel:false,
      commentsDescription:false
  };


    const dateFormat = (date: Date) =>
        date.toLocaleDateString('en-CA', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-');
    
    const dateParse = (date: string) => {
        const split = date.split('-');
        if (split.length !== 3) {
        return new Date();
        }
        const month = split[1];
        const day = split[2];
        const year = split[0];
        return new Date(`${year.padStart(4, '0')}-${month.padStart(2, '0')}-${day.padStart(2, '0')}T00:00:00`);
    };


    let groupsService = new GroupsServiceClient();
    const [loading,setLoading] = useState(false);
    const [modalInfo,setModalInfo] = useState({});
    const [enrollment,setEnrollment] = useState<any>({});
    const [isModalOpen,setIsModalOpen] = useState<boolean>(false)
    const [errors,setErrors] = useState<any>({});
    const [validationRules,setValidationRules] = useState<any>({});
    const [touched,setTouched] = useState<any>(touchDefault);

    useEffect(()=>{
      if(Object.keys(props.enrollment).length !== 0) {
            
            setIsModalOpen(true);
            setEnrollment({...props.enrollment});
        }
        else{
            setIsModalOpen(false);
            setEnrollment({});
        }
    },[props.enrollment]);

    useEffect(()=>{
        if(Object.keys(enrollment).length !== 0){
            validateEnrollment();
        }
    },[enrollment]);

    useEffect(()=>{
      setValidationRules(props.validationRules);
    },[props.validationRules]);

    
    const validateEnrollment = () => {
        let errors: Record<string, string> = {};
        !(enrollment?.name?.length>0) && (errors.name = Msg.localize('requredFormError'));
        !(enrollment?.aup?.url?.length>0 && reg_url.test(enrollment.aup.url)) &&  (errors.aup_url = Msg.localize('invalidUrlFormError'));
        !(enrollment?.aup?.url?.length>0) && (errors.aup_url = Msg.localize('requredFormError'));
        !(enrollment?.groupRoles?.length>0) && (errors.groupRoles=Msg.localize('groupRolesFormError'));
        (enrollment?.membershipExpirationDays&&!(enrollment?.membershipExpirationDays>0)) && (errors.membershipExpirationDays=Msg.localize('expirationDaysPositiveFormError'));        
        (typeof(enrollment?.membershipExpirationDays)!=='number') && (errors.membershipExpirationDays=Msg.localize('expirationDaysNumberFormError'));
        (enrollment?.commentsNeeded&& (!enrollment?.commentsLabel||enrollment?.commentsLabel.length<1) && (errors.commentsLabel=Msg.localize('requredFormError')));
        (enrollment?.commentsNeeded&& (!enrollment?.commentsDescription||enrollment?.commentsDescription.length<1) && (errors.commentsDescription=Msg.localize('requredFormError')));        
        if(enrollment?.validFrom){
          let parsedDate = dateParse(enrollment?.validFrom);
          if(parsedDate instanceof Date &&isFinite(parsedDate.getTime())){
            isPastDate(parsedDate) && props.enrollment.validFrom!==enrollment.validFrom && (errors.validFrom=Msg.localize('validFromPastFormError'))
          }
          else{
            !(parsedDate instanceof Date &&isFinite(parsedDate.getTime())) && (errors.validFrom=Msg.localize('validFromInvalidFormError'));
          }
        }
        if(Object.keys(validationRules).length !== 0){
          for(const field in validationRules){
            field==='validFrom'&& validationRules[field]?.required&& !enrollment?.validFrom && (errors.validFrom=Msg.localize('validFromRequiredFormError'));
            validationRules[field]?.max && parseInt(validationRules[field].max) && (enrollment[field] > parseInt(validationRules[field]?.max )) && (errors[field]=Msg.localize('fieldMaxFormError')+ " (" + validationRules[field]?.max + ")" )   
            validationRules[field]?.max && parseInt(validationRules[field].max) && enrollment[field]===0 && (errors[field]=  Msg.localize('fieldMaxZeroFormError')+ " (" + validationRules[field]?.max + ")" )
          }
        }
        
        setErrors(errors);
        //!(enrollemtn?)
    }

    const touchFields = ()=> {
        for (const property in touched){
            touched[property]= true;
        }            
        setTouched({...touched});
    }
     
    const close = () =>{
      setTouched(touchDefault);
      props.close();
    }
    
    const updateEnrollment = (attribute,value) =>{
        enrollment[attribute] = value;
        setEnrollment({...enrollment});
    }


    let roleHandler = (role)=>{
        if(enrollment.groupRoles.includes(role)){
          const index = enrollment.groupRoles.indexOf(role);
          if (index > -1) { // only splice array when item is found
            enrollment.groupRoles.splice(index, 1); // 2nd parameter means remove one item only
          }
        }
        else{
            enrollment.groupRoles.push(role);
        }
        setEnrollment({...enrollment});
      }

      const createEnrollment = () => {
        setLoading(true);
        if(enrollment.membershipExpirationDays===0){
            enrollment.membershipExpirationDays = null;
        }
        groupsService!.doPost<any>("/group-admin/group/"+props.groupId+"/configuration",{...enrollment})
        .then((response: HttpResponse<any>) => {
          setLoading(false);
          if(response.status===200||response.status===204){
            close();
            // setGroupMembers(response.data.results);
          }
        }).catch((err)=>{
          setLoading(false);
          close();
          console.log(err)})
      }


      const deleteEnrollment = (id) => {
        setLoading(true);
        groupsService!.doDelete<any>("/group-admin/group/"+props.groupId+"/configuration/"+ id)
        .then((response: HttpResponse<any>) => {
          setLoading(false);
          if(response.status===200||response.status===204){
            close();
            // setGroupMembers(response.data.results);
          }
        }).catch((err)=>{
          setLoading(false);
          close();
          console.log(err)})
      }

      const onMinus = () => {
        enrollment.membershipExpirationDays = (enrollment.membershipExpirationDays || 0) - 1;
        setEnrollment({...enrollment});
      };
    
      const onChange = (event: React.FormEvent<HTMLInputElement>) => {
        touched.membershipExpirationDay = true;
        enrollment.membershipExpirationDays = (event.target as HTMLInputElement).value;
        enrollment.membershipExpirationDays = enrollment.membershipExpirationDays === '' ? enrollment.membershipExpirationDays : +enrollment.membershipExpirationDays
        setEnrollment({...enrollment});
        setTouched({...touched});
      };
    
      const onPlus = () => {
        enrollment.membershipExpirationDays = (enrollment?.membershipExpirationDays || 0) + 1;
        setEnrollment({...enrollment});
      };
  

      const isPastDate = (date: Date): string => {
        const currentDate = new Date();
        if (date < currentDate&&(props.enrollment.validFrom!==dateFormat(date))) {
          return Msg.localize('validFromPastFormError');
        }
        else{
          return "";
        }
        
      };

    const validators: ((date: Date) => string)[] = [isPastDate];
    return (
      <React.Fragment>
        <Loading active={loading}/>

        <Modal
                variant={ModalVariant.large}
                header={
                  <React.Fragment >
                    <h1 className="pf-c-modal-box__title gm_modal-title gm_flex-center">
                      {(enrollment?.id?Msg.localize('enrollmentConfigurationModalTitleEdit'):Msg.localize('enrollmentConfigurationModalTitleCreate'))}
                      {enrollment?.id&&
                        <Tooltip content={
                                  <div>
                                      <Msg msgKey="deleteEnrollmentTooltip"/>
                                  </div>
                              }
                          >  
                          <TrashIcon onClick={()=>{
                            setModalInfo({
                              message:Msg.localize('deleteEnrollmentConfirmation'),
                              accept_message: Msg.localize("yes"),
                              cancel_message: Msg.localize("no"),
                              accept: function(){
                                  deleteEnrollment(enrollment?.id);
                                  setModalInfo({})},
                              cancel: function(){
                                  setModalInfo({})}
                            });

                          }}/>
                        </Tooltip>              
                      }
                    </h1>
                    
                  </React.Fragment>
                }
                isOpen={isModalOpen}
                onClose={()=>{close()}}
                actions={[
                    <Tooltip {...(!!(Object.keys(errors).length !== 0) ? { trigger:'manual', isVisible:false }:{trigger:'mouseenter'})}
                        content={
                            <div>
                                <Msg msgKey='createSubgroupFormError' />
                            </div>
                        }
                    >
                    <div>
                        <Button key="confirm" variant="primary"  onClick={()=>{
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
                                createEnrollment();
                            }
                            
                            }}>
                            <Msg msgKey={enrollment?.id?Msg.localize('Edit'):Msg.localize('Create')} />
                        </Button>
                    </div>

                    </Tooltip>
                    ,
                    
                    <Button key="cancel" variant="link" onClick={()=>{ close()}}>
                        <Msg msgKey='Cancel' />
                    </Button>
                    
                ]}
                >
                    <ConfirmationModal modalInfo={modalInfo}/>
                    {Object.keys(enrollment).length !== 0?
                    <Form>
                        <FormGroup
                            label= {Msg.localize('enrollmentConfigurationNameTitle')}
                            isRequired
                            fieldId="simple-form-name-01"
                            helperTextInvalid={touched.name&&errors.name}
                            onBlur={()=>{
                                touched.name = true;
                                setTouched({...touched});
                            }}
                            validated={errors.name&&touched.name?'error':'default'}
                        >
                            <TextInput
                            isRequired
                            type="text"
                            id="simple-form-name-01"
                            name="simple-form-name-01"
                            aria-describedby="simple-form-name-01-helper"
                            value={enrollment?.name}
                            validated={errors.name&&touched.name?'error':'default'}
                            onChange={(value)=>{updateEnrollment('name',value)}}
                            />
                        </FormGroup>
                          <FormGroup
                              label={Msg.localize('enrollmentConfigurationMembExpTitle')}
                              fieldId="simple-form-name-01"
                              helperTextInvalid={touched.membershipExpirationDays&&errors.membershipExpirationDays}
                              validated={errors.membershipExpirationDays&&touched.membershipExpirationDays?'error':'default'}
                              onBlur={()=>{
                                  touched.membershipExpirationDays = true;
                                  setTouched({...touched});
                              }}
                              // helperText=""
                          >
                          <Tooltip  {...((validationRules?.membershipExpirationDays?.max&&parseInt(validationRules.membershipExpirationDays.max)>0&&!(isIntegerOrNumericString(enrollment?.membershipExpirationDays)&&enrollment.membershipExpirationDays!==0)) ? { trigger:'manual', isVisible:false }:{trigger:'mouseenter'})} content={<div><Msg msgKey='enrollmentConfigurationExpirationSwitchDisabledTooltip' /></div>}>
                                                          
                              <Switch
                                id="simple-switch-membershipExpirationDays"
                                aria-label="simple-switch-membershipExpirationDays"
                                isDisabled={!(validationRules?.membershipExpirationDays?.max&&parseInt(validationRules.membershipExpirationDays.max)>0&&enrollment.membershipExpirationDays===0)}
                                isChecked={isIntegerOrNumericString(enrollment?.membershipExpirationDays)&&enrollment.membershipExpirationDays!==0}
                                onChange={(value)=>{
                                    
                                    if(isIntegerOrNumericString(enrollment?.membershipExpirationDays)&&enrollment.membershipExpirationDays!==0){
                                        enrollment.membershipExpirationDays = 0;
                                        setEnrollment({...enrollment});
                                    }
                                    else{
                                        enrollment.membershipExpirationDays = ((validationRules?.membershipExpirationDays?.defaultValue&& isIntegerOrNumericString(validationRules?.membershipExpirationDays?.defaultValue))?parseInt(validationRules.membershipExpirationDays.defaultValue):3)   
                                        setEnrollment({...enrollment});
                                    }
                                }}
                                />
                            
                            </Tooltip>
                            
                            {enrollment.membershipExpirationDays===0?
                              <HelperTextItem className="gm_expiration-warning-label" variant="warning" hasIcon>
                                <Msg msgKey='enrollmentConfigurationExpirationWaring' />
                              </HelperTextItem>
                            :null}
                            
                            {enrollment.membershipExpirationDays !== 0?
                              <div className="gm_number-input-container"> 
                                <NumberInput
                                value={enrollment?.membershipExpirationDays}
                                onMinus={onMinus}
                                onBlur={()=>{
                                  touched.membershipExpirationDay = true;
                                  setTouched({...touched});
                                }}
                                onChange={onChange}
                                onPlus={onPlus}
                                inputName="input"
                                inputAriaLabel="number input"
                                minusBtnAriaLabel="minus"
                                plusBtnAriaLabel="plus"
                              />
                              </div>
                            :null}
                          </FormGroup>   
                          <FormGroup
                            label={Msg.localize('enrollmentConfigurationValidFromTitle')}
                            fieldId="simple-form-name-09"
                            helperTextInvalid={touched.validFrom&&!enrollment?.validFrom&&errors.validFrom}
                            validated={errors.validFrom&&touched.validFrom?'error':'default'}
                            labelIcon={
                                <Popover
                                  bodyContent={
                                    <div>
                                      <Msg msgKey='enrollmentConfigurationTooltipValidFrom' />    .
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
                          >
                           <Switch
                            id="simple-switch-requireApproval"
                            aria-label="simple-switch-requireApproval"
                            isChecked={enrollment?.validFrom}
                            onChange={(value)=>{
                              touched.validFrom= true;
                              setTouched({...touched});
                                
                                if(enrollment?.validFrom){
                                    enrollment.validFrom=null;
                                    setEnrollment({...enrollment});
                                }
                                else{
                                  enrollment.validFrom=currentDate;                                     
                                    setEnrollment({...enrollment});
                                }
                            }}
                            />
                        </FormGroup>
                        {enrollment?.validFrom?                                
                                <DatePicker  value={enrollment?.validFrom} placeholder="DD-MM-YYYY" dateFormat={dateFormat} dateParse={dateParse}  validators={validators} onChange={(value,date)=>{    
                                        if(enrollment?.validFrom){
                                            enrollment.validFrom=value; 
                                            setEnrollment({...enrollment});
                                        }
                                    }} 
                                />
                        :null}
                        
                        <FormGroup
                            label={Msg.localize('enrollmentConfigurationApprovalTitle')}
                            fieldId="simple-form-name-01"
                            // helperText=""
                        >
                           <Switch
                            id="simple-switch-requireApproval1"
                            aria-label="simple-switch-requireApproval1"
                            isChecked={enrollment?.requireApproval}
                            onChange={(value)=>{updateEnrollment('requireApproval',value)}}
                            />
                        </FormGroup>
                        <FormFieldGroup
                          header={
                          <FormFieldGroupHeader
                              titleText={{ text: Msg.localize('enrollmentConfigurationCommentSectionTitle'), id: 'field-group4-non-expandable-titleText-id' }}
                          />
                          }                          
                        >
                          <FormGroup
                              label={Msg.localize('enrollmentConfigurationCommentEnableLabel')}
                              isRequired
                              fieldId="simple-form-name-01"
                              labelIcon={
                                  <Popover
                                    bodyContent={
                                      <div>
                                          <Msg msgKey='enrollmentConfigurationCommentEnableTooltip' />
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
                              // helperText=""
                          >
                            <Switch
                              id="simple-switch-comments"
                              aria-label="simple-switch-membershipExpirationDays"
                              isChecked={enrollment.commentsNeeded}
                              onChange={(value)=>{
                                enrollment.commentsNeeded = !enrollment.commentsNeeded;
                                setEnrollment({...enrollment});
                              }}
                              />

                          </FormGroup>
                          {enrollment.commentsNeeded?
                            <React.Fragment>
                              <FormGroup
                                label={Msg.localize('enrollmentConfigurationCommentLabelLabel')}
                                isRequired
                                fieldId="simple-form-name-01"
                                helperTextInvalid={touched.commentsLabel&&errors.commentsLabel}
                                validated={errors.commentsLabel&&touched.commentsLabel?'error':'default'}
                                onBlur={()=>{
                                    touched.commentsLabel = true;
                                    setTouched({...touched});
                                }}
                                // helperText=""
                              >
                                <TextInput
                                  isRequired
                                  type="url"
                                  id="simple-form-name-01"
                                  name="simple-form-name-01"
                                  aria-describedby="simple-form-name-01-helper"
                                  value={enrollment?.commentsLabel}
                                  onBlur={()=>{touched.commentsLabel=true; setTouched({...touched});}}
                                  validated={errors.commentsLabel&&touched.commentsLabel?'error':'default'}
                                  onChange={(value)=>{
                                      enrollment.commentsLabel=value;
                                      setEnrollment({...enrollment});
                                  }}
                                />
                              </FormGroup>
                              <FormGroup
                              label={Msg.localize('enrollmentConfigurationCommentDescriptionLabel')}
                              isRequired
                              fieldId="simple-form-name-01"
                              helperTextInvalid={touched.commentsDescription&&errors.commentsDescription}
                              validated={errors.commentsDescription&&touched.commentsDescription?'error':'default'}
                              onBlur={()=>{
                                  touched.commentsDescription = true;
                                  setTouched({...touched});
                              }}
                              // helperText=""
                            >
                              <TextInput
                                isRequired
                                type="url"
                                id="simple-form-name-01"
                                name="simple-form-name-01"
                                aria-describedby="simple-form-name-01-helper"
                                value={enrollment?.commentsDescription}
                                onBlur={()=>{touched.commentsDescription=true; setTouched({...touched});}}
                                validated={errors.commentsDescription&&touched.commentsDescription?'error':'default'}
                                onChange={(value)=>{
                                    enrollment.commentsDescription=value;
                                    setEnrollment({...enrollment});
                                }}
                              />
                            </FormGroup>
                            </React.Fragment>
                            
                          :null}

                        </FormFieldGroup>
                        
                        <FormFieldGroup
                            header={
                            <FormFieldGroupHeader
                                titleText={{ text: Msg.localize('enrollmentConfigurationAupTitle'), id: 'field-group4-non-expandable-titleText-id' }}
                            />
                            }
                        >
                            <FormGroup
                                label={Msg.localize('URL')}
                                isRequired
                                fieldId="simple-form-name-01"
                                helperTextInvalid={touched.aup_url&&errors.aup_url}
                                validated={errors.aup_url&&touched.aup_url?'error':'default'}
                                // helperText=""
                            >
                                <TextInput
                                isRequired
                                type="url"
                                id="simple-form-name-01"
                                name="simple-form-name-01"
                                aria-describedby="simple-form-name-01-helper"
                                value={enrollment?.aup?.url}
                                onBlur={()=>{touched.aup_url=true; setTouched({...touched});}}
                                validated={errors.aup_url&&touched.aup_url?'error':'default'}
                                onChange={(value)=>{
                                    enrollment.aup.url=value;
                                    setEnrollment({...enrollment});
                                }}
                                />
                            </FormGroup>
                        </FormFieldGroup>
                        <FormFieldGroup
                             header={
                            <FormFieldGroupHeader
                                titleText={{ text: Msg.localize('enrollmentConfigurationGroupRolesTitle'), id: 'field-group4-non-expandable-titleText-id' }}
                            />
                            }
                        >
                            <FormGroup
                                label={Msg.localize('enrollmentConfigurationTooltipGroupRoles')}
                                fieldId="simple-form-name-01"
                                helperTextInvalid={touched.groupRoles&&errors.groupRoles}
                                validated={errors.groupRoles&&touched.groupRoles?'error':'default'}
                            >
                            <table className="gm_roles-table">
                                    <tbody>
                                    {props.groupRoles&&props.groupRoles?.map((role,index)=>{
                                        return <tr onClick={()=>{roleHandler(role);}}>
                                            <td>
                                                {role}
                                            </td>
                                            <td>
                                                <Checkbox id="standalone-check" name="standlone-check" checked={enrollment?.groupRoles?.includes(role)} aria-label="Standalone input" />
                                            </td>   
                                        </tr>                    
                                    })}
                                    </tbody>
                                </table>
                                
                            </FormGroup>
                            <FormGroup
                                label={Msg.localize('enrollmentConfigurationMultiSelectTitle')}
                                fieldId="simple-form-name-01"
                                labelIcon={
                                    <Popover
                                      bodyContent={
                                        <div>
                                            <Msg msgKey='enrollmentConfigurationMultiSelectTooltip' />
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
                                // helperText=""
                            >
                            <Switch
                                aria-label="simple-switch-multiselectRole"
                                isChecked={enrollment?.multiselectRole}
                                onChange={(value)=>{updateEnrollment('multiselectRole',value)}}
                                />
                            </FormGroup>
                        </FormFieldGroup>
                        <FormGroup
                            label={Msg.localize('enrollmentConfigurationHideConfTitle')}
                            fieldId="simple-form-name-01"
                            labelIcon={
                                <Popover
                                  bodyContent={
                                    <div>
                                        <Msg msgKey='enrollmentConfigurationHideConfTooltip' />
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
                            // helperText=""
                        >
                           <Switch
                            id="simple-switch-visibleToNotMembers"
                            aria-label="simple-switch-visibleToNotMembers"
                            isChecked={enrollment?.visibleToNotMembers}
                            onChange={(value)=>{updateEnrollment('visibleToNotMembers',value)}}
                            />
                        </FormGroup>
                        <FormGroup
                                label={Msg.localize('enrollmentConfigurationActiveTitle')}
                                fieldId="simple-form-name-01"
                                // helperText=""
                            >
                            <Switch
                                aria-label="simple-switch-active"
                                isChecked={enrollment?.active}
                                onChange={(value)=>{updateEnrollment('active',value)}}
                                />
                        </FormGroup>
                        
                        



                    </Form>:null}
        </Modal>
        
        </React.Fragment>         
   
    )
}

