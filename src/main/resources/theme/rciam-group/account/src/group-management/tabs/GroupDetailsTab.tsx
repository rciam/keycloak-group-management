import * as React from "react";
import { FC, useState, useRef } from "react";
import {
  DataList,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  DataListCell,
  Button,
  TextInput,
  InputGroup,
  Tooltip,
  ClipboardCopy,
  AlertVariant,
} from "@patternfly/react-core";
import { ConfirmationModal } from "../../widgets/Modals.js";
import { MinusIcon, PlusIcon } from "@patternfly/react-icons";
import { Link } from "react-router-dom";
import { getError, kcPath } from "../../js/utils.js";
import { useTranslation } from "react-i18next";
import { useGroupsService } from "../../groups-service/GroupsServiceContext.js";
import { useLoader } from "../../widgets/LoaderContext.js";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { HttpResponse } from "../../groups-service/groups-service.js";

export const GroupDetails: FC<any> = (props) => {
  const roleRef = useRef<any>(null);
  const [roleInput, setRoleInput] = useState<string>("");
  const [modalInfo, setModalInfo] = useState({});
  const { startLoader, stopLoader } = useLoader();
  const { t } = useTranslation();
  const groupsService = useGroupsService();
  const { addAlert, addError } = useAlerts();

  const addGroupRole = (role: string) => {
    startLoader();
    groupsService!
      .doPost<any>(
        "/group-admin/group/" + props.groupId + "/roles",
        {},
        { params: { name: role } }
      )
      .then((response: HttpResponse<any>) => {
        stopLoader();
        setRoleInput("");
        setModalInfo({});
        if (response.status === 200 || response.status === 204) {
          addAlert(t("addRoleSuccess"), AlertVariant.success);
          props.fetchGroupConfiguration();
        } else {
          addError("addRoleError", getError(response));
        }
      })
      .catch((err) => {
        stopLoader();
        setModalInfo({});
        const response = err?.response ?? err;
        console.log(getError(response));
        addError("addRoleError", getError(response));
      });
  };

  const removeGroupRole = (role: string) => {
    startLoader();
    groupsService!
      .doDelete<any>("/group-admin/group/" + props.groupId + "/role/" + role)
      .then((response: any) => {
        setModalInfo({});
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          addAlert(t("deleteRoleSuccess"), AlertVariant.success);
          props.fetchGroupConfiguration();
        }
      })
      .catch((err) => {
        stopLoader();
        setModalInfo({});
        const response = err?.response ?? err;
        console.log(getError(response));
        addError("deleteRoleError", getError(response));
      });
  };

  return (
    <React.Fragment>
      <ConfirmationModal modalInfo={modalInfo} />
      <DataList
        aria-label="Compact data list example"
        isCompact
        wrapModifier={"breakWord"}
      >
        <DataListItem aria-labelledby="compact-item1">
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[
                <DataListCell key="primary content">
                  <span id="compact-item1">
                    <strong>{t("Path")}</strong>
                  </span>
                </DataListCell>,
                <DataListCell width={3} key="path-secondary">
                  <span>
                    /
                    {props.groupConfiguration?.parents?.map((group: any) => {
                      return (
                        <React.Fragment key={group.id}>
                          <Link to={kcPath("/groups/admingroups/" + group.id)}>
                            {group.name}
                          </Link>
                          {"/"}
                        </React.Fragment>
                      );
                    })}
                    {props.groupConfiguration.name}
                  </span>
                </DataListCell>,
              ]}
            />
          </DataListItemRow>
        </DataListItem>
        <DataListItem aria-labelledby="compact-item1">
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[
                <DataListCell key="primary content">
                  <span id="compact-item1">
                    <strong>{t("enrollmentDiscoveryLink")}</strong>
                  </span>
                </DataListCell>,
                <DataListCell width={3} key="enrollmentDiscoveryLink-secondary">
                  <ClipboardCopy
                    isReadOnly
                    hoverTip="Copy"
                    clickTip="Copied"
                    className="gm_copy-text-input"
                  >
                    {groupsService.getBaseUrl() +
                      "/account/enroll?groupPath=" +
                      encodeURI(props.groupConfiguration.path)}
                  </ClipboardCopy>
                </DataListCell>,
              ]}
            />
          </DataListItemRow>
        </DataListItem>
        <DataListItem aria-labelledby="compact-item2">
          <DataListItemRow className="gm_role_row">
            <DataListItemCells
              dataListCells={[
                <DataListCell key="primary content">
                  <span id="compact-item1">
                    <strong>{t("adminGroupRoles")}</strong>
                  </span>
                </DataListCell>,
                <DataListCell width={3} key="roles">
                  <div className="gm_role_add_container">
                    <InputGroup>
                      <TextInput
                        id="textInput-basic-1"
                        value={roleInput}
                        placeholder={t("adminGroupRolesAddPlaceholder")}
                        onChange={(_event, value) => {
                          setRoleInput(value.trim());
                        }}
                        onKeyDown={(e) => {
                          e.key === "Enter" && roleRef?.current?.click();
                        }}
                        type="email"
                        aria-label="email input field"
                      />
                    </InputGroup>
                    <Tooltip content={<div>{t("adminGroupRolesAdd")}</div>}>
                      <Button
                        ref={roleRef}
                        onClick={() => {
                          if (
                            props?.groupConfiguration?.groupRoles &&
                            Object.keys(
                              props.groupConfiguration.groupRoles
                            ).includes(roleInput)
                          ) {
                            setModalInfo({
                              title: t("adminGroupRoleExistsTitle"),
                              accept_message: t("OK"),
                              message:
                                t("adminGroupRoleExistsMessage1") +
                                " (" +
                                roleInput +
                                ") " +
                                t("adminGroupRoleExistsMessage2"),
                              accept: function () {
                                setModalInfo({});
                              },
                              cancel: function () {
                                setModalInfo({});
                              },
                            });
                          }
                          if (!roleInput) {
                            setModalInfo({
                              title: t("adminGroupRoleEmptyTitle"),
                              accept_message: t("OK"),
                              accept: function () {
                                setModalInfo({});
                              },
                              cancel: function () {
                                setModalInfo({});
                              },
                            });
                          } else {
                            setModalInfo({
                              title: t("Confirmation"),
                              accept_message: t("Yes"),
                              cancel_message: t("No"),
                              message:
                                t("adminGroupRoleAddConfirmation1") +
                                " " +
                                roleInput +
                                " " +
                                t("adminGroupRoleAddConfirmation2"),
                              accept: function () {
                                addGroupRole(roleInput);
                              },
                              cancel: function () {
                                setModalInfo({});
                              },
                            });
                          }
                        }}
                      >
                        <PlusIcon />
                      </Button>
                    </Tooltip>
                  </div>
                  <table className="gm_roles-table">
                    <thead>
                      <tr>
                        <th>Role Name</th>
                        <th>Role Entitlement</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {props?.groupConfiguration?.groupRoles &&
                        Object.keys(props.groupConfiguration.groupRoles).map(
                          (role, index) => {
                            return (
                              <tr key={index}>
                                <td>{role}</td>
                                <td>
                                  <ClipboardCopy
                                    isReadOnly
                                    hoverTip="Copy"
                                    clickTip="Copied"
                                  >
                                    {props.groupConfiguration.groupRoles[role]}
                                  </ClipboardCopy>
                                </td>
                                <td>
                                  <Tooltip
                                    content={
                                      <div>{t("adminGroupRoleRemove")}</div>
                                    }
                                  >
                                    <Button
                                      className="gm_roles-delete-button"
                                      variant="danger"
                                      onClick={() => {
                                        setModalInfo({
                                          title: t("Confirmation"),
                                          accept_message: t("Yes"),
                                          cancel_message: t("No"),
                                          message:
                                            t(
                                              "adminGroupRoleRemoveConfirmation1"
                                            ) +
                                            " " +
                                            role +
                                            " " +
                                            t(
                                              "adminGroupRoleRemoveConfirmation2"
                                            ),
                                          accept: function () {
                                            removeGroupRole(role);
                                          },
                                          cancel: function () {
                                            setModalInfo({});
                                          },
                                        });
                                      }}
                                    >
                                      <MinusIcon />
                                    </Button>
                                  </Tooltip>
                                </td>
                              </tr>
                            );
                          }
                        )}
                    </tbody>
                  </table>
                </DataListCell>,
              ]}
            />
          </DataListItemRow>
        </DataListItem>
      </DataList>
    </React.Fragment>
  );
};
