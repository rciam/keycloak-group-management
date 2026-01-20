import * as React from "react";
import { FC, useState, useEffect } from "react";
import {
  Button,
  Tooltip,
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
  MenuToggle,
  SelectList,
} from "@patternfly/react-core";
import { dateParse, formatDateToString, isFutureDate } from "../widgets/Date";
import { GroupRolesTable } from "../widgets/GroupRolesTable";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useGroupsService } from "../groups-service/GroupsServiceContext";
import { useLoader } from "../widgets/LoaderContext";
import { HttpResponse } from "../groups-service/groups-service";
import { ConfirmationModal } from "../widgets/Modals";
import { kcPath } from "../js/utils";
import { Page } from "@keycloak/keycloak-account-ui";
import { FormErrorText } from "@keycloak/keycloak-ui-shared";


export const EnrollmentDiscovery: FC = () => {
  const groupsService = useGroupsService();
  const { t } = useTranslation();
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
  const [newExpirationDate, setNewExpirationDate] = useState<string | null>();
  const [expirationChangeType, setExpirationChangeType] = useState<
    "extend" | "reduce" | "same" | "infinite" | "tofinite" | null
  >(null);
  const [daysDiff, setDaysDiff] = useState<number | null>(null);

  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // Add this function to fetch user info (including username)
  const fetchAccountInfo = async () => {
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
      });
  };

  // Fetch group membership for the user
  const fetchMembership = async (groupId: string) => {
    return groupsService!
      .doGet<any>(`/user/group/${groupId}/member`)
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          return response.data;
        } else {
          return null;
        }
      })
      .catch(() => {
        return null;
      });
  };

  const fetchGroupEnrollment = async (id: any) => {
    try {
      const response = await groupsService!.doGet<any>(
        `/user/configuration/${id}`
      );
      if (response.status === 200 && response.data) {
        return response.data;
      }
    } catch (error) {
      console.error("Error fetching group enrollment:", error);
    }
  };

  const fetchGroupEnrollments = async (groupPath: string) => {
    try {
      const response = await groupsService!.doGet<any>(
        "/user/groups/configurations",
        { params: { groupPath } }
      );
      if (response.status === 200 && response.data) {
        return response.data;
      }
    } catch (error) {
      console.error("Error fetching group enrollments:", error);
    }
  };

  const fetchGroupEnrollmentRequests = async (groupId: string) => {
    try {
      const response = await groupsService!.doGet<any>(
        "/user/enroll-requests",
        { params: { groupId } }
      );
      if (response.status === 200 && response.data) {
        setOpenRequest(
          response?.data?.results?.some(
            (request: any) =>
              request.status === "PENDING_APPROVAL" ||
              request.status === "WAITING_FOR_REPLY"
          )
        );
      }
    } catch (error) {
      console.error("Error fetching group enrollment requests:", error);
    }
  };

  // Consolidate useEffect hooks
  useEffect(() => {
    const initializeEnrollment = async () => {
      startLoader();
      const groupPath = decodeURI(searchParams.get("groupPath") || "");
      const id = decodeURI(searchParams.get("id") || "");
      let groupId;
      let defaultId: any;
      let enrollmentData: any = {};
      if (id) {
        enrollmentData = await fetchGroupEnrollment(id);
        if (enrollmentData) {
          groupId = enrollmentData.group?.id;
          setGroup(enrollmentData.group);
          setEnrollments([enrollmentData]);
          setIsParentGroup(enrollmentData.group?.path?.split("/").length === 2);
        }
        enrollmentData = [enrollmentData];
      } else if (groupPath) {
        enrollmentData = await fetchGroupEnrollments(groupPath);
        if (enrollmentData?.length > 0) {
          groupId = enrollmentData[0].group?.id;
          defaultId =
            enrollmentData[0].group?.attributes?.defaultConfiguration?.[0];
          // setDefaultId(defaultId || "");
          setGroup(enrollmentData[0].group);
          setEnrollments(enrollmentData);
          setIsParentGroup(
            enrollmentData[0].group?.path?.split("/").length === 2
          );
        }
      }
      if (groupId) {
        fetchGroupEnrollmentRequests(groupId);
        fetchAccountInfo().then((userData) => {
          if (userData && userData.username) {
            fetchMembership(groupId).then(
              (membershipData) => {
                const preselectedEnrollment = enrollmentData.find(
                  (enrollment: any) => enrollment.id === defaultId
                )
                  ? enrollmentData.find(
                      (enrollment: any) => enrollment.id === defaultId
                    )
                  : enrollmentData[0];
                setSelected(preselectedEnrollment.name);
                // Set the enrollment request with the default enrollment configuration
                setEnrollmentRequest((prev) => ({
                  ...prev,
                  groupEnrollmentConfiguration: {
                    id: preselectedEnrollment.id,
                  },
                  groupRoles:
                    preselectedEnrollment?.groupRoles.filter((role: any) =>
                      membershipData?.groupRoles.includes(role)
                    ) || [],
                }));
                setEnrollment(preselectedEnrollment);
                setMembership(membershipData);
                stopLoader();
              }
            );
          }
        });
      } else {
        stopLoader();
      }
    };

    initializeEnrollment();
  }, [searchParams]);

  useEffect(() => {
    calclulateMembershipExpirationAndType();
  }, [enrollment, user, membership, group]);
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

  const createEnrollmentRequest = (requiresApproval: boolean) => {
    startLoader();
    groupsService!
      .doPost<any>("/user/enroll-request", { ...enrollmentRequest })
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          if (requiresApproval) {
            navigate(kcPath("/groups/mygroupenrollments"));
          } else {
            navigate(kcPath("/groups/showgroups"));
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
      (errors.comments = t("requredFormError"));
    !(enrollmentRequest?.groupRoles?.length > 0) &&
      (errors.groupRoles = t("groupRolesFormError"));
    !enrollment.multiselectRole &&
      enrollmentRequest.groupRoles.length > 1 &&
      (errors.groupRoles = t("groupRolesFormErrorMulitple"));
    setErrors(errors);
  };

  const touchFields = () => {
    for (const property in touched) {
      touched[property] = true;
    }
    setTouched({ ...touched });
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
        setNewExpirationDate(t("never"));
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
    (role: string) => !enrollmentRequest.groupRoles.includes(role)
  );

  return (
    <React.Fragment>
      <div className="gm_content">
        <Breadcrumb className="gm_breadcumb">
          <BreadcrumbItem
            to="#"
            onClick={() => {
              navigate(kcPath("groups/showgroups"));
            }}
          >
            {t("groupLabel")}
          </BreadcrumbItem>
          {group?.path &&
            (() => {
              const pathParts = group.path.split("/").filter(Boolean);
              let accumulatedPath = "";
              return pathParts.map((part: string, idx: number) => {
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
                    onClick={() => {
                      navigate(
                        kcPath(
                          `enroll?groupPath=${encodeURIComponent(
                            accumulatedPath
                          )}`
                        )
                      );
                    }}
                  >
                    {part}
                  </BreadcrumbItem>
                );
              });
            })()}
        </Breadcrumb>
        <ConfirmationModal modalInfo={modalInfo} />
        <Page
          title={
            !group?.name
              ? ""
              : membership
              ? t("updateMembershipTo") + " " + group?.name
              : t("requestMembershipTo") + " " + group?.name
          }
          description={
            (group?.attributes?.description &&
              group?.attributes?.description[0]) ||
            t("noDescription")
          }
        >
          <div className="gm_enrollment_container">
            <Form>
              {membership && isFutureDate(dateParse(membership.validFrom)) ? (
                <Alert
                  className="gm_content-width"
                  variant="warning"
                  title={t("pendingMembershipExistsTitle")}
                >
                  {t("pendingMembershipExistsMessage")}
                </Alert>
              ) : !openRequest ? (
                <React.Fragment>
                  {enrollments?.length > 0 ? (
                    <React.Fragment>
                      <FormGroup
                        label={t("groupEnrollment")}
                        isRequired
                        fieldId="simple-form-name-01"
                      >
                        <Select
                          isOpen={isOpen}
                          onOpenChange={setIsOpen}
                          selected={selected}
                          onSelect={(_event, selection) => {
                            const value = String(selection);
                            setIsOpen(false);
                            if (
                              value ===
                              t("invitationEnrollmentSelectPlaceholder")
                            ) {
                              setSelected("");
                              setEnrollment({});
                            } else {
                              const enrollment = enrollments.find(
                                (e: any) => e.name === value
                              );
                              if (enrollment) {
                                setSelected(value);
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
                              }
                            }
                          }}
                          toggle={(toggleRef) => (
                            <MenuToggle
                              ref={toggleRef}
                              onClick={() => setIsOpen((v) => !v)}
                              isExpanded={isOpen}
                              isDisabled={enrollments.length === 1}
                            >
                              {selected ||
                                t("invitationEnrollmentSelectPlaceholder")}
                            </MenuToggle>
                          )}
                        >
                          <SelectList>
                            {enrollments.length !== 1 && (
                              <SelectOption
                                key="placeholder"
                                value={t(
                                  "invitationEnrollmentSelectPlaceholder"
                                )}
                              >
                                {t("invitationEnrollmentSelectPlaceholder")}
                              </SelectOption>
                            )}
                            {enrollments.map(
                              (enrollment: any, index: number) => (
                                <SelectOption
                                  key={index}
                                  value={enrollment?.name}
                                  isDisabled={!enrollment.active}
                                >
                                  {enrollment?.name}
                                </SelectOption>
                              )
                            )}
                          </SelectList>
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
                                ? t("membershipStartingDate")
                                : t("memberSince")
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
                                ? t("membershipExpirationInfiniteTitle")
                                : expirationChangeType === "extend"
                                ? t("membershipExpirationExtendedTitle")
                                : expirationChangeType === "reduce"
                                ? t("membershipExpirationReducedTitle")
                                : expirationChangeType === "tofinite"
                                ? t("membershipExpirationNowFiniteTitle")
                                : t("membershipExpirationUnchangedTitle")
                            }
                          >
                            <div>
                              {t("currentMembershipExpiration")}{" "}
                              <strong>
                                {membership.membershipExpiresAt
                                  ? formatDate(
                                      dateParse(membership.membershipExpiresAt)
                                    )
                                  : t("never")}
                              </strong>
                              <br />
                              {t("newMembershipExpiration")}{" "}
                              <strong>{newExpirationDate || t("never")}</strong>
                              <br />
                              {expirationChangeType === "extend" && (
                                <span>
                                  {t("membershipExpirationExtendedMsg")}{" "}
                                  <strong>
                                    {daysDiff !== null ? daysDiff : 0}{" "}
                                    {t("days")}
                                  </strong>
                                </span>
                              )}
                              {expirationChangeType === "reduce" && (
                                <span>
                                  {t("membershipExpirationReducedMsg")}{" "}
                                  <strong>
                                    {daysDiff !== null ? Math.abs(daysDiff) : 0}{" "}
                                    {t("days")}
                                  </strong>
                                </span>
                              )}
                              {expirationChangeType === "tofinite" && (
                                <span>
                                  {t("membershipExpirationNowFiniteMsg")}
                                </span>
                              )}
                              {expirationChangeType === "infinite" && (
                                <span>
                                  {t("membershipExpirationInfiniteMsg")}
                                </span>
                              )}
                              {expirationChangeType === "same" && (
                                <span>
                                  {t("membershipExpirationUnchangedMsg")}
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
                          title={t("membershipExpirationNewTitle")}
                        >
                          {enrollment.validFrom &&
                          isFutureDate(dateParse(enrollment.validFrom)) ? (
                            <span>
                              {t("membershipTakesEffectAt")}{" "}
                              <strong>
                                {formatDate(dateParse(enrollment.validFrom))}
                              </strong>
                              {enrollment.membershipExpirationDays && (
                                <>
                                  <br />
                                  {t("membershipExpiresAfter")}{" "}
                                  <strong>
                                    {enrollment.membershipExpirationDays}{" "}
                                    {t("days")}
                                  </strong>
                                </>
                              )}
                            </span>
                          ) : enrollment.membershipExpirationDays ? (
                            <span>
                              {t("membershipExpiresAfter")}{" "}
                              <strong>
                                {enrollment.membershipExpirationDays}{" "}
                                {t("days")}
                              </strong>
                            </span>
                          ) : (
                            <span>{t("membershipExpirationInfiniteMsg")}</span>
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
                            <p>{t("effectiveExpirationInfo")}</p>
                          </HelperTextItem>
                        </HelperText>
                      )}
                      <FormGroup
                        label={t("Select Your Group Role")}
                        isRequired
                        fieldId="simple-form-name-01"
                        onBlur={() => {
                          touched.groupRoles = true;
                          setTouched({ ...touched });
                        }}
                      >
                        {/* Current roles display */}
                        {membership?.grouprRoles?.length > 0 && (
                          <div style={{ marginBottom: 8 }}>
                            {t("currentGroupRolesLabel")}{" "}
                            {membership.groupRoles.map((role: string) => (
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
                        <GroupRolesTable
                          groupRoles={enrollment.groupRoles}
                          selectedRoles={enrollmentRequest.groupRoles}
                          setSelectedRoles={(roles: string[]) => {
                            enrollmentRequest.groupRoles = roles;
                            setEnrollmentRequest({ ...enrollmentRequest });
                          }}
                        />
                        {errors.groupRoles && touched.groupRoles && (
                          <FormErrorText message={t("required")} />
                        )}
                        {rolesToBeLost.length > 0 && (
                          <HelperText className="gm_enrollment-lost-roles-waring">
                            <HelperTextItem variant="warning" hasIcon>
                              {t("groupRolesWillBeLostWarning")}{" "}
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
                        )}
                      </FormGroup>
                      {enrollment.commentsNeeded && (
                        <FormGroup
                          label={enrollment.commentsLabel}
                          isRequired
                          fieldId="simple-form-name-01"
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
                            onChange={(_event, value) => {
                              enrollmentRequest.comments = value;
                              setEnrollmentRequest({ ...enrollmentRequest });
                            }}
                          />
                          {touched.comments && errors.comments && (
                            <FormErrorText message={t("required")} />
                          )}
                          <div className="gm_description-text">
                            {enrollment.commentsDescription}
                          </div>
                        </FormGroup>
                      )}
                      {enrollment?.aup?.url ? (
                        <>
                          <p>
                            {t("enrollmentFlowAupMessage1")}{" "}
                            <a
                              href={enrollment?.aup?.url}
                              target="_blank"
                              rel="noreferrer"
                            >
                              {t("invitationAUPMessage2")}
                            </a>{" "}
                            {t("invitationAUPMessage3")}
                          </p>
                          <div className="gm_checkbox-container gm_content-width">
                            <Checkbox
                              onClick={() => {
                                setAcceptAup(!acceptAup);
                              }}
                              checked={acceptAup}
                              id="description-check-1"
                              label={t("enrollmentConfigurationAupMessage")}
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
                              ? t("enrollmentUpdateRequiresApprovalAlert")
                              : t("enrollmentRequiresApprovalAlert")
                            : membership
                            ? t("enrollmentUpdateNoApprovalAlert")
                            : t("enrollmentNoApprovalAlert")
                        }
                      />
                      <div>
                        <Tooltip
                          {...(!(enrollment?.aup?.url && !acceptAup)
                            ? { trigger: "manual", isVisible: false }
                            : { trigger: "mouseenter" })}
                          content={<div>{t("invitationAUPErrorMessage")}</div>}
                        >
                          <div className="gm_invitation-response-button-container">
                            <Button
                              isDisabled={enrollment?.aup?.url && !acceptAup}
                              onClick={() => {
                                touchFields();
                                if (Object.keys(errors).length !== 0) {
                                  setModalInfo({
                                    message: t(
                                      "enrollmentConfigurationModalSubmitError"
                                    ),
                                    accept_message: t("OK"),
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
                      ? t("enrollmentUpdateRequestExistsTitle")
                      : t("enrollmentRequestExistsTitle")
                  }
                >
                  <p>
                    {membership
                      ? t("enrollmentUpdateRequestExistsMessage")
                      : t("enrollmentRequestExistsMessage")}{" "}
                    <Link to={"/groups/mygroupenrollments"}>
                      "{t("viewMyEnrollmentRequests")}”
                    </Link>
                  </p>
                </Alert>
              )}
            </Form>
          </div>
        </Page>
      </div>
    </React.Fragment>
  );
};
