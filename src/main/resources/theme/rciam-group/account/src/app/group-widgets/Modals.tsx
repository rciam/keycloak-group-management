import * as React from "react";
import {
  Modal,
  ModalVariant,
  Button,
  Tooltip,
  Form,
  FormGroup,
  TextInput,
  ListItem,
  List,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { ExclamationTriangleIcon } from "@patternfly/react-icons";
import {
  GroupsServiceClient,
  HttpResponse,
} from "../groups-mngnt-service/groups.service";
// import parse from '../../node_modules/react-html-parser';
import { Msg } from "../widgets/Msg";
import { getError } from "../js/utils.js";
import { useLoader } from "./LoaderContext";
import { start } from "repl";
import { addDays, dateParse, isFirstDateBeforeSecond } from "../widgets/Date";
import { Popover } from "@patternfly/react-core";
import { Badge } from "@patternfly/react-core";

interface ConfirmationModalProps {
  modalInfo: any;
}

export const ConfirmationModal: React.FC<ConfirmationModalProps> = (props) => {
  useEffect(() => {
    setIsModalOpen(Object.keys(props.modalInfo).length > 0);
  }, [props.modalInfo]);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const handleModalToggle = () => {
    props?.modalInfo?.cancel();
  };

  return (
    <React.Fragment>
      <Modal
        variant={
          props.modalInfo?.variant === "medium"
            ? ModalVariant.medium
            : ModalVariant.small
        }
        title={props?.modalInfo?.title}
        isOpen={isModalOpen}
        onClose={handleModalToggle}
        actions={[
          <Button
            key="confirm"
            variant={props?.modalInfo?.button_variant || "primary"}
            onClick={() => {
              props?.modalInfo?.accept();
            }}
          >
            {props?.modalInfo?.accept_message}
          </Button>,
          props?.modalInfo?.cancel_message && (
            <Button
              key="cancel"
              variant="link"
              onClick={() => {
                props?.modalInfo?.cancel();
              }}
            >
              {props?.modalInfo?.cancel_message}
            </Button>
          ),
        ]}
      >
        {props?.modalInfo?.message && props?.modalInfo?.message}
      </Modal>
    </React.Fragment>
  );
};

export const DeleteSubgroupModal: React.FC<any> = (props) => {
  let groupsService = new GroupsServiceClient();
  const [modalInfo, setModalInfo] = useState({});
  const { startLoader, stopLoader } = useLoader();

  useEffect(() => {
    if (props.active) {
      setModalInfo({
        message: Msg.localize("deleteGroupConfirmation"),
        accept_message: Msg.localize("YES"),
        cancel_message: Msg.localize("NO"),
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
            message: Msg.localize("deleteGroupSuccess"),
            accept_message: Msg.localize("OK"),
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
            message: Msg.localize("deleteGroupError", [getError(response)]),
            accept_message: Msg.localize("OK"),
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
  const { startLoader, stopLoader } = useLoader();
  let groupsService = new GroupsServiceClient();
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
    startLoader();
    groupsService!
      .doPost<any>(
        "/group-admin/group" +
          (props.groupId ? "/" + props.groupId + "/children" : ""),
        { ...groupConfig }
      )
      .then((response: HttpResponse<any>) => {
        stopLoader();
        props.close();
        if (
          response.status === 200 ||
          response.status === 204 ||
          response.status === 201
        ) {
          setModalInfo({
            title: props.groupId
              ? Msg.localize("createSubgroupSuccess")
              : Msg.localize("createGroupSuccess"),
            accept_message: Msg.localize("OK"),
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
              ? Msg.localize("createSubgroupError", [getError(response)])
              : Msg.localize("createGroupError", [getError(response)]),
            accept_message: Msg.localize("OK"),
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
      <Modal
        variant={ModalVariant.medium}
        title={
          props.groupId
            ? Msg.localize("createSubgroup")
            : Msg.localize("createGroup")
        }
        isOpen={isModalOpen}
        onClose={() => {
          props.close();
        }}
        actions={[
          <Tooltip
            {...(!!isValid
              ? { trigger: "manual", isVisible: false }
              : { trigger: "mouseenter" })}
            content={
              <div>
                <Msg msgKey="createGroupFormError" />
              </div>
            }
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
                <Msg msgKey="Create" />
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
            <Msg msgKey="Cancel" />
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
              onChange={(value) => {
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
              onChange={(value) => {
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
        <h2 className="gm_modal-title">
          <Msg msgKey="userDetails" />
        </h2>
        <FormGroup label={Msg.localize("username")} fieldId="user-username">
          <div>{membership.user.username || <Msg msgKey="notAvailable" />}</div>
        </FormGroup>
        <FormGroup label={Msg.localize("email")} fieldId="user-email">
          <div>{membership.user.email || <Msg msgKey="notAvailable" />}</div>
        </FormGroup>
        <FormGroup label="Full Name" fieldId="user-fullname">
          <div>
            {membership.user.firstName && membership.user.lastName ? (
              `${membership.user.firstName} ${membership.user.lastName}`
            ) : (
              <Msg msgKey="notAvailable" />
            )}
          </div>
        </FormGroup>
        <FormGroup
          label={Msg.localize("uid")}
          fieldId="user-preferred-username"
        >
          <div>
            {membership.user.attributes?.uid?.[0] || (
              <Msg msgKey="notAvailable" />
            )}
          </div>
        </FormGroup>
        <FormGroup
          label={Msg.localize("enrollmentIdentityProvidersLabel")}
          fieldId="user-federated-identities"
        >
          <div>
            {membership.user.federatedIdentities &&
            membership.user.federatedIdentities.length > 0 ? (
              membership.user.federatedIdentities.map((identity, index) => (
                <Badge
                  key={"single"}
                  className="gm_role_badge gm_dark_badge"
                  isRead
                >
                  {identity.identityProvider}
                </Badge>
              ))
            ) : (
              <Msg msgKey="notAvailable" />
            )}
          </div>
        </FormGroup>

        {/* Membership Information Section */}
        <h2 className="gm_modal-title">
          <Msg msgKey="membershipDetails" />
        </h2>
        <FormGroup
          label="Membership Expiration"
          fieldId="membership-expiration"
        >
          <div>
            {notificationWarningDirect ? (
              <Popover
                bodyContent={
                  <div>
                    <Msg msgKey="membershipExpirationNotification" />
                  </div>
                }
              >
                <span className="gm_effective-expiration-popover-trigger">
                  <div
                    style={{ display: "inline-block" }}
                    className={"gm_warning-text"}
                  >
                    {membership.membershipExpiresAt || <Msg msgKey="Never" />}
                  </div>
                  <div className="gm_effective-helper-warning">
                    <ExclamationTriangleIcon />
                  </div>
                </span>
              </Popover>
            ) : membership.membershipExpiresAt ? (
              <>{membership.membershipExpiresAt}</>
            ) : (
              <Msg msgKey="Never" />
            )}
          </div>
        </FormGroup>
        <FormGroup label="Group Roles" fieldId="membership-roles">
          <div>
            {membership.groupRoles.map((role, index) => (
              <Badge key={index} className="gm_role_badge" isRead>
                {role}
              </Badge>
            ))}
          </div>
        </FormGroup>
        <FormGroup label="Member Since" fieldId="membership-since">
          <div>{membership.validFrom || <Msg msgKey="notAvailable" />}</div>
        </FormGroup>
        <FormGroup label="Membership Status" fieldId="membership-status">
          <Tooltip
            content={
              <div>
                {membership.status === "ENABLED"
                  ? Msg.localize("adminGroupMemberUserActiveTooltip")
                  : membership.status === "SUSPENDED"
                  ? Msg.localize("adminGroupMemberUserSuspendedTooltip")
                  : membership.status === "PENDING"
                  ? Msg.localize("adminGroupMemberUserPendingTooltip")
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
