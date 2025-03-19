import * as React from 'react';
import { FC, useState, useEffect } from 'react';
import { Tabs, Tab, TabTitleText, DataList, DataListItem, DataListItemCells, DataListItemRow, DataListCell, Breadcrumb, BreadcrumbItem, Badge, Popover } from '@patternfly/react-core';
// @ts-ignore
import { ContentPage } from '../ContentPage';
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { Msg } from '../../widgets/Msg';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { HelpIcon, ExclamationTriangleIcon, InfoCircleIcon } from '@patternfly/react-icons';
import { dateParse, addDays, isFirstDateBeforeSecond } from '../../widgets/Date';
import { Link } from 'react-router-dom';
import { Button } from '@patternfly/react-core';
import { ConfirmationModal } from '../../group-widgets/Modals';
import { ContentAlert } from '../ContentAlert';
import { getError } from '../../js/utils.js'
import { useLoader } from '../../group-widgets/LoaderContext';

export interface GroupsPageProps {
  history: any;
  match: any;
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
export const GroupPage: FC<GroupsPageProps> = (props) => {
  const [groupMembership, setGroupMembership] = useState({} as GroupMembership);
  const [groupId, setGroupId] = useState(props.match.params.id);
  const [activeTabKey, setActiveTabKey] = React.useState<string | number>(0);
  const [expirationWarning, setExpirationWarning] = useState(false);
  const [effectiveGroupPath, setEffectiveGroupPath] = useState("");
  const [modalInfo, setModalInfo] = useState({});
  const { startLoader, stopLoader } = useLoader();

  let groupsService = new GroupsServiceClient();
  useEffect(() => {
    fetchGroups();
  }, [groupId]);

  useEffect(() => {
    if (groupMembership && groupMembership?.effectiveMembershipExpiresAt && groupMembership?.group?.attributes['expiration-notification-period'][0]) {
      let warning = isFirstDateBeforeSecond(dateParse(groupMembership.effectiveMembershipExpiresAt),
        addDays(new Date(new Date().setHours(0, 0, 0, 0)), parseInt(groupMembership.group.attributes['expiration-notification-period'][0])), 'warning'
      );
      setExpirationWarning(!!warning);
      if (groupMembership?.effectiveGroupId) {
        fetchParentPath();
      }
    }


  }, [groupMembership])

  useEffect(() => {
    setGroupId(props.match.params.id)
  }, [props.match.params.id])


  const handleTabClick = (
    event: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent,
    tabIndex: string | number
  ) => {
    setActiveTabKey(tabIndex);
  };


  const leaveGroup = () => {
    startLoader();
      groupsService!.doDelete<any>("/user/group/" + groupId + "/member")
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          props.history.push('/groups/showgroups');
        }
        else {
          ContentAlert.danger(Msg.localize('leaveGroupError', [getError(response)]))
        }
        stopLoader();
      });
  }


  let fetchParentPath = () => {
    groupsService!.doGet<any>("/user/group/" + groupMembership?.effectiveGroupId + "/member")
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setEffectiveGroupPath(response.data.group.path);
        }
      })
  }
  let fetchGroups = () => {
    startLoader();
    groupsService!.doGet<GroupMembership>("/user/group/" + groupId + "/member")
      .then((response: HttpResponse<GroupMembership>) => {
        stopLoader();
        if (response.status === 200 && response.data) {
          setGroupMembership(response.data);
        }
      })
  }
  return (
    <>
      <div className={"gm_content "} >
        <Breadcrumb className="gm_breadcumb">
          <BreadcrumbItem to="#">
            <Msg msgKey='accountConsole' />
          </BreadcrumbItem>
          <BreadcrumbItem to="#/groups/showgroups">
            <Msg msgKey='groupLabel' />
          </BreadcrumbItem>
          {groupMembership?.group?.path.split("/").filter(item => item).map((value, index) => {
            return <BreadcrumbItem>
              {value}
            </BreadcrumbItem>
          })}
        </Breadcrumb>
        <ConfirmationModal modalInfo={modalInfo} />
        <ContentPage title={groupMembership?.group?.name || ""}>
          <p className="gm_group_desc">
            {(groupMembership?.group?.attributes?.description && groupMembership?.group?.attributes?.description[0]) || Msg.localize('noDescription')}
          </p>
          <div className="gm_view-group-action-container">
            <Link to={'/enroll?groupPath=' + encodeURI(groupMembership?.group?.path)}><Button>Update Membership</Button></Link>
            <Button variant="danger" onClick={() => {
              setModalInfo({
                title: (Msg.localize('leaveGroup') + "?"),
                button_variant: "danger",
                accept_message: "Leave",
                cancel_message: "Cancel",
                message: (Msg.localize('leaveGroupConfirmation')),
                accept: function () {
                  leaveGroup();
                  setModalInfo({})
                },
                cancel: function () {
                  setModalInfo({})
                }
              });
            }}>Leave Group</Button>
          </div>
          <Tabs
            className="gm_tabs"
            activeKey={activeTabKey}
            onSelect={handleTabClick}
            isBox={false}
            aria-label={"Tabs in the default example"}
            role="region"
          >
            <Tab eventKey={0} title={<TabTitleText><Msg msgKey='groupMembershipTab' /></TabTitleText>} aria-label="Default content - users">
              <DataList className="gm_datalist" aria-label="Compact data list example" isCompact wrapModifier={"breakWord"}>
                <DataListItem aria-labelledby="group-path-item">
                  <DataListItemRow>
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell key="title">
                          <span id="compact-item2"><strong><Msg msgKey='groupPath' /></strong></span>
                        </DataListCell>,
                        <DataListCell key="value">
                          <span>{groupMembership?.group?.path || Msg.localize('notAvailable')}</span>
                        </DataListCell>
                      ]}
                    />
                  </DataListItemRow>
                </DataListItem>
                <DataListItem aria-labelledby="compact-item2">
                  <DataListItemRow>
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell key="primary content">
                          <span id="compact-item2"><strong><Msg msgKey='groupDatalistCellRoles' /></strong></span>
                        </DataListCell>,
                        <DataListCell key="secondary content ">
                          {groupMembership?.groupRoles ? groupMembership?.groupRoles.map((role, index) => {
                            return <Badge key={index} className="gm_role_badge" isRead>{role}</Badge>
                          }) : Msg.localize('groupDatalistCellNoRoles')}
                        </DataListCell>
                      ]}
                    />
                  </DataListItemRow>
                </DataListItem>
                <DataListItem aria-labelledby="compact-item1">
                  <DataListItemRow>
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell key="primary content">
                          <strong><Msg msgKey='adminGroupMemberCellMembershipExp' /></strong>
                        </DataListCell>,
                        <DataListCell key="secondary content">
                          {groupMembership?.effectiveGroupId || expirationWarning ?
                            <Popover
                              {...(!(groupMembership?.effectiveGroupId || expirationWarning) && { isVisible: false })}
                              bodyContent={
                                <div>
                                  {expirationWarning && groupMembership?.effectiveGroupId ?
                                    <>
                                      <Msg msgKey='membershipExpirationEffectiveNotification' />
                                      <Link to={'/enroll?groupPath=' + encodeURI(effectiveGroupPath)}>
                                        <Button className="gm_popover-expiration-button" isSmall>Extend</Button>
                                      </Link>
                                    </>
                                    : expirationWarning ?
                                      <>
                                        <Msg msgKey='membershipExpirationNotification' />
                                        <Link to={'/enroll?groupPath=' + encodeURI(groupMembership?.group?.path)}>
                                          <Button className="gm_popover-expiration-button" isSmall>Extend</Button>
                                        </Link>
                                      </>
                                      :
                                      <>
                                        <Msg msgKey='effectiveExpirationHelp' />
                                        <Link to={"/groups/showgroups/" + groupMembership?.effectiveGroupId}>
                                          <Button className="gm_popover-expiration-button" isSmall>View</Button>
                                        </Link>
                                      </>
                                  }
                                </div>
                              }
                            >
                              {expirationWarning ?
                                <span className="gm_effective-expiration-popover-trigger">
                                  <div style={{ display: 'inline-block' }} className={expirationWarning ? 'gm_warning-text' : ""}>
                                    {groupMembership?.effectiveMembershipExpiresAt || <Msg msgKey='Never' />}
                                  </div>
                                  <div className="gm_effective-helper-warning">
                                    <ExclamationTriangleIcon />
                                  </div>
                                </span>
                                :
                                <span className="gm_effective-expiration-popover-trigger">
                                  <div style={{ display: 'inline-block' }} className={expirationWarning ? 'gm_warning-text' : ""}>
                                    {groupMembership?.effectiveMembershipExpiresAt || <Msg msgKey='Never' />}
                                  </div>
                                  <div className="gm_effective-helper-info">
                                    <InfoCircleIcon />
                                  </div>
                                </span>
                              }
                            </Popover>
                            : <div>
                              {groupMembership?.effectiveMembershipExpiresAt || <Msg msgKey='Never' />}
                            </div>}

                        </DataListCell>
                      ]}
                    />
                  </DataListItemRow>
                </DataListItem>

              </DataList>
            </Tab>
          </Tabs>
        </ContentPage>
      </div>
    </>
  )

};