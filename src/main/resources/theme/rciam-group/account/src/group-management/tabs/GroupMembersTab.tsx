import * as React from "react";
import { FC, useState, useEffect } from "react";
import {
  DataList,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  DataListCell,
  Button,
  Tooltip,
  ModalVariant,
  DataListAction,
  Pagination,
  Badge,
  Modal,
  Checkbox,
  Form,
  FormGroup,
  Popover,
  TextArea,
  Spinner,
  AlertVariant,
} from "@patternfly/react-core";
import { ConfirmationModal } from "../../widgets/Modals";
import { TableActionBar } from "../../widgets/TableActionBar";
import {
  HelpIcon,
  PencilAltIcon,
  TimesIcon,
  LockIcon,
  LockOpenIcon,
  OutlinedClockIcon,
  ExclamationTriangleIcon,
  LongArrowAltUpIcon,
  LongArrowAltDownIcon,
  AngleDownIcon,
  UserIcon,
} from "@patternfly/react-icons";
import { Link } from "react-router-dom";
import {
  dateParse,
  addDays,
  isFirstDateBeforeSecond,
} from "../../widgets/Date";
import { getError } from "../../js/utils";
import { useLoader } from "../../widgets/LoaderContext";
import { useGroupsService } from "../../groups-service/GroupsServiceContext";
import { useTranslation } from "react-i18next";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { HttpResponse } from "../../groups-service/groups-service";
import { AddMemberWizard } from "../components/AddMemberWizard";
import { UserInfoModal } from "../components/Modals"; // Import the new modal
import { EditMembershipModal } from "../components/EditMembershipModal";
import { DatalistFilterSelect } from "../../widgets/DatalistFilterSelect";

interface FederatedIdentity {
  identityProvider: string;
}

interface User {
  id?: string;
  username: string;
  emailVerified: boolean;
  email: string;
  federatedIdentities: FederatedIdentity[];
  firstName: string;
  lastName: string;
  attributes: any;
}

interface Memberships {
  id?: string;
  user: User;
  validFrom: string;
  status: string;
  direct: boolean;
  membershipExpiresAt: string;
  effectiveMembershipExpiresAt?: string;
  effectiveGroupId?: string;
  group: any;
  groupRoles: string[];
}

const UserActionModal: FC<any> = (props) => {
  const { t } = useTranslation();
  const groupsService = useGroupsService();
  const [justification, setJustification] = useState("");
  const [user, setUser] = useState<any>({});
  const [isOpen, setIsOpen] = useState(false);
  const { startLoader, stopLoader } = useLoader();
  const { addAlert, addError } = useAlerts();

  useEffect(() => {
    setUser(props.user);
    setIsOpen(Object.keys(props.user).length > 0);
  }, [props.user]);

  let suspendGroupMember = () => {
    startLoader();
    groupsService!
      .doPost<any>(
        "/group-admin/group/" +
          user.group.id +
          "/member/" +
          user.id +
          "/suspend" +
          (justification ? "?justification=" + justification : ""),
        {}
      )
      .then((response: HttpResponse<any>) => {
        close();
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          props.fetchGroupMembers();
          addAlert(t("suspendMembershipSuccess"), AlertVariant.success);
        } else {
          addError("suspendMembershipError", getError(response));
        }
      });
  };

  let activateGroupMember = () => {
    startLoader();
    groupsService!
      .doPost<any>(
        "/group-admin/group/" +
          user.group.id +
          "/member/" +
          user.id +
          "/activate" +
          (justification ? "?justification=" + justification : ""),
        {}
      )
      .then((response: HttpResponse<any>) => {
        close();
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          props.fetchGroupMembers();
          addAlert(t("activateMembershipSuccess"), AlertVariant.success);
        } else {
          props.setLoading(false);
          addError("activateMembershipError", getError(response));
        }
      });
  };

  const close = () => {
    props.setUser({});
    setJustification("");
  };

  return (
    <>
      <Modal
        variant={ModalVariant.medium}
        title={t("Confirmation")}
        isOpen={isOpen}
        onClose={() => {
          close();
        }}
        actions={[
          <Button
            key="confirm"
            variant="primary"
            onClick={() => {
              if (user.status === "ENABLED") {
                suspendGroupMember();
              } else {
                activateGroupMember();
              }
            }}
          >
            {t("YES")}
          </Button>,
          <Button
            key="cancel"
            variant="link"
            onClick={() => {
              close();
            }}
          >
            {t("NO")}
          </Button>,
        ]}
      >
        <div>
          <p>
            {user.status === "ENABLED"
              ? t("adminGroupMemberSuspendConfirmation")
              : t("adminGroupMemberRevokeSuspendConfirmation")}
          </p>
          <Form>
            <FormGroup
              labelIcon={
                <Popover
                  bodyContent={
                    <div>
                      {user.status === "ENABLED"
                        ? t("adminGroupMemberSuspendJustification")
                        : t("adminGroupMemberRevokeSuspendJustification")}
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
              className="gm_suspend-justification-formgroup"
              label={t("justificationLabel")}
              fieldId={""}
            >
              <TextArea
                type="text"
                id="simple-form-name-01"
                name="simple-form-name-01"
                aria-describedby="simple-form-name-01-helper"
                value={justification}
                onChange={(_event, value) => {
                  setJustification(value);
                }}
              />
            </FormGroup>
          </Form>
        </div>
      </Modal>
    </>
  );
};

export const GroupMembers: FC<any> = (props) => {
  const [groupMembers, setGroupMembers] = useState<Memberships[]>([]);
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState<number>(0);
  const [modalInfo, setModalInfo] = useState({});
  const { t } = useTranslation();
  const [statusSelection, setStatusSelection] = useState("");
  const [roleSelection, setRoleSelection] = useState("");
  const [editMembership, setEditMembership] = useState<any>({});
  const [inviteModalActive, setInviteModalActive] = useState(false);
  const [selectedUser, setSelectedUser] = useState<any>({});
  const [directMembers, setDirectMembers] = useState<boolean>(true);
  const [loading, setLoading] = useState(false);
  const [searchString, setSearchString] = useState("");
  const [groupId, setGroupId] = useState(props.groupId);
  const [orderBy, setOrderBy] = useState("default");
  const [asc, setAsc] = useState<boolean>(true);
  const [selectedUserInfo, setSelectedUserInfo] = useState(null); // State to hold the selected user info
  const { addAlert, addError } = useAlerts();
  const groupsService = useGroupsService();





  useEffect(() => {
    if (props.groupId !== groupId) {
      setDirectMembers(true);
      setGroupId(props.groupId);
      const searchParams = new URLSearchParams(location.hash.split("?")[1]);
      let searchMember = searchParams.get("memberId");
      if (searchMember) {
        const newSearchParams = new URLSearchParams(searchParams);
        setSearchString(searchMember);
        newSearchParams.delete("memberId");
        props.history.push({
          search: newSearchParams.toString()
            ? `?${newSearchParams.toString()}`
            : "",
        });
      } else {
        setSearchString("");
      }
    }
    setPage(1);
  }, [props.groupId]);

  useEffect(() => {
    fetchGroupMembers(searchString);
  }, [
    statusSelection,
    roleSelection,
    page,
    perPage,
    groupId,
    directMembers,
    searchString,
    orderBy,
    asc,
  ]);

  const orderResults = (type: string) => {
    if (orderBy !== type) {
      setOrderBy(type);
      setAsc(true);
    } else if (asc) {
      setAsc(false);
    } else {
      setAsc(true);
    }
  };

  const onSetPage = (
    _event: React.MouseEvent | React.KeyboardEvent | MouseEvent,
    newPage: number
  ) => {
    setPage(newPage);
  };

  const onPerPageSelect = (
    _event: React.MouseEvent | React.KeyboardEvent | MouseEvent,
    newPerPage: number,
    newPage: number
  ) => {
    setPerPage(newPerPage);
    setPage(newPage);
  };

  let preselectedMembershipHandler = (memberships: any) => {
    const searchParams = new URLSearchParams(location.hash.split("?")[1]);
    let preselectedMembership = searchParams.get("membership");
    if (preselectedMembership) {
      memberships.forEach((membership: any) => {
        if ((membership.id = preselectedMembership)) {
          setEditMembership(membership);
        }
      });
      const newSearchParams = new URLSearchParams(searchParams);
      newSearchParams.delete("membership");
      props.history.push({
        search: newSearchParams.toString()
          ? `?${newSearchParams.toString()}`
          : "",
      });
    }
  };

  let fetchGroupMembers = (searchString: string | undefined = undefined) => {
    setLoading(true);
    groupsService!
      .doGet<any>(
        "/group-admin/group/" +
          props.groupId +
          "/members?first=" +
          perPage * (page - 1) +
          "&max=" +
          perPage +
          (searchString ? "&search=" + searchString : "") +
          (orderBy !== "default" ? "&order=" + orderBy : "") +
          "&asc=" +
          asc,
        {
          params: {
            ...(statusSelection ? { status: statusSelection } : {}),
            ...(roleSelection ? { role: roleSelection } : {}),
            ...(!directMembers ? { direct: "false" } : {}),
          },
        }
      )
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setLoading(false);
          setTotalItems(response.data.count);
          setGroupMembers(response.data.results);
          preselectedMembershipHandler(response.data.results);
        }
      });
  };

  let deleteGroupMember = (memberId: any, groupId: any) => {
    setLoading(true);
    groupsService!
      .doDelete<any>("/group-admin/group/" + groupId + "/member/" + memberId)
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          addAlert(t("deleteMemberSuccess"), AlertVariant.success);
          fetchGroupMembers();
        } else {
          setLoading(false);
          addError("deleteMemberError", getError(response));
        }
      });
  };

  let activatePendingMembership = (membership: any) => {
    const currentDate = new Date();
    const formattedDate = currentDate.toISOString().split("T")[0];
    membership.validFrom = formattedDate;
    setLoading(true);
    groupsService!
      .doPut<any>(
        "/group-admin/group/" + props.groupId + "/member/" + membership?.id,
        { ...membership }
      )
      .then((response: HttpResponse<any>) => {
        fetchGroupMembers();
        close();
        if (response.status === 200 || response.status === 204) {
          addAlert(t("activateMembershipSuccess"), AlertVariant.success);
        } else {
          setLoading(false);

          addError("activateMembershipError", getError(response));
        }
      })
      .catch((err) => {
        setLoading(false);
        console.log(err);
      });
  };

  const noMembers = () => {
    return (
      <DataListItem key="emptyItem" aria-labelledby="empty-item">
        <DataListItemRow key="emptyRow">
          <DataListItemCells
            dataListCells={[
              <DataListCell key="empty">
                <strong>{t("adminGroupNoMembers")}</strong>
              </DataListCell>,
            ]}
          />
        </DataListItemRow>
      </DataListItem>
    );
  };

  const openUserInfoModal = (membership: any) => {
    setSelectedUserInfo(membership); // Pass both user and membership
  };

  const closeUserInfoModal = () => {
    setSelectedUserInfo(null); // Clear the selected user info
  };

  
  return (
    <React.Fragment>
      <UserInfoModal
        membership={selectedUserInfo}
        onClose={closeUserInfoModal}
      /> 
      <ConfirmationModal modalInfo={modalInfo} />
      <UserActionModal
        user={selectedUser}
        setUser={setSelectedUser}
        groupId={props.groupId}
        fetchGroupMembers={fetchGroupMembers}
      />
      <EditMembershipModal
        membership={editMembership}
        setMembership={setEditMembership}
        fetchGroupMembers={fetchGroupMembers}
      /> 
      <TableActionBar
        childComponent={
          <div className="gm_table-action-bar-row">
            <Tooltip
              position="right"
              content={
                <div>
                  {directMembers
                    ? t("directMembersTooltip")
                    : t("indirectMembersTooltip")}
                </div>
              }
            >
              <Checkbox
                className="gm_direct-member-checkbox"
                label={t("adminGroupViewAllMembersButton")}
                checked={directMembers}
                onClick={() => {
                  setDirectMembers(!directMembers);
                }}
                id="required-check"
                name="required-check"
              />
            </Tooltip>
            {props.isGroupAdmin && (
              <Button
                className="gm_invite-member-button"
                onClick={() => {
                  setInviteModalActive(true);
                }}
              >
                {t("addMember")}
              </Button>
            )}
          </div>
        }
        searchString={searchString}
        searchText={t("adminGroupSearchMember")}
        cancelText={t("adminGroupSearchCancel")}
        search={(searchString: string) => {
          setSearchString(searchString);
          setPage(1);
        }}
        cancel={() => {
          setSearchString("");
          setPage(1);
        }}
      />
      <DataList
        aria-label="Group Member Datalist"
        isCompact
        wrapModifier={"breakWord"}
      >
        <DataListItem aria-labelledby="compact-item1">
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[
                <DataListCell
                  className="gm_vertical_center_cell"
                  width={3}
                  key="id-hd"
                >
                  <strong>{t("username")}</strong>
                </DataListCell>,
                <DataListCell
                  className="gm_vertical_center_cell"
                  width={3}
                  key="email-hd"
                >
                  <div
                    className="gm_order_by_container"
                    onClick={() => {
                      orderResults("default");
                    }}
                  >
                    <strong>{t("adminGroupMemberCellNameEmail")}</strong>
                    {orderBy !== "default" ? (
                      <AngleDownIcon />
                    ) : asc ? (
                      <LongArrowAltDownIcon />
                    ) : (
                      <LongArrowAltUpIcon />
                    )}
                  </div>
                </DataListCell>,
                <DataListCell
                  className="gm_vertical_center_cell"
                  width={3}
                  key="roles-hd"
                >
                  <strong>{t("Roles")}</strong>
                  {props.groupConfiguration?.groupRoles && (
                    <DatalistFilterSelect
                      default={roleSelection}
                      name="group-roles"
                      options={Object.keys(props.groupConfiguration.groupRoles)}
                      optionsType="raw"
                      action={(selection:any) => {
                        setRoleSelection(selection);
                      }}
                    />
                  )}
                </DataListCell>,
                <DataListCell
                  className="gm_vertical_center_cell"
                  width={3}
                  key="valid-from-hd"
                >
                  <strong>{t("memberSince")}</strong>
                </DataListCell>,
                <DataListCell
                  className="gm_vertical_center_cell"
                  width={3}
                  key="expiration-hd"
                >
                  <div
                    className="gm_order_by_container"
                    onClick={() => {
                      orderResults("f.effectiveMembershipExpiresAt");
                    }}
                  >
                    <strong>{t("adminGroupMemberCellMembershipExp")}</strong>
                    {orderBy !== "f.effectiveMembershipExpiresAt" ? (
                      <AngleDownIcon />
                    ) : asc ? (
                      <LongArrowAltDownIcon />
                    ) : (
                      <LongArrowAltUpIcon />
                    )}
                  </div>
                  <Popover
                    bodyContent={
                      <div>{t("membershipExpiresAtPopoverDatalist")}</div>
                    }
                  >
                    <button
                      type="button"
                      aria-label="More info for name field"
                      onClick={(e) => e.preventDefault()}
                      aria-describedby="simple-form-name-01"
                      className="pf-c-form__group-label-help gm_popover-info"
                    >
                      <HelpIcon />
                    </button>
                  </Popover>
                </DataListCell>,
                <DataListCell
                  className="gm_vertical_center_cell"
                  width={2}
                  key="status-hd"
                >
                  <strong>
                    {t("Status")}
                    <DatalistFilterSelect
                      default={statusSelection}
                      name="group-status"
                      options={["ENABLED", "SUSPENDED", "PENDING"]}
                      action={(selection:string) => {
                        setStatusSelection(selection);
                      }}
                    />
                  </strong>
                </DataListCell>,
                ...(!directMembers
                  ? [
                      <DataListCell
                        className="gm_vertical_center_cell"
                        width={3}
                        key="group-path-hd"
                      >
                        <strong>{t("groupPath")}</strong>
                      </DataListCell>,
                      <DataListCell
                        className="gm_vertical_center_cell"
                        width={2}
                        key="direct-hd"
                      >
                        <strong>{t("adminGroupDirectMembership")}</strong>
                      </DataListCell>,
                    ]
                  : []),
              ]}
            ></DataListItemCells>
            <DataListAction
              className="gm_cell-center"
              aria-labelledby="check-action-item1 check-action-action2"
              id="check-action-action1"
              aria-label="Actions"
              isPlainButtonAction
            >
              <div className="gm_cell-placeholder"></div>
            </DataListAction>
          </DataListItemRow>
        </DataListItem>
        {loading ? (
          <div
            tabIndex={0}
            id="modal-no-header-description"
            className="gm_loader-modal-container"
          >
            <Spinner
              diameter="100px"
              aria-label="Contents of the custom size example"
            />
          </div>
        ) : groupMembers.length > 0 ? (
          groupMembers.map((member, index) => {
            let notificationWarningEffective =
              member.effectiveMembershipExpiresAt &&
              member?.group?.attributes["expiration-notification-period"][0] &&
              isFirstDateBeforeSecond(
                dateParse(member.effectiveMembershipExpiresAt),
                addDays(
                  new Date(new Date().setHours(0, 0, 0, 0)),
                  parseInt(
                    member?.group?.attributes[
                      "expiration-notification-period"
                    ][0]
                  )
                ),
                "warning"
              );

            let notificationWarningDirect =
              member.membershipExpiresAt &&
              member?.group?.attributes["expiration-notification-period"][0] &&
              isFirstDateBeforeSecond(
                dateParse(member.membershipExpiresAt),
                addDays(
                  new Date(new Date().setHours(0, 0, 0, 0)),
                  parseInt(
                    member?.group?.attributes[
                      "expiration-notification-period"
                    ][0]
                  )
                ),
                "warning"
              );

            return (
              <DataListItem aria-labelledby={"member-" + index} key={index}>
                <DataListItemRow>
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell width={3} key="primary content">
                        {member.user.username}
                      </DataListCell>,
                      <DataListCell width={3} key="secondary content1">
                        <span className="gm_fullname_datalist pf-c-select__menu-item-main">
                          {member.user.firstName && member.user.lastName
                            ? member.user.firstName + " " + member.user.lastName
                            : t("notAvailable")}
                        </span>
                        <span className="gm_email_datalist pf-c-select__menu-item-description">
                          {member.user.email}
                        </span>
                      </DataListCell>,
                      <DataListCell width={3} key="secondary content2">
                        {member.groupRoles.map((role, index) => {
                          return (
                            <Badge key={index} className="gm_role_badge" isRead>
                              {role}
                            </Badge>
                          );
                        })}
                      </DataListCell>,
                      <DataListCell width={3}>{member.validFrom}</DataListCell>,
                      <DataListCell width={3}>
                        {member.effectiveGroupId && (
                          <div>
                            <strong>Effective: </strong>
                            {notificationWarningEffective ? (
                              <Popover
                                bodyContent={(hide:any) => (
                                  <div>
                                    {t(
                                      "membershipExpirationEffectiveNotification"
                                    )}
                                    <Button
                                      className="gm_popover-expiration-button"
                                      onClick={() => {
                                        if (
                                          member.effectiveGroupId === groupId
                                        ) {
                                          if (member.group.id !== groupId) {
                                            const searchParams =
                                              new URLSearchParams(
                                                location.hash.split("?")[1]
                                              );
                                            searchParams.set(
                                              "membership",
                                              member?.id || ""
                                            );
                                            props.history.push({
                                              search: searchParams.toString()
                                                ? `?${searchParams.toString()}`
                                                : "",
                                            });
                                            setDirectMembers(true);
                                            setSearchString(
                                              member.user.username
                                            );
                                          } else {
                                            setEditMembership(member);
                                          }
                                          hide();
                                        } else {
                                          props.history.push({
                                            pathname:
                                              "/groups/admingroups/" +
                                              member.effectiveGroupId,
                                            search:
                                              "?tab=members&memberId=" +
                                              member.user.username +
                                              "&membership=" +
                                              member.id,
                                          });
                                        }
                                      }}
                                    >
                                      Extend
                                    </Button>
                                  </div>
                                )}
                              >
                                <span className="gm_effective-expiration-popover-trigger">
                                  <div
                                    style={{ display: "inline-block" }}
                                    className={"gm_warning-text"}
                                  >
                                    {member.effectiveMembershipExpiresAt ||
                                      t("Never")}
                                  </div>
                                  <div className="gm_effective-helper-warning">
                                    <ExclamationTriangleIcon />
                                  </div>
                                </span>
                              </Popover>
                            ) : member.effectiveGroupId === groupId ? (
                              member.effectiveMembershipExpiresAt
                            ) : (
                              <a
                                onClick={() => {
                                  props.history.push({
                                    pathname:
                                      "/groups/admingroups/" +
                                      member.effectiveGroupId,
                                    search:
                                      "?tab=members&memberId=" +
                                      member.user.username,
                                  });
                                }}
                              >
                                {member.effectiveMembershipExpiresAt}
                              </a>
                            )}
                          </div>
                        )}
                        <div>
                          <strong>Direct: </strong>
                          {notificationWarningDirect ? (
                            <Popover
                              bodyContent={(hide:any) => (
                                <div>
                                  {t("membershipExpirationNotification")}
                                  <Button
                                    className="gm_popover-expiration-button"
                                    // isSmall
                                    onClick={() => {
                                      setEditMembership(member);
                                      hide();
                                    }}
                                  >
                                    Extend
                                  </Button>
                                </div>
                              )}
                            >
                              <span className="gm_effective-expiration-popover-trigger">
                                <div
                                  style={{ display: "inline-block" }}
                                  className={"gm_warning-text"}
                                >
                                  {member.membershipExpiresAt || t("Never")}
                                </div>
                                <div className="gm_effective-helper-warning">
                                  <ExclamationTriangleIcon />
                                </div>
                              </span>
                            </Popover>
                          ) : member.membershipExpiresAt ? (
                            <>{member.membershipExpiresAt}</>
                          ) : (
                            t("Never")
                          )}
                        </div>
                      </DataListCell>,
                      <DataListCell width={2}>
                        <Tooltip
                          content={
                            <div>
                              {member.status === "ENABLED"
                                ? t("adminGroupMemberUserActiveTooltip")
                                : member.status === "SUSPENDED"
                                ? t("adminGroupMemberUserSuspendedTooltip")
                                : member.status === "PENDING"
                                ? t("adminGroupMemberUserPendingTooltip")
                                : ""}
                            </div>
                          }
                        >
                          <div className="gm_user-status-container">
                            <div
                              className={
                                member.status === "ENABLED"
                                  ? "gm_icon gm_icon-active-user"
                                  : member.status === "SUSPENDED"
                                  ? "gm_icon gm_icon-suspended-user"
                                  : member.status === "PENDING"
                                  ? "gm_icon gm_icon-pending-user"
                                  : ""
                              }
                            ></div>
                          </div>
                        </Tooltip>
                      </DataListCell>,
                      ...(!directMembers
                        ? [
                            <DataListCell width={3} key="secondary content5">
                              <Link
                                to={{
                                  pathname:
                                    "/groups/admingroups/" + member.group.id,
                                  search: "?tab=members",
                                }}
                              >
                                {member.group.path}
                              </Link>
                            </DataListCell>,
                            <DataListCell width={2} key="secondary content6">
                              <Tooltip
                                content={
                                  <div>
                                    {member.direct
                                      ? t("adminGroupIsDirect")
                                      : t("adminGroupIsNotDirect")}
                                  </div>
                                }
                              >
                                <Checkbox
                                  id="disabled-check-1"
                                  className="gm_direct-checkbox"
                                  checked={member.direct ? true : false}
                                  isDisabled
                                />
                              </Tooltip>
                            </DataListCell>,
                          ]
                        : []),
                    ]}
                  />
                  <DataListAction
                    className="gm_cell-center"
                    aria-labelledby="check-action-item1 check-action-action2"
                    id="check-action-action1"
                    aria-label="Actions"
                    isPlainButtonAction
                  >
                    <div className="gm_actions_container">
                      {/* Kebab menu for small screens */}
                      {/* <Dropdown
                        onSelect={onKebabSelect}
                        toggle={
                          <KebabToggle
                            onToggle={(isOpen) => {
                              onKebabToggle(isOpen, member.id);
                            }}
                          />
                        }
                        isOpen={isKebabOpen === member.id}
                        isPlain
                        position="right"
                        dropdownItems={kebabItems(member)}
                      /> */}
                      {/* Buttons for larger screens */}
                      <Tooltip
                        content={
                          <div>{t("viewDetailsMembershipInformation")}</div>
                        }
                      >
                        <Button
                          size="sm"
                          variant="primary"
                          className="gm_small_icon_button"
                          onClick={() => openUserInfoModal(member)}
                        >
                          <UserIcon />
                        </Button>
                      </Tooltip>
                      {props.isGroupAdmin && (
                        <React.Fragment>
                          <Tooltip
                            content={
                              <div>
                                {member.user.id === props.user.userId
                                  ? t("leaveGroup")
                                  : t("adminGroupMemberRemove")}
                              </div>
                            }
                          >
                            <Button
                              // isSmall
                              size="sm"
                              variant="secondary"
                              ouiaId="DangerSecondary"
                              isDanger
                              className="gm_small_icon_button"
                              onClick={() => {
                                setModalInfo({
                                  title: t("Confirmation"),
                                  accept_message: t("YES"),
                                  cancel_message: t("NO"),
                                  message: t(
                                    "adminGroupMemberRemoveConfirmation"
                                  ),
                                  accept: function () {
                                    deleteGroupMember(
                                      member.id,
                                      member.group.id
                                    );
                                    setModalInfo({});
                                  },
                                  cancel: function () {
                                    setModalInfo({});
                                  },
                                });
                              }}
                            >
                              <TimesIcon />
                            </Button>
                          </Tooltip>
                          <Tooltip
                            content={
                              <div>
                                {member.status === "SUSPENDED"
                                  ? t("editMembershipDisabled")
                                  : t("editMembership")}
                              </div>
                            }
                          >
                            <div>
                              <Button
                                // isSmall
                                size="sm"
                                variant="tertiary"
                                isDisabled={member.status === "SUSPENDED"}
                                className="gm_small_icon_button"
                                onClick={() => {
                                  if (member.direct) {
                                    setEditMembership(member);
                                  } else {
                                    setEditMembership(member);
                                  }
                                }}
                              >
                                <PencilAltIcon />
                              </Button>
                            </div>
                          </Tooltip>
                        </React.Fragment>
                      )}
                      {props.isGroupAdmin ? (
                        <Tooltip
                          content={
                            <div>
                              {member.status === "ENABLED"
                                ? t("adminGroupMemberSuspendTooltip")
                                : member.status === "SUSPENDED"
                                ? t("adminGroupMemberActivateTooltip")
                                : t("adminGroupMemberActivatePendingTooltip")}
                            </div>
                          }
                        >
                          <Button
                            // isSmall
                            size="sm"
                            variant={
                              member.status === "ENABLED" ? "danger" : "warning"
                            }
                            className="gm_small_icon_button"
                            onClick={() => {
                              if (member.status === "PENDING") {
                                setModalInfo({
                                  title: t("Confirmation"),
                                  accept_message: t("YES"),
                                  cancel_message: t("NO"),
                                  message: t(
                                    "Do you want to activate this membership?"
                                  ),
                                  accept: function () {
                                    activatePendingMembership(member);
                                    setModalInfo({});
                                  },
                                  cancel: function () {
                                    setModalInfo({});
                                  },
                                });
                              } else {
                                setSelectedUser(member);
                              }
                            }}
                          >
                            {member.status === "ENABLED" ? (
                              <LockIcon />
                            ) : member.status === "SUSPENDED" ? (
                              <LockOpenIcon />
                            ) : (
                              <OutlinedClockIcon />
                            )}
                          </Button>
                        </Tooltip>
                      ) : (
                        <div className="gm_placeholder_membership_action"></div>
                      )}
                    </div>
                  </DataListAction>
                </DataListItemRow>
              </DataListItem>
            );
          })
        ) : (
          noMembers()
        )}
      </DataList>
      <Pagination
        itemCount={totalItems}
        perPage={perPage}
        page={page}
        onSetPage={onSetPage}
        widgetId="top-example"
        onPerPageSelect={onPerPageSelect}
      />
      {props.isGroupAdmin && (
        <AddMemberWizard
          active={inviteModalActive}
          setActive={setInviteModalActive}
          groupConfiguration={props.groupConfiguration}
          fetchGroupMembers={fetchGroupMembers}
          groupId={props.groupId}
        />
      )}
    </React.Fragment>
  );
};
