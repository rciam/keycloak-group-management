import * as React from 'react';
import { FC, useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Button, Tooltip, SelectVariant, Checkbox, Select, SelectOption, Alert, Form, FormGroup, Breadcrumb, BreadcrumbItem, TextArea, HelperText, HelperTextItem } from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import { dateParse, formatDateToString, isFutureDate } from '../../js/utils.js'
import { Msg } from '../../widgets/Msg';
// @ts-ignore
import { ContentPage } from '../../content/ContentPage';
import { GroupRolesTable } from '../GroupRolesTable';
import { Link } from 'react-router-dom';
import { useLoader } from '../LoaderContext';


const reg_url = /^(https?|chrome):\/\/[^\s$.?#].[^\s]*$/

export const CreateEnrollment: FC<any> = (props) => {
  const touchDefault = {
    comments: false,
    groupRoles: false
  };
  const [errors, setErrors] = useState<any>({});
  const [modalInfo, setModalInfo] = useState({});
  const [touched, setTouched] = useState<any>(touchDefault);
  const [enrollments, setEnrollments] = useState<any>([]);
  const [selected, setSelected] = useState('');
  const [group, setGroup] = useState<any>({});
  const [isOpen, setIsOpen] = useState(false);
  const [enrollment, setEnrollment] = useState<any>({});
  const [acceptAup, setAcceptAup] = useState(false);
  const [openRequest, setOpenRequest] = useState(false);
  const [defaultId, setDefaultId] = useState("");
  const [enrollmentRequest, setEnrollmentRequest] = useState({
    groupEnrollmentConfiguration: { id: '' },
    groupRoles: [],
    comments: ""
  });
  const [isParentGroup,setIsParentGroup] = useState<boolean>(false);
  const { startLoader, stopLoader } = useLoader();
  const activeRequests = useRef(0); // Tracks the number of active requests

  const startLoaderWithTracking = () => {
    if (activeRequests.current === 0) {
      startLoader();
    }
    activeRequests.current++;
  };

  const stopLoaderWithTracking = () => {
    activeRequests.current--;
    if (activeRequests.current === 0) {
      stopLoader();
    }
  };


  let groupsService = new GroupsServiceClient();

  // Consolidate API calls into a separate service file
  const fetchGroupEnrollment = async (id) => {
    try {
      startLoaderWithTracking();
      const response = await groupsService!.doGet<any>(`/user/configuration/${id}`);
      if (response.status === 200 && response.data) {
        return response.data;
      }
    } catch (error) {
      console.error('Error fetching group enrollment:', error);
    } finally {
      stopLoaderWithTracking();
    }
  };

  const fetchGroupEnrollments = async (groupPath) => {
    try {
      startLoaderWithTracking();
      const response = await groupsService!.doGet<any>('/user/groups/configurations', { params: { groupPath } });
      if (response.status === 200 && response.data) {
        return response.data;
      }
    } catch (error) {
      console.error('Error fetching group enrollments:', error);
    } finally {
      stopLoaderWithTracking();
    }
  };

  const fetchGroupEnrollmentRequests = async (groupId) => {
    try {
      startLoaderWithTracking();
      const response = await groupsService!.doGet<any>('/user/enroll-requests', { params: { groupId } });
      if (response.status === 200 && response.data) {
        return response.data.results;
      }
    } catch (error) {
      console.error('Error fetching group enrollment requests:', error);
    } finally {
      stopLoaderWithTracking();
    }
  };

  // Consolidate useEffect hooks
  useEffect(() => {
    const initializeEnrollment = async () => {
      const query = new URLSearchParams(props.location.search);
      const groupPath = decodeURI(query.get('groupPath') || '');
      const id = decodeURI(query.get('id') || '');

      if (id) {
        const enrollmentData = await fetchGroupEnrollment(id);
        if (enrollmentData) {
          setGroup(enrollmentData.group);
          setEnrollments([enrollmentData]);
          setIsParentGroup(enrollmentData.group?.path?.split('/').length === 2);
        }
      } else if (groupPath) {
        const enrollmentsData = await fetchGroupEnrollments(groupPath);
        if (enrollmentsData?.length > 0) {
          const defaultConfig = enrollmentsData[0].group?.attributes?.defaultConfiguration?.[0];
          setDefaultId(defaultConfig || '');
          setGroup(enrollmentsData[0].group);
          setEnrollments(enrollmentsData);
          setIsParentGroup(enrollmentsData[0].group?.path?.split('/').length === 2);
        }
      }
    };

    initializeEnrollment();
  }, [props.location.search]);

  useEffect(() => {
    const fetchRequests = async () => {
      if (group.id) {
        const requests = await fetchGroupEnrollmentRequests(group.id);
        if (requests) {
          const hasOpenRequest = requests.some(
            (request) => request.status === 'PENDING_APPROVAL' || request.status === 'WAITING_FOR_REPLY'
          );
          setOpenRequest(hasOpenRequest);
        }
      }
    };

    fetchRequests();
  }, [group]);

  useEffect(() => {
    if (enrollments.length > 0 && defaultId) {
      const defaultEnrollment = enrollments.find((enrollment) => enrollment.id === defaultId);
      if (defaultEnrollment) {
        setSelected(defaultEnrollment.name);
        setEnrollmentRequest((prev) => ({
          ...prev,
          groupEnrollmentConfiguration: { id: defaultEnrollment.id },
        }));
        setEnrollment(defaultEnrollment);
      }
    }
  }, [enrollments, defaultId]);

  useEffect(() => {
    if (enrollments.length === 1) {
      const singleEnrollment = enrollments[0];
      setEnrollmentRequest((prev) => ({
        ...prev,
        groupEnrollmentConfiguration: { id: singleEnrollment.id },
      }));
      setEnrollment(singleEnrollment);
    }
  }, [enrollments]);

  useEffect(() => {
    validateEnrollmentRequest();
  }, [enrollmentRequest]);

  const createEnrollmentRequest = (requiresApproval) => {
    startLoader();
    groupsService!.doPost<any>("/user/enroll-request", { ...enrollmentRequest })
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          if (requiresApproval) {
            props.history.push('/groups/mygroupenrollments');
          }
          else {
            props.history.push('/groups/showgroups');
          }
        }
      }).catch((err) => {
        stopLoader();
        console.log(err)
      })
  }


  const validateEnrollmentRequest = () => {
    let errors: Record<string, string> = {};
    (!enrollmentRequest?.comments && enrollment.commentsNeeded) && (errors.comments = Msg.localize('requredFormError'));
    !(enrollmentRequest?.groupRoles?.length > 0) && (errors.groupRoles = Msg.localize('groupRolesFormError'));
    (!enrollment.multiselectRole && enrollmentRequest.groupRoles.length > 1) && (errors.groupRoles = Msg.localize('groupRolesFormErrorMulitple'));
    setErrors(errors);
  }

  const touchFields = () => {
    for (const property in touched) {
      touched[property] = true;
    }
    setTouched({ ...touched });
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
        <ConfirmationModal modalInfo={modalInfo} />
        <ContentPage title={group?.name || ""}>
          {group?.name &&
            <p className="gm_group_desc">
              {(group?.attributes?.description && group?.attributes?.description[0]) || Msg.localize('noDescription')}
            </p>
          }
          <div className="gm_enrollment_container">
            <Form>
              {!openRequest ?
                <React.Fragment>
                  {enrollments && enrollments.length > 0 ?
                    <React.Fragment>
                      <FormGroup
                        label={Msg.localize('Group Enrollment')}
                        isRequired
                        fieldId="simple-form-name-01"
                      >
                        <Select
                          variant={SelectVariant.single}
                          aria-label="Select Input"
                          className="gm_form-input"
                          onToggle={onToggle}
                          onSelect={onSelect}
                          isDisabled={enrollments.length === 1}
                          selections={selected}
                          isOpen={isOpen}
                          aria-labelledby={"Test"}
                        >
                          <SelectOption {...(enrollments.length !== 1 && { ...{ key: "placeholder", isPlaceholder: true } })} value={Msg.localize('invitationEnrollmentSelectPlaceholder')} onClick={() => {
                            setEnrollment({});
                          }}

                          />
                          {enrollments.map((enrollment, index) => {
                            return <SelectOption {...(enrollments.length === 1 && { ...{ key: "placeholder", isPlaceholder: true } })} key={index} value={enrollment?.name} isDisabled={!enrollment.active} onClick={() => {
                              enrollmentRequest.groupEnrollmentConfiguration.id = enrollment.id
                              setEnrollmentRequest({ ...enrollmentRequest });
                              setEnrollment(enrollment);
                            }} />
                          })}
                        </Select>
                      </FormGroup>
                    </React.Fragment>
                    :
                    <Alert className='gm_content-width' variant="warning" title="This group has no available enrollments" />
                  }



                  {Object.keys(enrollment).length !== 0 ?
                    <React.Fragment>
                      <Alert variant="warning" className='gm_content-width' title={
                        ("The membership ") +
                        (enrollment.validFrom && isFutureDate(dateParse(enrollment.validFrom)) ? "will take effect at " + formatDateToString(dateParse(enrollment.validFrom)) : "") +
                        (enrollment.validFrom && isFutureDate(dateParse(enrollment.validFrom)) && parseInt(enrollment.membershipExpirationDays) > 0 ? " and it " : "") +
                        (parseInt(enrollment.membershipExpirationDays) > 0 ? "will expire on " + enrollment.membershipExpirationDays + " days after activation" : " does not have an expiration date.")}
                      />
                      {!isParentGroup&&
                        <HelperText className="gm_helper-text-create-enrollment">
                          <HelperTextItem variant="warning" hasIcon>
                            <p><Msg msgKey='effectiveExpirationInfo' /></p>
                          </HelperTextItem>
                        </HelperText>
                      }
                      {enrollment.commentsNeeded &&
                        <FormGroup
                          label={enrollment.commentsLabel}
                          isRequired

                          fieldId="simple-form-name-01"
                          helperTextInvalid={touched.comments && errors.comments}
                          validated={errors.comments && touched.comments ? 'error' : 'default'}
                        >
                          <TextArea
                            className="gm_form-input"
                            isRequired
                            type="text"
                            id="simple-form-name-01"
                            onBlur={() => { touched.comments = true; setTouched({ ...touched }); }}
                            name="simple-form-name-01"
                            aria-describedby="simple-form-name-01-helper"
                            value={enrollmentRequest.comments}
                            validated={errors.comments && touched.comments ? 'error' : 'default'}
                            onChange={(value) => { enrollmentRequest.comments = value; setEnrollmentRequest({ ...enrollmentRequest }); }}
                          />
                          <div className="gm_description-text">{enrollment.commentsDescription}</div>
                        </FormGroup>
                      }

                      <FormGroup
                        label={Msg.localize('Select Your Group Role')}
                        isRequired
                        fieldId="simple-form-name-01"
                        helperTextInvalid={touched.groupRoles && errors.groupRoles}
                        onBlur={() => {
                          touched.groupRoles = true;
                          setTouched({ ...touched });
                        }}
                        validated={errors.groupRoles && touched.groupRoles ? 'error' : 'default'}
                      >
                        <GroupRolesTable groupRoles={enrollment.groupRoles} selectedRoles={enrollmentRequest.groupRoles} setSelectedRoles={(roles) => {
                          enrollmentRequest.groupRoles = roles;
                          setEnrollmentRequest({ ...enrollmentRequest });
                        }} />
                      </FormGroup>
                      {enrollment?.aup?.url ?
                        <>
                          <p>
                            <Msg msgKey='enrollmentFlowAupMessage1' /> <a href={enrollment?.aup?.url} target="_blank" rel="noreferrer"><Msg msgKey='invitationAUPMessage2' /></a> <Msg msgKey='invitationAUPMessage3' />
                          </p>
                          <div className="gm_checkbox-container gm_content-width">
                            <Checkbox
                              onClick={() => { setAcceptAup(!acceptAup) }}
                              checked={acceptAup}
                              id="description-check-1"
                              label={Msg.localize('enrollmentConfigurationAupMessage')}
                            />
                          </div>
                        </>
                        : ""}
                      <Alert variant="info" className='gm_content-width' title={enrollment?.requireApproval ? Msg.localize('enrollmentRequiresApprovalAlert') : Msg.localize('enrollmentNoApprovalAlert')} />
                      <div>
                        <Tooltip  {...(!(enrollment?.aup?.url && !acceptAup) ? { trigger: 'manual', isVisible: false } : { trigger: 'mouseenter' })} content={<div><Msg msgKey='invitationAUPErrorMessage' /></div>}>
                          <div className="gm_invitation-response-button-container">
                            <Button isDisabled={enrollment?.aup?.url && !acceptAup} onClick={() => {
                              touchFields();
                              if (Object.keys(errors).length !== 0) {
                                setModalInfo({
                                  message: Msg.localize('enrollmentConfigurationModalSubmitError'),
                                  accept_message: Msg.localize('OK'),
                                  accept: function () {
                                    setModalInfo({})
                                  },
                                  cancel: function () {
                                    setModalInfo({})
                                  }
                                });
                              }
                              else {
                                createEnrollmentRequest(enrollment?.requireApproval);
                              }
                            }}>Submit</Button>
                          </div>
                        </Tooltip>

                      </div>

                    </React.Fragment>
                    : null}
                </React.Fragment>
                :
                <Alert className='gm_content-width' variant="warning" title={Msg.localize('enrollmentRequestExistsTitle')}>
                  <p>
                    <Msg msgKey='enrollmentRequestExistsMessage' />{' '}
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

