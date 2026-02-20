import * as React from "react";
import { FC, useState, useEffect } from "react";
import {
  Tabs,
  Tab,
  TabTitleText,
  DataList,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  DataListCell,
  Breadcrumb,
  Page,
  BreadcrumbItem,
  Badge,
  Popover,
  Title,
} from "@patternfly/react-core";
import {
  ExclamationTriangleIcon,
  InfoCircleIcon,
} from "@patternfly/react-icons";
import {
  dateParse,
  addDays,
  isFirstDateBeforeSecond,
  formatDateToString,
} from "../widgets/Date";
import { Link, useParams, useNavigate } from "react-router-dom";
import { Button } from "@patternfly/react-core";
import { ConfirmationModal } from "../widgets/Modals";
import { getError, kcPath } from "../js/utils.js";
import { useLoader } from "../widgets/LoaderContext";
import { useTranslation } from "react-i18next";
import { useGroupsService } from "../groups-service/GroupsServiceContext";
import { HttpResponse } from "../groups-service/groups-service.js";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

export interface GroupsPageProps {
  // history: any;
  // match: any;
}

export interface GroupsPageState {
  group_id: any;
  group_membership: GroupMembership;
}
interface User {
  id: string;
  username: string;
  emailVerified: boolean;
  email: string;
  federatedIdentities: object;
}

interface Attributes {
  description: string[];
  "expiration-notification-period": any;
}

interface Group {
  id: string;
  name: string;
  path: string;
  attributes: Attributes;
}

interface GroupMembership {
  id?: string;
  group: Group;
  user: User;
  status: string;
  membershipExpiresAt: string;
  effectiveMembershipExpiresAt: string;
  effectiveGroupId: string;
  aupExpiresAt: string;
  validFrom: string;
  groupRoles: string[];
}

// export class GroupPage extends React.Component<GroupsPageProps, GroupsPageState> {
export const GroupPage: FC<GroupsPageProps> = () => {
  const { t } = useTranslation();
  const groupsService = useGroupsService();
  const { groupId } = useParams<any>();
  const navigate = useNavigate();
  const [groupMembership, setGroupMembership] = useState({} as GroupMembership);
  const [activeTabKey, setActiveTabKey] = React.useState<string | number>(0);
  const [expirationWarning, setExpirationWarning] = useState(false);
  const [effectiveGroupPath, setEffectiveGroupPath] = useState("");
  const [modalInfo, setModalInfo] = useState({});
  const { startLoader, stopLoader } = useLoader();
  const { addError } = useAlerts();

  useEffect(() => {
    fetchGroups();
  }, [groupId]);

  useEffect(() => {
    if (
      groupMembership &&
      groupMembership?.effectiveMembershipExpiresAt &&
      groupMembership?.group?.attributes["expiration-notification-period"][0]
    ) {
      let warning = isFirstDateBeforeSecond(
        dateParse(groupMembership.effectiveMembershipExpiresAt),
        addDays(
          new Date(new Date().setHours(0, 0, 0, 0)),
          parseInt(
            groupMembership.group.attributes[
              "expiration-notification-period"
            ][0],
          ),
        ),
        "warning",
      );
      setExpirationWarning(!!warning);
      if (groupMembership?.effectiveGroupId) {
        fetchParentPath();
      }
    }
  }, [groupMembership]);

  const handleTabClick = (
    _event: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent,
    tabIndex: string | number,
  ) => {
    setActiveTabKey(tabIndex);
  };

  const leaveGroup = () => {
    startLoader();
    groupsService!
      .doDelete<any>("/user/group/" + groupId + "/member")
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          navigate("/groups/showgroups");
        } else {
          addError("leaveGroupError", getError(response));
        }
        stopLoader();
      });
  };

  let fetchParentPath = () => {
    groupsService!
      .doGet<any>(
        "/user/group/" + groupMembership?.effectiveGroupId + "/member",
      )
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setEffectiveGroupPath(response.data.group.path);
        }
      });
  };

  let fetchGroups = () => {
    startLoader();
    groupsService!
      .doGet<GroupMembership>("/user/group/" + groupId + "/member")
      .then((response: HttpResponse<GroupMembership>) => {
        stopLoader();
        if (response.status === 200 && response.data) {
          setGroupMembership(response.data);
        }
      });
  };

  return (
    <>
      <div className={"gm_content "}>
        <div className="pf-v5-c-page__main-section pf-m-light gm_breadcrumb-container">
          <Breadcrumb className="gm_breadcrumb">
            <BreadcrumbItem
              to="#"
              onClick={() => {
                navigate(kcPath("groups/showgroups"));
              }}
            >
              {t("groupLabel")}
            </BreadcrumbItem>
            {groupMembership?.group?.path
              .split("/")
              .filter((item) => item)
              .map((value) => {
                return <BreadcrumbItem>{value}</BreadcrumbItem>;
              })}
          </Breadcrumb>
        </div>
        <ConfirmationModal modalInfo={modalInfo} />
        <Page className="pf-v5-c-page__main-section pf-m-light gm_page">
          <div className="gm_group-header">
            <Title headingLevel="h1">
              {groupMembership?.group?.name || ""}
            </Title>

            <div className="gm_page-description">
              {(groupMembership?.group?.attributes?.description &&
                groupMembership?.group?.attributes?.description[0]) ||
                t("noDescription")}
            </div>
          </div>
          <div className="gm_view-group-action-container">
            <Link
              to={
                kcPath("/enroll?groupPath=") +
                encodeURI(groupMembership?.group?.path)
              }
            >
              <Button>Update Membership</Button>
            </Link>
            <Button
              variant="danger"
              onClick={() => {
                setModalInfo({
                  title: t("leaveGroup") + "?",
                  button_variant: "danger",
                  accept_message: "Leave",
                  cancel_message: "Cancel",
                  message: t("leaveGroupConfirmation"),
                  accept: function () {
                    leaveGroup();
                    setModalInfo({});
                  },
                  cancel: function () {
                    setModalInfo({});
                  },
                });
              }}
            >
              Leave Group
            </Button>
          </div>
          <Tabs
            className="gm_tabs"
            activeKey={activeTabKey}
            onSelect={handleTabClick}
            isBox={false}
            aria-label={"Tabs in the default example"}
            role="region"
          >
            <Tab
              eventKey={0}
              title={<TabTitleText>{t("groupMembershipTab")}</TabTitleText>}
              aria-label="Default content - users"
            >
              <DataList
                className="gm_datalist"
                aria-label="Compact data list example"
                isCompact
                wrapModifier={"breakWord"}
              >
                <DataListItem aria-labelledby="group-path-item">
                  <DataListItemRow>
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell key="title">
                          <span id="compact-item2">
                            <strong>{t("groupPath")}</strong>
                          </span>
                        </DataListCell>,
                        <DataListCell key="value">
                          <span>
                            {groupMembership?.group?.path || t("notAvailable")}
                          </span>
                        </DataListCell>,
                      ]}
                    />
                  </DataListItemRow>
                </DataListItem>
                <DataListItem aria-labelledby="compact-item2">
                  <DataListItemRow>
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell key="primary content">
                          <span id="compact-item2">
                            <strong>{t("groupDatalistCellRoles")}</strong>
                          </span>
                        </DataListCell>,
                        <DataListCell key="group-roles-secondary">
                          {groupMembership?.groupRoles
                            ? groupMembership?.groupRoles.map((role, index) => {
                                return (
                                  <Badge
                                    key={index}
                                    className="gm_role_badge"
                                    isRead
                                  >
                                    {role}
                                  </Badge>
                                );
                              })
                            : t("groupDatalistCellNoRoles")}
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
                          <strong>
                            {t("adminGroupMemberCellMembershipExp")}
                          </strong>
                        </DataListCell>,
                        <DataListCell key="membership-expiration-secondary">
                          {groupMembership?.effectiveGroupId ||
                          expirationWarning ? (
                            <Popover
                              {...(!(
                                groupMembership?.effectiveGroupId ||
                                expirationWarning
                              ) && { isVisible: false })}
                              bodyContent={
                                <div>
                                  {expirationWarning &&
                                  groupMembership?.effectiveGroupId ? (
                                    <>
                                      {t(
                                        "membershipExpirationEffectiveNotification",
                                      )}
                                      <Link
                                        to={
                                          kcPath("/enroll?groupPath=") +
                                          encodeURI(effectiveGroupPath)
                                        }
                                      >
                                        <Button
                                          className="gm_popover-expiration-button"
                                          size="sm"
                                        >
                                          Extend
                                        </Button>
                                      </Link>
                                    </>
                                  ) : expirationWarning ? (
                                    <>
                                      {t("membershipExpirationNotification")}
                                      <Link
                                        to={
                                          kcPath("/enroll?groupPath=") +
                                          encodeURI(
                                            groupMembership?.group?.path,
                                          )
                                        }
                                      >
                                        <Button
                                          className="gm_popover-expiration-button"
                                          size="sm"
                                        >
                                          Extend
                                        </Button>
                                      </Link>
                                    </>
                                  ) : (
                                    <>
                                      {t("effectiveExpirationHelp")}
                                      <Link
                                        to={
                                          "/groups/showgroups/" +
                                          groupMembership?.effectiveGroupId
                                        }
                                      >
                                        <Button
                                          className="gm_popover-expiration-button"
                                          size="sm"
                                        >
                                          View
                                        </Button>
                                      </Link>
                                    </>
                                  )}
                                </div>
                              }
                            >
                              {expirationWarning ? (
                                <span className="gm_effective-expiration-popover-trigger">
                                  <div
                                    style={{ display: "inline-block" }}
                                    className={
                                      expirationWarning ? "gm_warning-text" : ""
                                    }
                                  >
                                    {groupMembership?.effectiveMembershipExpiresAt
                                      ? formatDateToString(
                                          dateParse(
                                            groupMembership?.effectiveMembershipExpiresAt,
                                          ),
                                        )
                                      : t("Never")}
                                  </div>
                                  <div className="gm_effective-helper-warning">
                                    <ExclamationTriangleIcon />
                                  </div>
                                </span>
                              ) : (
                                <span className="gm_effective-expiration-popover-trigger">
                                  <div
                                    style={{ display: "inline-block" }}
                                    className={
                                      expirationWarning ? "gm_warning-text" : ""
                                    }
                                  >
                                    {groupMembership?.effectiveMembershipExpiresAt
                                      ? formatDateToString(
                                          dateParse(
                                            groupMembership?.effectiveMembershipExpiresAt,
                                          ),
                                        )
                                      : t("Never")}
                                  </div>
                                  <div className="gm_effective-helper-info">
                                    <InfoCircleIcon />
                                  </div>
                                </span>
                              )}
                            </Popover>
                          ) : (
                            <div>
                              {groupMembership?.effectiveMembershipExpiresAt
                                ? formatDateToString(
                                    dateParse(
                                      groupMembership?.effectiveMembershipExpiresAt,
                                    ),
                                  )
                                : t("Never")}
                            </div>
                          )}
                        </DataListCell>,
                      ]}
                    />
                  </DataListItemRow>
                </DataListItem>
              </DataList>
            </Tab>
          </Tabs>
        </Page>
      </div>
    </>
  );
};
