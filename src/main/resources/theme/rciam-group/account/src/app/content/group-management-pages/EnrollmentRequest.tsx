import * as React from "react";
import { FC, useState, useEffect, useRef } from "react";
import {
  Button,
  Tooltip,
  Alert,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  FormFieldGroupHeader,
  FormFieldGroup,
  TextArea,
  Badge,
  ExpandableSection,
  DataList,
  DataListItem,
  DataListItemRow,
  DataListItemCells,
  DataListCell,
  HelperText,
  HelperTextItem,
} from "@patternfly/react-core";
// @ts-ignore
import {
  HttpResponse,
  GroupsServiceClient,
} from "../../groups-mngnt-service/groups.service";
import { Msg } from "../../widgets/Msg";
import {
  CopyIcon,
  ExternalLinkSquareAltIcon,
  HelpIcon,
} from "@patternfly/react-icons";
import { Popover, List, ListItem } from "@patternfly/react-core";
import { dateParse, formatDateToString, isFutureDate } from "../../js/utils.js";
import { getError } from "../../js/utils.js";
import { ContentAlert } from "../ContentAlert";
import { useLoader } from "../../group-widgets/LoaderContext";

export const EnrollmentRequest: FC<any> = (props) => {
  const { startLoader, stopLoader } = useLoader();
  const [enrollmentRequest, setEnrollmentRequest] = useState<any>({});
  const [membership, setMembership] = useState<any>(null); // <-- Add this line
  const [copyTooltip, setCopyTooltip] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  let groupsService = new GroupsServiceClient();
  const [reviewerComment, setReviewerComment] = useState("");
  const [expandUserDetails, setExpandUserDetails] = useState(false);

  useEffect(() => {
    if (Object.keys(props.enrollmentRequest).length !== 0) {
      setIsModalOpen(true);
      setEnrollmentRequest({ ...props.enrollmentRequest });
      // Fetch membership if username and groupId are available
      const username = props.enrollmentRequest?.userIdentifier;
      const groupId =
        props.enrollmentRequest?.groupEnrollmentConfiguration?.group?.id;
      if (username && groupId) {
        groupsService
          .doGet<any>(`/group-admin/group/${groupId}/members`, {
            params: { search: username },
          })
          .then((response: HttpResponse<any>) => {
            // Handle response with results array
            if (
              response.status === 200 &&
              response.data &&
              Array.isArray(response.data.results) &&
              response.data.results.length > 0
            ) {
              setMembership(response.data.results[0]);
            } else {
              setMembership(null);
            }
          })
          .catch((err) => {
            setMembership(null);
          });
      } else {
        setMembership(null);
      }
    } else {
      setIsModalOpen(false);
      setEnrollmentRequest({});
      setMembership(null);
    }
  }, [props.enrollmentRequest]);

  const disapearingTooltip = () => {
    setCopyTooltip(true);
    setTimeout(() => {
      setCopyTooltip(false);
    }, 2000);
  };

  let reviewEnrollmentRequest = (action) => {
    close();
    startLoader();
    groupsService!
      .doPost<any>(
        "/group-admin/enroll-request/" + enrollmentRequest.id + "/" + action,
        {},
        {
          params: {
            ...(reviewerComment ? { adminJustification: reviewerComment } : {}),
          },
        }
      )
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          props.refresh();
          ContentAlert.success(Msg.localize("reviewEnrollmentSuccess"));
        } else {
          ContentAlert.danger(
            Msg.localize("reviewEnrollmentError") + " " + getError(response)
          );
        }
      })
      .catch((err) => {
        console.log(err);
      });
  };

  let close = () => {
    setExpandUserDetails(false);
    props.close();
  };

  // Helper to format date as string
  const formatDate = (date: Date) => formatDateToString(date);

  let newExpirationDate: string | null = null;
  let expirationChangeType:
    | "extend"
    | "reduce"
    | "same"
    | "infinite"
    | "tofinite"
    | "toinfinite"
    | null = null;
  let daysDiff: number | null = null;

  const membershipExpirationDays =
    enrollmentRequest?.groupEnrollmentConfiguration?.membershipExpirationDays;
  const currentExpiration = membership?.membershipExpiresAt
    ? dateParse(membership.membershipExpiresAt)
    : null;

  if (membership && membershipExpirationDays) {
    // Existing member: new expiration is now + configured duration
    const today = new Date();
    const newExp = new Date(today);
    newExp.setDate(today.getDate() + parseInt(membershipExpirationDays));
    newExpirationDate = formatDateToString(newExp);

    if (currentExpiration) {
      daysDiff = Math.floor(
        (newExp.getTime() - currentExpiration.getTime()) / (1000 * 60 * 60 * 24)
      );
      if (daysDiff > 0) expirationChangeType = "extend";
      else if (daysDiff < 0) expirationChangeType = "reduce";
      else expirationChangeType = "same";
    } else {
      expirationChangeType = "tofinite"; // Was infinite, now will expire
    }
  } else if (membership && !membershipExpirationDays) {
    // Infinite membership
    newExpirationDate = Msg.localize("never");
    if (currentExpiration) {
      expirationChangeType = "toinfinite"; // Was finite, now infinite
    } else {
      expirationChangeType = "infinite";
    }
  }

  return (
    <React.Fragment>
      <Modal
        variant={ModalVariant.large}
        header={
          <React.Fragment>
            <h1 className="pf-c-modal-box__title gm_flex-center">
              {enrollmentRequest?.status === "PENDING_APPROVAL" &&
              props.managePage ? (
                membership ? (
                  <Msg msgKey="reviewUpdateMembershipRequestTitle" />
                ) : (
                  <Msg msgKey="reviewEnrollmentRequestTitle" />
                )
              ) : membership && enrollmentRequest?.status === "PENDING_APPROVAL" ? (
                <Msg msgKey="viewUpdateMembershipRequestTitle" />
              ) : (
                <Msg msgKey="viewEnrollmentRequestTitle" />
              )}
              {props.managePage && (
                <Tooltip
                  {...(!!copyTooltip
                    ? { trigger: "manual", isVisible: true }
                    : { trigger: "mouseenter" })}
                  content={
                    <div>
                      {copyTooltip ? (
                        <Msg msgKey="copiedTooltip" />
                      ) : (
                        <Msg msgKey="copyTooltip" />
                      )}
                    </div>
                  }
                >
                  <Button
                    isSmall
                    className={"gm_grey-button gm_title-button"}
                    onClick={() => {
                      disapearingTooltip();
                      let link =
                        groupsService.getBaseUrl() +
                        "/account/#/groups/groupenrollments?id=" +
                        encodeURI(enrollmentRequest?.id);
                      navigator.clipboard.writeText(link);
                    }}
                  >
                    <CopyIcon />{" "}
                  </Button>
                </Tooltip>
              )}
            </h1>
          </React.Fragment>
        }
        isOpen={isModalOpen}
        onClose={() => {
          close();
        }}
        actions={[
          ...(enrollmentRequest?.status === "PENDING_APPROVAL" &&
          props.managePage
            ? [
                <Tooltip
                  content={
                    <div>
                      <Msg msgKey={"approveRequestTooltip"} />
                    </div>
                  }
                >
                  <div>
                    <Button
                      key="confirm"
                      className="gm_green-button gm_button-spacer"
                      onClick={() => {
                        reviewEnrollmentRequest("accept");
                      }}
                    >
                      <Msg msgKey={"Approve"} />
                    </Button>
                  </div>
                </Tooltip>,
                <Button
                  key="cancel"
                  variant="danger"
                  onClick={() => {
                    reviewEnrollmentRequest("reject");
                  }}
                >
                  <Msg msgKey="Deny" />
                </Button>,
              ]
            : [
                <Button
                  key="back"
                  className="gm_grey-button"
                  variant="primary"
                  onClick={() => close()}
                >
                  <Msg msgKey="Back" />
                </Button>,
              ]),
        ]}
      >
        <Alert
          variant={
            enrollmentRequest?.status === "ACCEPTED"
              ? "success"
              : enrollmentRequest?.status === "REJECTED"
              ? "danger"
              : "info"
          }
          title={
            <React.Fragment>
              <p>
                <Msg msgKey="reviewAlertSubmitted" />
                <span className="gm_normal-text">
                  {" "}
                  {enrollmentRequest?.submittedDate}
                </span>
              </p>
              {enrollmentRequest?.approvedDate && (
                <p className="gm_margin-top-1rem">
                  <Msg msgKey={enrollmentRequest?.status} />:{" "}
                  <span className="gm_normal-text">
                    {enrollmentRequest?.approvedDate}
                  </span>
                </p>
              )}
              {!enrollmentRequest?.approvedDate && (
                <p className="gm_margin-top-1rem">
                  <Msg msgKey="reviewAlertStatus" />{" "}
                  <span className="gm_normal-text">
                    <Msg msgKey={enrollmentRequest?.status} />
                  </span>
                </p>
              )}
              {enrollmentRequest?.checkAdmin && (
                <p className="gm_margin-top-1rem">
                  <Msg msgKey="reviewAlertReviewer" />
                  <span className="gm_normal-text">
                    {enrollmentRequest?.checkAdmin?.firstName +
                      " " +
                      enrollmentRequest?.checkAdmin?.lastName +
                      " (" +
                      enrollmentRequest?.checkAdmin?.email +
                      ")"}
                  </span>
                </p>
              )}
              {enrollmentRequest?.adminJustification && (
                <p className="gm_margin-top-1rem">
                  <Msg msgKey="reviewAlertComment" />{" "}
                  <span className="gm_normal-text">
                    {enrollmentRequest?.adminJustification}
                  </span>
                </p>
              )}
            </React.Fragment>
          }
        />
        <Form className="gm_enrollment-request-view-form" isHorizontal>
          <FormFieldGroup>
            <div className="gm_form-field-group-title">
              <Msg msgKey="userDetails" />
              <Popover
                headerContent={
                  <div>
                    <Msg msgKey="enrollmentUserDetails" />
                  </div>
                }
                bodyContent={undefined}
              >
                <button
                  type="button"
                  aria-label="More info for name field"
                  onClick={(e) => e.preventDefault()}
                  aria-describedby="simple-form-name-02"
                  className="pf-c-form__group-label-help"
                >
                  <HelpIcon noVerticalAlign />
                </button>
              </Popover>
            </div>
            <FormGroup
              label={Msg.localize("username") + ":"}
              fieldId="simple-form-name-02"
            >
              <div>
                {enrollmentRequest?.userIdentifier
                  ? enrollmentRequest?.userIdentifier
                  : Msg.localize("notAvailable")}
              </div>
            </FormGroup>
            <FormGroup
              label={Msg.localize("enrollmentEmailLabel")}
              fieldId="simple-form-name-02"
            >
              <div>
                {enrollmentRequest?.userEmail
                  ? enrollmentRequest?.userEmail
                  : Msg.localize("notAvailable")}
              </div>
            </FormGroup>
            <FormGroup
              label={Msg.localize("enrollmentFullNameLabel")}
              fieldId="simple-form-name-01"
              // helperText=""
            >
              <div>
                {enrollmentRequest?.userFirstName ||
                enrollmentRequest?.userLastName
                  ? enrollmentRequest?.userFirstName +
                    " " +
                    enrollmentRequest?.userLastName
                  : "Not Available"}
              </div>
            </FormGroup>
            <FormGroup
              label={Msg.localize("userAuthnAuthorityLabel") + ":"}
              fieldId="simple-form-name-03"
            >
              {enrollmentRequest?.userAuthnAuthorities &&
              Array.isArray(
                JSON.parse(enrollmentRequest?.userAuthnAuthorities)
              ) ? (
                <List>
                  {JSON.parse(enrollmentRequest.userAuthnAuthorities).map(
                    (value, index) => (
                      <ListItem
                        key={index}
                        style={{ marginLeft: `${index}rem` }}
                      >
                        {value.id}
                        {value.name &&
                          value.name !== value.id &&
                          ` - ${value.name}`}
                        {!value.name && value.name !== value.id && (
                          <>
                            {" "}
                            - <Msg msgKey="notAvailable" />
                          </>
                        )}
                      </ListItem>
                    )
                  )}
                </List>
              ) : (
                <Msg msgKey="notAvailable" />
              )}
            </FormGroup>
            <FormGroup
              label={Msg.localize("enrollmentAssuranceLabel")}
              fieldId="simple-form-name-03"
            >
              <DataList
                id="groups-list"
                aria-label={Msg.localize("groupLabel")}
                isCompact
                wrapModifier={"breakWord"}
              >
                <DataListItem
                  id="groups-list-header"
                  aria-labelledby="Columns names"
                >
                  <DataListItemRow>
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell key="group-name-header" width={2}>
                          <strong>Value</strong>
                        </DataListCell>,
                        <DataListCell key="group-path-header" width={2}>
                          <strong>Description</strong>
                        </DataListCell>,
                      ]}
                    />
                  </DataListItemRow>
                </DataListItem>

                {enrollmentRequest?.userAssurance &&
                Array.isArray(enrollmentRequest.userAssurance) &&
                enrollmentRequest.userAssurance.length > 0 ? (
                  enrollmentRequest?.userAssurance.map((value, index) => {
                    return (
                      <DataListItem
                        id={`${index}-group`}
                        key={"group-" + index}
                        aria-labelledby="groups-list"
                      >
                        <DataListItemRow>
                          <DataListItemCells
                            dataListCells={[
                              <DataListCell
                                id={`${index}-value`}
                                width={2}
                                key={"value-" + index}
                              >
                                {value}
                              </DataListCell>,
                              <DataListCell
                                id={`${index}-description`}
                                width={2}
                                key={"path-" + index}
                              >
                                {Msg.localize(value.replace(":", "")) !=
                                value.replace(":", "") ? (
                                  <div
                                    dangerouslySetInnerHTML={{
                                      __html: Msg.localize(
                                        value.replace(":", "")
                                      ),
                                    }}
                                  />
                                ) : (
                                  <Msg msgKey="notAvailable" />
                                )}
                              </DataListCell>,
                            ]}
                          />
                        </DataListItemRow>
                      </DataListItem>
                    );
                  })
                ) : (
                  <DataListItem key="emptyItem" aria-labelledby="empty-item">
                    <DataListItemRow key="emptyRow">
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="empty">
                            <Msg msgKey="noAssurance" />
                          </DataListCell>,
                        ]}
                      />
                    </DataListItemRow>
                  </DataListItem>
                )}
              </DataList>
            </FormGroup>
            {props.managePage && (
              <ExpandableSection
                toggleText={
                  expandUserDetails
                    ? "Hide current user details"
                    : "Show current user details"
                }
                onToggle={() => {
                  setExpandUserDetails(!expandUserDetails);
                }}
                isExpanded={expandUserDetails}
              >
                <FormGroup
                  label={Msg.localize("enrollmentEmailLabel")}
                  fieldId="email-field"
                >
                  <div>{enrollmentRequest?.user?.email}</div>
                </FormGroup>
                <FormGroup
                  label={Msg.localize("enrollmentFullNameLabel")}
                  fieldId="full-name-field"
                >
                  <div>
                    {enrollmentRequest?.user?.firstName +
                      " " +
                      enrollmentRequest?.user?.lastName}
                  </div>
                </FormGroup>
                <FormGroup
                  label={Msg.localize("uid") + ""}
                  fieldId="user-preferred-username"
                >
                  <div>
                    {enrollmentRequest?.user?.attributes?.uid?.[0] || (
                      <Msg msgKey="notAvailable" />
                    )}
                  </div>
                </FormGroup>
                <FormGroup
                  label={Msg.localize("enrollmentIdentityProvidersLabel") + ":"}
                  fieldId="simple-form-name-04"
                >
                  <div>
                    {enrollmentRequest?.user?.federatedIdentities &&
                    enrollmentRequest?.user?.federatedIdentities.length > 0 ? (
                      enrollmentRequest?.user?.federatedIdentities.map(
                        (federatedIdentity, index) => {
                          return (
                            <Badge
                              key={"single"}
                              className="gm_role_badge"
                              isRead
                            >
                              {federatedIdentity.identityProvider}
                            </Badge>
                          );
                        }
                      )
                    ) : (
                      <Msg msgKey="none" />
                    )}
                  </div>
                </FormGroup>
              </ExpandableSection>
            )}
          </FormFieldGroup>
          <FormFieldGroup
            header={
              <FormFieldGroupHeader
                titleText={{
                  text: Msg.localize("enrollmentMembershipTitle"),
                  id: "field-group4-non-expandable-titleText-id",
                }}
              />
            }
          >
            <FormGroup
              label={Msg.localize("enrollmentGroupNameLabel")}
              fieldId="simple-form-name-05"
            >
              <div>
                {enrollmentRequest?.groupEnrollmentConfiguration?.group?.name}
              </div>
            </FormGroup>

            {membership?.validFrom && enrollmentRequest.status === "PENDING_APPROVAL" && (
              <FormGroup
                label={Msg.localize("memberSince")}
                fieldId="simple-form-member-since"
              >
                <div>{formatDate(dateParse(membership.validFrom))}</div>
              </FormGroup>
            )}

            <FormGroup
              label={Msg.localize("enrollmentEnrollmentNameLabel")}
              fieldId="simple-form-name-06"
            >
              <div>{enrollmentRequest?.groupEnrollmentConfiguration?.name}</div>
            </FormGroup>
            <FormGroup
              label={Msg.localize("enrollmentGroupRolesLabel")}
              fieldId="simple-form-name-07"
            >
              {/* Roles the user will have after the request */}
              <div style={{ marginBottom: 8 }}>
                {enrollmentRequest?.groupRoles?.length > 0 ? (
                  enrollmentRequest.groupRoles.map((role, index) => (
                    <Badge key={index} className="gm_role_badge" isRead>
                      {role}
                    </Badge>
                  ))
                ) : (
                  <span>
                    <Msg msgKey="noGroupRolesRequested" />
                  </span>
                )}
              </div>

              {/* Roles the user will lose */}
              {membership?.groupRoles && enrollmentRequest.status === "PENDING_APPROVAL"&&
                Array.isArray(membership.groupRoles) &&
                (() => {
                  const rolesToBeLost = membership.groupRoles.filter(
                    (role) => !enrollmentRequest?.groupRoles?.includes(role)
                  );
                  return rolesToBeLost.length > 0 ? (
                    <HelperText className="gm_enrollment-lost-roles-warning">
                      <HelperTextItem variant="warning" hasIcon>
                        <Msg msgKey="groupRolesWillBeLostWarningRequest" />{" "}
                        {rolesToBeLost.map((role, idx) => (
                          <span
                            key={role}
                            className="pf-c-badge pf-m-read"
                            style={{ marginRight: 4 }}
                          >
                            {role}
                          </span>
                        ))}
                      </HelperTextItem>
                    </HelperText>
                  ) : null;
                })()}
            </FormGroup>
            <FormGroup
              label={Msg.localize("enrollmentAUPLabel")}
              fieldId="simple-form-name-08"
            >
              <div>
                {enrollmentRequest?.groupEnrollmentConfiguration?.aup?.url ? (
                  <Button
                    variant="link"
                    className="gm_button-enrollment-link"
                    icon={<ExternalLinkSquareAltIcon />}
                    iconPosition="right"
                  >
                    <a
                      href={
                        enrollmentRequest?.groupEnrollmentConfiguration?.aup
                          ?.url
                      }
                      target="_blank"
                      rel="noreferrer"
                    >
                      link
                    </a>
                  </Button>
                ) : (
                  <Msg msgKey="notAvailable" />
                )}{" "}
              </div>
            </FormGroup>
            {!membership || enrollmentRequest.status !== "PENDING_APPROVAL" ? (
              <FormGroup
                label={Msg.localize("enrollmentExpirationLabel")}
                fieldId="simple-form-name-09"
                labelIcon={
                  <Popover
                    bodyContent={
                      <div>
                        <Msg msgKey="membershipExpiresAtHelperText" />
                        {!props.managePage && (
                          <>
                            <Msg msgKey="membershipExpiresAtMemberHelperText" />{" "}
                            <a
                              onClick={() => {
                                props.history.push("/groups/showgroups");
                              }}
                            >
                              My Groups
                            </a>{" "}
                            page.
                          </>
                        )}
                      </div>
                    }
                  >
                    <button
                      type="button"
                      aria-label="More info for name field"
                      onClick={(e) => e.preventDefault()}
                      aria-describedby="simple-form-name-01"
                      className="pf-c-form__group-label-help"
                    >
                      <HelpIcon noVerticalAlign />
                    </button>
                  </Popover>
                }
              >
                <div>
                  {enrollmentRequest?.groupEnrollmentConfiguration
                    ?.membershipExpirationDays
                    ? enrollmentRequest?.groupEnrollmentConfiguration
                        ?.membershipExpirationDays
                    : Msg.localize("reviewEnrollmentMembershipNoExpiration")}
                </div>
              </FormGroup>
            ) : (
              <FormGroup
                label={Msg.localize("newMembershipExpirationRequest")}
                fieldId="simple-form-name-09"
              >
                <div>
                  <strong>{newExpirationDate || Msg.localize("never")}</strong>
                  <br />
                  <HelperText>
                    {expirationChangeType === "extend" && (
                      <HelperTextItem variant="success" hasIcon={false}>
                        <Msg msgKey="membershipExpirationExtendedMsgRequest" />{" "}
                        <strong>
                          {daysDiff !== null ? daysDiff : 0}{" "}
                          {Msg.localize("days")}
                        </strong>
                      </HelperTextItem>
                    )}
                    {expirationChangeType === "reduce" && (
                      <HelperTextItem variant="warning" hasIcon>
                        <Msg msgKey="membershipExpirationReducedMsgRequest" />{" "}
                        <strong>
                          {daysDiff !== null ? Math.abs(daysDiff) : 0}{" "}
                          {Msg.localize("days")}
                        </strong>
                      </HelperTextItem>
                    )}
                    {expirationChangeType === "same" && (
                      <HelperTextItem>
                        <Msg msgKey="membershipExpirationUnchangedMsgRequest" />
                      </HelperTextItem>
                    )}
                    {expirationChangeType === "tofinite" && (
                      <HelperTextItem variant="warning" hasIcon>
                        <Msg msgKey="membershipExpirationNowFiniteMsgRequest" />
                      </HelperTextItem>
                    )}
                    {expirationChangeType === "toinfinite" && (
                      <HelperTextItem variant="success" hasIcon={false}>
                        <Msg msgKey="membershipExpirationNowInfiniteMsgRequest" />
                      </HelperTextItem>
                    )}
                    {expirationChangeType === "infinite" && (
                      <HelperTextItem>
                        <Msg msgKey="membershipExpirationInfiniteMsgRequest" />
                      </HelperTextItem>
                    )}
                  </HelperText>
                </div>
              </FormGroup>
            )}
            {enrollmentRequest?.groupEnrollmentConfiguration
              ?.commentsNeeded && (
              <FormGroup
                label={
                  (enrollmentRequest?.groupEnrollmentConfiguration
                    ?.commentsLabel ||
                    Msg.localize("enrollmentUserCommentLabel")) + ":"
                }
                fieldId="simple-form-name-10"
              >
                <div>{enrollmentRequest?.comments}</div>
              </FormGroup>
            )}
          </FormFieldGroup>
          {enrollmentRequest?.status === "PENDING_APPROVAL" &&
            props.managePage && (
              <FormFieldGroup
                header={
                  <FormFieldGroupHeader
                    titleText={{
                      text: Msg.localize("enrollmentReviewResponseLabel"),
                      id: "field-group4-non-expandable-titleText-id",
                    }}
                  />
                }
              >
                <FormGroup
                  label={Msg.localize("enrollmentReviewerCommentLabel")}
                  fieldId="simple-form-name-11"
                >
                  <TextArea
                    className="gm_form-input"
                    type="text"
                    id="simple-form-name-12"
                    name="simple-form-name-12"
                    aria-describedby="simple-form-name-01-helper"
                    value={reviewerComment}
                    onChange={(value) => {
                      setReviewerComment(value);
                    }}
                  />
                </FormGroup>
              </FormFieldGroup>
            )}
        </Form>
      </Modal>
    </React.Fragment>
  );
};
