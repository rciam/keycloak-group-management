import * as React from "react";
import {
  Modal,
  ModalVariant,
  Button,
  Tooltip,
  Form,
  FormGroup,
  TextInput,
  Badge,
  Popover,
} from "@patternfly/react-core";
import { ExclamationTriangleIcon } from "@patternfly/react-icons";
import { useEffect, useState } from "react";
import { useLoader } from "../../widgets/LoaderContext";
import { useGroupsService } from "../../groups-service/GroupsServiceContext";
import { useTranslation } from "react-i18next";
import { HttpResponse } from "../../groups-service/groups-service";
import { getError } from "../../js/utils";
import { ConfirmationModal } from "../../widgets/Modals";
import {
  addDays,
  dateParse,
  isFirstDateBeforeSecond,
} from "../../widgets/Date";

export const DeleteSubgroupModal: React.FC<any> = (props) => {
  const groupsService = useGroupsService();
  const { t } = useTranslation();
  const [modalInfo, setModalInfo] = useState({});
  const { startLoader, stopLoader } = useLoader();

  useEffect(() => {
    if (props.active) {
      setModalInfo({
        message: t("deleteGroupConfirmation"),
        accept_message: t("YES"),
        cancel_message: t("NO"),
        accept: function () {
          setModalInfo({});
          deleteGroup();
        },
        cancel: function () {
          setModalInfo({});
          props.close();
        },
      });
    }
  }, [props.active]);

  const deleteGroup = () => {
    startLoader();
    groupsService!
      .doDelete<any>("/group-admin/group/" + props.groupId)
      .then((response: HttpResponse<any>) => {
        stopLoader();
        props.close();
        if (response.status === 200 || response.status === 204) {
          setModalInfo({
            message: t("deleteGroupSuccess"),
            accept_message: t("OK"),
            accept: function () {
              props.afterSuccess();
              setModalInfo({});
            },
            cancel: function () {
              props.afterSuccess();
              setModalInfo({});
            },
          });
        } else {
          setModalInfo({
            message: t("deleteGroupError", getError(response)),
            accept_message: t("OK"),
            accept: function () {
              props.afterSuccess();
              setModalInfo({});
            },
            cancel: function () {
              props.afterSuccess();
              setModalInfo({});
            },
          });
        }
      })
      .catch((err) => {
        props.close();
        stopLoader();
        console.log(err);
      });
  };

  return (
    <React.Fragment>
      <ConfirmationModal modalInfo={modalInfo} />
    </React.Fragment>
  );
};

export const CreateGroupModal: React.FC<any> = (props) => {
  useEffect(() => {
    setIsModalOpen(props.active);
    setGroupConfig(groupConfigDefault);
  }, [props.active]);

  const [isModalOpen, setIsModalOpen] = React.useState(false);
  let groupConfigDefault = {
    name: "",
    attributes: {
      description: [""],
    },
  };
  const groupsService = useGroupsService();
  const { t } = useTranslation();
  // const { startLoader, stopLoader } = useLoader();
  const [modalInfo, setModalInfo] = useState({});
  const [groupConfig, setGroupConfig] = useState(groupConfigDefault);
  const [isValid, setIsValid] = useState(false);

  useEffect(() => {
    setIsValid(
      groupConfig.name.length > 0 &&
        groupConfig.attributes.description[0].length > 0
    );
  }, [groupConfig]);

  const createGroup = () => {
    // startLoader();
    groupsService!
      .doPost<any>(
        "/group-admin/group" +
          (props.groupId ? "/" + props.groupId + "/children" : ""),
        { ...groupConfig }
      )
      .then((response: any) => {
        // stopLoader();
        props.close();
        if (
          response.status === 200 ||
          response.status === 204 ||
          response.status === 201
        ) {
          setModalInfo({
            title: props.groupId
              ? t("createSubgroupSuccess")
              : t("createGroupSuccess"),
            accept_message: t("OK"),
            accept: function () {
              props.afterSuccess();
              setModalInfo({});
            },
            cancel: function () {
              props.afterSuccess();
              setModalInfo({});
            },
          });
          // setGroupMembers(response.data.results);
        } else {
          setModalInfo({
            title: props.groupId
              ? t("createSubgroupError", "placeholder")
              : t("createGroupError", "placeholder"),
            accept_message: t("OK"),
            accept: function () {
              props.afterSuccess();
              setModalInfo({});
            },
            cancel: function () {
              props.afterSuccess();
              setModalInfo({});
            },
          });
        }
      })
      .catch((err) => {
        props.close();
        // stopLoader();
        console.log(err);
      });
  };

  return (
    <React.Fragment>
      <ConfirmationModal modalInfo={modalInfo} />
      <Modal
        variant={ModalVariant.medium}
        title={props.groupId ? t("createSubgroup") : t("createGroup")}
        isOpen={isModalOpen}
        onClose={() => {
          props.close();
        }}
        actions={[
          <Tooltip
            {...(!!isValid
              ? { trigger: "manual", isVisible: false }
              : { trigger: "mouseenter" })}
            content={<div>{t("createGroupFormError")}</div>}
          >
            <div>
              <Button
                key="confirm"
                variant="primary"
                isDisabled={!isValid}
                onClick={() => {
                  createGroup();
                }}
              >
                {t("Create")}
              </Button>
            </div>
          </Tooltip>,
          <Button
            key="cancel"
            variant="link"
            onClick={() => {
              props.close();
            }}
          >
            {t("Cancel")}
          </Button>,
        ]}
      >
        <Form>
          <FormGroup
            label="Group Name"
            isRequired
            fieldId="simple-form-name-01"
            // helperText=""
          >
            <TextInput
              isRequired
              type="text"
              id="simple-form-name-01"
              name="simple-form-name-01"
              aria-describedby="simple-form-name-01-helper"
              value={groupConfig.name}
              onChange={(_event:any, value: string) => {
                groupConfig.name = value;
                setGroupConfig({ ...groupConfig });
              }}
            />
          </FormGroup>
          <FormGroup
            label="Description"
            isRequired
            fieldId="simple-form-desription-01"
          >
            <TextInput
              isRequired
              type="text"
              id="simple-form-email-01"
              name="simple-form-email-01"
              value={groupConfig.attributes.description[0]}
              onChange={(_event:any, value:string) => {
                groupConfig.attributes.description[0] = value;
                setGroupConfig({ ...groupConfig });
              }}
            />
          </FormGroup>
        </Form>
      </Modal>
    </React.Fragment>
  );
};

export const UserInfoModal: React.FC<{
  membership: any;
  onClose: () => void;
}> = ({ membership, onClose }) => {
  if (!membership) return null; // Don't render the modal if no user or membership is selected
  const { t } = useTranslation();

  const notificationWarningDirect =
    membership.membershipExpiresAt &&
    membership?.group?.attributes["expiration-notification-period"]?.[0] &&
    isFirstDateBeforeSecond(
      dateParse(membership.membershipExpiresAt),
      addDays(
        new Date(new Date().setHours(0, 0, 0, 0)),
        parseInt(
          membership?.group?.attributes["expiration-notification-period"]?.[0]
        )
      ),
      "warning"
    );

  return (
    <Modal
      variant={ModalVariant.medium}
      isOpen={!!membership.user}
      onClose={onClose}
      actions={[
        <Button key="close" variant="primary" onClick={onClose}>
          Close
        </Button>,
      ]}
    >
      <Form>
        {/* User Information Section */}
        <h2 className="gm_modal-title">{t("userDetails")}</h2>
        <FormGroup label={t("username")} fieldId="user-username">
          <div>{membership.user.username || t("notAvailable")}</div>
        </FormGroup>
        <FormGroup label={t("email")} fieldId="user-email">
          <div>{membership.user.email || t("notAvailable")}</div>
        </FormGroup>
        <FormGroup label="Full Name" fieldId="user-fullname">
          <div>
            {membership.user.firstName && membership.user.lastName
              ? `${membership.user.firstName} ${membership.user.lastName}`
              : t("notAvailable")}
          </div>
        </FormGroup>
        <FormGroup label={t("uid")} fieldId="user-preferred-username">
          <div>{membership.user.attributes?.uid?.[0] || t("notAvailable")}</div>
        </FormGroup>
        <FormGroup
          label={t("enrollmentIdentityProvidersLabel")}
          fieldId="user-federated-identities"
        >
          <div>
            {membership.user.federatedIdentities &&
            membership.user.federatedIdentities.length > 0
              ? membership.user.federatedIdentities.map((identity: any) => (
                  <Badge
                    key={"single"}
                    className="gm_role_badge gm_dark_badge"
                    isRead
                  >
                    {identity.identityProvider}
                  </Badge>
                ))
              : t("notAvailable")}
          </div>
        </FormGroup>

        {/* Membership Information Section */}
        <h2 className="gm_modal-title">{t("membershipDetails")}</h2>
        <FormGroup
          label="Membership Expiration"
          fieldId="membership-expiration"
        >
          <div>
            {notificationWarningDirect ? (
              <Popover
                bodyContent={<div>{t("membershipExpirationNotification")}</div>}
              >
                <span className="gm_effective-expiration-popover-trigger">
                  <div
                    style={{ display: "inline-block" }}
                    className={"gm_warning-text"}
                  >
                    {membership.membershipExpiresAt || t("Never")}
                  </div>
                  <div className="gm_effective-helper-warning">
                    <ExclamationTriangleIcon />
                  </div>
                </span>
              </Popover>
            ) : membership.membershipExpiresAt ? (
              <>{membership.membershipExpiresAt}</>
            ) : (
              t("Never")
            )}
          </div>
        </FormGroup>
        <FormGroup label="Group Roles" fieldId="membership-roles">
          <div>
            {membership.groupRoles.map((role:string, index:number) => (
              <Badge key={index} className="gm_role_badge" isRead>
                {role}
              </Badge>
            ))}
          </div>
        </FormGroup>
        <FormGroup label="Member Since" fieldId="membership-since">
          <div>{membership.validFrom || t("notAvailable")}</div>
        </FormGroup>
        <FormGroup label="Membership Status" fieldId="membership-status">
          <Tooltip
            content={
              <div>
                {membership.status === "ENABLED"
                  ? t("adminGroupMemberUserActiveTooltip")
                  : membership.status === "SUSPENDED"
                  ? t("adminGroupMemberUserSuspendedTooltip")
                  : membership.status === "PENDING"
                  ? t("adminGroupMemberUserPendingTooltip")
                  : ""}
              </div>
            }
          >
            <div className="gm_user-status-container">
              <div
                className={
                  membership.status === "ENABLED"
                    ? "gm_icon gm_icon-active-user"
                    : membership.status === "SUSPENDED"
                    ? "gm_icon gm_icon-suspended-user"
                    : membership.status === "PENDING"
                    ? "gm_icon gm_icon-pending-user"
                    : ""
                }
              ></div>
            </div>
          </Tooltip>
        </FormGroup>
      </Form>
    </Modal>
  );
};
