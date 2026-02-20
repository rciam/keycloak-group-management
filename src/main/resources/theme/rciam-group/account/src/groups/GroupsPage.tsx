import * as React from "react";
import { Link } from "react-router-dom";
import { FC, useState, useEffect } from "react";
import {
  LongArrowAltDownIcon,
  LongArrowAltUpIcon,
  AngleDownIcon,
  ExclamationTriangleIcon,
  InfoCircleIcon,
  EllipsisVIcon,
} from "@patternfly/react-icons";
import {
  DataList,
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListItemCells,
  Pagination,
  Badge,
  DataListAction,
  Popover,
  //  Dropdown, DropdownItem,
  Spinner,
  Breadcrumb,
  BreadcrumbItem,
  AlertVariant,
  Dropdown,
  DropdownItem,
  MenuToggleElement,
  MenuToggle,
  DropdownList,
} from "@patternfly/react-core";
import {
  dateParse,
  addDays,
  isFirstDateBeforeSecond,
  formatDateToString,
} from "../widgets/Date";
import { getError, kcPath } from "../js/utils";
// @ts-ignore
// import { ContentPage } from '../ContentPage';
// import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { Button } from "@patternfly/react-core";
// import { ConfirmationModal } from '../../group-widgets/Modals';
// import { ContentAlert } from '../ContentAlert';
import { useTranslation } from "react-i18next";
import { Page } from "@keycloak/keycloak-account-ui";
import { useGroupsService } from "../groups-service/GroupsServiceContext";
import { ConfirmationModal } from "../widgets/Modals";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { HttpResponse } from "../groups-service/groups-service";
// import { useAlerts } from "@keycloak/keycloak-ui-shared";
// import { AlertVariant } from "@patternfly/react-core";
// import { GroupsServiceError } from "../groups-service/groups-service";

export interface GroupsPageProps {
  // history: any;
}

export interface GroupsPageState {
  groups: Group[];
  directGroups: Group[];
  isDirectMembership: boolean;
}

interface Group {
  id?: string;
  name: string;
  path: string;
}

interface GroupsResponse {
  results: Group[];
  count: number;
}
export const GroupsPage: FC<GroupsPageProps> = () => {
  const { t } = useTranslation();
  const groupsService = useGroupsService();
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);

  const [groups, setGroups] = useState([] as Group[]);
  const [totalItems, setTotalItems] = useState<number>(0);
  const [orderBy, setOrderBy] = useState<string>("");
  const [asc, setAsc] = useState<boolean>(true);
  const [loading, setLoading] = useState<boolean>(false);
  const [modalInfo, setModalInfo] = useState({});

  useEffect(() => {
    fetchGroups();
  }, []);

  useEffect(() => {
    fetchGroups();
  }, [perPage, page, orderBy, asc]);

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

  const fetchGroups = () => {
    setLoading(true);
    groupsService!
      .doGet<GroupsResponse>("/user/groups", {
        params: {
          first: perPage * (page - 1),
          max: perPage,
          ...(orderBy ? { order: orderBy } : {}),
          asc: asc ? "true" : "false",
        },
      })
      .then((response: any) => {
        setLoading(false);
        let count = response?.data?.count || 0;
        setTotalItems(count as number);
        setGroups(response?.data?.results || ([] as Group[]));
      });
  };

  const emptyGroup = () => {
    return (
      <DataListItem key="emptyItem" aria-labelledby="empty-item">
        <DataListItemRow key="emptyRow">
          <DataListItemCells
            dataListCells={[
              <DataListCell key="empty">
                <strong>{t("noGroupsText")}</strong>
              </DataListCell>,
            ]}
          />
        </DataListItemRow>
      </DataListItem>
    );
  };

  const orderResults = (type: any) => {
    if (orderBy !== type) {
      setOrderBy(type);
      setAsc(true);
    } else if (asc) {
      setAsc(false);
    } else {
      setAsc(true);
    }
  };

  return (
    <div className="gm_content">
      <div className="pf-v5-c-page__main-section pf-m-light gm_breadcrumb-container">
      <Breadcrumb className="gm_breadcrumb">
        <BreadcrumbItem isActive>{t("showgroupsSidebarTitle")}</BreadcrumbItem>
      </Breadcrumb>
      </div>
      <Page
        title={t("showgroupsSidebarTitle")}
        description={t("showgroupsIntroMessage")}
      >
        <ConfirmationModal modalInfo={modalInfo} />
        <DataList
          id="groups-list"
          aria-label={t("groupLabel")}
          isCompact
          wrapModifier={"breakWord"}
        >
          <DataListItem id="groups-list-header" aria-labelledby="Columns names">
            <DataListItemRow className="gm_view-groups-header">
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    key="group-name-header"
                    width={2}
                    onClick={() => {
                      orderResults("");
                    }}
                  >
                    <strong>{t("nameDatalistTitle")}</strong>
                    {!orderBy ? (
                      <AngleDownIcon />
                    ) : asc ? (
                      <LongArrowAltDownIcon />
                    ) : (
                      <LongArrowAltUpIcon />
                    )}
                  </DataListCell>,
                  <DataListCell key="group-path" width={2}>
                    <strong>{t("groupPath")}</strong>
                  </DataListCell>,
                  <DataListCell key="group-roles" width={2}>
                    <strong>{t("rolesDatalistTitle")}</strong>
                  </DataListCell>,
                  <DataListCell
                    key="group-membership-expiration-header"
                    width={2}
                  >
                    <div
                      className="gm_order_by_container"
                      onClick={() => {
                        orderResults("effectiveMembershipExpiresAt");
                      }}
                    >
                      <strong>{t("membershipDatalistTitle")}</strong>
                      {orderBy !== "effectiveMembershipExpiresAt" ? (
                        <AngleDownIcon />
                      ) : asc ? (
                        <LongArrowAltDownIcon />
                      ) : (
                        <LongArrowAltUpIcon />
                      )}
                    </div>
                    {/* <div className="gm_group-memberships-more-info">
                    <Popover
                      bodyContent={
                        <div>
                          t('membershipExpiresAtPopoverDatalist' />
                        </div>
                      }
                    >
                      <button
                        type="button"
                        aria-label="More info for name field"
                        onClick={e => e.preventDefault()}
                        aria-describedby="simple-form-name-01"
                        className="pf-c-form__group-label-help"
                      >
                        <HelpIcon />
                      </button>
                    </Popover>
                  </div> */}
                  </DataListCell>,
                ]}
              />
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
          ) : groups.length === 0 ? (
            emptyGroup()
          ) : (
            groups.map((group: Group, appIndex: number) => {
              return (
                <MembershipDatalistItem
                  key={
                    group.id
                      ? `membership-${group.id}`
                      : `membership-${appIndex}`
                  }
                  membership={group}
                  // history={props.history}
                  fetchGroups={fetchGroups}
                  setLoading={setLoading}
                  setModalInfo={setModalInfo}
                  currentDate={new Date(new Date().setHours(0, 0, 0, 0))}
                  appIndex={appIndex}
                />
              );
            })
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
      </Page>
    </div>
  );
};

const MembershipDatalistItem = (props: any) => {
  const [isOpen, setIsOpen] = useState(false);
  const { t } = useTranslation();

  const [expirationWarning, setExpirationWarning] = useState(false);
  const groupsService = useGroupsService();
  const { addAlert, addError } = useAlerts();
  const [effectiveGroupPath, setEffectiveGroupPath] = useState("");

  // Compute expirationWarning and fetch group data on membership change
  useEffect(() => {
    if (
      props.membership?.effectiveMembershipExpiresAt &&
      props.membership?.group?.attributes["expiration-notification-period"][0]
    ) {
      const warning = isFirstDateBeforeSecond(
        dateParse(props.membership.effectiveMembershipExpiresAt),
        addDays(
          props.currentDate,
          parseInt(
            props.membership.group.attributes[
              "expiration-notification-period"
            ][0]
          )
        ),
        "warning"
      );
      setExpirationWarning(!!warning); // Set warning as true or false
    }
    if (props.membership?.effectiveGroupId) {
      fetchGroup();
    }
  }, [props.membership]);

  const onToggle = () => {
    setIsOpen(!isOpen);
  };

  const leaveGroup = () => {
    props.setLoading(true);
    groupsService!
      .doDelete<any>("/user/group/" + props.membership.group.id + "/member")
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          addAlert(t("leaveGroupSuccess"), AlertVariant.success);
          props.fetchGroups();
        } else {
          addError("leaveGroupError", getError(response));
        }
        props.setLoading(false);
      })
      .catch((err) => {
        const response = err?.response ?? err;
        addError("leaveGroupError", getError(response));
        console.log(err);
      });
  };

  // Fetch the group path based on the effectiveGroupId
  const fetchGroup = () => {
    groupsService!
      .doGet<any>(`/user/group/${props.membership?.effectiveGroupId}/member`)
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setEffectiveGroupPath(response.data.group.path);
        }
      });
  };

  const onSelect = () => {
    setIsOpen(false);
    const element = document.getElementById("toggle-kebab");
    element && element.focus();
  };

  return (
    <DataListItem
      id={`${props.appIndex}-group`}
      key={
        props.membership?.group?.id
          ? "group-" + props.membership.group.id
          : "group-" + props.appIndex
      }
      aria-labelledby="groups-list"
    >
      <DataListItemRow>
        <DataListItemCells
          dataListCells={[
            <DataListCell
              id={`${props.appIndex}-group-name`}
              width={2}
              key={"name-" + props.appIndex}
            >
              <Link
                to={kcPath("/groups/showgroups/" + props.membership.group.id)}
              >
                {props.membership.group.name}
              </Link>
            </DataListCell>,
            <DataListCell
              id={`${props.appIndex}-group-path`}
              width={2}
              key={"groupPath-" + props.appIndex}
            >
              {props.membership.group.path}
            </DataListCell>,
            <DataListCell
              id={`${props.appIndex}-group-roles`}
              width={2}
              key={"roles-" + props.appIndex}
            >
              {props.membership.groupRoles.map((role: any, index: any) => (
                <Badge
                  key={`role-${index}-${role}`}
                  className="gm_role_badge"
                  isRead
                >
                  {role}
                </Badge>
              ))}
            </DataListCell>,
            <DataListCell
              id={`${props.appIndex}-group-membershipExpiration`}
              width={2}
              key={"membershipExpiration-" + props.appIndex}
            >
              <Popover
                {...(!(
                  props.membership?.effectiveGroupId || expirationWarning
                ) && { isVisible: false })}
                bodyContent={
                  <div>
                    {expirationWarning && props.membership?.effectiveGroupId ? (
                      <>
                        {t("membershipExpirationEffectiveNotification")}
                        <Link
                          to={kcPath(
                            `/enroll?groupPath=${encodeURI(effectiveGroupPath)}`
                          )}
                        >
                          <Button className="gm_popover-expiration-button">
                            Extend
                          </Button>
                        </Link>
                      </>
                    ) : expirationWarning ? (
                      <>
                        {t("membershipExpirationNotification")}
                        <Link
                          to={kcPath(
                            `/enroll?groupPath=${encodeURI(
                              props.membership.group.path
                            )}`
                          )}
                        >
                          <Button className="gm_popover-expiration-button">
                            Extend
                          </Button>
                        </Link>{" "}
                      </>
                    ) : (
                      <>
                        {t("effectiveExpirationHelp")}
                        <Link
                          to={kcPath(
                            `/groups/showgroups/${props.membership?.effectiveGroupId}`
                          )}
                        >
                          <Button className="gm_popover-expiration-button">
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
                      className={expirationWarning ? "gm_warning-text" : ""}
                    >
                      {props.membership.effectiveMembershipExpiresAt
                        ? formatDateToString(
                            dateParse(
                              props.membership.effectiveMembershipExpiresAt
                            )
                          )
                        : t("Never")}
                    </div>
                    <div className="gm_effective-helper-warning">
                      <ExclamationTriangleIcon />
                    </div>
                  </span>
                ) : props.membership?.effectiveGroupId ? (
                  <span className="gm_effective-expiration-popover-trigger">
                    <div
                      style={{ display: "inline-block" }}
                      className={expirationWarning ? "gm_warning-text" : ""}
                    >
                      {props.membership.effectiveMembershipExpiresAt
                        ? formatDateToString(
                            dateParse(
                              props.membership.effectiveMembershipExpiresAt
                            )
                          )
                        : t("Never")}
                    </div>
                    <div className="gm_effective-helper-info">
                      <InfoCircleIcon />
                    </div>
                  </span>
                ) : (
                  <span className="gm_effective-expiration-popover-trigger">
                    <div style={{ display: "inline-block" }}>
                      {props.membership.effectiveMembershipExpiresAt
                        ? formatDateToString(
                            dateParse(
                              props.membership.effectiveMembershipExpiresAt
                            )
                          )
                        : t("Never")}
                    </div>
                  </span>
                )}
              </Popover>
            </DataListCell>,
          ]}
        />
        <DataListAction
          className="gm_cell-center gm_kebab-menu-cell"
          aria-labelledby="check-action-item1 check-action-action2"
          id="check-action-action1"
          aria-label="Actions"
          isPlainButtonAction
        >
          <Dropdown
            // alignments={{ sm: 'right', md: 'right', lg: 'right', xl: 'right', '2xl': 'right' }}
            onSelect={onSelect}
            popperProps={{ position: "right" }}
            onOpenChange={(isOpen: boolean) => setIsOpen(isOpen)}
            toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
              <MenuToggle
                ref={toggleRef}
                onClick={onToggle}
                isExpanded={isOpen}
                variant="plain"
                className="gm_badge_toggle"
              >
                <EllipsisVIcon />
              </MenuToggle>
            )}
            // toggle={<KebabToggle id="toggle-kebab" onToggle={onToggle} />}
            isOpen={isOpen}
            isPlain
          >
            <DropdownList>
              <Link
                to={kcPath(
                  `/enroll?groupPath=${encodeURI(props.membership.group.path)}`
                )}
              >
                <DropdownItem key="link">
                  {t("enrollmentDiscoveryPageLink")}
                </DropdownItem>
              </Link>
              <DropdownItem
                onClick={() => {
                  props.setModalInfo({
                    title: t("leaveGroup") + "?",
                    button_variant: "danger",
                    accept_message: "Leave",
                    cancel_message: "Cancel",
                    message: t("leaveGroupConfirmation"),
                    accept: function () {
                      leaveGroup();
                      props.setModalInfo({});
                    },
                    cancel: function () {
                      props.setModalInfo({});
                    },
                  });
                }}
              >
                {t("leaveGroup")}
              </DropdownItem>
            </DropdownList>
          </Dropdown>
        </DataListAction>
      </DataListItemRow>
    </DataListItem>
  );
};
