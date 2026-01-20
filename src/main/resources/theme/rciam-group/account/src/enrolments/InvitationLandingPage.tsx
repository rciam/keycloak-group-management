import * as React from "react";
import { FC, useState, useEffect } from "react";
import {
  Button,
  Checkbox,
  HelperText,
  HelperTextItem,
  Hint,
  HintBody,
  Modal,
  ModalVariant,
  Tooltip,
} from "@patternfly/react-core";
import { useLoader } from "../widgets/LoaderContext";
import { useTranslation } from "react-i18next";
import { useGroupsService } from "../groups-service/GroupsServiceContext";
import { HttpResponse } from "../groups-service/groups-service";
import { kcPath } from "../js/utils";
import { useNavigate, useParams } from "react-router-dom";



// export class GroupPage extends React.Component<GroupsPageProps, GroupsPageState> {
export const InvitationLandingPage: FC<any> = () => {
  const { t } = useTranslation();
  const groupsService = useGroupsService();
  const { invitation_id } = useParams<any>();
  const [invitationData, setInvitationData] = useState<any>({});
  const [acceptAup, setAcceptAup] = useState(false);
  const [actionBlocked, setActionBlocked] = useState(false);
  const [isParentGroup, setIsParentGroup] = useState(false);
  const { startLoader, stopLoader } = useLoader();
  const navigate = useNavigate();

  useEffect(() => {
    getInvitation();
  }, []);

  let getInvitation = () => {
    startLoader();
    groupsService!
      .doGet<any>("/user/invitation/" + invitation_id)
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 && response.data) {
          setInvitationData(response.data);
          // Check if group is a parent group
          if (
            response.data?.groupEnrollmentConfiguration?.group?.path &&
            response.data.groupEnrollmentConfiguration.group.path.split("/")
              .length === 2
          ) {
            setIsParentGroup(true);
          }
        }
      })
      .catch((err) => {
        console.log(err);
        stopLoader();
      });
  };

  const acceptInvitation = () => {
    startLoader();
    groupsService!
      .doPost<any>("/user/invitation/" + invitation_id + "/accept", {})
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          if (invitationData?.forMember) {
            navigate(kcPath("/groups/showgroups"));
          } else {
            navigate(kcPath("/groups/admingroups"));
          }
        } else {
          setActionBlocked(true);
        }
      })
      .catch(() => {
        setActionBlocked(true);
        stopLoader();
      });
  };

  const rejectInvitation = () => {
    startLoader();
    groupsService!
      .doPost<any>("/user/invitation/" + invitation_id + "/reject", {})
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          if (invitationData?.forMember) {
            navigate(kcPath("/groups/showgroups"));
          } else {
            navigate(kcPath("/groups/admingroups"));
          }
        } else {
          setActionBlocked(true);
        }
      })
      .catch(() => {
        setActionBlocked(true);

        stopLoader();
      });
  };

  return (
    <>
      <div className="gm_invitation-container">
        <ResponseModal
          type={invitationData?.forMember}
          close={() => {
            setActionBlocked(false);
            if (invitationData?.forMember) {
              navigate(kcPath("/groups/showgroups"));
            } else {
              navigate(kcPath("/groups/admingroups"));
            }
          }}
          active={actionBlocked}
        />
        {Object.keys(invitationData).length > 0 ? (
          <>
            <span className="gm_invitation-landing-title">
              {t("invitationGreetings")}{" "}
              {invitationData?.groupEnrollmentConfiguration?.group?.name ||
                invitationData?.group?.name}
            </span>
            <div className="gm_invitation-content-container">
              {(invitationData?.groupEnrollmentConfiguration?.group?.attributes
                ?.description ||
                invitationData?.group?.attributes?.description) && (
                <div className="gm_invitation-purpuse">
                  <h1>{t("Description")}</h1>
                  {invitationData?.groupEnrollmentConfiguration?.group
                    ?.attributes?.description[0] ||
                    invitationData?.group?.attributes?.description[0]}
                </div>
              )}
              <div className="gm_invitation-purpuse">
                <h1>{t("groupPath")}</h1>
                {invitationData?.groupEnrollmentConfiguration?.group?.path ||
                  "Group/Path/test"}
              </div>
              <Hint>
                <HintBody>
                  {t("invitationMessage")}
                  {invitationData?.forMember
                    ? invitationData?.groupRoles.map(
                        (role: string, index: number) => {
                          return (
                            <strong>
                              {" "}
                              {role}
                              {index !== invitationData.groupRoles.length - 1 &&
                                ","}
                            </strong>
                          );
                        }
                      )
                    : " admin"}
                  .
                </HintBody>
              </Hint>
              {invitationData?.forMember && (
                <>
                  <HelperText>
                    <HelperTextItem variant="warning" hasIcon>
                      <p>
                        {invitationData?.groupEnrollmentConfiguration
                          ?.membershipExpirationDays ? (
                          <React.Fragment>
                            <div
                              dangerouslySetInnerHTML={{
                                __html: invitationData
                                  ?.groupEnrollmentConfiguration?.validFrom
                                  ? t("invitationExpirationMeddageValidFrom", {
                                      param_0:
                                        invitationData
                                          .groupEnrollmentConfiguration
                                          .validFrom,
                                      param_1: JSON.stringify(
                                        invitationData
                                          ?.groupEnrollmentConfiguration
                                          ?.membershipExpirationDays
                                      ),
                                    })
                                  : t("invitationExpirationMessage", {
                                      param_0: JSON.stringify(
                                        invitationData
                                          ?.groupEnrollmentConfiguration
                                          ?.membershipExpirationDays
                                      ),
                                    }),
                              }}
                            />
                          </React.Fragment>
                        ) : invitationData?.groupEnrollmentConfiguration
                            ?.validFrom ? (
                          <div
                            dangerouslySetInnerHTML={{
                              __html: t(
                                "invitationExpirationMeddageInfiniteValidFrom",
                                {
                                  param_0:
                                    invitationData.groupEnrollmentConfiguration
                                      .validFrom,
                                }
                              ),
                            }}
                          />
                        ) : (
                          t("invitationExpirationMessageInfinite")
                        )}
                      </p>
                    </HelperTextItem>
                  </HelperText>
                  {!isParentGroup && (
                    <HelperText>
                      <HelperTextItem variant="warning" hasIcon>
                        <p>
                          {t("invitationExpirationInfo")}{" "}
                          <a
                            onClick={() => {
                              navigate(kcPath("/groups/showgroups"));
                            }}
                          >
                            My Groups
                          </a>{" "}
                          page.
                        </p>
                      </HelperTextItem>
                    </HelperText>
                  )}
                </>
              )}

              {invitationData?.groupEnrollmentConfiguration?.aup?.url ? (
                <>
                  <p>
                    {t("invitationAUPMessage1")}{" "}
                    <a
                      href={
                        invitationData?.groupEnrollmentConfiguration?.aup?.url
                      }
                      target="_blank"
                      rel="noreferrer"
                    >
                      {t("invitationAUPMessage2")}
                    </a>{" "}
                    {t("invitationAUPMessage3")}
                  </p>
                  <div className="gm_checkbox-container">
                    <Checkbox
                      onClick={() => {
                        setAcceptAup(!acceptAup);
                      }}
                      checked={acceptAup}
                      id="description-check-1"
                      label="I have read the terms and accept them"
                    />
                  </div>
                </>
              ) : (
                ""
              )}

              <div className="gm_invitation-response-container">
                <Tooltip
                  {...(!(
                    invitationData?.groupEnrollmentConfiguration?.aup?.url &&
                    !acceptAup
                  )
                    ? { trigger: "manual", isVisible: false }
                    : { trigger: "mouseenter" })}
                  content={<div>{t("invitationAUPErrorMessage")}</div>}
                >
                  <div className="gm_invitation-response-button-container">
                    <Button
                      isDisabled={
                        invitationData?.groupEnrollmentConfiguration?.aup
                          ?.url && !acceptAup
                      }
                      onClick={acceptInvitation}
                    >
                      {t("Accept")}
                    </Button>
                  </div>
                </Tooltip>
                <Button variant="danger" onClick={rejectInvitation}>
                  {t("Reject")}
                </Button>
              </div>
            </div>
          </>
        ) : (
          <span className="gm_invitation-landing-title">
            {t("invitationNotFound")}
          </span>
        )}
      </div>
    </>
  );
};

const ResponseModal: React.FC<any> = (props) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const { t } = useTranslation();

  useEffect(() => {
    setIsModalOpen(!!props.active);
  }, [props.active]);

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("invitationErrorResponseTitle")}
      isOpen={isModalOpen}
      onClose={() => {
        props.close();
      }}
      actions={[
        <Button
          key="confirm"
          variant="primary"
          onClick={() => {
            props.close();
          }}
        >
          {t("OK")}
        </Button>,
      ]}
    >
      <>
        {t("invitationErrorResponseMessage")}
        {props.type ? " member" : " admin"}.
      </>
    </Modal>
  );
};
