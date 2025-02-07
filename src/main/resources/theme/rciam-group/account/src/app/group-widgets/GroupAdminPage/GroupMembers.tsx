import * as React from 'react';
import { FC, useState, useEffect } from 'react';
import { DataList, DataListItem, DataListItemCells, DataListItemRow, DataListCell, Button, Tooltip, ModalVariant, DataListAction, Pagination, Badge, Modal, Checkbox, Form, FormGroup, Popover, TextArea } from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import { TableActionBar } from './TableActionBar';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '
import { AddMemberModal } from './AddMemberModal';
import { Msg } from '../../widgets/Msg';
import { DatalistFilterSelect } from '../DatalistFilterSelect';
import { HelpIcon, PencilAltIcon, TimesIcon, LockIcon, LockOpenIcon, OutlinedClockIcon, ExclamationTriangleIcon, LongArrowAltUpIcon, LongArrowAltDownIcon, AngleDownIcon } from '@patternfly/react-icons';
import { Loading } from '../LoadingModal';
import { Link } from 'react-router-dom';
import { EditMembershipModal } from './EditMembershipModal';
import { Alerts } from '../../widgets/Alerts';
import { dateParse, addDays, isFirstDateBeforeSecond } from '../../widgets/Date';

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
  const [justification, setJustification] = useState("");
  let groupsService = new GroupsServiceClient();
  const [user, setUser] = useState<any>({});
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [alert, setAlert] = useState({});


  useEffect(() => {
    setUser(props.user);
    setIsOpen(Object.keys(props.user).length > 0);
  }, [props.user])

  let suspendGroupMember = () => {
    setLoading(true);
    groupsService!.doPost<any>("/group-admin/group/" + user.group.id + "/member/" + user.id + "/suspend" + (justification ? "?justification=" + justification : ""), {})
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          props.fetchGroupMembers();
          setAlert({ message: Msg.localize('updateMembershipMessage'), variant: "success", description: Msg.localize('updateMembershipSuccessMessage') })
        }
        else {
          setAlert({ message: Msg.localize('updateMembershipMessage'), variant: "danger", description: response?.data?.error ? Msg.localize('updateMembershipErrorMessage', [response.data.error]) : Msg.localize("updateMembershipErrorMessageUnexpected") })
        }
        setLoading(false);
        close();
      })
  }

  let activateGroupMember = () => {
    setLoading(true);
    groupsService!.doPost<any>("/group-admin/group/" + user.group.id + "/member/" + user.id + "/activate" + (justification ? "?justification=" + justification : ""), {})
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          props.fetchGroupMembers();
          setAlert({ message: Msg.localize('updateMembershipMessage'), variant: "success", description: Msg.localize('updateMembershipSuccessMessage') })
        }
        else {
          setAlert({ message: Msg.localize('updateMembershipMessage'), variant: "danger", description: response?.data?.error ? Msg.localize('updateMembershipErrorMessage', [response.data.error]) : Msg.localize("updateMembershipErrorMessageUnexpected") })
        }
        setLoading(false);
        close();
      })
  }

  const close = () => {
    props.setUser({});
    setJustification("");
  }

  return (
    <>
      <Alerts alert={alert} close={() => { setAlert({}) }} />
      <Modal
        variant={ModalVariant.medium}
        title={Msg.localize('Confirmation')}
        isOpen={isOpen}
        onClose={() => { close() }}
        actions={[
          <Button key="confirm" variant="primary" onClick={() => {
            if (user.status === "ENABLED") {
              suspendGroupMember();
            }
            else {
              activateGroupMember();
            }
          }}>
            {Msg.localize('YES')}
          </Button>,
          <Button key="cancel" variant="link" onClick={() => { close(); }}>
            {Msg.localize('NO')}
          </Button>

        ]}
      >
        <div>
          <Loading active={loading} />
          <p>{user.status === "ENABLED" ? Msg.localize('adminGroupMemberSuspendConfirmation') : Msg.localize('adminGroupMemberRevokeSuspendConfirmation')}</p>
          <Form>
            <FormGroup
              labelIcon={
                <Popover
                  bodyContent={
                    <div>
                      {user.status === "ENABLED" ? Msg.localize('adminGroupMemberSuspendJustification') : Msg.localize('adminGroupMemberRevokeSuspendJustification')}
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
                    <HelpIcon noVerticalAlign />
                  </button>
                </Popover>
              }
              className="gm_suspend-justification-formgroup"
              label={Msg.localize('justificationLabel')} fieldId={''}                                    >
              <TextArea
                type="text"
                id="simple-form-name-01"
                name="simple-form-name-01"
                aria-describedby="simple-form-name-01-helper"
                value={justification}
                onChange={(value) => { setJustification(value) }}
              />
            </FormGroup>
          </Form>
        </div>
      </Modal>
    </>
  )
}

export const GroupMembers: FC<any> = (props) => {
  const [groupMembers, setGroupMembers] = useState<Memberships[]>([]);
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState<number>(0);
  const [modalInfo, setModalInfo] = useState({});
  const [statusSelection, setStatusSelection] = useState("");
  const [roleSelection, setRoleSelection] = useState("")
  const [editMembership, setEditMembership] = useState<any>({});
  const [inviteModalActive, setInviteModalActive] = useState(false);
  const [selectedUser, setSelectedUser] = useState<any>({});
  const [directMembers, setDirectMembers] = useState<boolean>(true);
  const [loading, setLoading] = useState(false);
  const [alert, setAlert] = useState({});
  const [searchString, setSearchString] = useState("");
  const [groupId, setGroupId] = useState(props.groupId);
  const [orderBy, setOrderBy] = useState("default");
  const [asc, setAsc] = useState<boolean>(true);


  let groupsService = new GroupsServiceClient();

  useEffect(() => {
   
    if (props.groupId !== groupId) {
      setDirectMembers(true);
      setGroupId(props.groupId);
      const searchParams = new URLSearchParams(location.hash.split('?')[1]);
      let searchMember = searchParams.get('memberId');
      if (searchMember) {
        const newSearchParams = new URLSearchParams(searchParams);
        setSearchString(searchMember);
        newSearchParams.delete('memberId');
        props.history.push({
          search: newSearchParams.toString() ? `?${newSearchParams.toString()}` : '',
        });
      }
      else {
        setSearchString("");
      }
    }
    setPage(1);

  }, [props.groupId]);

  useEffect(() => {
    fetchGroupMembers(searchString);
  }, [statusSelection, roleSelection, page, perPage, groupId, directMembers, searchString,orderBy,asc]);

  const orderResults = (type) => {
    if (orderBy !== type) {
      setOrderBy(type); setAsc(true);
    }
    else if (asc) {
      setAsc(false);
    }
    else {
      setAsc(true);
    }
  }

  const onSetPage = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => {
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

  let preselectedMembershipHandler = (memberships) => {
    const searchParams = new URLSearchParams(location.hash.split('?')[1]);
    let preselectedMembership = searchParams.get('membership');
    if (preselectedMembership) {
      memberships.forEach((membership) => {
        if (membership.id = preselectedMembership) {
          setEditMembership(membership)
        }
      })
      const newSearchParams = new URLSearchParams(searchParams);
      newSearchParams.delete('membership');
      props.history.push({
        search: newSearchParams.toString() ? `?${newSearchParams.toString()}` : '',
      });
    }

  }

  let fetchGroupMembers = (searchString: string | undefined = undefined) => {
    groupsService!.doGet<any>("/group-admin/group/" + props.groupId + "/members?first=" + (perPage * (page - 1)) + "&max=" + perPage + (searchString ? "&search=" + searchString : "") + (orderBy!=="default"?"&order="+ orderBy:"") + "&asc=" + asc, {
      params: { ...(statusSelection ? { status: statusSelection } : {}), ...(roleSelection ? { role: roleSelection } : {}), ...(!directMembers ? { direct: 'false' } : {}) }
    })
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setTotalItems(response.data.count);
          setGroupMembers(response.data.results);
          preselectedMembershipHandler(response.data.results);
        }
      });
  }


  let deleteGroupMember = (memberId, groupId) => {
    groupsService!.doDelete<any>("/group-admin/group/" + groupId + "/member/" + memberId)
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          fetchGroupMembers();
        }
      })
  }

  let activatePendingMembership = (membership) => {
    const currentDate = new Date();
    const formattedDate = currentDate.toISOString().split('T')[0];
    membership.validFrom = formattedDate;
    setLoading(true);
    groupsService!.doPut<any>("/group-admin/group/" + props.groupId + "/member/" + membership?.id, { ...membership })
      .then((response: HttpResponse<any>) => {
        fetchGroupMembers();
        setLoading(false);
        if (response.status === 200 || response.status === 204) {
          setAlert({ message: Msg.localize('updateMembershipMessage'), variant: "success", description: Msg.localize('updateMembershipSuccessMessage') })
          close();
          // setGroupMembers(response.data.results);
        }
        else {
          props.setAlert({ message: Msg.localize('updateMembershipMessage'), variant: "danger", description: response?.data?.error ? Msg.localize('updateMembershipErrorMessage', [response.data.error]) : Msg.localize("updateMembershipErrorMessageUnexpected") })
        }
      }).catch(err => {
        console.log(err);
      })
  }




  const noMembers = () => {
    return (
      <DataListItem key='emptyItem' aria-labelledby="empty-item">
        <DataListItemRow key='emptyRow'>
          <DataListItemCells dataListCells={[
            <DataListCell key='empty'><strong><Msg msgKey='adminGroupNoMembers' /></strong></DataListCell>
          ]} />
        </DataListItemRow>
      </DataListItem>
    )
  }

  return (
    <React.Fragment>
      <Alerts alert={alert} close={() => { setAlert({}) }} />
      <Loading active={loading} />
      <ConfirmationModal modalInfo={modalInfo} />
      <UserActionModal setAlert={setAlert} user={selectedUser} setUser={setSelectedUser} groupId={props.groupId} fetchGroupMembers={fetchGroupMembers} />
      <EditMembershipModal membership={editMembership} setMembership={setEditMembership} fetchGroupMembers={fetchGroupMembers} />
      <TableActionBar
        childComponent={
          <React.Fragment>
            <Checkbox className="gm_direct-member-checkbox" label={Msg.localize('adminGroupViewAllMembersButton')} checked={directMembers} onClick={() => { setDirectMembers(!directMembers); }} id="required-check" name="required-check" />
            {props.isGroupAdmin &&
              <Button className="gm_invite-member-button" onClick={() => { setInviteModalActive(true) }}>
                <Msg msgKey='addMember' />
              </Button>}
          </React.Fragment>
        }
        searchString={searchString}
        searchText={Msg.localize('adminGroupSearchMember')} cancelText={Msg.localize('adminGroupSearchCancel')} search={(searchString) => {
          setSearchString(searchString);
          setPage(1);
        }} cancel={() => {
          setSearchString("");
          setPage(1);
        }}
      />
      <DataList aria-label="Group Member Datalist" isCompact wrapModifier={"breakWord"}>
        <DataListItem aria-labelledby="compact-item1">
          <DataListItemRow>
            <DataListItemCells dataListCells={[
              <DataListCell className="gm_vertical_center_cell" width={3} key="id-hd">
                <strong><Msg msgKey='UniqueIdentifier' /></strong>
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={3} key="email-hd">
                <div className="gm_order_by_container" onClick={() => { orderResults('default') }}>
                  <strong><Msg msgKey='adminGroupMemberCellNameEmail' /></strong>
                  {orderBy !== 'default' ? <AngleDownIcon /> : asc ? <LongArrowAltDownIcon /> : <LongArrowAltUpIcon />}
                </div>
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={3} key="email-hd">
                <strong><Msg msgKey='Roles' /></strong>
                {props.groupConfiguration?.groupRoles &&
                  <DatalistFilterSelect default={roleSelection} name="group-roles" options={Object.keys(props.groupConfiguration.groupRoles)} optionsType="raw" action={(selection) => { setRoleSelection(selection) }} />
                }
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={3} key="valid-from-hd">
                <strong><Msg msgKey='groupDatalistCellMembershipSince' /></strong>
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={3} key="expiration-hd">
                <div className="gm_order_by_container" onClick={() => { orderResults('f.effectiveMembershipExpiresAt') }}>
                  <strong><Msg msgKey='adminGroupMemberCellMembershipExp' /></strong>
                  {orderBy !== 'f.effectiveMembershipExpiresAt' ? <AngleDownIcon /> : asc ? <LongArrowAltDownIcon /> : <LongArrowAltUpIcon />}
                </div>
                <Popover
                    bodyContent={
                      <div>
                        <Msg msgKey='membershipExpiresAtPopoverDatalist' />
                      </div>
                    }
                  >
                    <button
                      type="button"
                      aria-label="More info for name field"
                      onClick={e => e.preventDefault()}
                      aria-describedby="simple-form-name-01"
                      className="pf-c-form__group-label-help gm_popover-info"
                    >
                      <HelpIcon noVerticalAlign />
                    </button>
                  </Popover>


              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={2} key="status-hd">
                <strong><Msg msgKey='Status' />
                  <DatalistFilterSelect default={statusSelection} name="group-status" options={['ENABLED', 'SUSPENDED', 'PENDING']} action={(selection) => { setStatusSelection(selection) }} />
                </strong>
              </DataListCell>,
              ...(!directMembers ? [
                <DataListCell className="gm_vertical_center_cell" width={3} key="group-path-hd">
                  <strong><Msg msgKey='groupPath' /></strong>
                </DataListCell>,
                <DataListCell className="gm_vertical_center_cell" width={2} key="direct-hd">
                  <strong><Msg msgKey='adminGroupDirectMembership' /></strong>
                </DataListCell>
              ] : [])

            ]}>
            </DataListItemCells>
            <DataListAction
              className="gm_cell-center"
              aria-labelledby="check-action-item1 check-action-action2"
              id="check-action-action1"
              aria-label="Actions"
              isPlainButtonAction
            ><div className="gm_cell-placeholder"></div></DataListAction>
          </DataListItemRow>
        </DataListItem>
        {groupMembers.length > 0 ? groupMembers.map((member, index) => {
          let notificationWarningEffective = member.effectiveMembershipExpiresAt && member?.group?.attributes['expiration-notification-period'][0] && isFirstDateBeforeSecond(
            dateParse(member.effectiveMembershipExpiresAt),
            addDays(new Date(new Date().setHours(0, 0, 0, 0)), parseInt(member?.group?.attributes['expiration-notification-period'][0])),
            'warning'
          );

          let notificationWarningDirect = member.membershipExpiresAt && member?.group?.attributes['expiration-notification-period'][0] && isFirstDateBeforeSecond(
            dateParse(member.membershipExpiresAt),
            addDays(new Date(new Date().setHours(0, 0, 0, 0)), parseInt(member?.group?.attributes['expiration-notification-period'][0])),
            'warning'
          );

          return <DataListItem aria-labelledby={"member-" + index}>
            <DataListItemRow>
              <DataListItemCells
                dataListCells={[
                  <DataListCell width={3} key="primary content">
                    {member.user?.attributes?.voPersonID || member.user.username}
                  </DataListCell>,
                  <DataListCell width={3} key="secondary content1">
                    <span className="gm_fullname_datalist pf-c-select__menu-item-main">{member.user.firstName && member.user.lastName ? member.user.firstName + " " + member.user.lastName : Msg.localize('notAvailable')}</span>
                    <span className="gm_email_datalist pf-c-select__menu-item-description">{member.user.email}</span>
                  </DataListCell>,
                  <DataListCell width={3} key="secondary content2">
                    {member.groupRoles.map((role, index) => {
                      return <Badge key={index} className="gm_role_badge" isRead>{role}</Badge>
                    })}
                  </DataListCell>,
                  <DataListCell width={3}>
                    {member.validFrom}
                  </DataListCell>,
                  <DataListCell width={3}>
                    {
                      member.effectiveGroupId &&
                      <div>
                        <strong>Effective: </strong>
                        {notificationWarningEffective ?
                          <Popover
                            bodyContent={hide =>
                              <div>
                                <Msg msgKey='membershipExpirationEffectiveNotification' />
                                <Button className="gm_popover-expiration-button" isSmall onClick={() => {
                                  if (member.effectiveGroupId === groupId) {
                                    if(member.group.id!==groupId){
                                      const searchParams = new URLSearchParams(location.hash.split('?')[1]);
                                      searchParams.set("membership",member?.id ||"");
                                      props.history.push({
                                          search: searchParams.toString() ? `?${searchParams.toString()}` : '',
                                      });
                                      setDirectMembers(true);
                                      setSearchString(member.user.username);                                      
                                    }
                                    else{
                                      setEditMembership(member);
                                    }
                                    hide();
                                  }
                                  else {
                                    props.history.push({ pathname: "/groups/admingroups/" + member.effectiveGroupId, search: '?tab=members&memberId=' + member.user.username + '&membership=' + member.id })
                                  }
                                }}>Extend</Button>
                              </div>
                            }
                          >
                            <span className="gm_effective-expiration-popover-trigger">
                              <div style={{ display: 'inline-block' }} className={'gm_warning-text'}>
                                {member.effectiveMembershipExpiresAt || <Msg msgKey='Never' />}
                              </div>
                              <div className="gm_effective-helper-warning">
                                <ExclamationTriangleIcon />
                              </div>
                            </span>
                          </Popover>
                          : member.effectiveGroupId === groupId ?
                            member.effectiveMembershipExpiresAt
                            :
                            <a onClick={() => { props.history.push({ pathname: '/groups/admingroups/' + member.effectiveGroupId, search: '?tab=members&memberId=' + member.user.username }) }}>{member.effectiveMembershipExpiresAt}</a>
                        }
                      </div>
                    }
                    <div>
                      <strong>Direct: </strong>
                      {notificationWarningDirect ?
                        <Popover
                          bodyContent={hide =>
                            <div>
                              <Msg msgKey='membershipExpirationNotification' />
                              <Button className="gm_popover-expiration-button" isSmall onClick={() => {
                                setEditMembership(member);
                                hide();
                              }}>Extend</Button>
                            </div>
                          }
                        >
                          <span className="gm_effective-expiration-popover-trigger">
                            <div style={{ display: 'inline-block' }} className={'gm_warning-text'}>
                              {member.membershipExpiresAt || <Msg msgKey='Never' />}
                            </div>
                            <div className="gm_effective-helper-warning">
                              <ExclamationTriangleIcon />
                            </div>
                          </span>
                        </Popover>
                        : member.membershipExpiresAt ? <>{member.membershipExpiresAt}</>
                          : <Msg msgKey='Never' />}
                    </div>
                  </DataListCell>,
                  <DataListCell width={2}>
                    <Tooltip
                      content={
                        <div>
                          {member.status === 'ENABLED' ? Msg.localize('adminGroupMemberUserActiveTooltip') : member.status === "SUSPENDED" ? Msg.localize('adminGroupMemberUserSuspendedTooltip') : member.status === "PENDING" ? Msg.localize('adminGroupMemberUserPendingTooltip') : ""}
                        </div>
                      }
                    >
                      <div className="gm_user-status-container">
                        <div className={member.status === 'ENABLED' ? "gm_icon gm_icon-active-user" : member.status === "SUSPENDED" ? "gm_icon gm_icon-suspended-user" : member.status === "PENDING" ? "gm_icon gm_icon-pending-user" : ""}></div>
                      </div>
                    </Tooltip>
                  </DataListCell>,
                  ...(!directMembers ? [
                    <DataListCell width={3} key="secondary content5">
                      <Link to={{ pathname: "/groups/admingroups/" + member.group.id, search: "?tab=members" }}>
                        {member.group.path}
                      </Link>
                    </DataListCell>,
                    <DataListCell width={2} key="secondary content6">
                      <Tooltip content={<div>{member.direct ? Msg.localize('adminGroupIsDirect') : Msg.localize('adminGroupIsNotDirect')}</div>}>
                        <Checkbox id="disabled-check-1" className="gm_direct-checkbox" checked={member.direct ? true : false} isDisabled />
                      </Tooltip>
                    </DataListCell>
                  ] : [])
                ]}
              />
              <DataListAction
                className="gm_cell-center"
                aria-labelledby="check-action-item1 check-action-action1"
                id="check-action-action1"
                aria-label="Actions"
                isPlainButtonAction
              >
                <div className="gm_actions_container">

                  {props.isGroupAdmin &&
                    <React.Fragment>

                      <Tooltip
                        content={
                          <div>
                            {member.user.id === props.user.userId ? Msg.localize('leaveGroup') : Msg.localize('adminGroupMemberRemove')}
                          </div>
                        }
                      >
                        <Button isSmall variant="secondary" ouiaId="DangerSecondary" isDanger className="gm_small_icon_button" onClick={() => {
                          setModalInfo({
                            title: (Msg.localize('Confirmation')),
                            accept_message: (Msg.localize('YES')),
                            cancel_message: (Msg.localize('NO')),
                            message: (Msg.localize('adminGroupMemberRemoveConfirmation')),
                            accept: function () {
                              deleteGroupMember(member.id, member.group.id);
                              setModalInfo({})
                            },
                            cancel: function () {
                              setModalInfo({})
                            }
                          });
                        }}>
                          <TimesIcon />
                        </Button>
                      </Tooltip>
                      <Tooltip
                        content={
                          <div>
                            {member.status === "SUSPENDED" ? Msg.localize('editMembershipDisabled') : Msg.localize('editMembership')}
                          </div>
                        }
                      >
                        <div>
                          <Button isSmall variant="tertiary" isDisabled={member.status === "SUSPENDED"} className="gm_small_icon_button" onClick={() => {
                            if (member.direct) { setEditMembership(member); } else { 
                              setEditMembership(member);
                            }
                          }}>
                            <PencilAltIcon />
                          </Button>
                        </div>
                      </Tooltip>
                    </React.Fragment>
                  }
                  {props.isGroupAdmin ?
                    <Tooltip
                      content={
                        <div>
                          {member.status === 'ENABLED' ? Msg.localize('adminGroupMemberSuspendTooltip') : member.status === "SUSPENDED" ? Msg.localize('adminGroupMemberActivateTooltip') : Msg.localize("adminGroupMemberActivatePendingTooltip")}
                        </div>
                      }
                    >
                      <Button isSmall variant={member.status === "ENABLED" ? "danger" : "warning"} className={"gm_small_icon_button"} onClick={() => {
                        if (member.status === "PENDING") {
                          setModalInfo({
                            title: (Msg.localize('Confirmation')),
                            accept_message: (Msg.localize('YES')),
                            cancel_message: (Msg.localize('NO')),
                            message: (Msg.localize("Do you want to activate this membership?")),
                            accept: function () {
                              activatePendingMembership(member);
                              setModalInfo({})
                            },
                            cancel: function () {
                              setModalInfo({})
                            }
                          });
                        }
                        else {
                          setSelectedUser(member);
                        }
                      }}>
                        {member.status === "ENABLED" ? <LockIcon /> : member.status === 'SUSPENDED' ? <LockOpenIcon /> : <OutlinedClockIcon />}
                      </Button>
                    </Tooltip>
                    : <div className="gm_placeholder_membership_action"></div>

                  }
                </div>
              </DataListAction>
            </DataListItemRow>
          </DataListItem>
        }) : noMembers()}
      </DataList>
      <Pagination
        itemCount={totalItems}
        perPage={perPage}
        page={page}
        onSetPage={onSetPage}
        widgetId="top-example"
        onPerPageSelect={onPerPageSelect}
      />
      {props.isGroupAdmin && <AddMemberModal active={inviteModalActive} setActive={setInviteModalActive} groupConfiguration={props.groupConfiguration} fetchGroupMembers={fetchGroupMembers} groupId={props.groupId} />}
    </React.Fragment>

  )
}
