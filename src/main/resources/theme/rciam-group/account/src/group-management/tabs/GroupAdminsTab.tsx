import {
  DataList,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  DataListCell,
  Button,
  Tooltip,
  DataListAction,
  Checkbox,
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
  TextInputGroup,
  TextInputGroupMain,
  TextInputGroupUtilities,
  FormAlert,
  Alert,
  AlertVariant,
  SelectOptionProps,
} from "@patternfly/react-core";
import { TimesIcon } from "@patternfly/react-icons";
import * as React from "react";
import { FC, useState, useEffect } from "react";
// @ts-ignore
// @ts-ignore
import { ConfirmationModal } from "../../widgets/Modals";
import { ValidateEmail, getError } from "../../js/utils.js";
import { useLoader } from "../../widgets/LoaderContext.js";
import { useTranslation } from "react-i18next";
import { useGroupsService } from "../../groups-service/GroupsServiceContext.js";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { HttpResponse } from "../../groups-service/groups-service.js";

export const GroupAdmins: FC<any> = (props) => {
  interface AdminSelectOption extends SelectOptionProps {
    id?: string; // userId
    email?: string; // membership email
    disabled?: boolean; // already admin
    isInviteOption?: boolean;
  }

  const CREATE_INVITE_REG = /Send invite to this email address:\s*"([^"]+)"/;

  const [focusedItemIndex, setFocusedItemIndex] = useState<number | null>(null);
  const [activeItemId, setActiveItemId] = useState<string | null>(null);

  const [inputValue, setInputValue] = useState<string>("");

  // this replaces rendering <SelectOption/> from `options` directly
  const [selectOptions, setSelectOptions] = useState<AdminSelectOption[]>([]);

  const [isOpen, setIsOpen] = useState<boolean>(false);
  const [selected, setSelected] = useState<any>(null);
  const [emailError, setEmailError] = useState<boolean>(false);
  const [inviteAddress, setInviteAddress] = useState<string>("");
  const [selectedUserId, setSelectedUserId] = useState<string>("");
  const [modalInfo, setModalInfo] = useState({});
  const { startLoader, stopLoader } = useLoader();
  const [groupIds, setGroupIds] = useState([]);
  const [groupAdminIds, setGroupAdminIds] = useState<any>([]);
  const [initialRender, setInitialRender] = useState(true);
  const { t } = useTranslation();
  const groupsService = useGroupsService();
  const { addAlert, addError } = useAlerts();

  useEffect(() => {
    fetchGroupAdminIds();
  }, []);

  useEffect(() => {
    if (initialRender) {
      setInitialRender(false);
      return;
    }
    fetchGroupAdminIds();
  }, [props.groupId]);

  useEffect(() => {
    if (groupIds.length > 0) {
      fetchGroupMembers();
    }
  }, [groupIds]);

  useEffect(() => {
    if (initialRender) {
      setInitialRender(false);
      return;
    }
    let groupadminids = [] as any;
    props.groupConfiguration?.admins?.length > 0 &&
      props.groupConfiguration?.admins.map((admin: any) => {
        groupadminids.push(admin.user.id);
      });
    setGroupAdminIds(groupadminids);
  }, [props.groupConfiguration]);

  const noAdmins = () => {
    return (
      <DataListItem key="emptyItem" aria-labelledby="empty-item">
        <DataListItemRow key="emptyRow">
          <DataListItemCells
            dataListCells={[
              <DataListCell key="empty">
                <strong>{t("adminGroupNoAdmins")}</strong>
              </DataListCell>,
            ]}
          />
        </DataListItemRow>
      </DataListItem>
    );
  };

  let fetchGroupMembers = async (searchString = "") => {
    groupsService!
      .doGet<any>("/group-admin/groups/members", {
        params: { max: 20, search: searchString, groups: groupIds.join(",") },
      })
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          const members: AdminSelectOption[] = response.data.results.map(
            (membership: any) => {
              const label = getUserIdentifier(membership);
              const alreadyAdmin = groupAdminIds.includes(membership.id);

              return {
                value: membership.id, // IMPORTANT: use stable id as value
                id: membership.id,
                email: membership.email,
                isDisabled: alreadyAdmin,
                children: label, // main line (name/identifier)
                description: membership.email, // second line
              };
            }
          );



          // Add "invite this email" option when typing something
          if (searchString?.trim()) {
            members.push({
              value: `Send invite to this email address: "${searchString.trim()}"`,
              children: `Send invite to this email address: "${searchString.trim()}"`,
              isInviteOption: true,
            });
          }

          setSelectOptions(members);
        }
      })
      .catch((err) => console.log(err));
  };

  const makeAdmin = (userId: string) => {
    startLoader();
    groupsService!
      .doPost<any>(
        "/group-admin/group/" + props.groupId + "/admin",
        {},
        { params: { userId: userId } }
      )
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          props.fetchGroupConfiguration();
          addAlert(t("addAdminSuccess"), AlertVariant.success);
          clearSelection();
        } else {
          addError("addAdminError", getError(response));
        }
      })
      .catch((err) => {
        stopLoader();
        const response = err?.response ?? err;
        console.log(getError(response));
        addError("addAdminError", getError(response));
      });
  };

  const removeAdmin = (userId: string) => {
    startLoader();
    groupsService!
      .doDelete<any>("/group-admin/group/" + props.groupId + "/admin", {
        params: { userId: userId },
      })
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          addAlert(t("removeAdminSuccess"), AlertVariant.success);
          props.fetchGroupConfiguration();
        } else {
          addError("removeAdminError", getError(response));
        }
        stopLoader();
      })
      .catch((err) => {
        stopLoader();
        const response = err?.response ?? err;
        console.log(getError(response));
        addError("removeAdminError", getError(response));
      });
  };

  const sendInvitation = (email: string) => {
    startLoader();
    groupsService!
      .doPost<any>("/group-admin/group/" + props.groupId + "/admin/invite", {
        email: email,
      })
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          addAlert(t("adminInvitationSuccess"), AlertVariant.success);
          props.fetchGroupConfiguration();
        } else {
          addError("adminInvitationError", getError(response));
        }
      })
      .catch((err) => {
        stopLoader();
        const response = err?.response ?? err;
        console.log(getError(response));
        addError("adminInvitationError", getError(response));
      });
  };

  let fetchGroupAdminIds = () => {
    groupsService!
      .doGet<any>("/group-admin/groupids/all")
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setGroupIds(response.data);
        }
      })
      .catch((err) => {
        console.log(err);
      });
  };

  let getUserIdentifier = (user: any) => {
    return user.firstName || user.lastName
      ? (user.firstName && user.firstName + " ") + user.lastName
      : user.username
      ? user.username
      : user.email
      ? user.email
      : user.id
      ? user.id
      : t("infoNotAvailable");
  };

  const clearSelection = () => {
    setInviteAddress("");
    setSelectedUserId("");
    setSelected(null);
    setInputValue("");
    setIsOpen(false);
    setEmailError(false);
    resetActiveAndFocusedItem();
    fetchGroupMembers();
  };


  const createItemId = (value: any) =>
    `group-admin-typeahead-${String(value).replace(/\s+/g, "-")}`;

  const setActiveAndFocusedItem = (itemIndex: number) => {
    setFocusedItemIndex(itemIndex);
    const focusedItem = selectOptions[itemIndex];
    setActiveItemId(createItemId(focusedItem.value));
  };

  const resetActiveAndFocusedItem = () => {
    setFocusedItemIndex(null);
    setActiveItemId(null);
  };

  const closeMenu = () => {
    setIsOpen(false);
    resetActiveAndFocusedItem();
  };

  const onInputClick = () => {
    if (!isOpen) setIsOpen(true);
    else if (!inputValue) closeMenu();
  };

  const handleMenuArrowKeys = (key: string) => {
    let indexToFocus = 0;

    if (!isOpen) setIsOpen(true);
    if (selectOptions.length === 0) return;

    // skip aria-disabled items
    const isDisabled = (opt: any) => !!opt.isDisabled || !!opt.isAriaDisabled;

    if (key === "ArrowUp") {
      if (focusedItemIndex === null || focusedItemIndex === 0)
        indexToFocus = selectOptions.length - 1;
      else indexToFocus = focusedItemIndex - 1;

      while (isDisabled(selectOptions[indexToFocus])) {
        indexToFocus--;
        if (indexToFocus < 0) indexToFocus = selectOptions.length - 1;
      }
    }

    if (key === "ArrowDown") {
      if (
        focusedItemIndex === null ||
        focusedItemIndex === selectOptions.length - 1
      )
        indexToFocus = 0;
      else indexToFocus = focusedItemIndex + 1;

      while (isDisabled(selectOptions[indexToFocus])) {
        indexToFocus++;
        if (indexToFocus >= selectOptions.length) indexToFocus = 0;
      }
    }

    setActiveAndFocusedItem(indexToFocus);
  };
  const onSelect = (
  _event: React.MouseEvent<Element, MouseEvent> | undefined,
  value: any
) => {
  if (!value) return;

  // Invite flow
  const inviteEmail = String(value).match(CREATE_INVITE_REG)?.[1];
  if (inviteEmail) {
    setSelectedUserId("");
    setInviteAddress("");
    setEmailError(false);

    if (ValidateEmail(inviteEmail)) {
      setInviteAddress(inviteEmail);
      setEmailError(false);
    } else {
      setInviteAddress("");
      setEmailError(true);
    }

    setSelected(inviteEmail);
    setInputValue(inviteEmail);
    closeMenu();
    return;
  }

  // Existing user flow (value is membership.id)
  const option = selectOptions.find((o) => String(o.value) === String(value));
  if (!option || option.isDisabled) return;

  setInviteAddress("");
  setEmailError(false);

  setSelectedUserId(String(option.id ?? value));

  const display = (option.children as string) || option.description || "";
  setSelected(display);
  setInputValue(String(display));
  closeMenu();
};


  const onInputKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    const focusedItem =
      focusedItemIndex !== null ? selectOptions[focusedItemIndex] : null;

    switch (event.key) {
      case "Enter":
        if (
          isOpen &&
          focusedItem &&
          !focusedItem.isAriaDisabled &&
          !focusedItem.isDisabled
        ) {
          onSelect(undefined, focusedItem.value as string);
        }
        if (!isOpen) setIsOpen(true);
        break;

      case "ArrowUp":
      case "ArrowDown":
        event.preventDefault();
        handleMenuArrowKeys(event.key);
        break;
    }
  };

  return (
    <React.Fragment>
      <ConfirmationModal modalInfo={modalInfo} />
      <DataList
        aria-label="Group Member Datalist"
        isCompact
        wrapModifier={"breakWord"}
      >
        <DataListItem aria-labelledby="compact-item1">
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[
                <DataListCell width={1} key="username-hd">
                  <strong>{t("username")}</strong>
                </DataListCell>,
                <DataListCell width={1} key="name-email-hd">
                  <strong>{t("adminGroupMemberCellNameEmail")}</strong>
                </DataListCell>,
                <DataListCell width={1} key="direct-admin-hd">
                  <strong>{t("directAdmin")}</strong>
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
        {props.groupConfiguration?.admins?.length > 0
          ? props.groupConfiguration.admins.map((admin: any, index: number) => {
              return (
                <DataListItem key={`member-${admin.user.id ?? index}`} aria-labelledby={"member-" + index}>
                  <DataListItemRow>
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell width={1} key={`username-${admin.user.id ?? index}`}>
                          {admin.user.username}
                        </DataListCell>,
                        <DataListCell width={1} key={`name-email-${admin.user.id ?? index}`}>
                          <span className="gm_fullname_datalist pf-c-select__menu-item-main">
                            {admin?.user?.firstName && admin?.user?.lastName
                              ? admin?.user?.firstName +
                                " " +
                                admin?.user?.lastName
                              : t("notAvailable")}
                          </span>
                          <span className="gm_email_datalist pf-c-select__menu-item-description">
                            {admin?.user?.email}
                          </span>
                        </DataListCell>,
                        <DataListCell width={1} key={`direct-admin-${admin.user.id ?? index}`}>
                          <Tooltip
                            content={
                              <div>
                                {admin.direct
                                  ? t("adminGroupIsDirect")
                                  : t("adminGroupIsNotDirect")}
                              </div>
                            }
                          >
                            <Checkbox
                              id="disabled-check-1"
                              className="gm_direct-checkbox"
                              checked={admin.direct ? true : false}
                              isDisabled
                            />
                          </Tooltip>
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
                      {admin.direct ? (
                        <Tooltip
                          content={
                            <div>
                              {admin.user.id === props.user.userId
                                ? t("adminGroupRevokeAdminTooltip")
                                : t("adminGroupRevokeAdminTooltip")}
                            </div>
                          }
                        >
                          <Button
                            variant="danger"
                            className={"gm_x-button-small"}
                            onClick={() => {
                              setModalInfo({
                                title: "Confirmation",
                                accept_message: "YES",
                                cancel_message: "NO",
                                message: t("adminGroupRevokeAdminConfirmation"),
                                accept: function () {
                                  removeAdmin(admin.user.id);
                                  setModalInfo({});
                                },
                                cancel: function () {
                                  setModalInfo({});
                                },
                              });
                            }}
                          >
                            <div className={"gm_x-button"}></div>
                          </Button>
                        </Tooltip>
                      ) : (
                        <div className="gm_cell-placeholder"></div>
                      )}
                    </DataListAction>
                  </DataListItemRow>
                </DataListItem>
              );
            })
          : noAdmins()}
      </DataList>
      <div className="gm_add-admin-container">
        <h1>{t("adminGroupAddNewTitle")}</h1>
        <p>{t("adminGroupAddNewDescription")}</p>

        <div className="gm_add-admin-input">
          <div>
            <Select
              id="group-admin-typeahead-select"
              isOpen={isOpen}
              selected={selected}
              onSelect={onSelect}
              onOpenChange={(open: boolean) => {
                if (!open) closeMenu();
              }}
              toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                <MenuToggle
                  ref={toggleRef}
                  variant="typeahead"
                  aria-label={t("adminGroupSelectUser")}
                  onClick={() => {
                    setIsOpen((v) => !v);
                  }}
                  isExpanded={isOpen}
                  isFullWidth
                >
                  <TextInputGroup isPlain>
                    <TextInputGroupMain
                      value={inputValue}
                      onClick={onInputClick}
                      onChange={(_e, val) => {
                        setInputValue(val);
                        setSelected(null);
                        setSelectedUserId("");
                        setInviteAddress("");
                        setEmailError(false);
                        resetActiveAndFocusedItem();
                        fetchGroupMembers(val);
                        if (!isOpen) setIsOpen(true);
                      }}
                      onKeyDown={onInputKeyDown}
                      id="group-admin-typeahead-input"
                      autoComplete="off"
                      innerRef={undefined as any} // optional; remove if you add a ref like in AddUserStep
                      placeholder={t("adminGroupSelectUser")}
                      {...(activeItemId && {
                        "aria-activedescendant": activeItemId,
                      })}
                      role="combobox"
                      isExpanded={isOpen}
                      aria-controls="group-admin-typeahead-listbox"
                    />
                    <TextInputGroupUtilities
                      {...(!inputValue ? { style: { display: "none" } } : {})}
                    >
                      <Button
                        variant="plain"
                        onClick={() => {
                          setSelected(null);
                          setInputValue("");
                          setInviteAddress("");
                          setSelectedUserId("");
                          setEmailError(false);
                          resetActiveAndFocusedItem();
                          fetchGroupMembers("");
                        }}
                        aria-label="Clear input value"
                      >
                        <TimesIcon aria-hidden />
                      </Button>
                    </TextInputGroupUtilities>
                  </TextInputGroup>
                </MenuToggle>
              )}
              shouldFocusFirstItemOnOpen={false}
            >
              <SelectList id="group-admin-typeahead-listbox">
                {selectOptions.map((option: any, index: number) => (
                  <SelectOption
                    key={String(option.value)}
                    id={createItemId(option.value)}
                    value={option.value}
                    isDisabled={option.isDisabled}
                    isFocused={focusedItemIndex === index}
                    description={option.description}
                  >
                    {option.children}
                  </SelectOption>
                ))}
              </SelectList>
            </Select>

            {/* <Select
              variant={SelectVariant.typeahead}
              typeAheadAriaLabel="Select a state"
              onToggle={onToggle}
              onSelect={() => { }}
              onClear={clearSelection}
              selections={selected}
              createText={Msg.localize('adminGroupInviteTypeahead')}
              onCreateOption={(value) => {
                if (ValidateEmail(value)) {
                  setInviteAddress(value)
                }
                else {
                  setInviteAddress("");
                  setEmailError(true);
                }
                setSelected(value);
                setIsOpen(false);

              }}
              onFilter={(e, searchString) => {
                setInviteAddress("");
                setSelectedUserId("");
                setEmailError(false);
                let filterOptions: any = [];
                fetchGroupMembers(searchString);
                options.forEach((option, index) => (
                  filterOptions.push(
                    <SelectOption
                      isDisabled={option.disabled}
                      key={index}
                      onClick={() => {
                        setInviteAddress("");
                        if (option.id) {
                          setSelectedUserId(option.id);
                          if (option.value === Msg.localize('adminGroupNameNotAvailable')) {
                            setSelected(option.description);
                          }
                          else {
                            setSelected(option.value);
                          }
                        }
                        setIsOpen(false);
                      }}
                      value={option.value + (option.disabled ? ' ' + Msg.localize('adminGroupAlreadyAdmin') : "")}
                      {...(option.description && { description: option.description })}
                    />)
                ));
                return filterOptions;
              }}
              isOpen={isOpen}
              aria-labelledby={titleId}
              isInputValuePersisted={true}
              placeholderText={Msg.localize('adminGroupSelectUser')}
              isCreatable={true}
            >
              {options.map((option, index) => (
                <SelectOption
                  isDisabled={option.disabled}
                  key={index}
                  onClick={() => {
                    setInviteAddress("");
                    if (option.id) {
                      setSelectedUserId(option.id);
                      if (option.value === Msg.localize('adminGroupNameNotAvailable')) {
                        setSelected(option.description);
                      }
                      else {
                        setSelected(option.value);
                      }
                    }
                    setIsOpen(false);
                  }}
                  value={option.value + (option.disabled ? ' ' + Msg.localize('adminGroupAlreadyAdmin') : "")}
                  {...(option.description && { description: option.description })}
                />
              ))}
            </Select> */}
            {emailError ? (
              <FormAlert>
                <Alert
                  variant="danger"
                  title={t("adminGroupInvalidEmail")}
                  aria-live="polite"
                  isInline
                />
              </FormAlert>
            ) : null}
          </div>
          <Tooltip
            content={
              <div>
                {selectedUserId
                  ? t("adminGroupAddTooltip")
                  : emailError
                  ? t("adminGroupEmailErrorTooltip")
                  : inviteAddress
                  ? t("adminGroupSendEmailTooltip")
                  : t("adminGroupAddDescriptionTooltip")}
              </div>
            }
          >
            <Button
              isDisabled={!(selectedUserId || (!emailError && inviteAddress))}
              className={
                "gm_admin-button " +
                (inviteAddress || emailError
                  ? "gm_invitation-button"
                  : "gm_add-admin-button")
              }
              onClick={() => {
                if (selectedUserId) {
                  setModalInfo({
                    title: t("Confirmation"),
                    accept_message: "YES",
                    cancel_message: "NO",
                    message:
                      t("adminGroupAddConfirmation1") +
                      selected +
                      t("adminGroupAddConfirmation2"),
                    accept: function () {
                      makeAdmin(selectedUserId);
                      setModalInfo({});
                    },
                    cancel: function () {
                      setModalInfo({});
                    },
                  });
                }
                if (inviteAddress) {
                  setModalInfo({
                    title: t("Confirmation"),
                    accept_message: "YES",
                    cancel_message: "NO",
                    message:
                      t("adminGroupInviteConfirmation") +
                      " (" +
                      selected +
                      ").",
                    accept: function () {
                      sendInvitation(inviteAddress);
                      setModalInfo({});
                    },
                    cancel: function () {
                      setModalInfo({});
                    },
                  });
                }
              }}
            >
              <div></div>
            </Button>
          </Tooltip>
        </div>
      </div>
    </React.Fragment>
  );
};
