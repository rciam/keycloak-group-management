import { FC, useState, useEffect } from "react";
import {
  // Tabs,
  // Tab,
  // TabTitleText,
  Breadcrumb,
  BreadcrumbItem,
  TextArea,
  Button,
  Title,
  Page,
  Tab,
  TabTitleText,
  AlertVariant,
} from "@patternfly/react-core";
// @ts-ignore
// import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// import { GroupMembers } from '../../group-widgets/GroupAdminPage/GroupMembers';
// import { GroupAttributes } from '../../group-widgets/GroupAdminPage/GroupAttributes';
// import { GroupDetails } from '../../group-widgets/GroupAdminPage/GroupDetails';
// import { ConfirmationModal, DeleteSubgroupModal } from '../../group-widgets/Modals';
// import { GroupAdmins } from '../../group-widgets/GroupAdminPage/GroupAdmins';
// import { GroupSubGroups } from '../../group-widgets/GroupAdminPage/GroupSubgroups';
// import { GroupEnrollment } from '../../group-widgets/GroupAdminPage/GroupEnrollment';
import { useNavigate } from "react-router-dom";
import { TrashIcon } from "@patternfly/react-icons";
import { RoutableTabs, useRoutableTab } from "../widgets/RoutableTabs";
// import { ContentPage } from '../ContentPage';
// import { ContentAlert } from '../ContentAlert';
// import { getError } from '../../js/utils.js';
// import { useLoader } from '../../group-widgets/LoaderContext';
import { useTranslation } from "react-i18next";
import { useGroupsService } from "../groups-service/GroupsServiceContext";
import { useParams } from "react-router-dom";
import { getError, kcPath } from "../js/utils";
import { GroupDetails } from "./tabs/GroupDetailsTab";
import { useLoader } from "../widgets/LoaderContext";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { ConfirmationModal } from "../widgets/Modals";
import { GroupSubGroups } from "./tabs/GroupSubgroupsTab";
import { GroupAttributes } from "./tabs/GroupAttributesTab";
import { GroupMembers } from "./tabs/GroupMembersTab";
import { GroupEnrollment } from "./tabs/GroupEnrollmentTab";
import { GroupAdmins } from "./tabs/GroupAdminsTab";
import { DeleteSubgroupModal } from "./components/Modals";

export interface AdminGroupPageProps {
  // match: any;
  // history: any;
  // location: any;
}

type AdminGroupPageTabs =
  | "details"
  | "members"
  | "admins"
  | "enrollments"
  | "attributes"
  | "subgroups";

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

export const AdminGroupPage: FC<AdminGroupPageProps> = () => {
  const groupsService = useGroupsService();
  const { t } = useTranslation();
  const [groupConfiguration, setGroupConfiguration] = useState(
    {} as GroupConfiguration,
  );
  const navigate = useNavigate();

  const { groupId } = useParams<any>();
  const [descriptionInput, setDescriptionInput] = useState<string>("");
  const [editDescription, setEditDescription] = useState<boolean>(false);
  const [user, setUser] = useState<User>({} as User);
  const [modalInfo, setModalInfo] = useState({});
  const [deleteGroup, setDeleteGroup] = useState(false);
  const [defaultConfiguration, setDefaultConfiguration] = useState("");
  const [initialRender, setInitialRender] = useState(true);
  const { addAlert, addError } = useAlerts();
  const [isGroupAdmin, setIsGroupAdmin] = useState<boolean>(false);
  const [enrollmentRules, setEnrollmentRules] = useState<Record<string, any>>(
    {},
  );
  const { startLoader, stopLoader } = useLoader();

  useEffect(() => {
    startLoader();
    Promise.all([fetchAccountInfo(), fetchGroupConfiguration()])
      .then(() => {
        stopLoader();
      })
      .catch(() => {
        stopLoader();
      });
  }, []);

  const useTab = (tab: AdminGroupPageTabs) => useRoutableTab(tab);

  const detailsTab = useTab("details");
  const membersTab = useTab("members");
  const attributesTab = useTab("attributes");
  const adminsTab = useTab("admins");
  const subgroupsTab = useTab("subgroups");
  const enrollmentsTab = useTab("enrollments");
  useEffect(() => {
    if (initialRender) {
      setInitialRender(false);
      return;
    }
    startLoader();
    fetchGroupConfiguration().finally(() => {
      stopLoader();
    });
  }, [groupId]);

  useEffect(() => {
    let isAdmin = false;
    if (groupConfiguration?.admins?.length > 0) {
      groupConfiguration.admins.forEach((admin) => {
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
    return "/" + groupConfiguration?.name !== groupConfiguration?.path
      ? "SUBGROUP"
      : "TOP_LEVEL";
  };

  const fetchGroupConfiguration = () => {
    return groupsService!
      .doGet<GroupConfiguration>("/group-admin/group/" + groupId + "/all")
      .then((response: any) => {
        if (response.status === 200 && response.data) {
          if (!Object.keys(enrollmentRules ?? {}).length) {
            fetchGroupEnrollmentRules(getGroupType(response.data));
          }
          if (
            response.data?.attributes?.description?.[0] !== descriptionInput
          ) {
            setDescriptionInput(response.data?.attributes?.description?.[0]);
          }
          if (
            response.data?.attributes?.defaultConfiguration?.[0] !==
            defaultConfiguration
          ) {
            setDefaultConfiguration(
              response.data?.attributes?.defaultConfiguration?.[0],
            );
          }
          setGroupConfiguration(response.data);
        }
      });
  };

  const updateAttributes = (
    attributes: any,
    success_message = t("updateAttributesSuccess"),
    error_message = "updateAttributesError",
  ) => {
    startLoader();
    groupsService!
      .doPost<GroupConfiguration>(
        "/group-admin/group/" + groupId + "/attributes",
        attributes ? { ...attributes } : {},
      )
      .then((response: any) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          addAlert(success_message, AlertVariant.success);
        } else {
          addError(error_message, getError(response));
        }
        fetchGroupConfiguration();
      })
      .catch((err) => {
        stopLoader();
        setModalInfo({});
        const response = err?.response ?? err;
        addError(error_message, getError(response));
      });
  };

  const fetchAccountInfo = () => {
    return groupsService!
      .doGet<any>("/", { target: "base_account" })
      .then((response: any) => {
        if (response.status === 200 && response.data) {
          setUser(response.data);
          return response.data;
        }
      })
      .catch((err) => {
        console.log(err);
        return null;
      });
  };

  const fetchGroupEnrollmentRules = (type: any) => {
    return groupsService!
      .doGet<any>("/group-admin/configuration-rules", {
        params: { type: type },
      })
      .then((response: any) => {
        if (response.status === 200 && response.data) {
          if (response.data.length > 0) {
            let rules: Record<string, any> = {};
            response.data.forEach((field_rules: any) => {
              rules[field_rules.field] = {
                max: parseInt(field_rules.max),
                required: field_rules.required,
                ...(field_rules.defaultValue && {
                  defaultValue: field_rules.defaultValue,
                }),
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
      <DeleteSubgroupModal
        groupId={groupId}
        active={deleteGroup}
        afterSuccess={() => {
          navigate(kcPath("/groups/admingroups"));
        }}
        close={() => {
          setDeleteGroup(false);
        }}
      />
      <div className="pf-v5-c-page__main-section pf-m-light gm_breadcrumb-container">
        <Breadcrumb className="gm_breadcrumb">
          <BreadcrumbItem
            to="#"
            onClick={() => {
              navigate(kcPath("groups/admingroups"));
            }}
          >
            {t("adminGroupLabel")}
          </BreadcrumbItem>
          {groupConfiguration?.parents?.map((group: any, index: number) => {
            return (
              <BreadcrumbItem
                onClick={() => {
                  navigate(kcPath("/groups/admingroups/" + group.id));
                }}
                to="#"
                key={index}
              >
                {group.name}
              </BreadcrumbItem>
            );
          })}
          <BreadcrumbItem isActive>{groupConfiguration?.name}</BreadcrumbItem>
        </Breadcrumb>
      </div>
      <Page className="pf-v5-c-page__main-section pf-m-light gm_page">
        <div className="gm_group-header">
          <Title headingLevel="h1">
            {groupConfiguration?.name}{" "}
            {(isGroupAdmin ||
              "/" + groupConfiguration?.name !== groupConfiguration?.path) &&
              !(
                groupConfiguration?.extraSubGroups &&
                groupConfiguration?.extraSubGroups.length > 0
              ) && (
                <TrashIcon
                  onClick={() => {
                    setDeleteGroup(true);
                  }}
                />
              )}
          </Title>
          {editDescription ? (
            <div className="gm_description-input-container">
              <TextArea
                value={descriptionInput}
                onChange={(_event, value) => setDescriptionInput(value)}
                aria-label="text area example"
              />
              <Button
                className={"gm_button-small"}
                onClick={() => {
                  setModalInfo({
                    title: t("confirmation"),
                    accept_message: t("yes"),
                    cancel_message: t("no"),
                    message: t("descriptionUpdateConfirmation"),
                    accept: function () {
                      if (groupConfiguration.attributes) {
                        groupConfiguration.attributes.description = [
                          descriptionInput,
                        ];
                        updateAttributes(
                          groupConfiguration.attributes,
                          t("updateGroupDescriptionSuccess"),
                          "updateGroupDescriptionError",
                        );
                        setEditDescription(false);
                        setModalInfo({});
                      }
                    },
                    cancel: function () {
                      setEditDescription(false);
                      setModalInfo({});
                    },
                  });
                }}
              >
                <div className={"gm_check-button"}></div>
              </Button>
              <Button
                variant="tertiary"
                className={"gm_button-small"}
                onClick={() => {
                  setEditDescription(false);
                }}
              >
                <div className={"gm_cancel-button"}></div>
              </Button>
            </div>
          ) : (
            <p>
              {(groupConfiguration?.attributes?.description &&
                groupConfiguration?.attributes?.description[0]) ||
                t("noDescription")}
              <div
                className="gm_edit-icon"
                onClick={() => {
                  setEditDescription(true);
                }}
              ></div>
            </p>
          )}
        </div>
        <RoutableTabs className="gm_tabs" isBox={false} defaultTab={"details"}>
          <Tab
            {...detailsTab}
            id="details"
            title={<TabTitleText>{t("adminGroupDetailsTab")}</TabTitleText>}
            aria-label="Default content - users"
          >
            <GroupDetails
              groupConfiguration={groupConfiguration}
              groupId={groupId}
              setGroupConfiguration={setGroupConfiguration}
              fetchGroupConfiguration={fetchGroupConfiguration}
            />
          </Tab>
          <Tab
            {...membersTab}
            id="members"
            title={<TabTitleText>{t("adminGroupMembersTab")}</TabTitleText>}
            aria-label="Default content - members"
          >
            <GroupMembers
              isGroupAdmin={isGroupAdmin}
              membersTab={membersTab}
              // history={props.history}
              groupConfiguration={groupConfiguration}
              enrollmentRules={enrollmentRules}
              groupId={groupId}
              user={user}
            />
          </Tab>
          <Tab
            {...adminsTab}
            id="admins"
            title={<TabTitleText>{t("adminGroupAdminsTab")}</TabTitleText>}
            aria-label="Default content - admins"
          >
            <GroupAdmins
              isGroupAdmin={isGroupAdmin}
              groupId={groupId}
              user={user}
              groupConfiguration={groupConfiguration}
              setGroupConfiguration={setGroupConfiguration}
              fetchGroupConfiguration={fetchGroupConfiguration}
            />
          </Tab>

          <Tab
            {...enrollmentsTab}
            id="enrollments"
            title={<TabTitleText>{t("adminGroupEnrollmentTab")}</TabTitleText>}
            aria-label="Default content - attributes"
          >
            <GroupEnrollment
              isGroupAdmin={isGroupAdmin}
              groupConfiguration={groupConfiguration}
              enrollmentRules={enrollmentRules}
              defaultConfiguration={defaultConfiguration}
              groupId={groupId}
              setGroupConfiguration={setGroupConfiguration}
              fetchGroupConfiguration={fetchGroupConfiguration}
              updateAttributes={updateAttributes}
            />
          </Tab>
          <Tab
            {...attributesTab}
            id="attributes"
            title={<TabTitleText>{t("adminGroupAttributesTab")}</TabTitleText>}
            aria-label="Default content - attributes"
          >
            <GroupAttributes
              isGroupAdmin={isGroupAdmin}
              groupConfiguration={groupConfiguration}
              setGroupConfiguration={setGroupConfiguration}
              fetchGroupConfiguration={fetchGroupConfiguration}
              updateAttributes={updateAttributes}
            />
          </Tab>
          <Tab
            {...subgroupsTab}
            id="subgroups"
            title={<TabTitleText>{t("adminGroupSubgroupsTab")}</TabTitleText>}
            aria-label="Default content - attributes"
          >
            <GroupSubGroups
              isGroupAdmin={isGroupAdmin}
              groupConfiguration={groupConfiguration}
              groupId={groupId}
              setGroupConfiguration={setGroupConfiguration}
              fetchGroupConfiguration={fetchGroupConfiguration}
            />
          </Tab>
        </RoutableTabs>
      </Page>
    </div>
  );
};
