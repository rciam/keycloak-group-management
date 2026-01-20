import * as React from "react";
import { FC, useState, useEffect } from "react";
import {
  DataListContent,
  DataList,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  DataListCell,
  Breadcrumb,
  BreadcrumbItem,
  Pagination,
  DataListAction,
  // Dropdown,
  // KebabToggle,
  // DropdownItem,
  Tooltip,
  DropdownItem,
  Dropdown,
  MenuToggle,
  MenuToggleElement,
  DropdownList,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
//==import { fa-search } from '@patternfly/react-icons';
//import { faSearch } from '@fortawesome/free-solid-svg-icons';

// @ts-ignore
// import { ContentPage } from "../ContentPage";
// import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
// import { Msg } from "../../widgets/Msg";
import { TableActionBar } from "../widgets/TableActionBar";
// import { CreateGroupModal, DeleteSubgroupModal } from '../../group-widgets/Modals';
import { Spinner } from "@patternfly/react-core";
import { useGroupsService } from "../groups-service/GroupsServiceContext";
import { Page } from "@keycloak/keycloak-account-ui";
import { kcPath } from "../js/utils";
import { EllipsisVIcon } from "@patternfly/react-icons";
import { CreateGroupModal, DeleteSubgroupModal } from "./components/Modals";

export interface AdminGroupsPageProps {
  // match: any;
}

export interface AdminGroupsPageState {
  groups: AdminGroup[];
  directGroups: AdminGroup[];
  isDirectMembership: boolean;
}

interface AdminGroup {
  id?: string;
  name: string;
  path: string;
  extraSubGroups: AdminGroup[];
}

// interface User {}

// interface Response {
//   results: AdminGroup[];
//   count: BigInteger;
// }

// interface User {
//   userId?: string;
//   displayName: string;
// }

export const AdminGroupsPage: FC<AdminGroupsPageProps> = () => {
  // export class AdminGroupsPage extends React.Component<AdminGroupsPageProps, AdminGroupsPageState> {
  const groupsService = useGroupsService();
  const { t } = useTranslation();

  const [groups, setGroups] = useState([] as AdminGroup[]);
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState<number>(0);
  const [initialRender, setInitialRender] = useState(true);
  const [userRoles, setUserRoles] = useState<String[]>([]);
  const [loading, setLoading] = useState(false);

  const onSetPage = (
    _event: React.MouseEvent | React.KeyboardEvent | MouseEvent,
    newPage: number,
  ) => {
    setPage(newPage);
  };

  const onPerPageSelect = (
    _event: React.MouseEvent | React.KeyboardEvent | MouseEvent,
    newPerPage: number,
    newPage: number,
  ) => {
    setPerPage(newPerPage);
    setPage(newPage);
  };

  useEffect(() => {
    if (initialRender) {
      setInitialRender(false);
      return;
    }
    fetchAdminGroups();
  }, [perPage, page]);

  useEffect(() => {
    setUserRoles(groupsService.getUserRoles());
    fetchAdminGroups();
  }, []);

  let fetchAdminGroups = (searchString = undefined) => {
    setLoading(true);
    groupsService!
      .doGet<Response>(
        "/group-admin/groups?first=" +
          perPage * (page - 1) +
          "&max=" +
          perPage +
          (searchString ? "&search=" + searchString : ""),
      )
      .then((response: any) => {
        setLoading(false);
        const count = response?.data?.count || 0;
        setTotalItems(count as number);
        setGroups(response?.data?.results || ([] as AdminGroup[]));
      });
  };

  const emptyGroup = () => {
    return (
      <DataListItem key="emptyItem" aria-labelledby="empty-item">
        <DataListItemRow key="emptyRow">
          <DataListItemCells
            dataListCells={[
              <DataListCell key="empty">
                <strong>{t("noGroups")}</strong>
              </DataListCell>,
            ]}
          />
        </DataListItemRow>
      </DataListItem>
    );
  };

  return (
    <div className="gm_content">
      <Breadcrumb className="gm_breadcumb">
        <BreadcrumbItem isActive>{t("adminGroupLabel")}</BreadcrumbItem>
      </Breadcrumb>
      <Page
        title={t("adminGroupLabel")}
        description={t("adminGroupsIntroMessage")}
      >
        <TableActionBar
          searchText={t("searchBoxPlaceholder")}
          afterCreate={() => {
            fetchAdminGroups();
          }}
          createButton={userRoles.includes("manage-groups")}
          cancelText={t("searchBoxCancel")}
          search={(searchString: any) => {
            fetchAdminGroups(searchString);
            setPage(1);
          }}
        />
        <DataList
          id="groups-list"
          aria-label={t("groupLabel")}
          isCompact
          wrapModifier={"breakWord"}
        >
          <DataListItem id="groups-list-header" aria-labelledby="Columns names">
            <DataListItemRow className="gm_datalist-header">
              <DataListItemCells
                dataListCells={[
                  <DataListCell key="group-name-header" width={2}>
                    <strong>{t("Name")}</strong>
                  </DataListCell>,
                  <DataListCell key="group-path-header" width={2}>
                    <strong>{t("Path")}</strong>
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
            groups.map((group: AdminGroup, appIndex: number) => {
              return (
                <GroupListItem
                  group={group as AdminGroup}
                  userRoles={userRoles}
                  fetchAdminGroups={fetchAdminGroups}
                  appIndex={appIndex}
                  depth={0}
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

export interface GroupListItemProps {
  group: AdminGroup;
  appIndex: number;
  depth: number;
  userRoles: String[];
  fetchAdminGroups: Function;
  isGroupAdmin?: boolean;
  hasNoPadding?: boolean;
}

export const GroupListItem: FC<GroupListItemProps> = ({
  group,
  appIndex,
  depth,
  fetchAdminGroups,
  userRoles,
  isGroupAdmin = false,
}) => {
  useEffect(() => {
    setExpanded(false);
  }, [group]);
  const [expanded, setExpanded] = useState<boolean>(false);
  const { t } = useTranslation();
  const groupsService = useGroupsService();

  const [isOpen, setIsOpen] = useState(false);
  const [createSubGroup, setCreateSubGroup] = useState(false);
  const [deleteGroup, setDeleteGroup] = useState(false);
  const [tooltip, setTooltip] = useState(false);

  const disapearingTooltip = () => {
    setTooltip(true);
    setTimeout(() => {
      setTooltip(false);
    }, 2000);
  };

  const onToggle = () => {
    setIsOpen(!isOpen);
  };

  const onFocus = () => {
    const element = document.getElementById("toggle-kebab");
    element && element.focus();
  };

  const onSelect = () => {
    setIsOpen(false);
    onFocus();
  };

  const onCopyLink = () => {
    disapearingTooltip();
    let link =
      groupsService.getBaseUrl() +
      "/account/enroll?groupPath=" +
      encodeURI(group.path);
    navigator.clipboard.writeText(link);
  };

  return (
    <React.Fragment>
      <CreateGroupModal
        groupId={group.id}
        active={createSubGroup}
        afterSuccess={() => {
          fetchAdminGroups();
        }}
        close={() => {
          setCreateSubGroup(false);
        }}
      />
      <DeleteSubgroupModal
        groupId={group.id}
        active={deleteGroup}
        afterSuccess={() => {
          fetchAdminGroups();
        }}
        close={() => {
          setDeleteGroup(false);
        }}
      />
      <DataListItem
        id={`${appIndex}-group`}
        key={"group-" + appIndex}
        className={
          "gm_expandable-list" +
          (group?.extraSubGroups.length > 0 ? " gm_expandable-list-item" : "")
        }
        aria-labelledby="groups-list"
        isExpanded={expanded}
      >
        <DataListItemRow
          style={{
            paddingLeft:
              2 + depth + (group?.extraSubGroups.length > 0 ? 0 : 2) + "rem",
          }}
        >
          {group?.extraSubGroups.length > 0 ? (
            <div
              className={"gm_epxand-toggle"}
              onClick={() => {
                setExpanded(!expanded);
              }}
            >
              <div
                className={
                  expanded
                    ? "gm_epxand-toggle-expanded"
                    : "gm_epxand-toggle-hidden"
                }
              ></div>
            </div>
          ) : null}
          <Link to={kcPath("/groups/admingroups/" + group.id)}>
            <DataListItemCells
              dataListCells={[
                <DataListCell
                  id={`${appIndex}-group-name`}
                  width={2}
                  key={"name-" + appIndex}
                >
                  {group.name}
                </DataListCell>,
                <DataListCell
                  id={`${appIndex}-group-path`}
                  width={2}
                  key={"path-" + appIndex}
                >
                  {group.path}
                </DataListCell>,
              ]}
            />
          </Link>
          <DataListAction
            className="gm_cell-center gm_kebab-menu-cell"
            aria-labelledby="check-action-item1 check-action-action2"
            id="check-action-action1"
            aria-label="Actions"
            isPlainButtonAction
          >
            <Tooltip
              {...(!!tooltip
                ? { trigger: "manual", isVisible: true }
                : { trigger: "manual", isVisible: false })}
              content={<div>{t("copiedTooltip")}</div>}
            >
              <Dropdown
                isOpen={isOpen}
                onSelect={onSelect}
                popperProps={{ position: "right" }}
                onOpenChange={(isOpen: boolean) => setIsOpen(isOpen)}
                toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                  <MenuToggle
                    ref={toggleRef}
                    aria-label="kebab dropdown toggle"
                    variant="plain"
                    onClick={() => onToggle()}
                    isExpanded={isOpen}
                  >
                    <EllipsisVIcon />
                  </MenuToggle>
                )}
                shouldFocusToggleOnSelect
              >
                <DropdownList>
                  <DropdownItem
                    key="link"
                    onClick={() => {
                      setCreateSubGroup(true);
                    }}
                  >
                    {t("createSubGroup")}
                  </DropdownItem>
                  <DropdownItem key="link" onClick={() => onCopyLink()}>
                    {t("copyGroupEnrollmentLink")}
                  </DropdownItem>
                  {...isGroupAdmin &&
                  "/" + group.name !== group.path &&
                  !(group?.extraSubGroups.length > 0)
                    ? [
                        <DropdownItem
                          key="action"
                          onClick={() => {
                            setDeleteGroup(true);
                          }}
                          component="button"
                        >
                          {t("deleteGroup")}
                        </DropdownItem>,
                      ]
                    : []}
                </DropdownList>
              </Dropdown>
              {/* <Dropdown
                alignments={{
                  sm: 'right',
                  md: 'right',
                  lg: 'right',
                  xl: 'right',
                  '2xl': 'right'
                }}
                onSelect={onSelect}
                toggle={<KebabToggle id="toggle-kebab" onToggle={onToggle} />}
                isOpen={isOpen}
                isPlain
                dropdownItems={dropdownItems}
              /> */}
            </Tooltip>
          </DataListAction>
        </DataListItemRow>
        <DataListContent
          aria-label="First expandable content details"
          id="ex-expand1"
          isHidden={!expanded}
          hasNoPadding={true}
        >
          {group?.extraSubGroups.length > 0
            ? group?.extraSubGroups.map(
                (subGroup: AdminGroup, appSubIndex: number) => {
                  return (
                    <GroupListItem
                      group={subGroup as AdminGroup}
                      userRoles={userRoles}
                      appIndex={appSubIndex}
                      depth={depth + 1}
                      fetchAdminGroups={fetchAdminGroups}
                      hasNoPadding={true}
                    />
                  );
                },
              )
            : null}
        </DataListContent>
      </DataListItem>
    </React.Fragment>
  );
};
