import * as React from "react";
import { FC, useState, useEffect } from "react";
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
  AlertVariant,
} from "@patternfly/react-core";

import {
  CopyIcon,
  ExternalLinkSquareAltIcon,
  HelpIcon,
} from "@patternfly/react-icons";
import { Popover, List, ListItem } from "@patternfly/react-core";
import { useLoader } from "../widgets/LoaderContext.js";
import { useGroupsService } from "../groups-service/GroupsServiceContext.js";
import { HttpResponse } from "../groups-service/groups-service.js";
import { dateParse, formatDateToString } from "../widgets/Date.js";
import { getError, kcPath } from "../js/utils.js";
import { useTranslation } from "react-i18next";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useNavigate } from "react-router-dom";

export const EnrollmentRequest: FC<any> = (props) => {
  const { startLoader, stopLoader } = useLoader();
  const [enrollmentRequest, setEnrollmentRequest] = useState<any>({});
  const [membership, setMembership] = useState<any>(null); // <-- Add this line
  const [copyTooltip, setCopyTooltip] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const groupsService = useGroupsService();
  const [reviewerComment, setReviewerComment] = useState("");
  const [expandUserDetails, setExpandUserDetails] = useState(false);
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();

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
          .catch(() => {
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

  let reviewEnrollmentRequest = (action: any) => {
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
          addAlert(t("reviewEnrollmentSuccess"), AlertVariant.success);
        } else {
          addError("reviewEnrollmentError", getError(response));
        }
      })
      .catch((err) => {
        stopLoader();
        props.refresh();
        const response = err?.response ?? err;
        addError("reviewEnrollmentError", getError(response));
        console.log(err);
      });
  };

  let close = () => {
    setExpandUserDetails(false);
    setReviewerComment("");
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
    newExpirationDate = t("never");
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
              props.managePage
                ? membership
                  ? t("reviewUpdateMembershipRequestTitle")
                  : t("reviewEnrollmentRequestTitle")
                : membership && enrollmentRequest?.status === "PENDING_APPROVAL"
                ? t("viewUpdateMembershipRequestTitle")
                : t("viewEnrollmentRequestTitle")}
              {props.managePage && (
                <Tooltip
                  {...(!!copyTooltip
                    ? { trigger: "manual", isVisible: true }
                    : { trigger: "mouseenter" })}
                  content={
                    <div>
                      {copyTooltip ? t("copiedTooltip") : t("copyTooltip")}
                    </div>
                  }
                >
                  <Button
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
                <Tooltip content={<div>{t("approveRequestTooltip")}</div>}>
                  <div>
                    <Button
                      key="confirm"
                      className="gm_green-button gm_button-spacer"
                      onClick={() => {
                        reviewEnrollmentRequest("accept");
                      }}
                    >
                      {t("Approve")}
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
                  {t("Deny")}
                </Button>,
              ]
            : [
                <Button
                  key="back"
                  className="gm_grey-button"
                  variant="primary"
                  onClick={() => close()}
                >
                  {t("Back")}
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
                {t("reviewAlertSubmitted")}
                <span className="gm_normal-text">
                  {" "}
                  {enrollmentRequest?.submittedDate}
                </span>
              </p>
              {enrollmentRequest?.approvedDate && (
                <p className="gm_margin-top-1rem">
                  {t("reviewAlertStatus")}:{" "}
                  <span className="gm_normal-text">
                    {enrollmentRequest?.approvedDate}
                  </span>
                </p>
              )}
              {!enrollmentRequest?.approvedDate && (
                <p className="gm_margin-top-1rem">
                  {t("reviewAlertStatus")}:{" "}
                  <span className="gm_normal-text">
                    {t(enrollmentRequest?.status)}
                  </span>
                </p>
              )}
              {enrollmentRequest?.checkAdmin && (
                <p className="gm_margin-top-1rem">
                  {t("reviewAlertReviewer")}{" "}
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
                  {t("reviewAlertComment")}{" "}
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
              {t("userDetails")}
              <Popover
                headerContent={<div>{t("enrollmentUserDetails")}</div>}
                bodyContent={undefined}
              >
                <button
                  type="button"
                  aria-label="More info for name field"
                  onClick={(e) => e.preventDefault()}
                  aria-describedby="simple-form-name-02"
                  className="pf-c-form__group-label-help"
                >
                  <HelpIcon />
                </button>
              </Popover>
            </div>
            <FormGroup
              label={t("username") + ":"}
              fieldId="simple-form-name-02"
            >
              <div>
                {enrollmentRequest?.userIdentifier
                  ? enrollmentRequest?.userIdentifier
                  : t("notAvailable")}
              </div>
            </FormGroup>
            <FormGroup
              label={t("enrollmentEmailLabel")}
              fieldId="simple-form-name-02"
            >
              <div>
                {enrollmentRequest?.userEmail
                  ? enrollmentRequest?.userEmail
                  : t("notAvailable")}
              </div>
            </FormGroup>
            <FormGroup
              label={t("enrollmentFullNameLabel")}
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
              label={t("userAuthnAuthorityLabel") + ":"}
              fieldId="simple-form-name-03"
            >
              {enrollmentRequest?.userAuthnAuthorities &&
              Array.isArray(
                JSON.parse(enrollmentRequest?.userAuthnAuthorities)
              ) ? (
                <List>
                  {JSON.parse(enrollmentRequest.userAuthnAuthorities).map(
                    (value: any, index: number) => (
                      <ListItem
                        key={index}
                        style={{ marginLeft: `${index}rem` }}
                      >
                        {value.id}
                        {value.name &&
                          value.name !== value.id &&
                          ` - ${value.name}`}
                        {!value.name && value.name !== value.id && (
                          <> - {t("notAvailable")}</>
                        )}
                      </ListItem>
                    )
                  )}
                </List>
              ) : (
                t("notAvailable")
              )}
            </FormGroup>
            <FormGroup
              label={t("enrollmentAssuranceLabel")}
              fieldId="simple-form-name-03"
            >
              <DataList
                id="groups-list"
                aria-label={t("groupLabel")}
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
                  enrollmentRequest?.userAssurance.map(
                    (value: any, index: number) => {
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
                                  {t(value.replace(":", "")) !=
                                  value.replace(":", "") ? (
                                    <div
                                      dangerouslySetInnerHTML={{
                                        __html: t(value.replace(":", "")),
                                      }}
                                    />
                                  ) : (
                                    t("notAvailable")
                                  )}
                                </DataListCell>,
                              ]}
                            />
                          </DataListItemRow>
                        </DataListItem>
                      );
                    }
                  )
                ) : (
                  <DataListItem key="emptyItem" aria-labelledby="empty-item">
                    <DataListItemRow key="emptyRow">
                      <DataListItemCells
                        dataListCells={[
                          <DataListCell key="empty">
                            {t("noAssurance")}
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
                  label={t("enrollmentEmailLabel")}
                  fieldId="email-field"
                >
                  <div>{enrollmentRequest?.user?.email}</div>
                </FormGroup>
                <FormGroup
                  label={t("enrollmentFullNameLabel")}
                  fieldId="full-name-field"
                >
                  <div>
                    {enrollmentRequest?.user?.firstName +
                      " " +
                      enrollmentRequest?.user?.lastName}
                  </div>
                </FormGroup>
                <FormGroup
                  label={t("uid") + ""}
                  fieldId="user-preferred-username"
                >
                  <div>
                    {enrollmentRequest?.user?.attributes?.uid?.[0] ||
                      t("notAvailable")}
                  </div>
                </FormGroup>
                <FormGroup
                  label={t("enrollmentIdentityProvidersLabel") + ":"}
                  fieldId="simple-form-name-04"
                >
                  <div>
                    {enrollmentRequest?.user?.federatedIdentities &&
                    enrollmentRequest?.user?.federatedIdentities.length > 0
                      ? enrollmentRequest?.user?.federatedIdentities.map(
                          (federatedIdentity: any) => {
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
                      : t("none")}
                  </div>
                </FormGroup>
              </ExpandableSection>
            )}
          </FormFieldGroup>
          <FormFieldGroup
            header={
              <FormFieldGroupHeader
                titleText={{
                  text: t("enrollmentMembershipTitle"),
                  id: "field-group4-non-expandable-titleText-id",
                }}
              />
            }
          >
            <FormGroup
              label={t("enrollmentGroupNameLabel")}
              fieldId="simple-form-name-05"
            >
              <div>
                {enrollmentRequest?.groupEnrollmentConfiguration?.group?.name}
              </div>
            </FormGroup>

            {membership?.validFrom &&
              enrollmentRequest.status === "PENDING_APPROVAL" && (
                <FormGroup
                  label={t("memberSince")}
                  fieldId="simple-form-member-since"
                >
                  <div>{formatDate(dateParse(membership.validFrom))}</div>
                </FormGroup>
              )}

            <FormGroup
              label={t("enrollmentEnrollmentNameLabel")}
              fieldId="simple-form-name-06"
            >
              <div>{enrollmentRequest?.groupEnrollmentConfiguration?.name}</div>
            </FormGroup>
            <FormGroup
              label={t("enrollmentGroupRolesLabel")}
              fieldId="simple-form-name-07"
            >
              {/* Roles the user will have after the request */}
              <div style={{ marginBottom: 8 }}>
                {enrollmentRequest?.groupRoles?.length > 0 ? (
                  enrollmentRequest.groupRoles.map(
                    (role: any, index: number) => (
                      <Badge key={index} className="gm_role_badge" isRead>
                        {role}
                      </Badge>
                    )
                  )
                ) : (
                  <span>{t("noGroupRolesRequested")}</span>
                )}
              </div>

              {/* Roles the user will lose */}
              {membership?.groupRoles &&
                enrollmentRequest.status === "PENDING_APPROVAL" &&
                Array.isArray(membership.groupRoles) &&
                (() => {
                  const rolesToBeLost = membership.groupRoles.filter(
                    (role: any) =>
                      !enrollmentRequest?.groupRoles?.includes(role)
                  );
                  return rolesToBeLost.length > 0 ? (
                    <HelperText className="gm_enrollment-lost-roles-warning">
                      <HelperTextItem variant="warning" hasIcon>
                        {t("groupRolesWillBeLostWarningRequest")}{" "}
                        {rolesToBeLost.map((role: string) => (
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
              label={t("enrollmentAUPLabel")}
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
                  t("notAvailable")
                )}{" "}
              </div>
            </FormGroup>
            {!membership || enrollmentRequest.status !== "PENDING_APPROVAL" ? (
              <FormGroup
                label={t("enrollmentExpirationLabel")}
                fieldId="simple-form-name-09"
                labelIcon={
                  <Popover
                    bodyContent={
                      <div>
                        {t("membershipExpiresAtHelperText")}
                        {!props.managePage && (
                          <>
                            {t("membershipExpiresAtMemberHelperText")}{" "}
                            <a
                              onClick={() => {
                                navigate(kcPath("/groups/showgroups"));
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
                      <HelpIcon />
                    </button>
                  </Popover>
                }
              >
                <div>
                  {enrollmentRequest?.groupEnrollmentConfiguration
                    ?.membershipExpirationDays
                    ? enrollmentRequest?.groupEnrollmentConfiguration
                        ?.membershipExpirationDays
                    : t("reviewEnrollmentMembershipNoExpiration")}
                </div>
              </FormGroup>
            ) : (
              <FormGroup
                label={t("newMembershipExpirationRequest")}
                fieldId="simple-form-name-09"
              >
                <div>
                  <strong>{newExpirationDate || t("never")}</strong>
                  <br />
                  <HelperText>
                    {expirationChangeType === "extend" && (
                      <HelperTextItem variant="success" hasIcon={false}>
                        {t("membershipExpirationExtendedMsgRequest")}{" "}
                        <strong>
                          {daysDiff !== null ? daysDiff : 0} {t("days")}
                        </strong>
                      </HelperTextItem>
                    )}
                    {expirationChangeType === "reduce" && (
                      <HelperTextItem variant="warning" hasIcon>
                        {t("membershipExpirationReducedMsgRequest")}{" "}
                        <strong>
                          {daysDiff !== null ? Math.abs(daysDiff) : 0}{" "}
                          {t("days")}
                        </strong>
                      </HelperTextItem>
                    )}
                    {expirationChangeType === "same" && (
                      <HelperTextItem>
                        {t("membershipExpirationUnchangedMsgRequest")}
                      </HelperTextItem>
                    )}
                    {expirationChangeType === "tofinite" && (
                      <HelperTextItem variant="warning" hasIcon>
                        {t("membershipExpirationNowFiniteMsgRequest")}
                      </HelperTextItem>
                    )}
                    {expirationChangeType === "toinfinite" && (
                      <HelperTextItem variant="success" hasIcon={false}>
                        {t("membershipExpirationNowInfiniteMsgRequest")}
                      </HelperTextItem>
                    )}
                    {expirationChangeType === "infinite" && (
                      <HelperTextItem>
                        {t("membershipExpirationInfiniteMsgRequest")}
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
                    ?.commentsLabel || t("enrollmentUserCommentLabel")) + ":"
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
                      text: t("enrollmentReviewResponseLabel"),
                      id: "field-group4-non-expandable-titleText-id",
                    }}
                  />
                }
              >
                <FormGroup
                  label={t("enrollmentReviewerCommentLabel")}
                  fieldId="simple-form-name-11"
                >
                  <TextArea
                    className="gm_form-input"
                    type="text"
                    id="simple-form-name-12"
                    name="simple-form-name-12"
                    aria-describedby="simple-form-name-01-helper"
                    value={reviewerComment}
                    onChange={(_event, value) => {
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
