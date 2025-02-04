import * as React from 'react';
import { FC, useState, useEffect, useRef } from 'react';

import { DataList, DataListItem, DataListItemCells, DataListItemRow, DataListCell, Button, Tooltip, DataListAction, Pagination, InputGroup, TextInput, Dropdown, BadgeToggle, DropdownItem, Badge, Modal, Checkbox, KebabToggle } from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import { ExternalLinkAltIcon, EyeIcon } from '@patternfly/react-icons';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '
import { Msg } from '../../widgets/Msg';
import { EnrollmentModal } from '../GroupEnrollment/CreateEnrollmentModal';
import { Link } from 'react-router-dom';
import { isIntegerOrNumericString } from '../../js/utils.js'
import { TableActionBar } from './TableActionBar';



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



export const GroupEnrollment: FC<any> = (props) => {

  const [modalInfo, setModalInfo] = useState({});
  const [groupEnrollments, setGroupEnrollments] = useState<any>([]);
  const [enrollmentModal, setEnrollmentModal] = useState({});
  const [tooltip, setTooltip] = useState(false);

  const staticDefaultEnrollmentConfiguration = {
    group: { id: "" },
    membershipExpirationDays: 32,
    name: "",
    active: true,
    requireApproval: true,
    aup: {
      type: "URL",
      url: ""
    },
    requireApprovalForExtension: false,
    multiselectRole: true,
    visibleToNotMembers: false,
    validFrom: null,
    commentsNeeded: true,
    commentsLabel: Msg.localize('enrollmentConfigurationCommentsDefaultLabel'),
    commentsDescription: Msg.localize('enrollmentConfigurationCommentsDefaultDescription'),
    groupRoles: []
  }

  let groupsService = new GroupsServiceClient();


  useEffect(() => {
    if (props.groupId) {
      fetchGroupEnrollments();
    }
  }, [props.groupId]);

  let fetchGroupEnrollments = () => {
    groupsService!.doGet<any>("/group-admin/group/" + props.groupId + "/configuration/all")
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setGroupEnrollments(response.data);
        }
      })
  }

  let getDefaultEnrollmentConfiguration = () => {
    let defaultConfig = staticDefaultEnrollmentConfiguration;
    for (let field in props.enrollmentRules) {
      if (props.enrollmentRules[field].defaultValue) {
        if (isIntegerOrNumericString(props.enrollmentRules[field].defaultValue)) {
          defaultConfig[field] = parseInt(props.enrollmentRules[field].defaultValue);
        }
        else {
          defaultConfig[field] = props.enrollmentRules[field].defaultValue;
        }
      }
    }
    defaultConfig.group.id = props.groupId;
    return defaultConfig;
  }


  const noGroupEnrollments = () => {
    return (
      <DataListItem key='emptyItem' aria-labelledby="empty-item">
        <DataListItemRow key='emptyRow'>
          <DataListItemCells dataListCells={[
            <DataListCell key='empty'><strong><Msg msgKey='adminGroupNoEnrollments' /></strong></DataListCell>
          ]} />
        </DataListItemRow>
      </DataListItem>
    )
  }




  return (
    <React.Fragment>
      <ConfirmationModal modalInfo={modalInfo} />
      <EnrollmentModal enrollment={enrollmentModal} validationRules={props.enrollmentRules} groupRoles={props.groupConfiguration.groupRoles} close={() => { fetchGroupEnrollments(); setEnrollmentModal({}); }} groupId={props.groupId} />
      <DataList aria-label="Group Member Datalist" isCompact wrapModifier={"breakWord"}>
        <DataListItem aria-labelledby="compact-item1">
          <DataListItemRow>
            <DataListItemCells dataListCells={[
              <DataListCell className="gm_vertical_center_cell" width={3} key="id-hd">
                <strong><Msg msgKey='Name' /></strong>
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={3} key="username-hd">
                <strong><Msg msgKey='Status' /></strong>
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={3} key="email-hd">
                <strong><Msg msgKey='Aup' /></strong>
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={3} key="email-hd">
                <strong><Msg msgKey='Default' /></strong>
              </DataListCell>,
              <DataListCell className="gm_vertical_center_cell" width={3} key="email-hd">
                <strong><Msg msgKey='Visible' /></strong>
              </DataListCell>
            ]}>
            </DataListItemCells>
            <DataListAction
              className="gm_cell-center"
              aria-labelledby="check-action-item1 check-action-action2"
              id="check-action-action1"
              aria-label="Actions"
              isPlainButtonAction
            >
              <Tooltip content={<div><Msg msgKey='createEnrollmentButton' /></div>}>
                <Button className={"gm_plus-button-small"} onClick={() => { setEnrollmentModal(getDefaultEnrollmentConfiguration()); }}>
                  <div className={"gm_plus-button"}></div>
                </Button>
              </Tooltip>
            </DataListAction>
          </DataListItemRow>
        </DataListItem>
        {groupEnrollments.length > 0 ? groupEnrollments.map((enrollment, index) => {
          return <GroupEnrollmentItem {...{ enrollment, index, defaultConfiguration: props.defaultConfiguration, groupConfiguration: props.groupConfiguration, updateAttributes: props.updateAttributes, setEnrollmentModal, groupId: props.groupId }} />
        }) : noGroupEnrollments()}
      </DataList>


    </React.Fragment>

  )
}


interface GroupEnrollmentItemProps {
  enrollment: any; // Replace 'any' with the actual type of 'enrollment'
  index: number;
  updateAttributes: (any) => void;
  defaultConfiguration: any;
  groupConfiguration: any;
  groupId: string;
  setEnrollmentModal: (any) => void;
}

const GroupEnrollmentItem: FC<GroupEnrollmentItemProps> = ({
  enrollment,
  index,
  defaultConfiguration,
  groupConfiguration,
  updateAttributes,
  setEnrollmentModal,
  groupId,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [tooltip, setTooltip] = useState(false);

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onFocus = () => {
    const element = document.getElementById('toggle-kebab');
    element && element.focus();
  };

  const onSelect = () => {
    setIsOpen(false);
    onFocus();
  };
  const disapearingTooltip = () => {
    setTooltip(true);
    setTimeout(() => {
      setTooltip(false);
    }, 2000);

  }

  let groupsService = new GroupsServiceClient();

  const onMakeDefault = () => {
    if (defaultConfiguration) {
      groupConfiguration.attributes.defaultConfiguration[0] = enrollment?.id;
    }
    else {
      groupConfiguration.attributes.defaultConfiguration = [enrollment?.id]
    }
    updateAttributes({ ...groupConfiguration.attributes });
  }

  const onCopyLink = () => {
    disapearingTooltip();
    let link = groupsService.getBaseUrl() + '/account/#/enroll?id=' + encodeURI(enrollment.id);
    navigator.clipboard.writeText(link)
  }



  const dropdownItems = [
    ...(enrollment?.id !== defaultConfiguration ? [<DropdownItem key="link" onClick={() => onMakeDefault()}>
      <Msg msgKey='makeDefault' />
    </DropdownItem>] : []),
    <DropdownItem key="link" onClick={() => onCopyLink()}>
      <Msg msgKey='copyEnrollmentLink' />
    </DropdownItem>,
  ];

  return (
    <DataListItem aria-labelledby={"enrollment-" + index}>
      <DataListItemRow>
        <DataListItemCells
          dataListCells={[
            <DataListCell width={3} key="primary content" onClick={() => {
              enrollment?.aup?.id && delete enrollment.aup.id;
              if (!enrollment.validFrom) {
                enrollment.validFrom = null;
              }
              if (!enrollment.aup) {
                enrollment.aup = {
                  type: "URL",
                  url: ""
                }
              }
              if (!enrollment.hasOwnProperty("membershipExpirationDays")) {
                enrollment.membershipExpirationDays = 0;
              }
              setEnrollmentModal(enrollment)
            }}>
              <Link to={"/groups/admingroups/" + groupId}>{enrollment.name || Msg.localize('notAvailable')}</Link>
            </DataListCell>,
            <DataListCell width={3} className={enrollment.active ? "gm_group-enrollment-active" : "gm_group-enrollment-inactive"} key="secondary content ">
              <strong>{enrollment.active ? Msg.localize('Active') : Msg.localize('Inactive')}</strong>
            </DataListCell>,
            <DataListCell width={3} key="secondary content ">
              {enrollment?.aup?.url ? <a href={enrollment?.aup?.url} target="_blank" rel="noreferrer">link <ExternalLinkAltIcon /> </a> : Msg.localize('notAvailable')}
            </DataListCell>,
            <DataListCell width={3} key="secondary content ">
              <Tooltip content={<div><Msg msgKey='DefaultEnrollmentTooltip' /></div>}>
                <Checkbox id="disabled-check-1" className="gm_direct-checkbox" isChecked={enrollment?.id === defaultConfiguration} isDisabled />
              </Tooltip>
            </DataListCell>,
            <DataListCell width={3} key="secondary content ">
              {enrollment?.visibleToNotMembers &&
                <Tooltip content={<div><Msg msgKey={'visibleEnrollmentTooltip'} /></div>}>
                  <EyeIcon className="gm_primary-color" />
                </Tooltip>
              }
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
          <Tooltip {...(!!(tooltip) ? { trigger: 'manual', isVisible: true } : { trigger: 'manual', isVisible: false })}
            content={
              <div><Msg msgKey='copiedTooltip' /></div>
            }
          >
            <Dropdown
              alignments={{
                sm: 'right',
                md: 'right',
                lg: 'right',
                xl: 'right',
                '2xl': 'right'
              }}
              onSelect={onSelect}
              toggle={<KebabToggle id="toggle-kebab-1" onToggle={onToggle} />}
              isOpen={isOpen}
              isPlain
              dropdownItems={dropdownItems}
            />
          </Tooltip>
        </DataListAction>
      </DataListItemRow>
    </DataListItem>




  );
};
