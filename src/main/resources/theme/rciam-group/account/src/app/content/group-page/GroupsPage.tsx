import * as React from 'react';
import { Link } from 'react-router-dom';
import { FC, useState, useEffect } from 'react';
import { LongArrowAltDownIcon, LongArrowAltUpIcon, AngleDownIcon, ExclamationTriangleIcon, InfoCircleIcon } from '@patternfly/react-icons';
import {
  DataList,
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListItemCells,
  Pagination,
  Badge, DataListAction,
  Popover, KebabToggle, Dropdown, DropdownItem,
  Spinner
} from '@patternfly/react-core';
import { dateParse, addDays, isFirstDateBeforeSecond } from '../../widgets/Date';
// @ts-ignore
import { ContentPage } from '../ContentPage';
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { Msg } from '../../widgets/Msg';
import { Button } from '@patternfly/react-core';
import { ConfirmationModal } from '../../group-widgets/Modals';
import { ContentAlert } from '../ContentAlert';

export interface GroupsPageProps {
  history: any;
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

interface Response {
  results: Group[];
  count: BigInteger;
}
export const GroupsPage: FC<GroupsPageProps> = (props) => {

  let groupsService = new GroupsServiceClient();
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const [groups, setGroups] = useState([] as Group[]);
  const [totalItems, setTotalItems] = useState<number>(0);
  const [orderBy, setOrderBy] = useState<string>('');
  const [asc, setAsc] = useState<boolean>(true);
  const [loading, setLoading] = useState<boolean>(false);
  const [modalInfo, setModalInfo] = useState({});

  useEffect(() => {
    fetchGroups();
  }, []);



  useEffect(() => {
    fetchGroups();
  }, [perPage, page, orderBy, asc]);

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




  const fetchGroups = () => {
    setLoading(true);
    groupsService!.doGet<Response>("/user/groups", { params: { first: (perPage * (page - 1)), max: perPage, ...(orderBy ? { order: orderBy } : {}), asc: asc ? "true" : "false" } }).then((response: HttpResponse<Response>) => {
      setLoading(false);
      let count = response?.data?.count || 0;
      setTotalItems(count as number);
      setGroups(response?.data?.results || [] as Group[]);
    });

  }


  const emptyGroup = () => {

    return (
      <DataListItem key='emptyItem' aria-labelledby="empty-item">
        <DataListItemRow key='emptyRow'>
          <DataListItemCells dataListCells={[
            <DataListCell key='empty'><strong><Msg msgKey='noGroupsText' /></strong></DataListCell>
          ]} />
        </DataListItemRow>
      </DataListItem>
    )
  }



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


  return (
    <ContentPage title={Msg.localize('groupLabel')}>
      <ConfirmationModal modalInfo={modalInfo} />
      <DataList id="groups-list" aria-label={Msg.localize('groupLabel')} isCompact wrapModifier={"breakWord"}>
        <DataListItem id="groups-list-header" aria-labelledby="Columns names">
          <DataListItemRow className="gm_view-groups-header">
            <DataListItemCells
              dataListCells={[
                <DataListCell key='group-name-header' width={2} onClick={() => { orderResults('') }}>
                  <strong><Msg msgKey='nameDatalistTitle' /></strong>{!orderBy ? <AngleDownIcon /> : asc ? <LongArrowAltDownIcon /> : <LongArrowAltUpIcon />}
                </DataListCell>,
                <DataListCell key='group-path' width={2}>
                  <strong><Msg msgKey='groupPath' /></strong>
                </DataListCell>,
                <DataListCell key='group-roles' width={2}>
                  <strong><Msg msgKey='rolesDatalistTitle' /></strong>
                </DataListCell>,
                <DataListCell key='group-membership-expiration-header' width={2}>
                  <div className="gm_order_by_container" onClick={() => { orderResults('effectiveMembershipExpiresAt') }}>
                    <strong><Msg msgKey='membershipDatalistTitle' /></strong>
                    {orderBy !== 'effectiveMembershipExpiresAt' ? <AngleDownIcon /> : asc ? <LongArrowAltDownIcon /> : <LongArrowAltUpIcon />}
                  </div>
                  {/* <div className="gm_group-memberships-more-info">
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
            ><div className="gm_cell-placeholder"></div></DataListAction>
          </DataListItemRow>
        </DataListItem>
        {loading ?
          <div tabIndex={0} id="modal-no-header-description" className="gm_loader-modal-container">
            <Spinner isSVG diameter="100px" aria-label="Contents of the custom size example" />
          </div> : groups.length === 0
            ? emptyGroup()
            : groups.map((group: Group, appIndex: number) => {
              return <MembershipDatalistItem membership={group} history={props.history} fetchGroups={fetchGroups} setLoading={setLoading} setModalInfo={setModalInfo} currentDate={new Date(new Date().setHours(0, 0, 0, 0))} appIndex={appIndex} />
            })

        }
      </DataList>
      <Pagination
        itemCount={totalItems}
        perPage={perPage}
        page={page}
        onSetPage={onSetPage}
        widgetId="top-example"
        onPerPageSelect={onPerPageSelect}
      />
    </ContentPage>
  );

};

const MembershipDatalistItem = (props) => {
  const [isOpen, setIsOpen] = useState(false);
  const [expirationWarning, setExpirationWarning] = useState(false);
  const [effectiveGroupPath, setEffectiveGroupPath] = useState("");
  let groupsService = new GroupsServiceClient();

  // Compute expirationWarning and fetch group data on membership change
  useEffect(() => {
    if (props.membership?.effectiveMembershipExpiresAt && props.membership?.group?.attributes['expiration-notification-period'][0]) {
      const warning = isFirstDateBeforeSecond(
        dateParse(props.membership.effectiveMembershipExpiresAt),
        addDays(props.currentDate, parseInt(props.membership.group.attributes['expiration-notification-period'][0])),
        'warning'
      );
      setExpirationWarning(!!warning);  // Set warning as true or false
    }
    if (props.membership?.effectiveGroupId) {
      fetchGroup();
    }
  }, [props.membership]);

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const leaveGroup = () => {
    props.setLoading(true);
    groupsService!.doDelete<any>("/user/group/" + props.membership.group.id + "/member")
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          ContentAlert.success(Msg.localize('leaveGroupSuccess'));
          props.fetchGroups();
        }
        else {
          ContentAlert.danger(Msg.localize('leaveGroupError', [response.data.error]));
        }
        props.setLoading(false);
      });
  }

  // Fetch the group path based on the effectiveGroupId
  const fetchGroup = () => {
    groupsService!.doGet<any>(`/user/group/${props.membership?.effectiveGroupId}/member`)
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setEffectiveGroupPath(response.data.group.path);
        }
      });
  };

  const onSelect = () => {
    setIsOpen(false);
    const element = document.getElementById('toggle-kebab');
    element && element.focus();
  };

  return (
    <DataListItem id={`${props.appIndex}-group`} key={'group-' + props.appIndex + expirationWarning} aria-labelledby="groups-list" >
      <DataListItemRow>
        <DataListItemCells
          dataListCells={[
            <DataListCell id={`${props.appIndex}-group-name`} width={2} key={'name-' + props.appIndex}>
              <Link to={"/groups/showgroups/" + props.membership.group.id}>
                {props.membership.group.name}
              </Link>
            </DataListCell>,
            <DataListCell id={`${props.appIndex}-group-path`} width={2} key={'groupPath-' + props.appIndex}>
              {props.membership.group.path}
            </DataListCell>,
            <DataListCell id={`${props.appIndex}-group-roles`} width={2} key={'directMembership-' + props.appIndex}>
              {props.membership.groupRoles.map((role, index) => (
                <Badge key={index} className="gm_role_badge" isRead>{role}</Badge>
              ))}
            </DataListCell>,
            <DataListCell id={`${props.appIndex}-group-membershipExpiration`} width={2} key={'directMembership-' + props.appIndex}>
              <Popover
                {...(!(props.membership?.effectiveGroupId || expirationWarning) && { isVisible: false })}
                bodyContent={
                  <div>
                    {expirationWarning && props.membership?.effectiveGroupId ? (
                      <>
                        <Msg msgKey='membershipExpirationEffectiveNotification' />
                        <Link to={`/enroll?groupPath=${encodeURI(effectiveGroupPath)}`}>
                          <Button className="gm_popover-expiration-button" isSmall>Extend</Button>
                        </Link>
                      </>
                    ) : expirationWarning ? (
                      <>
                        <Msg msgKey='membershipExpirationNotification' />
                        <Link to={`/enroll?groupPath=${encodeURI(props.membership.group.path)}`}>
                          <Button className="gm_popover-expiration-button" isSmall>Extend</Button>
                        </Link>                      </>
                    ) : (
                      <>
                        <Msg msgKey='effectiveExpirationHelp' />
                        <Link to={`/groups/showgroups/${props.membership?.effectiveGroupId}`}>
                          <Button className="gm_popover-expiration-button" isSmall>View</Button>
                        </Link>
                      </>
                    )}
                  </div>
                }
              >
                {expirationWarning ? (
                  <span className="gm_effective-expiration-popover-trigger">
                    <div style={{ display: 'inline-block' }} className={expirationWarning ? 'gm_warning-text' : ""}>
                      {props.membership.effectiveMembershipExpiresAt || <Msg msgKey='Never' />}
                    </div>
                    <div className="gm_effective-helper-warning">
                      <ExclamationTriangleIcon />
                    </div>
                  </span>
                ) : props.membership?.effectiveGroupId ? (
                  <span className="gm_effective-expiration-popover-trigger">
                    <div style={{ display: 'inline-block' }} className={expirationWarning ? 'gm_warning-text' : ""}>
                      {props.membership.effectiveMembershipExpiresAt || <Msg msgKey='Never' />}
                    </div>
                    <div className="gm_effective-helper-info">
                      <InfoCircleIcon />
                    </div>
                  </span>
                ) : (
                  props.membership.effectiveMembershipExpiresAt || <Msg msgKey='Never' />
                )}
              </Popover>
            </DataListCell>
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
            alignments={{ sm: 'right', md: 'right', lg: 'right', xl: 'right', '2xl': 'right' }}
            onSelect={onSelect}
            toggle={<KebabToggle id="toggle-kebab" onToggle={onToggle} />}
            isOpen={isOpen}
            isPlain
            dropdownItems={[
              <Link to={`/enroll?groupPath=${encodeURI(props.membership.group.path)}`}><DropdownItem key="link">
                <Msg msgKey='enrollmentDiscoveryPageLink' />
              </DropdownItem></Link>,
              <DropdownItem onClick={() => {
                props.setModalInfo({
                  title: (Msg.localize('leaveGroup')+"?"),
                  button_variant: "danger",
                  accept_message: "Leave",
                  cancel_message: "Cancel",
                  message: (Msg.localize('leaveGroupConfirmation')),
                  accept: function () {
                    leaveGroup();
                    props.setModalInfo({})
                  },
                  cancel: function () {
                    props.setModalInfo({})
                  }
                });
              }}>
                <Msg msgKey="leaveGroup" />
              </DropdownItem>
            ]}
          />
        </DataListAction>
      </DataListItemRow>
    </DataListItem >
  );
};
