import * as React from "react";
import { FC, useState, useEffect, useCallback, useMemo, useRef } from "react";
import {
  Button,
  Tooltip,
  SelectVariant,
  Checkbox,
  Select,
  SelectOption,
  Alert,
  Form,
  FormGroup,
  Breadcrumb,
  BreadcrumbItem,
  TextArea,
  HelperText,
  HelperTextItem,
} from "@patternfly/react-core";
// @ts-ignore
import {
  HttpResponse,
  GroupsServiceClient,
} from "../../groups-mngnt-service/groups.service";
// @ts-ignore
import { ConfirmationModal } from "../Modals";
import { dateParse, formatDateToString, isFutureDate } from "../../js/utils.js";
import { Msg } from "../../widgets/Msg";
// @ts-ignore
import { ContentPage } from "../../content/ContentPage";
import { GroupRolesTable } from "../GroupRolesTable";
import { Link } from "react-router-dom";
import { useLoader } from "../LoaderContext";
import { start } from "repl";

const reg_url = /^(https?|chrome):\/\/[^\s$.?#].[^\s]*$/;

export const CreateEnrollment: FC<any> = (props) => {
  const touchDefault = {
    comments: false,
    groupRoles: false,
  };
  const [errors, setErrors] = useState<any>({});
  const [modalInfo, setModalInfo] = useState({});
  const [touched, setTouched] = useState<any>(touchDefault);
  const [enrollments, setEnrollments] = useState<any>([]);
  const [selected, setSelected] = useState("");
  const [group, setGroup] = useState<any>({});
  const [isOpen, setIsOpen] = useState(false);
  const [enrollment, setEnrollment] = useState<any>({});
  const [acceptAup, setAcceptAup] = useState(false);
  const [openRequest, setOpenRequest] = useState(false);
  const [defaultId, setDefaultId] = useState("");
  const [enrollmentRequest, setEnrollmentRequest] = useState<{
    groupEnrollmentConfiguration: { id: string };
    groupRoles: string[];
    comments: string;
  }>({
    groupEnrollmentConfiguration: { id: "" },
    groupRoles: [],
    comments: "",
  });
  const [isParentGroup, setIsParentGroup] = useState<boolean>(false);
  const [user, setUser] = useState<any>(null);
  const [membership, setMembership] = useState<any>(null);
  const { startLoader, stopLoader } = useLoader();
  const activeRequests = useRef(0); // Tracks the number of active requests
  const [newExpirationDate, setNewExpirationDate] = useState<string | null>();
  const [expirationChangeType, setExpirationChangeType] = useState<
    "extend" | "reduce" | "same" | "infinite" | "tofinite" | null
  >(null);
  const [daysDiff, setDaysDiff] = useState<number | null>(null);

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

  // Add this function to fetch user info (including username)
  const fetchAccountInfo = () => {
    startLoaderWithTracking();
    return groupsService!
      .doGet<any>("/", { target: "base_account" })
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setUser(response.data);
          return response.data;
        }
      })
      .catch((err) => {
        console.log(err);
        return null;
      })
      .finally(() => {
        stopLoaderWithTracking();
      });
  };

  // Fetch group membership for the user
  const fetchMembership = (groupId: string, username: string) => {
    startLoaderWithTracking();
    return groupsService!
      .doGet<any>(`/user/group/${groupId}/member`)
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          
          setMembership(response.data);
        } else {
          setMembership(null);
        }
      })
      .catch((err) => {
        setMembership(null);
      })
      .finally(() => {
        stopLoaderWithTracking();
      });
  };

  const fetchGroupEnrollment = async (id) => {
    try {
      startLoaderWithTracking();
      const response = await groupsService!.doGet<any>(
        `/user/configuration/${id}`
      );
      if (response.status === 200 && response.data) {
        return response.data;
      }
    } catch (error) {
      console.error("Error fetching group enrollment:", error);
    } finally {
      stopLoaderWithTracking();
    }
  };

  const fetchGroupEnrollments = async (groupPath) => {
    try {
      startLoaderWithTracking();
      const response = await groupsService!.doGet<any>(
        "/user/groups/configurations",
        { params: { groupPath } }
      );
      if (response.status === 200 && response.data) {
        return response.data;
      }
    } catch (error) {
      console.error("Error fetching group enrollments:", error);
    } finally {
      stopLoaderWithTracking();
    }
  };

  const fetchGroupEnrollmentRequests = async (groupId) => {
    try {
      startLoaderWithTracking();
      const response = await groupsService!.doGet<any>(
        "/user/enroll-requests",
        { params: { groupId } }
      );
      if (response.status === 200 && response.data) {
        return response.data.results;
      }
    } catch (error) {
      console.error("Error fetching group enrollment requests:", error);
    } finally {
      stopLoaderWithTracking();
    }
  };

  // Consolidate useEffect hooks
  useEffect(() => {
    const initializeEnrollment = async () => {
      const query = new URLSearchParams(props.location.search);
      const groupPath = decodeURI(query.get("groupPath") || "");
      const id = decodeURI(query.get("id") || "");

      if (id) {
        const enrollmentData = await fetchGroupEnrollment(id);
        if (enrollmentData) {
          setGroup(enrollmentData.group);
          setEnrollments([enrollmentData]);
          setIsParentGroup(enrollmentData.group?.path?.split("/").length === 2);
        }
      } else if (groupPath) {
        const enrollmentsData = await fetchGroupEnrollments(groupPath);
        if (enrollmentsData?.length > 0) {
          const defaultConfig =
            enrollmentsData[0].group?.attributes?.defaultConfiguration?.[0];
          setDefaultId(defaultConfig || "");
          setGroup(enrollmentsData[0].group);
          setEnrollments(enrollmentsData);
          setIsParentGroup(
            enrollmentsData[0].group?.path?.split("/").length === 2
          );
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
            (request) =>
              request.status === "PENDING_APPROVAL" ||
              request.status === "WAITING_FOR_REPLY"
          );
          setOpenRequest(hasOpenRequest);
        }
      }
    };
    if (group?.id) {
      fetchAccountInfo().then((userData) => {
        if (userData && userData.username) {
          fetchMembership(group.id, userData.username);
        }
      });
    } else {
      setMembership(null);
    }

    fetchRequests();
  }, [group]);

  useEffect(() => {
    if (
      !enrollments ||
      enrollments.length === 0 ||
      Object.keys(enrollment).length !== 0 ||
      activeRequests.current !== 0
    )
      return;
    const preselectedEnrollment = enrollments.find(
      (enrollment) => enrollment.id === defaultId
    )
      ? enrollments.find((enrollment) => enrollment.id === defaultId)
      : enrollments[0];
    setSelected(preselectedEnrollment.name);
    // Set the enrollment request with the default enrollment configuration
    setEnrollmentRequest((prev) => ({
      ...prev,
      groupEnrollmentConfiguration: { id: preselectedEnrollment.id },
      groupRoles:
        preselectedEnrollment?.groupRoles.filter((role) =>
          membership?.groupRoles.includes(role)
        ) || [],
    }));
    setEnrollment(preselectedEnrollment);
  }, [enrollments, defaultId, membership, activeRequests.current]);

  useEffect(() => {
    calclulateMembershipExpirationAndType();
  }, [enrollment,user,membership,group]);
  // useEffect(() => {
  //   if (enrollments.length === 1) {
  //     const singleEnrollment = enrollments[0];
  //     setEnrollmentRequest((prev) => ({
  //       ...prev,
  //       groupEnrollmentConfiguration: { id: singleEnrollment.id },
  //       groupRoles:
  //         singleEnrollment?.groupRoles.filter((role) =>
  //           membership?.groupRoles.includes(role)
  //         ) || [],
  //     }));
  //     setEnrollment(singleEnrollment);
  //   }
  // }, [enrollments]);

  useEffect(() => {
    validateEnrollmentRequest();
  }, [enrollmentRequest]);

  const createEnrollmentRequest = (requiresApproval) => {
    startLoader();
    groupsService!
      .doPost<any>("/user/enroll-request", { ...enrollmentRequest })
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          if (requiresApproval) {
            props.history.push("/groups/mygroupenrollments");
          } else {
            props.history.push("/groups/showgroups");
          }
        }
      })
      .catch((err) => {
        stopLoader();
        console.log(err);
      });
  };

  const validateEnrollmentRequest = () => {
    let errors: Record<string, string> = {};
    !enrollmentRequest?.comments &&
      enrollment.commentsNeeded &&
      (errors.comments = Msg.localize("requredFormError"));
    !(enrollmentRequest?.groupRoles?.length > 0) &&
      (errors.groupRoles = Msg.localize("groupRolesFormError"));
    !enrollment.multiselectRole &&
      enrollmentRequest.groupRoles.length > 1 &&
      (errors.groupRoles = Msg.localize("groupRolesFormErrorMulitple"));
    setErrors(errors);
  };

  const touchFields = () => {
    for (const property in touched) {
      touched[property] = true;
    }
    setTouched({ ...touched });
  };

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

  // Helper to format date as string
  const formatDate = (date: Date) => formatDateToString(date);
  const calclulateMembershipExpirationAndType = () => {
    try {
        // If existing membership and enrollment is finite
        if (
          membership &&
          enrollment?.membershipExpirationDays &&
          !isFutureDate(dateParse(membership.validFrom))
        ) {
          // Existing member: new expiration is today + configured duration
          const today = new Date();
          const newExp = new Date(today);
          newExp.setDate(
            today.getDate() + parseInt(enrollment.membershipExpirationDays)
          );
          setNewExpirationDate(formatDate(newExp));
          if (membership.membershipExpiresAt) {
            const currentExp = new Date(membership.membershipExpiresAt);
            let days = Math.floor(
              (newExp.getTime() - currentExp.getTime()) / (1000 * 60 * 60 * 24)
            );
            setDaysDiff(days);
            if (days > 0) setExpirationChangeType("extend");
            else if (days < 0) setExpirationChangeType("reduce");
            else setExpirationChangeType("same");
          } else {
            // Was infinite, now will expire
            setExpirationChangeType("tofinite");
          }
        }
        // Existing member, new enrollment is infinite 
        else if (
          membership &&
          enrollment &&
          !enrollment.membershipExpirationDays
        ) {
          // Infinite membership
          setNewExpirationDate(Msg.localize("never"));
          // If current is also infinite, mark as 'same'
          if (!membership.membershipExpiresAt) {
            setExpirationChangeType("same");
          } else {
            setExpirationChangeType("infinite");
          }
        } else if (
          !membership &&
          enrollment &&
          enrollment.validFrom &&
          isFutureDate(dateParse(enrollment.validFrom))
        ) {
          // New membership with future validFrom
          setNewExpirationDate(null); // Will be handled in the alert below
        }
    } catch (error) {
      console.error("Error calculating membership expiration:", error);
    }

    // Scheduled membership, user selects enrollment with no future start
  };

  const rolesToBeLost = (membership?.groupRoles || []).filter(
    (role) => !enrollmentRequest.groupRoles.includes(role)
  );

  return (
    <React.Fragment>
      <div className="gm_content">
        <Breadcrumb className="gm_breadcumb">
          <BreadcrumbItem to="#">
            <Msg msgKey="accountConsole" />
          </BreadcrumbItem>
          <BreadcrumbItem to="#/groups/showgroups">
            <Msg msgKey="groupLabel" />
          </BreadcrumbItem>
          {group?.path &&
            (() => {
              const pathParts = group.path.split("/").filter(Boolean);
              let accumulatedPath = "";
              return pathParts.map((part, idx) => {
                accumulatedPath += "/" + part;
                if (idx === pathParts.length - 1) {
                  return (
                    <BreadcrumbItem key={part} isActive>
                      {part}
                    </BreadcrumbItem>
                  );
                }
                return (
                  <BreadcrumbItem
                    key={part}
                    to={`#/enroll?groupPath=${encodeURIComponent(
                      accumulatedPath
                    )}`}
                  >
                    {part}
                  </BreadcrumbItem>
                );
              });
            })()}
        </Breadcrumb>
        <ConfirmationModal modalInfo={modalInfo} />
        <ContentPage
          title={
            !group?.name
              ? ""
              : membership
              ? Msg.localize("updateMembershipTo") + " " + group?.name
              : Msg.localize("requestMembershipTo") + " " + group?.name
          }
        >
          {group?.name && (
            <p className="gm_group_desc">
              {(group?.attributes?.description &&
                group?.attributes?.description[0]) ||
                Msg.localize("noDescription")}
            </p>
          )}
          <div className="gm_enrollment_container">
            <Form>
              {membership && isFutureDate(dateParse(membership.validFrom)) ? (
                <Alert
                  className="gm_content-width"
                  variant="warning"
                  title={Msg.localize("pendingMembershipExistsTitle")}
                >
                  <Msg msgKey="pendingMembershipExistsMessage" />
                </Alert>
              ) : !openRequest ? (
                <React.Fragment>
                  {enrollments && enrollments.length > 0 ? (
                    <React.Fragment>
                      <FormGroup
                        label={Msg.localize("groupEnrollment")}
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
                          <SelectOption
                            {...(enrollments.length !== 1 && {
                              ...{ key: "placeholder", isPlaceholder: true },
                            })}
                            value={Msg.localize(
                              "invitationEnrollmentSelectPlaceholder"
                            )}
                            onClick={() => {
                              setEnrollment({});
                            }}
                          />
                          {enrollments.map((enrollment, index) => {
                            return (
                              <SelectOption
                                {...(enrollments.length === 1 && {
                                  ...{
                                    key: "placeholder",
                                    isPlaceholder: true,
                                  },
                                })}
                                key={index}
                                value={enrollment?.name}
                                isDisabled={!enrollment.active}
                                onClick={() => {
                                  enrollmentRequest.groupEnrollmentConfiguration.id =
                                    enrollment.id;
                                  setEnrollmentRequest({
                                    ...enrollmentRequest,
                                    groupRoles:
                                      enrollment?.groupRoles.filter(
                                        (role: string) =>
                                          membership?.groupRoles.includes(role)
                                      ) || [],
                                  });
                                  setEnrollment(enrollment);
                                }}
                              />
                            );
                          })}
                        </Select>
                      </FormGroup>
                    </React.Fragment>
                  ) : (
                    <Alert
                      className="gm_content-width"
                      variant="warning"
                      title="This group has no available enrollments"
                    />
                  )}

                  {Object.keys(enrollment).length !== 0 ? (
                    <React.Fragment>
                      {membership ? (
                        <React.Fragment>
                          <FormGroup
                            label={
                              membership.validFrom &&
                              isFutureDate(dateParse(membership.validFrom))
                                ? Msg.localize("membershipStartingDate")
                                : Msg.localize("memberSince")
                            }
                            fieldId="simple-form-name-01"
                          >
                            {formatDate(dateParse(membership.validFrom))}
                          </FormGroup>
                          <Alert
                            variant={
                              expirationChangeType === "reduce" ||
                              expirationChangeType === "tofinite"
                                ? "warning"
                                : "info"
                            }
                            className="gm_content-width"
                            title={
                              expirationChangeType === "infinite"
                                ? Msg.localize(
                                    "membershipExpirationInfiniteTitle"
                                  )
                                : expirationChangeType === "extend"
                                ? Msg.localize(
                                    "membershipExpirationExtendedTitle"
                                  )
                                : expirationChangeType === "reduce"
                                ? Msg.localize(
                                    "membershipExpirationReducedTitle"
                                  )
                                : expirationChangeType === "tofinite"
                                ? Msg.localize(
                                    "membershipExpirationNowFiniteTitle"
                                  )
                                : Msg.localize(
                                    "membershipExpirationUnchangedTitle"
                                  )
                            }
                          >
                            <div>
                              <Msg msgKey="currentMembershipExpiration" />{" "}
                              <strong>
                                {membership.membershipExpiresAt
                                  ? formatDate(
                                      dateParse(membership.membershipExpiresAt)
                                    )
                                  : Msg.localize("never")}
                              </strong>
                              <br />
                              <Msg msgKey="newMembershipExpiration" />{" "}
                              <strong>
                                {newExpirationDate || Msg.localize("never")}
                              </strong>
                              <br />
                              {expirationChangeType === "extend" && (
                                <span>
                                  <Msg msgKey="membershipExpirationExtendedMsg" />{" "}
                                  <strong>
                                    {daysDiff !== null ? daysDiff : 0}{" "}
                                    {Msg.localize("days")}
                                  </strong>
                                </span>
                              )}
                              {expirationChangeType === "reduce" && (
                                <span>
                                  <Msg msgKey="membershipExpirationReducedMsg" />{" "}
                                  <strong>
                                    {daysDiff !== null ? Math.abs(daysDiff) : 0}{" "}
                                    {Msg.localize("days")}
                                  </strong>
                                </span>
                              )}
                              {expirationChangeType === "tofinite" && (
                                <span>
                                  <Msg msgKey="membershipExpirationNowFiniteMsg" />
                                </span>
                              )}
                              {expirationChangeType === "infinite" && (
                                <span>
                                  <Msg msgKey="membershipExpirationInfiniteMsg" />
                                </span>
                              )}
                              {expirationChangeType === "same" && (
                                <span>
                                  <Msg msgKey="membershipExpirationUnchangedMsg" />
                                </span>
                              )}
                            </div>
                          </Alert>
                        </React.Fragment>
                      ) : (
                        // For new memberships
                        <Alert
                          variant="info"
                          className="gm_content-width"
                          title={Msg.localize("membershipExpirationNewTitle")}
                        >
                          {enrollment.validFrom &&
                          isFutureDate(dateParse(enrollment.validFrom)) ? (
                            <span>
                              <Msg msgKey="membershipTakesEffectAt" />{" "}
                              <strong>
                                {formatDate(dateParse(enrollment.validFrom))}
                              </strong>
                              {enrollment.membershipExpirationDays && (
                                <>
                                  <br />
                                  <Msg msgKey="membershipExpiresAfter" />{" "}
                                  <strong>
                                    {enrollment.membershipExpirationDays}{" "}
                                    {Msg.localize("days")}
                                  </strong>
                                </>
                              )}
                            </span>
                          ) : enrollment.membershipExpirationDays ? (
                            <span>
                              <Msg msgKey="membershipExpiresAfter" />{" "}
                              <strong>
                                {enrollment.membershipExpirationDays}{" "}
                                {Msg.localize("days")}
                              </strong>
                            </span>
                          ) : (
                            <span>
                              <Msg msgKey="membershipExpirationInfiniteMsg" />
                            </span>
                          )}
                        </Alert>
                      )}
                      {/* <Alert variant="warning" className='gm_content-width' title={
                        ("The membership ") +
                        (enrollment.validFrom && isFutureDate(dateParse(enrollment.validFrom)) ? "will take effect at " + formatDateToString(dateParse(enrollment.validFrom)) : "") +
                        (enrollment.validFrom && isFutureDate(dateParse(enrollment.validFrom)) && parseInt(enrollment.membershipExpirationDays) > 0 ? " and it " : "") +
                        (parseInt(enrollment.membershipExpirationDays) > 0 ? "will expire on " + enrollment.membershipExpirationDays + " days after activation" : " does not have an expiration date.")}
                      /> */}
                      {!isParentGroup && (
                        <HelperText className="gm_helper-text-create-enrollment">
                          <HelperTextItem variant="warning" hasIcon>
                            <p>
                              <Msg msgKey="effectiveExpirationInfo" />
                            </p>
                          </HelperTextItem>
                        </HelperText>
                      )}
                      <FormGroup
                        label={Msg.localize("Select Your Group Role")}
                        isRequired
                        fieldId="simple-form-name-01"
                        helperTextInvalid={
                          touched.groupRoles && errors.groupRoles
                        }
                        onBlur={() => {
                          touched.groupRoles = true;
                          setTouched({ ...touched });
                        }}
                        validated={
                          errors.groupRoles && touched.groupRoles
                            ? "error"
                            : "default"
                        }
                      >
                        {/* Current roles display */}
                        {membership &&
                          membership.groupRoles &&
                          membership.groupRoles.length > 0 && (
                            <div style={{ marginBottom: 8 }}>
                              <Msg msgKey="currentGroupRolesLabel" />{" "}
                              {membership.groupRoles.map((role) => (
                                <span
                                  key={role}
                                  className="pf-c-badge pf-m-read"
                                  style={{ marginRight: 4 }}
                                >
                                  {role}
                                </span>
                              ))}
                            </div>
                          )}

                        {/* GroupRolesTable for selectable roles */}
                        <GroupRolesTable
                          groupRoles={enrollment.groupRoles}
                          selectedRoles={enrollmentRequest.groupRoles}
                          setSelectedRoles={(roles) => {
                            enrollmentRequest.groupRoles = roles;
                            setEnrollmentRequest({ ...enrollmentRequest });
                          }}
                        />

                        {/* Show read-only badges for roles the user will lose */}
                        {rolesToBeLost.length > 0 && (
                          <HelperText className="gm_enrollment-lost-roles-waring">
                            <HelperTextItem variant="warning" hasIcon>
                              <Msg msgKey="groupRolesWillBeLostWarning" />{" "}
                              {rolesToBeLost.map((role) => (
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
                        )}
                      </FormGroup>
                      {enrollment.commentsNeeded && (
                        <FormGroup
                          label={enrollment.commentsLabel}
                          isRequired
                          fieldId="simple-form-name-01"
                          helperTextInvalid={
                            touched.comments && errors.comments
                          }
                          validated={
                            errors.comments && touched.comments
                              ? "error"
                              : "default"
                          }
                        >
                          <TextArea
                            className="gm_form-input"
                            isRequired
                            type="text"
                            id="simple-form-name-01"
                            onBlur={() => {
                              touched.comments = true;
                              setTouched({ ...touched });
                            }}
                            name="simple-form-name-01"
                            aria-describedby="simple-form-name-01-helper"
                            value={enrollmentRequest.comments}
                            validated={
                              errors.comments && touched.comments
                                ? "error"
                                : "default"
                            }
                            onChange={(value) => {
                              enrollmentRequest.comments = value;
                              setEnrollmentRequest({ ...enrollmentRequest });
                            }}
                          />
                          <div className="gm_description-text">
                            {enrollment.commentsDescription}
                          </div>
                        </FormGroup>
                      )}
                      {enrollment?.aup?.url ? (
                        <>
                          <p>
                            <Msg msgKey="enrollmentFlowAupMessage1" />{" "}
                            <a
                              href={enrollment?.aup?.url}
                              target="_blank"
                              rel="noreferrer"
                            >
                              <Msg msgKey="invitationAUPMessage2" />
                            </a>{" "}
                            <Msg msgKey="invitationAUPMessage3" />
                          </p>
                          <div className="gm_checkbox-container gm_content-width">
                            <Checkbox
                              onClick={() => {
                                setAcceptAup(!acceptAup);
                              }}
                              checked={acceptAup}
                              id="description-check-1"
                              label={Msg.localize(
                                "enrollmentConfigurationAupMessage"
                              )}
                            />
                          </div>
                        </>
                      ) : (
                        ""
                      )}
                      <Alert
                        variant="info"
                        className="gm_content-width"
                        title={
                          enrollment?.requireApproval
                            ? membership
                              ? Msg.localize(
                                  "enrollmentUpdateRequiresApprovalAlert"
                                )
                              : Msg.localize("enrollmentRequiresApprovalAlert")
                            : membership
                            ? Msg.localize("enrollmentUpdateNoApprovalAlert")
                            : Msg.localize("enrollmentNoApprovalAlert")
                        }
                      />
                      <div>
                        <Tooltip
                          {...(!(enrollment?.aup?.url && !acceptAup)
                            ? { trigger: "manual", isVisible: false }
                            : { trigger: "mouseenter" })}
                          content={
                            <div>
                              <Msg msgKey="invitationAUPErrorMessage" />
                            </div>
                          }
                        >
                          <div className="gm_invitation-response-button-container">
                            <Button
                              isDisabled={enrollment?.aup?.url && !acceptAup}
                              onClick={() => {
                                touchFields();
                                if (Object.keys(errors).length !== 0) {
                                  setModalInfo({
                                    message: Msg.localize(
                                      "enrollmentConfigurationModalSubmitError"
                                    ),
                                    accept_message: Msg.localize("OK"),
                                    accept: function () {
                                      setModalInfo({});
                                    },
                                    cancel: function () {
                                      setModalInfo({});
                                    },
                                  });
                                } else {
                                  createEnrollmentRequest(
                                    enrollment?.requireApproval
                                  );
                                }
                              }}
                            >
                              Submit
                            </Button>
                          </div>
                        </Tooltip>
                      </div>
                    </React.Fragment>
                  ) : null}
                </React.Fragment>
              ) : (
                <Alert
                  className="gm_content-width"
                  variant="warning"
                  title={
                    membership
                      ? Msg.localize("enrollmentUpdateRequestExistsTitle")
                      : Msg.localize("enrollmentRequestExistsTitle")
                  }
                >
                  <p>
                    {membership ? (
                      <Msg msgKey="enrollmentUpdateRequestExistsMessage" />
                    ) : (
                      <Msg msgKey="enrollmentRequestExistsMessage" />
                    )}{" "}
                    <Link to={"/groups/mygroupenrollments"}>
                      "<Msg msgKey="viewMyEnrollmentRequests" />”
                    </Link>
                  </p>
                </Alert>
              )}
            </Form>
          </div>
        </ContentPage>
      </div>
    </React.Fragment>
  );
};
