import * as React from 'react';
import { FC, useState, useEffect } from 'react';
import { Button, Tooltip, Alert, Form, FormGroup, Modal, ModalVariant, FormFieldGroupHeader, FormFieldGroup, TextArea, Badge, ExpandableSection, DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell } from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
import { Msg } from '../../widgets/Msg';
import { CopyIcon, ExternalLinkSquareAltIcon, HelpIcon } from '@patternfly/react-icons';
import { Popover, List, ListItem } from '@patternfly/react-core';
import { getError } from '../../js/utils.js'
import { ContentAlert } from '../ContentAlert';
import { useLoader } from '../../group-widgets/LoaderContext';

export const EnrollmentRequest: FC<any> = (props) => {
  const { startLoader, stopLoader } = useLoader();
  const [enrollmentRequest, setEnrollmentRequest] = useState<any>({});
  const [copyTooltip, setCopyTooltip] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false)
  let groupsService = new GroupsServiceClient();
  const [reviewerComment, setReviewerComment] = useState("")
  const [expandUserDetails, setExpandUserDetails] = useState(false);

  useEffect(() => {
    if (Object.keys(props.enrollmentRequest).length !== 0) {
      setIsModalOpen(true);
      setEnrollmentRequest({ ...props.enrollmentRequest });
    }
    else {
      setIsModalOpen(false);
      setEnrollmentRequest({});
    }
  }, [props.enrollmentRequest]);

  const disapearingTooltip = () => {
    setCopyTooltip(true);
    setTimeout(() => {
      setCopyTooltip(false);
    }, 2000);
  }

  let reviewEnrollmentRequest = (action) => {
    close();
    startLoader();
    groupsService!.doPost<any>("/group-admin/enroll-request/" + enrollmentRequest.id + "/" + action, {}, { params: { ...(reviewerComment ? { adminJustification: reviewerComment } : {}) } })
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          props.refresh();
          ContentAlert.success(Msg.localize("reviewEnrollmentSuccess"))
        }
        else {
          ContentAlert.danger(Msg.localize("reviewEnrollmentError") + " " + getError(response))
        }
      }).catch((err) => { console.log(err) })
  }

  let close = () => {
    setExpandUserDetails(false);
    props.close();
  }



  return (
    <React.Fragment>
      <Modal
        variant={ModalVariant.large}
        header={
          <React.Fragment >
            <h1 className="pf-c-modal-box__title gm_flex-center">
              {enrollmentRequest?.status === 'PENDING_APPROVAL' && props.managePage ? <Msg msgKey='reviewRequestTitle' /> : <Msg msgKey='viewRequestTitle' />}
              {props.managePage && <Tooltip {...(!!(copyTooltip) ? { trigger: 'manual', isVisible: true } : { trigger: 'mouseenter' })}
                content={
                  <div>
                    {copyTooltip ? <Msg msgKey='copiedTooltip' /> : <Msg msgKey='copyTooltip' />}
                  </div>
                }
              >
                <Button isSmall className={'gm_grey-button gm_title-button'} onClick={() => {
                  disapearingTooltip();
                  let link = groupsService.getBaseUrl() + '/account/#/groups/groupenrollments?id=' + encodeURI(enrollmentRequest?.id);
                  navigator.clipboard.writeText(link)
                }}><CopyIcon /> </Button>
              </Tooltip>}
            </h1>

          </React.Fragment>
        }
        isOpen={isModalOpen}
        onClose={() => { close() }}
        actions={[
          ...(enrollmentRequest?.status === 'PENDING_APPROVAL' && props.managePage ?
            [<Tooltip
              content={
                <div>
                  <Msg msgKey={'approveRequestTooltip'} />
                </div>
              }
            >
              <div>
                <Button key="confirm" className="gm_green-button gm_button-spacer" onClick={() => {
                  reviewEnrollmentRequest('accept');
                }}>
                  <Msg msgKey={"Approve"} />
                </Button>
              </div>

            </Tooltip>
              ,

            <Button key="cancel" variant="danger" onClick={() => {
              reviewEnrollmentRequest('reject');
            }}>
              <Msg msgKey='Deny' />
            </Button>
            ] : [
              <Button key="back" className="gm_grey-button" variant="primary" onClick={() =>
                close()
              }
              >
                <Msg msgKey="Back" />
              </Button>
            ]
          )
        ]}
      >

        <Alert variant={enrollmentRequest?.status === 'ACCEPTED' ? "success" : enrollmentRequest?.status === 'REJECTED' ? "danger" : "info"} title={
          <React.Fragment>
            <p><Msg msgKey='reviewAlertSubmitted' /><span className="gm_normal-text"> {enrollmentRequest?.submittedDate}</span></p>
            {enrollmentRequest?.approvedDate &&
              <p className="gm_margin-top-1rem"><Msg msgKey={enrollmentRequest?.status} />: <span className="gm_normal-text">{enrollmentRequest?.approvedDate}</span></p>
            }
            {!enrollmentRequest?.approvedDate && <p className="gm_margin-top-1rem"><Msg msgKey='reviewAlertStatus' /> <span className="gm_normal-text"><Msg msgKey={enrollmentRequest?.status} /></span></p>}
            {enrollmentRequest?.checkAdmin && <p className="gm_margin-top-1rem"><Msg msgKey='reviewAlertReviewer' /><span className="gm_normal-text">{enrollmentRequest?.checkAdmin?.firstName + " " + enrollmentRequest?.checkAdmin?.lastName + " (" + enrollmentRequest?.checkAdmin?.email + ")"}</span></p>}
            {enrollmentRequest?.adminJustification && <p className="gm_margin-top-1rem"><Msg msgKey='reviewAlertComment' /> <span className="gm_normal-text">{enrollmentRequest?.adminJustification}</span></p>}
          </React.Fragment>
        } />
        <Form className="gm_enrollment-request-view-form" isHorizontal>
          <FormFieldGroup>
            <div className="gm_form-field-group-title">
              User Details
              <Popover
                headerContent={<div>
                  <Msg msgKey='enrollmentUserDetails' />
                </div>} bodyContent={undefined}              >
                <button
                  type="button"
                  aria-label="More info for name field"
                  onClick={e => e.preventDefault()}
                  aria-describedby="simple-form-name-02"
                  className="pf-c-form__group-label-help"
                >
                  <HelpIcon noVerticalAlign />
                </button>
              </Popover>
            </div>

            <FormGroup
              label={Msg.localize('enrollmentFullNameLabel')}
              fieldId="simple-form-name-01"
            // helperText=""
            >
              <div>
                {enrollmentRequest?.userFirstName || enrollmentRequest?.userLastName ? enrollmentRequest?.userFirstName + " " + enrollmentRequest?.userLastName : "Not Available"}
              </div>
            </FormGroup>
            <FormGroup
              label={Msg.localize('enrollmentEmailLabel')}
              fieldId="simple-form-name-02"
            >
              <div>{enrollmentRequest?.userEmail ? enrollmentRequest?.userEmail : Msg.localize('notAvailable')}</div>
            </FormGroup>
            <FormGroup
              label={Msg.localize('username')+""}
              fieldId="simple-form-name-02"
            >
              <div>{enrollmentRequest?.userIdentifier ? enrollmentRequest?.userIdentifier : Msg.localize('notAvailable')}</div>
            </FormGroup>

            {/* <FormGroup
              label={Msg.localize('enrollmentAssuranceLabel')}
              fieldId="simple-form-name-03"
            >
              <div>
                {enrollmentRequest?.user?.attributes?.eduPersonAssurance && Array.isArray(enrollmentRequest?.user?.attributes?.eduPersonAssurance) ?
                  enrollmentRequest?.user?.attributes?.eduPersonAssurance.map((value, index) => {
                    return <Badge key={index} className="gm_role_badge" isRead>{value}</Badge>
                  })
                  : enrollmentRequest?.user?.attributes?.eduPersonAssurance ?
                    <Badge key={'single'} className="gm_role_badge" isRead>{enrollmentRequest?.user?.attributes?.eduPersonAssurance}</Badge>
                    : <Msg msgKey='notAvailable' />
                }
              </div>
            </FormGroup>
            */}
            <FormGroup
              label={Msg.localize('userAuthnAuthorityLabel')}
              fieldId="simple-form-name-03"
            >
              {enrollmentRequest?.userAuthnAuthorities && Array.isArray(JSON.parse(enrollmentRequest?.userAuthnAuthorities)) ?
                <List>
                  {JSON.parse(enrollmentRequest.userAuthnAuthorities).map((value, index) => (
                    <ListItem key={index} style={{ marginLeft: `${index}rem` }}>
                      {value.id}
                      {value.name && value.name !== value.id && ` - ${value.name}`}
                      {!value.name && value.name !== value.id && <> - <Msg msgKey="notAvailable" /></>}
                    </ListItem>
                  ))}
                </List>
                : <Msg msgKey='notAvailable' />}
            </FormGroup>
            <FormGroup
              label={"Assurance"}
              fieldId="simple-form-name-03"
            >
              <DataList id="groups-list" aria-label={Msg.localize('groupLabel')} isCompact wrapModifier={"breakWord"}>
                <DataListItem id="groups-list-header" aria-labelledby="Columns names">
                  <DataListItemRow >
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell key='group-name-header' width={2}>
                          <strong>Value</strong>
                        </DataListCell>,
                        <DataListCell key='group-path-header' width={2}>
                          <strong>Description</strong>
                        </DataListCell>
                      ]}
                    />
                  </DataListItemRow>
                </DataListItem>

                {enrollmentRequest?.userAssurance && Array.isArray(enrollmentRequest.userAssurance) && enrollmentRequest.userAssurance.length > 0 ? enrollmentRequest?.userAssurance.map((value, index) => {
                  return (
                    <DataListItem id={`${index}-group`} key={'group-' + index} aria-labelledby="groups-list">
                      <DataListItemRow>
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell id={`${index}-value`} width={2} key={'value-' + index}>
                              {value}
                            </DataListCell>,
                            <DataListCell id={`${index}-description`} width={2} key={'path-' + index}>
                              {Msg.localize(value.replace(":", "")) != value.replace(":", "") ?
                                <div dangerouslySetInnerHTML={{ __html: Msg.localize(value.replace(":", "")) }} />
                                :
                                <Msg msgKey='notAvailable' />}
                            </DataListCell>
                          ]}
                        />
                      </DataListItemRow>
                    </DataListItem>
                  )
                })
                  :
                  <DataListItem key='emptyItem' aria-labelledby="empty-item">
                    <DataListItemRow key='emptyRow'>
                      <DataListItemCells dataListCells={[
                        <DataListCell key='empty'><Msg msgKey='noAssurance' /></DataListCell>
                      ]} />
                    </DataListItemRow>
                  </DataListItem>
                }

              </DataList>
            </FormGroup>
            {props.managePage &&
              <ExpandableSection toggleText={expandUserDetails ? 'Hide current user details' : 'Show current user details'} onToggle={() => { setExpandUserDetails(!expandUserDetails) }} isExpanded={expandUserDetails}>
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
                {/* <FormGroup
                  label={Msg.localize('enrollmentAssuranceLabel')}
                  fieldId="simple-form-name-03"
                >
                  <div>
                    {enrollmentRequest?.user?.attributes?.eduPersonAssurance && Array.isArray(enrollmentRequest?.user?.attributes?.eduPersonAssurance) ?
                      enrollmentRequest?.user?.attributes?.eduPersonAssurance.map((value, index) => {
                        return <Badge key={index} className="gm_role_badge" isRead>{value}</Badge>
                      })
                      : enrollmentRequest?.user?.attributes?.eduPersonAssurance ?
                        <Badge key={'single'} className="gm_role_badge" isRead>{enrollmentRequest?.user?.attributes?.eduPersonAssurance}</Badge>
                        : <Msg msgKey='notAvailable' />
                    }
                  </div>
                </FormGroup> */}
                <FormGroup
                  label={Msg.localize('enrollmentIdentityProvidersLabel')}
                  fieldId="simple-form-name-04"
                >
                  <div>{enrollmentRequest?.user?.federatedIdentities && enrollmentRequest?.user?.federatedIdentities.length > 0 ? enrollmentRequest?.user?.federatedIdentities.map((federatedIdentity, index) => {
                    return <Badge key={'single'} className="gm_role_badge" isRead>{federatedIdentity.identityProvider}</Badge>
                  }) :
                    <Msg msgKey='none' />}</div>
                </FormGroup>
              </ExpandableSection>
            }
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
              {enrollmentRequest?.groupEnrollmentConfiguration?.groupRoles.map((role, index) => {
                return <Badge key={index} className="gm_role_badge" isRead>{role}</Badge>
              })}
            </FormGroup>
            <FormGroup
              label={Msg.localize('enrollmentAUPLabel')}
              fieldId="simple-form-name-08"
            >
              <div>{enrollmentRequest?.groupEnrollmentConfiguration?.aup?.url ?
                <Button variant="link" className="gm_button-enrollment-link" icon={<ExternalLinkSquareAltIcon />} iconPosition="right">
                  <a href={enrollmentRequest?.groupEnrollmentConfiguration?.aup?.url} target="_blank" rel="noreferrer">link</a>
                </Button>
                : <Msg msgKey='notAvailable' />} </div>
            </FormGroup>
            <FormGroup
              label={Msg.localize('enrollmentExpirationLabel')}
              fieldId="simple-form-name-09"
              labelIcon={
                <Popover
                  bodyContent={
                    <div>
                      <Msg msgKey='membershipExpiresAtHelperText' />
                      {!props.managePage &&
                        <>
                          <Msg msgKey='membershipExpiresAtMemberHelperText' /> <a onClick={() => { props.history.push('/groups/showgroups'); }}>My Groups</a>  page.
                        </>}
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
              <div>{enrollmentRequest?.groupEnrollmentConfiguration?.membershipExpirationDays ? enrollmentRequest?.groupEnrollmentConfiguration?.membershipExpirationDays : Msg.localize('reviewEnrollmentMembershipNoExpiration')}</div>
            </FormGroup>
            {
              enrollmentRequest?.groupEnrollmentConfiguration?.commentsNeeded &&
              <FormGroup
                label={(enrollmentRequest?.groupEnrollmentConfiguration?.commentsLabel || Msg.localize('enrollmentUserCommentLabel')) + ":"}
                fieldId="simple-form-name-10"
              >
                <div>{enrollmentRequest?.comments}</div>
              </FormGroup>
            }
          </FormFieldGroup>
          {enrollmentRequest?.status === 'PENDING_APPROVAL' && props.managePage &&
            <FormFieldGroup
              header={
                <FormFieldGroupHeader
                  titleText={{ text: Msg.localize('enrollmentReviewResponseLabel'), id: 'field-group4-non-expandable-titleText-id' }}
                />
              }
            >
              <FormGroup
                label={Msg.localize('enrollmentReviewerCommentLabel')}
                fieldId="simple-form-name-11"
              >
                <TextArea
                  className="gm_form-input"
                  type="text"
                  id="simple-form-name-12"
                  name="simple-form-name-12"
                  aria-describedby="simple-form-name-01-helper"
                  value={reviewerComment}
                  onChange={(value) => { setReviewerComment(value); }}
                />
              </FormGroup>
            </FormFieldGroup>
          }
        </Form>
      </Modal>
    </React.Fragment>
  )
}

