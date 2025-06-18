import * as React from 'react';
import { FC, useState, useEffect } from 'react';
import { Tabs, Tab, TabTitleText, Breadcrumb, BreadcrumbItem, TextArea, Button } from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
import { GroupMembers } from '../../group-widgets/GroupAdminPage/GroupMembers';
import { GroupAttributes } from '../../group-widgets/GroupAdminPage/GroupAttributes';
import { GroupDetails } from '../../group-widgets/GroupAdminPage/GroupDetails';
import { ConfirmationModal, DeleteSubgroupModal } from '../../group-widgets/Modals';
import { GroupAdmins } from '../../group-widgets/GroupAdminPage/GroupAdmins';
import { GroupSubGroups } from '../../group-widgets/GroupAdminPage/GroupSubgroups';
import { GroupEnrollment } from '../../group-widgets/GroupAdminPage/GroupEnrollment';
import { TrashIcon } from '@patternfly/react-icons';
import { Msg } from '../../widgets/Msg';
import { RoutableTabs, useRoutableTab } from '../../widgets/RoutableTabs';
import { ContentPage } from '../ContentPage';
import { ContentAlert } from '../ContentAlert';
import { getError } from '../../js/utils.js';
import { useLoader } from '../../group-widgets/LoaderContext';

export interface AdminGroupPageProps {
  match: any;
  history: any;
  location: any;
}

type AdminGroupPageTabs = "details" | "members" | "admins" | "enrollments" | "attributes" | "subgroups";

interface Group {
  id: string;
  name: string;
  path: string;
  extraSubGroups: Group[];
}

interface AUP {
  id: string;
  type: string;
  url: string;
}

interface EnrollmentAttributes {
  id?: string;
  attribute: string;
  label: string;
  order: number;
  defaultValue: string;
  hidden: boolean;
  modifiable: boolean;
}

interface EnrollmentConfiration {
  id?: string;
  group: Group;
  name: string;
  active: boolean;
  requiredAupAcceptance: boolean;
  reaquireApproval: boolean;
  aupExpiryDays: number;
  membershipExpirationDays: number;
  aup: AUP;
  visibleToNotMembers: boolean;
  attributes: EnrollmentAttributes[];
  groupRoles: string[];
}

interface FederatedIdentity {
  identityProvider: string;
}

interface User {
  id?: string;
  username: string;
  emailVerified: boolean;
  email: string;
  federatedIdentities: FederatedIdentity[];
}

interface UserGroupConfig {
  id?: string;
  username: string;
  emailVerified: boolean;
  email: string;
  federatedIdentities: FederatedIdentity[];
}

interface Admin {
  user: UserGroupConfig;
  direct: boolean;
}

interface GroupConfiguration {
  id?: string;
  name: string;
  path: string;
  attributes: any;
  groupRoles: any;
  enrollmentConfigurationList: EnrollmentConfiration[];
  status: string;
  membershipExpiresAt: string;
  validFrom: string;
  admins: Admin[];
  parents: any;
  extraSubGroups: Group[];
  error_description?: any;
  error?: any;
}

export const AdminGroupPage: FC<AdminGroupPageProps> = (props) => {
  const [groupConfiguration, setGroupConfiguration] = useState({} as GroupConfiguration);
  const [groupId, setGroupId] = useState(props.match.params.id);
  const [activeTabKey, setActiveTabKey] = useState<string | number>(0);
  const [descriptionInput, setDescriptionInput] = useState<string>("");
  const [editDescription, setEditDescription] = useState<boolean>(false);
  const [user, setUser] = useState<User>({} as User);
  const [modalInfo, setModalInfo] = useState({});
  const [deleteGroup, setDeleteGroup] = useState(false);
  const [defaultConfiguration, setDefaultConfiguration] = useState("");
  const [initialRender, setInitialRender] = useState(true);
  const [userRoles, setUserRoles] = useState<String[]>([]);
  const [isGroupAdmin, setIsGroupAdmin] = useState<boolean>(false);
  const [enrollmentRules, setEnrollmentRules] = useState({});
  const { startLoader, stopLoader } = useLoader();

  let groupsService = new GroupsServiceClient();

  useEffect(() => {
    startLoader();
    Promise.all([fetchAccountInfo(), fetchGroupConfiguration()])
      .then(() => {
        stopLoader();
      })
      .catch(() => {
        stopLoader();
      });
    setUserRoles(groupsService.getUserRoles());
  }, []);

  const useTab = (tab: AdminGroupPageTabs) => useRoutableTab(tab);

  const detailsTab = useTab("details");
  const membersTab = useTab("members");
  const attributesTab = useTab("attributes");
  const adminsTab = useTab("admins");
  const subgroupsTab = useTab("subgroups");
  const enrollmentsTab = useTab("enrollments");

  useEffect(() => {
    setGroupId(props.match.params.id);
  }, [props.match.params.id]);

  useEffect(() => {
    if (initialRender) {
      setInitialRender(false);
      return;
    }
    startLoader();
    fetchGroupConfiguration().finally(() => {
      stopLoader();
    });
    setActiveTabKey(0);
  }, [groupId]);

  useEffect(() => {
    let isAdmin = false;
    if (groupConfiguration?.admins?.length > 0) {
      groupConfiguration.admins.forEach((admin) => {
        console.log(user);
        if (admin.user.id === user.id) {
          isAdmin = true;
        }
      });
    }
    setIsGroupAdmin(isAdmin);
  }, [groupConfiguration, user]);

  useEffect(() => {
    if (Object.keys(groupConfiguration).length !== 0) {
      fetchGroupEnrollmentRules(getGroupType(groupConfiguration));
    }
  }, [groupConfiguration]);

  const getGroupType = (groupConfiguration: GroupConfiguration): string => {
    return ("/" + groupConfiguration?.name) !== groupConfiguration?.path ? 'SUBGROUP' : 'TOP_LEVEL';
  };

  const fetchGroupConfiguration = () => {
    return groupsService!.doGet<GroupConfiguration>("/group-admin/group/" + groupId + "/all")
      .then((response: HttpResponse<GroupConfiguration>) => {
        if (response.status === 200 && response.data) {
          if (!Object.keys(enrollmentRules ?? {}).length) {
            fetchGroupEnrollmentRules(getGroupType(response.data));
          }
          if (response.data?.attributes?.description?.[0] !== descriptionInput) {
            setDescriptionInput(response.data?.attributes?.description?.[0]);
          }
          if (response.data?.attributes?.defaultConfiguration?.[0] !== defaultConfiguration) {
            setDefaultConfiguration(response.data?.attributes?.defaultConfiguration?.[0]);
          }
          setGroupConfiguration(response.data);
        }
      });
  };

  const updateAttributes = (attributes, success_message = Msg.localize('updateAttributesSuccess'), error_message = Msg.localize("updateAttributesError")) => {
    startLoader();
    groupsService!.doPost<GroupConfiguration>("/group-admin/group/" + groupId + "/attributes", attributes ? { ...attributes } : {})
      .then((response: HttpResponse<GroupConfiguration>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          fetchGroupConfiguration();
          ContentAlert.success(success_message);
        } else {
          fetchGroupConfiguration();
          ContentAlert.danger(error_message + " " + getError(response));
        }
      });
  };

    const fetchAccountInfo = () => {
      return groupsService!.doGet<any>("/", { target: "base_account" })
        .then((response: HttpResponse<any>) => {
          if (response.status === 200 && response.data) {
            setUser(response.data);
            return response.data;
          }
        }).catch(err => {
          console.log(err);
          return null;
        });
    };


  const fetchGroupEnrollmentRules = (type) => {
    return groupsService!.doGet<any>("/group-admin/configuration-rules", { params: { type: type } })
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          if (response.data.length > 0) {
            let rules = {};
            response.data.forEach(field_rules => {
              rules[field_rules.field] = {
                "max": parseInt(field_rules.max),
                "required": field_rules.required,
                ...(field_rules.defaultValue && { "defaultValue": field_rules.defaultValue })
              };
            });
            setEnrollmentRules(rules);
          } else {
            setEnrollmentRules({});
          }
        }
      });
  };

  return (
    <div className="gm_content">
      <ConfirmationModal modalInfo={modalInfo} />
      <DeleteSubgroupModal groupId={groupId} active={deleteGroup} afterSuccess={() => { props.history.push('/groups/admingroups'); }} close={() => { setDeleteGroup(false); }} />
      <Breadcrumb className="gm_breadcumb">
        <BreadcrumbItem to="#">
          <Msg msgKey='accountConsole' />
        </BreadcrumbItem>
        <BreadcrumbItem to="#/groups/admingroups">
          <Msg msgKey='adminGroupLabel' />
        </BreadcrumbItem>
        {groupConfiguration?.parents?.map((group, index) => {
          return (
            <BreadcrumbItem to={"#/groups/admingroups/" + group.id} key={index}>
              {group.name}
            </BreadcrumbItem>
          );
        })}
        <BreadcrumbItem isActive>
          {groupConfiguration?.name}
        </BreadcrumbItem>
      </Breadcrumb>
      <ContentPage>
        <h1 className="pf-c-title pf-m-2xl pf-u-mb-xl gm_group-title gm_flex-center">{groupConfiguration?.name} {(isGroupAdmin || ("/" + groupConfiguration?.name) !== groupConfiguration?.path) && !(groupConfiguration?.extraSubGroups && groupConfiguration?.extraSubGroups.length > 0) && <TrashIcon onClick={() => { setDeleteGroup(true); }} />}</h1>
        {editDescription ?
          <div className="gm_description-input-container">
            <TextArea value={descriptionInput} onChange={value => setDescriptionInput(value)} aria-label="text area example" />
            <Button className={"gm_button-small"}
              onClick={() => {
                setModalInfo({
                  title: Msg.localize('confirmation'),
                  accept_message: Msg.localize('yes'),
                  cancel_message: Msg.localize('no'),
                  message: (Msg.localize('descriptionUpdateConfirmation')),
                  accept: function () {
                    if (groupConfiguration.attributes) {
                      groupConfiguration.attributes.description = [descriptionInput];
                      updateAttributes(groupConfiguration.attributes, "Group description was completed successfully.", "Group description could not be updated due to:");
                      setEditDescription(false);
                      setModalInfo({});
                    }
                  },
                  cancel: function () {
                    setEditDescription(false);
                    setModalInfo({});
                  }
                });
              }}
            >
              <div className={"gm_check-button"}></div>
            </Button>
            <Button variant="tertiary" className={"gm_button-small"} onClick={() => { setEditDescription(false); }}>
              <div className={"gm_cancel-button"}></div>
            </Button>
          </div>
          : <p className="gm_group_desc">
            {(groupConfiguration?.attributes?.description && groupConfiguration?.attributes?.description[0]) || Msg.localize('noDescription')}
            <div className="gm_edit-icon" onClick={() => { setEditDescription(true); }}></div>
          </p>
        }
        <RoutableTabs className="gm_tabs"
          isBox={false}
          defaultTab={"details"}
        >
          <Tab {...detailsTab} id="details" title={<TabTitleText><Msg msgKey='adminGroupDetailsTab' /></TabTitleText>} aria-label="Default content - users">
            <GroupDetails groupConfiguration={groupConfiguration} groupId={groupId} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration} />
          </Tab>

          <Tab {...membersTab} id="members" title={<TabTitleText><Msg msgKey='adminGroupMembersTab' /></TabTitleText>} aria-label="Default content - members">
            <GroupMembers isGroupAdmin={isGroupAdmin} membersTab={membersTab} history={props.history} groupConfiguration={groupConfiguration} enrollmentRules={enrollmentRules} groupId={groupId} user={user} />
          </Tab>
          <Tab {...adminsTab} id="admins" title={<TabTitleText><Msg msgKey='adminGroupAdminsTab' /></TabTitleText>} aria-label="Default content - admins">
            <GroupAdmins isGroupAdmin={isGroupAdmin} groupId={groupId} user={user} groupConfiguration={groupConfiguration} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration} />
          </Tab>
          <Tab {...enrollmentsTab} id="enrollments" title={<TabTitleText><Msg msgKey='adminGroupEnrollmentTab' /></TabTitleText>} aria-label="Default content - attributes">
            <GroupEnrollment isGroupAdmin={isGroupAdmin} groupConfiguration={groupConfiguration} enrollmentRules={enrollmentRules} defaultConfiguration={defaultConfiguration} groupId={groupId} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration} updateAttributes={updateAttributes} />
          </Tab>
          <Tab {...attributesTab} id="attributes" title={<TabTitleText><Msg msgKey='adminGroupAttributesTab' /></TabTitleText>} aria-label="Default content - attributes">
            <GroupAttributes isGroupAdmin={isGroupAdmin} groupConfiguration={groupConfiguration} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration} updateAttributes={updateAttributes} />
          </Tab>
          <Tab {...subgroupsTab} id="subgroups" title={<TabTitleText><Msg msgKey='adminGroupSubgroupsTab' /></TabTitleText>} aria-label="Default content - attributes">
            <GroupSubGroups isGroupAdmin={isGroupAdmin} groupConfiguration={groupConfiguration} groupId={groupId} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration} />
          </Tab>
        </RoutableTabs>
      </ContentPage>
    </div>
  );
};


