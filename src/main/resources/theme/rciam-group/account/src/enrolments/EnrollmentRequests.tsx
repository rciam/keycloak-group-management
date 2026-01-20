import * as React from "react";
import { FC, useState, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import {
  DataList,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  DataListCell,
  Button,
  Tooltip,
  DataListAction,
  Pagination,
  TextInput,
  Badge,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import {
  AngleDownIcon,
  CaretDownIcon,
  CaretUpIcon,
  FilterIcon,
  LongArrowAltDownIcon,
  LongArrowAltUpIcon,
} from "@patternfly/react-icons";
import { TableActionBar } from "../widgets/TableActionBar";
import { useLoader } from "../widgets/LoaderContext";
import { useGroupsService } from "../groups-service/GroupsServiceContext";
import { HttpResponse } from "../groups-service/groups-service";
import { Page } from "@keycloak/keycloak-account-ui";
import { EnrollmentRequest } from "./EnrollmentRequest";
import { DatalistFilterSelect } from "../widgets/DatalistFilterSelect";

export const EnrollmentRequests: FC<any> = (props) => {

  const [searchParams] = useSearchParams();
  const groupsService = useGroupsService();
  let statusOptions = [
    "PENDING_APPROVAL",
    "WAITING_FOR_REPLY",
    "ACCEPTED",
    "REJECTED",
    "ARCHIVED",
    "SELF_APPROVED",
  ];
  const [selectedRequest, setSelectedRequest] = useState<any>({});
  const [enrollmentRequests, setEnrollmentRequests] = useState<any>([]);
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState<number>(0);
  const [statusSelection, setStatusSelection] = useState("");
  const [initialRender, setInitialRender] = useState(true);
  const [orderBy, setOrderBy] = useState<string>("submittedDate");
  const [asc, setAsc] = useState<boolean>(false);
  const [searchStringGroup, setSearchStringGroup] = useState("");
  const [searchStringUser, setSearchStringUser] = useState("");
  const [isExpanded, setIsExpanded] = useState(false);
  const [requestId, setRequestId] = useState<any>(0);
  const { startLoader, stopLoader } = useLoader();
  const { t } = useTranslation();

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

  useEffect(() => {
    let id;
    if (statusSelection === "PENDING_APPROVAL") {
      setPage(1);
      fetchEnrollmentRequests();
    } else {
      setStatusSelection("PENDING_APPROVAL");
    }
    id = decodeURI(searchParams.get("id") || "");
    id && setRequestId(id);
  }, [props.manage, searchParams]);

  useEffect(() => {
    if (initialRender) {
      setInitialRender(false);
      return;
    }
    fetchEnrollmentRequests();
  }, [perPage, page, orderBy, asc]);

  useEffect(() => {
    if (requestId) {
      fetchEnrollmentRequest(requestId);
    }
  }, [requestId]);

  useEffect(() => {
    if (initialRender) {
      setInitialRender(false);
      return;
    }
    setPage(1);
    fetchEnrollmentRequests();
  }, [statusSelection]);

  let fetchEnrollmentRequests = (
    searchStringUser = "",
    searchStringGroupOverwrite = ""
  ) => {
    startLoader();
    groupsService!
      .doGet<any>(
        props.manage ? "/group-admin/enroll-requests" : "/user/enroll-requests",
        {
          params: {
            first: perPage * (page - 1),
            max: perPage,
            ...(statusSelection ? { status: statusSelection } : {}),
            ...(searchStringGroup || searchStringGroupOverwrite
              ? { groupName: searchStringGroup || searchStringGroupOverwrite }
              : {}),
            ...(searchStringUser ? { userSearch: searchStringUser } : {}),
            ...(orderBy ? { order: orderBy } : {}),
            asc: asc ? "true" : "false",
          },
        }
      )
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 && response.data) {
          let count = response?.data?.count || 0;
          setTotalItems(count as number);
          setEnrollmentRequests(response.data.results);
        } else {
          fetchEnrollmentRequests();
        }
      })
      .catch((err: any) => {
        console.log(err);
      });
  };

  let fetchEnrollmentRequest = (id: string) => {
    startLoader();
    groupsService!
      .doGet<any>(
        (props.manage
          ? "/group-admin/enroll-request/"
          : "/user/enroll-request/") + id
      )
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 && response.data) {
          let count = 1;
          setTotalItems(count as number);
          setSelectedRequest(response.data);
        }
      })
      .catch((err: any) => {
        console.log(err);
      });
  };

  const noEnrollmentRequests = () => {
    return (
      <DataListItem key="emptyItem" aria-labelledby="empty-item">
        <DataListItemRow key="emptyRow">
          <DataListItemCells
            dataListCells={[
              <DataListCell key="empty">
                <strong>{t("noEnrollmentRequests")}</strong>
              </DataListCell>,
            ]}
          />
        </DataListItemRow>
      </DataListItem>
    );
  };

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

  const search = () => {
    setPage(1);
    fetchEnrollmentRequests(searchStringUser, searchStringGroup);
  };

  const toggleExpansion = () => {
    setIsExpanded(!isExpanded);
  };

  return (
    <React.Fragment key={props.manage}>
      <div className="gm_content">
        <Page
          title={
            props.manage
              ? t("groupManageEnrollmentsLabel")
              : t("groupMyEnrollmentsLabel")
          }
          description={props.manage ? t("groupManageEnrollmentsDescription") : t("groupMyEnrollmentsDescription")}
        >
          {props.manage ? (
            <React.Fragment>
              <div className="gm_toggle-filters-container">
                <Button
                  className="gm_toggle-filters"
                  variant="control"
                  onClick={toggleExpansion}
                >
                  <FilterIcon />{" "}
                  {isExpanded ? <CaretUpIcon /> : <CaretDownIcon />}
                </Button>
              </div>
              <div
                className={`expandable-section ${isExpanded ? "expanded" : ""}`}
              >
                <div className="gm_search-input-container-2">
                  <div className="gm_search-input-double">
                    <TextInput
                      name="searchInputUser"
                      id="searchInputUser"
                      type="text"
                      value={searchStringUser}
                      onChange={(_event, value) => setSearchStringUser(value)}
                      placeholder={t("UserSearchPlaceholder")}
                      aria-label="searchInputUser"
                      onKeyDown={(e) => e.key === "Enter" && search()}
                    />
                    <TextInput
                      name="searchInputGroup"
                      id="searchInputGroup"
                      type="text"
                      value={searchStringGroup}
                      onChange={(_event, value) => setSearchStringGroup(value)}
                      placeholder={t("GroupSearchPlaceholder")}
                      aria-label="searchInputGroup"
                      onKeyDown={(e) => e.key === "Enter" && search()}
                    />
                  </div>
                  <div className="gm_search-input-double-controls">
                    <Tooltip
                      content={<div>{t("EnrollmentRequestFilterTooltip")}</div>}
                    >
                      <Button
                        variant="control"
                        aria-label="popover for input"
                        onClick={() => search()}
                      >
                        <div className="gm_search-icon-container"></div>
                      </Button>
                    </Tooltip>
                    <Tooltip
                      content={<div>{t("EnrollmentRequestFilterCancel")}</div>}
                    >
                      <Button
                        variant="control"
                        aria-label="popover for input"
                        onClick={() => {
                          setSearchStringUser("");
                          setSearchStringGroup("");
                          setPage(1);
                          fetchEnrollmentRequests();
                        }}
                      >
                        <div className="gm_cancel-icon-container"></div>
                      </Button>
                    </Tooltip>
                  </div>
                </div>
              </div>
            </React.Fragment>
          ) : (
            <TableActionBar
              searchText={t("searchBoxPlaceholder")}
              cancelText={t("searchBoxCancel")}
              search={(searchString: string) => {
                setPage(1);
                fetchEnrollmentRequests("", searchString);
              }}
            />
          )}
          <EnrollmentRequest enrollmentRequest={selectedRequest} managePage={props.manage} refresh={()=>{fetchEnrollmentRequests();}} close={()=>{setSelectedRequest({});}}/>
          <DataList
            aria-label="Enrollment Request Datalist"
            isCompact
            wrapModifier={"breakWord"}
          >
            <DataListItem aria-labelledby="compact-item1">
              <DataListItemRow>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell
                      className="gm_vertical_center_cell"
                      width={1}
                      key="submitted-hd"
                      onClick={() => {
                        orderResults("submittedDate");
                      }}
                    >
                      <strong>{t("Submitted Date")}</strong>
                      {orderBy !== "submittedDate" ? (
                        <AngleDownIcon />
                      ) : !asc ? (
                        <LongArrowAltDownIcon />
                      ) : (
                        <LongArrowAltUpIcon />
                      )}
                    </DataListCell>,
                    <DataListCell
                      className="gm_vertical_center_cell"
                      width={1}
                      key="group-hd"
                      onClick={() => {
                        orderResults("groupEnrollmentConfiguration.group.name");
                      }}
                    >
                      <strong>{t("Group Path")}</strong>
                    </DataListCell>,
                    <DataListCell
                      className="gm_vertical_center_cell"
                      width={1}
                      key="enrollment-hd"
                    >
                      <strong>{t("enrollmentName")}</strong>
                    </DataListCell>,
                    ...(props.manage
                      ? [
                          <DataListCell
                            className="gm_vertical_center_cell"
                            width={1}
                            key="state-hd"
                          >
                            <strong>
                              {t("adminGroupMemberCellNameEmail")}
                            </strong>
                          </DataListCell>,
                        ]
                      : []),
                    <DataListCell
                      className="gm_vertical_center_cell"
                      width={1}
                      key="state-hd"
                    >
                      <strong>
                        {t("State")}
                        <DatalistFilterSelect default={"PENDING_APPROVAL"} name="status"  options={statusOptions} action={(selection:any)=>{setStatusSelection(selection)}}/>
                      </strong>
                    </DataListCell>,
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
            {enrollmentRequests.length > 0
              ? enrollmentRequests.map((enrollment: any, index: number) => {
                  return (
                    <DataListItem aria-labelledby={"member-" + index}>
                      <DataListItemRow>
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell width={1} key="primary content">
                              {enrollment?.submittedDate &&
                                new Date(enrollment?.submittedDate)
                                  .toISOString()
                                  .split("T")[0]}
                            </DataListCell>,
                            <DataListCell width={1} key="secondary content ">
                              {
                                enrollment.groupEnrollmentConfiguration.group
                                  .path
                              }
                            </DataListCell>,
                            <DataListCell width={1} key="secondary content ">
                              {enrollment.groupEnrollmentConfiguration.name}
                            </DataListCell>,
                            ...(props.manage
                              ? [
                                  <DataListCell width={1} key="primary content">
                                    <span className="gm_fullname_datalist pf-c-select__menu-item-main">
                                      {enrollment?.user?.firstName &&
                                      enrollment?.user?.lastName
                                        ? enrollment?.user?.firstName +
                                          " " +
                                          enrollment?.user?.lastName
                                        : t("notAvailable")}
                                    </span>
                                    <span className="gm_email_datalist pf-c-select__menu-item-description">
                                      {enrollment?.user?.email}
                                    </span>
                                  </DataListCell>,
                                ]
                              : []),
                            <DataListCell width={1} key="secondary content ">
                              <Badge
                                className={
                                  enrollment.status === "ACCEPTED"
                                    ? "gm_badge-success"
                                    : enrollment.status === "REJECTED"
                                    ? "gm_badge-danger"
                                    : enrollment.status === "PENDING_APPROVAL"
                                    ? "gm_badge-warning"
                                    : ""
                                }
                              >
                                {formatStatus(enrollment.status)}
                              </Badge>
                            </DataListCell>,
                          ]}
                        />
                        <DataListAction
                          className="gm_cell-center"
                          aria-labelledby="check-action-item1 check-action-action1"
                          id="check-action-action1"
                          aria-label="Actions"
                          isPlainButtonAction
                        >
                          <Tooltip
                            content={
                              <div>
                                {enrollment.status === "PENDING_APPROVAL" &&
                                props.manage
                                  ? t("ReviewEnrollmentTooltip")
                                  : t("ViewEnrollmentTooltip")}
                              </div>
                            }
                          >
                            <Button
                              variant="primary"
                              onClick={() => {
                                setSelectedRequest(enrollment);
                              }}
                            >
                              {enrollment.status === "PENDING_APPROVAL" &&
                              props.manage
                                ? "Review"
                                : "View"}
                            </Button>
                          </Tooltip>
                        </DataListAction>
                      </DataListItemRow>
                    </DataListItem>
                  );
                })
              : noEnrollmentRequests()}
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
    </React.Fragment>
  );
};

function formatStatus(inputString:string) {
  const words = inputString.split("_");
  const formattedWords = words.map(
    (word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
  );
  const formattedString = formattedWords.join(" ");
  return formattedString;
}
