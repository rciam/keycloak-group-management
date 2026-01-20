import * as React from "react";
import { FC, useState, useEffect } from "react";

import {
  DataList,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  DataListCell,
  Button,
  Tooltip,
  DataListAction,
  Dropdown,
  DropdownItem,
  Checkbox,
  MenuToggleElement,
  MenuToggle,
  DropdownList,
} from "@patternfly/react-core";
import {
  EllipsisVIcon,
  ExternalLinkAltIcon,
  EyeIcon,
} from "@patternfly/react-icons";
import { EnrollmentModal } from "../components/EnrollmentModal";
import { Link } from "react-router-dom";
import { isIntegerOrNumericString, kcPath } from "../../js/utils.js";
import { useLoader } from "../../widgets/LoaderContext.js";
import { useTranslation } from "react-i18next";
import { useGroupsService } from "../../groups-service/GroupsServiceContext.js";
import { HttpResponse } from "../../groups-service/groups-service.js";

// interface FederatedIdentity {
//   identityProvider: string;
// }

// interface User {
//   id?: string;
//   username: string;
//   emailVerified: boolean;
//   email: string;
//   federatedIdentities: FederatedIdentity[];
//   firstName: string;
//   lastName: string;
//   attributes: any;
// }

export const GroupEnrollment: FC<any> = (props) => {
  const [groupEnrollments, setGroupEnrollments] = useState<any>([]);
  const [enrollmentModal, setEnrollmentModal] = useState({});
  const { startLoader, stopLoader } = useLoader();
  const { t } = useTranslation();

  const staticDefaultEnrollmentConfiguration = {
    group: { id: "" },
    membershipExpirationDays: 32,
    name: "",
    active: true,
    requireApproval: true,
    aup: {
      type: "URL",
      url: "",
    },
    requireApprovalForExtension: false,
    multiselectRole: true,
    visibleToNotMembers: false,
    validFrom: null,
    commentsNeeded: true,
    commentsLabel: t("enrollmentConfigurationCommentsDefaultLabel"),
    commentsDescription: t("enrollmentConfigurationCommentsDefaultDescription"),
    groupRoles: [],
  };

  const groupsService = useGroupsService();

  useEffect(() => {
    if (props.groupId) {
      fetchGroupEnrollments();
    }
  }, [props.groupId]);

  let fetchGroupEnrollments = () => {
    startLoader();
    groupsService!
      .doGet<any>("/group-admin/group/" + props.groupId + "/configuration/all")
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 && response.data) {
          setGroupEnrollments(response.data);
        }
      });
  };

  let getDefaultEnrollmentConfiguration = () => {
    let defaultConfig = staticDefaultEnrollmentConfiguration as any;
    for (let field in props.enrollmentRules) {
      if (props.enrollmentRules[field].defaultValue) {
        if (
          isIntegerOrNumericString(props.enrollmentRules[field].defaultValue)
        ) {
          defaultConfig[field] = parseInt(
            props.enrollmentRules[field].defaultValue
          );
        } else {
          defaultConfig[field] = props.enrollmentRules[field].defaultValue;
        }
      }
    }
    defaultConfig.group.id = props.groupId;
    return defaultConfig;
  };

  const noGroupEnrollments = () => {
    return (
      <DataListItem key="emptyItem" aria-labelledby="empty-item">
        <DataListItemRow key="emptyRow">
          <DataListItemCells
            dataListCells={[
              <DataListCell key="empty">
                <strong>{t("adminGroupNoEnrollments")}</strong>
              </DataListCell>,
            ]}
          />
        </DataListItemRow>
      </DataListItem>
    );
  };

  return (
    <React.Fragment>
      <EnrollmentModal
        enrollment={enrollmentModal}
        validationRules={props.enrollmentRules}
        groupRoles={props.groupConfiguration.groupRoles}
        close={() => {
          setEnrollmentModal({});
        }}
        refresh={() => {
          fetchGroupEnrollments();
        }}
        groupId={props.groupId}
      />
      <DataList
        aria-label="Group Member Datalist"
        isCompact
        wrapModifier={"breakWord"}
      >
        <DataListItem aria-labelledby="compact-item1">
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[
                <DataListCell
                  className="gm_vertical_center_cell"
                  width={3}
                  key="id-hd"
                >
                  <strong>{t("Name")}</strong>
                </DataListCell>,
                <DataListCell
                  className="gm_vertical_center_cell"
                  width={3}
                  key="username-hd"
                >
                  <strong>{t("Status")}</strong>
                </DataListCell>,
                <DataListCell
                  className="gm_vertical_center_cell"
                  width={3}
                  key="aup-hd"
                >
                  <strong>{t("Aup")}</strong>
                </DataListCell>,
                <DataListCell
                  className="gm_vertical_center_cell"
                  width={3}
                  key="default-hd"
                >
                  <strong>{t("Default")}</strong>
                </DataListCell>,
                <DataListCell
                  className="gm_vertical_center_cell"
                  width={3}
                  key="visible-hd"
                >
                  <strong>{t("Visible")}</strong>
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
              <Tooltip content={<div>{t("createEnrollmentButton")}</div>}>
                <Button
                  className={"gm_plus-button-small"}
                  onClick={() => {
                    setEnrollmentModal(getDefaultEnrollmentConfiguration());
                  }}
                >
                  <div className={"gm_plus-button"}></div>
                </Button>
              </Tooltip>
            </DataListAction>
          </DataListItemRow>
        </DataListItem>
        {groupEnrollments.length > 0
          ? groupEnrollments.map((enrollment: any, index: number) => {
              return (
                <GroupEnrollmentItem
                  {...{
                    enrollment,
                    index,
                    defaultConfiguration: props.defaultConfiguration,
                    groupConfiguration: props.groupConfiguration,
                    updateAttributes: props.updateAttributes,
                    setEnrollmentModal,
                    groupId: props.groupId,
                  }}
                />
              );
            })
          : noGroupEnrollments()}
      </DataList>
    </React.Fragment>
  );
};

interface GroupEnrollmentItemProps {
  enrollment: any; // Replace 'any' with the actual type of 'enrollment'
  index: number;
  updateAttributes: any;
  defaultConfiguration: any;
  groupConfiguration: any;
  groupId: string;
  setEnrollmentModal: (arg0: any) => void;
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
  const { t } = useTranslation();

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
  const disapearingTooltip = () => {
    setTooltip(true);
    setTimeout(() => {
      setTooltip(false);
    }, 2000);
  };

  const groupsService = useGroupsService();

  const onMakeDefault = () => {
    if (defaultConfiguration) {
      groupConfiguration.attributes.defaultConfiguration[0] = enrollment?.id;
    } else {
      groupConfiguration.attributes.defaultConfiguration = [enrollment?.id];
    }
    updateAttributes(
      { ...groupConfiguration.attributes },
      t("updateDefaultEnrollmentSuccess"),
      t("updateDefaultEnrollmentError")
    );
  };

  const onCopyLink = () => {
    disapearingTooltip();
    let link =
      groupsService.getBaseUrl() +
      "/account/enroll?id=" +
      encodeURI(enrollment.id);
    navigator.clipboard.writeText(link);
  };


  return (
    <DataListItem aria-labelledby={"enrollment-" + index}>
      <DataListItemRow>
        <DataListItemCells
          dataListCells={[
            <DataListCell
              width={3}
              key="primary content"
              onClick={() => {
                enrollment?.aup?.id && delete enrollment.aup.id;
                if (!enrollment.validFrom) {
                  enrollment.validFrom = null;
                }
                if (!enrollment.aup) {
                  enrollment.aup = {
                    type: "URL",
                    url: "",
                  };
                }
                if (!enrollment.hasOwnProperty("membershipExpirationDays")) {
                  enrollment.membershipExpirationDays = 0;
                }
                setEnrollmentModal(enrollment);
              }}
            >
              <Link
                to={kcPath(
                  "/groups/admingroups/" + groupId + "?tab=enrollments"
                )}
              >
                {enrollment.name || t("notAvailable")}
              </Link>
            </DataListCell>,
            <DataListCell
              width={3}
              className={
                enrollment.active
                  ? "gm_group-enrollment-active"
                  : "gm_group-enrollment-inactive"
              }
              key="status-content"
            >
              <strong>{enrollment.active ? t("Active") : t("Inactive")}</strong>
            </DataListCell>,
            <DataListCell width={3} key="aup-content">
              {enrollment?.aup?.url ? (
                <a href={enrollment?.aup?.url} target="_blank" rel="noreferrer">
                  link <ExternalLinkAltIcon />{" "}
                </a>
              ) : (
                t("notAvailable")
              )}
            </DataListCell>,
            <DataListCell width={3} key="default-content">
              <Tooltip content={<div>{t("DefaultEnrollmentTooltip")}</div>}>
                <Checkbox
                  id="disabled-check-1"
                  className="gm_direct-checkbox"
                  isChecked={enrollment?.id === defaultConfiguration}
                  isDisabled
                />
              </Tooltip>
            </DataListCell>,
            <DataListCell width={3} key="visible-content">
              {enrollment?.visibleToNotMembers && (
                <Tooltip content={<div>{t("visibleEnrollmentTooltip")}</div>}>
                  <EyeIcon className="gm_primary-color" />
                </Tooltip>
              )}
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
                {...enrollment?.id !== defaultConfiguration
                  ? [
                      <DropdownItem key="link" onClick={() => onMakeDefault()}>
                        {t("makeDefault")}
                      </DropdownItem>,
                    ]
                  : []}
                <DropdownItem key="link" onClick={() => onCopyLink()}>
                  {t("copyEnrollmentLink")}
                </DropdownItem>
              </DropdownList>
            </Dropdown>

          </Tooltip>
        </DataListAction>
      </DataListItemRow>
    </DataListItem>
  );
};
