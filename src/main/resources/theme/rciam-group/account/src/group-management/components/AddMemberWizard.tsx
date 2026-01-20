import * as React from "react";
import { useState, useEffect } from "react";
// @ts-ignore
import { ValidateEmail } from "../../js/utils.js";
import {
  Alert,
  Button,
  Checkbox,
  DataList,
  Radio,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  FormAlert,
  Modal,
  ModalVariant,
  Select,
  SelectOption,
  // SelectVariant,
  Wizard,
  WizardStep,
  Popover,
  AlertVariant,
  MenuToggle,
  SelectList,
  TextInputGroup,
  TextInputGroupMain,
  TextInputGroupUtilities,
  SelectOptionProps,
  MenuToggleElement,
} from "@patternfly/react-core";
import {
  HelpIcon,
  ExternalLinkAltIcon,
  TimesIcon,
} from "@patternfly/react-icons";
import { Tooltip } from "@patternfly/react-core";
// import { ContentAlert } from "../../content/ContentAlert";
import { getError } from "../../js/utils.js";
import { useLoader } from "../../widgets/LoaderContext.js";
import { useGroupsService } from "../../groups-service/GroupsServiceContext.js";
import { useTranslation } from "react-i18next";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { HttpResponse } from "../../groups-service/groups-service.js";
import { useLocation } from "react-router-dom";
export const AddMemberWizard: React.FC<any> = (props) => {
  const [isStep1Complete, setIsStep1Complete] = useState<boolean>(false);
  const [isStep2Complete, setIsStep2Complete] = useState<boolean>(false);
  const [adminGroupIds, setAdminGroupIds] = useState<String[]>([]);
  const [selectedRoles, setSelectedRoles] = useState<String[]>([]);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [selectedUser, setSelectedUser] = useState<any>({});
  const [addUserDirectly, setAddUserDirectly] = useState<Boolean>(false);
  const [selectedEnrollment, setSelectedEnrollment] = useState<any>({});
  const [enrollmentConfigurations, setEnrollmentConfigurations] = useState<any>(
    [],
  );
  const [invitationEmail, setInvitationEmail] = useState<String>("");
  const { startLoader, stopLoader } = useLoader();
  const groupsService = useGroupsService();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const location = useLocation();
  useEffect(() => {
    setIsModalOpen(props.active);
  }, [props.active]);

  useEffect(() => {
    fetchAdminGroupIds();
  }, []);

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search.split("?")[1]);
    const activeTab = searchParams.get("tab");
    if (activeTab === "members") {
      fetchGroupEnrollments();
    }
  }, [location.search, location.hash, props.groupId]);

  useEffect(() => {
    setIsStep1Complete(!!(selectedEnrollment?.id && selectedRoles.length > 0));
  }, [selectedRoles, selectedEnrollment]);

  useEffect(() => {
    setIsStep2Complete(
      !!(
        !addUserDirectly &&
        invitationEmail &&
        ValidateEmail(invitationEmail as string)
      ) ||
        !!(
          selectedUser?.id &&
          addUserDirectly &&
          !selectedEnrollment?.aup?.url
        ),
    );
  }, [invitationEmail, selectedUser, addUserDirectly, selectedEnrollment]);

  useEffect(() => {
    if (!isStep1Complete) {
      setSelectedUser({});
      setInvitationEmail("");
      setAddUserDirectly(false);
    }
  }, [isStep1Complete]);

  const onSave = () => {
    if (addUserDirectly) {
      addNewMember();
    } else {
      sendInvitation();
    }
  };

  function getExpirationDate(days: any) {
    const today = new Date(); // Get the current date
    const futureDate = new Date(today);
    futureDate.setDate(today.getDate() + days); // Add the number of days
    // Format the date as YYYY-MM-DD
    const year = futureDate.getFullYear();
    const month = String(futureDate.getMonth() + 1).padStart(2, "0"); // Months are 0-based
    const day = String(futureDate.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  const addNewMember = () => {
    const requestBody: any = {
      user: selectedUser,
      groupEnrollmentConfiguration: selectedEnrollment,
      groupRoles: selectedRoles,
    };
    // Add membershipExpiresAt only if selectedEnrollment.membershipExpirationDays is defined
    if (selectedEnrollment?.membershipExpirationDays) {
      requestBody.membershipExpiresAt = getExpirationDate(
        selectedEnrollment.membershipExpirationDays,
      );
    }
    startLoader();
    closeWizard();
    groupsService!
      .doPost<any>(
        "/group-admin/group/" + props.groupId + "/members",
        requestBody,
      )
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          addAlert(t("addUserSuccess"), AlertVariant.success);
          props.fetchGroupMembers();
        } else {
          addError("addUserError", getError(response));
        }
        stopLoader();
      })
      .catch(() => {
        stopLoader();
        addError("addUserError", t("unexpectedError"));
      });
  };

  const sendInvitation = () => {
    startLoader();
    let requestBody = {
      email: invitationEmail,
      groupEnrollmentConfiguration: {
        id: selectedEnrollment?.id,
      },
      groupRoles: selectedRoles,
      withoutAcceptance: true,
    };
    closeWizard();
    groupsService!
      .doPost<any>(
        "/group-admin/group/" + props.groupId + "/members/invitation",
        requestBody,
      )
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          addAlert(t("userInvitationSuccess"), AlertVariant.success);
          // setGroupMembers(response.data.results);
        } else {
          addError("userInvitationError", getError(response));
        }
      })
      .catch((err) => {
        stopLoader();
        addError("userInvitationError", t("unexpectedError"));
        console.log(err);
      });
  };

  let fetchGroupEnrollments = () => {
    console.log("fetchGroupEnrollments called");
    groupsService!
      .doGet<any>("/group-admin/group/" + props.groupId + "/configuration/all")
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setEnrollmentConfigurations(response.data);
        }
        console.log(response.data);
      });
  };

  let fetchAdminGroupIds = () => {
    groupsService!
      .doGet<any>("/group-admin/groupids/all")
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setAdminGroupIds(response.data);
        }
      })
      .catch((err) => {
        console.log(err);
      });
  };

  const closeWizard = () => {
    // eslint-disable-next-line no-console
    props.setActive(false);
    setSelectedUser({});
    setIsStep1Complete(false);
    setIsStep2Complete(false);
    setInvitationEmail("");
    setAddUserDirectly(false);
    setSelectedEnrollment({});
    setSelectedRoles([]);
  };

  const title = t("invitationSend");

  return (
    <React.Fragment>
      <Modal
        variant={ModalVariant.medium}
        title={t("addMemberGroup") + ' "' + props.groupConfiguration.path + '"'}
        isOpen={isModalOpen}
        onClose={() => {
          closeWizard();
        }}
        actions={[]}
        onEscapePress={() => {
          props.setActive(false);
        }}
      >
        <Wizard
          navAriaLabel={`${title} steps`}
          onClose={closeWizard}
          height={400}
          onSave={onSave}
        >
          <WizardStep
            id="incrementallyEnabled-1"
            name={t("invitationStep1")}
            footer={{ isNextDisabled: !isStep1Complete }}
          >
            {" "}
            <EnrollmentStep
              setIsStep1Complete={setIsStep1Complete}
              setSelectedEnrollment={setSelectedEnrollment}
              selectedEnrollment={selectedEnrollment}
              selectedRoles={selectedRoles}
              setSelectedRoles={setSelectedRoles}
              enrollmentConfigurations={enrollmentConfigurations}
            />
          </WizardStep>
          <WizardStep
            id="incrementallyEnabled-2"
            name={t("addOrInviteMember")}
            isDisabled={!isStep1Complete}
            footer={{ isNextDisabled: !isStep2Complete }} // ✅ disable Finish/Save
          >
            <AddUserStep
              groupId={props.groupId}
              setSelectedUser={setSelectedUser}
              selectedUser={selectedUser}
              setAddUserDirectly={setAddUserDirectly}
              addUserDirectly={addUserDirectly}
              isStep2Complete={isStep2Complete}
              setIsStep2Complete={setIsStep2Complete}
              adminGroupIds={adminGroupIds}
              invitationEmail={invitationEmail}
              selectedEnrollment={selectedEnrollment}
              setInvitationEmail={setInvitationEmail}
            />
          </WizardStep>
        </Wizard>
      </Modal>
    </React.Fragment>
  );
};

const EnrollmentStep: React.FC<any> = (props) => {
  // const toggleRef = useRef<any>(null);
  const [isOpen, setIsOpen] = useState(false);
  const { t } = useTranslation();
  const [selections, setSelections] = useState(
    props.selectedEnrollment?.name ? props.selectedEnrollment.name : "",
  );

  const roleHandler = (role: any) => {
    let roles = [...props.selectedRoles];
    if (roles.includes(role)) {
      const index = roles.indexOf(role);
      if (index > -1) {
        // only splice array when item is found
        roles.splice(index, 1); // 2nd parameter means remove one item only
      }
    } else {
      roles.push(role);
    }
    props.setSelectedRoles([...roles]);
  };

  return (
    <React.Fragment>
      <Select
        isOpen={isOpen}
        onOpenChange={setIsOpen}
        selected={selections} // optional but good
        onSelect={(_event, selection) => {
          const value = String(selection);
          setSelections(value);
          setIsOpen(false);
          const enrollment = props.enrollmentConfigurations.find(
            (e: any) => e.name === value,
          );
          if (enrollment) {
            props.setSelectedRoles([]);
            props.setSelectedEnrollment(enrollment);
          }
        }}
        toggle={(toggleRef) => (
          <MenuToggle
            ref={toggleRef}
            onClick={() => setIsOpen((v) => !v)}
            isExpanded={isOpen}
          >
            {selections || t("invitationEnrollmentSelectPlaceholder")}
          </MenuToggle>
        )}
      >
        <SelectList>
          {props.enrollmentConfigurations?.map((enrollment: any) => (
            <SelectOption
              key={enrollment.id ?? enrollment.name}
              value={enrollment.name}
            >
              {enrollment.name}
            </SelectOption>
          ))}
        </SelectList>
      </Select>
      {props.selectedEnrollment?.id && (
        <React.Fragment>
          <DataList
            aria-label="Compact data list example"
            isCompact
            wrapModifier={"breakWord"}
          >
            <DataListItem aria-labelledby="compact-item1">
              <DataListItemRow>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key="primary content">
                      <span id="compact-item1">
                        <strong>{t("invitationMemberhipDuration")}</strong>
                        <Popover
                          bodyContent={
                            <div>{t("helpTextInvitationExpiration")}</div>
                          }
                        >
                          <button
                            type="button"
                            aria-label="More info for name field"
                            onClick={(e) => e.preventDefault()}
                            aria-describedby="simple-form-name-01"
                            className="pf-c-form__group-label-help gm_popover-info"
                          >
                            <HelpIcon />
                          </button>
                        </Popover>
                      </span>
                    </DataListCell>,
                    <DataListCell width={3} key="membership-duration-secondary">
                      <span>
                        {props.selectedEnrollment?.membershipExpirationDays
                          ? props.selectedEnrollment?.membershipExpirationDays +
                            " " +
                            t("Days")
                          : t("Permanent")}{" "}
                      </span>
                    </DataListCell>,
                  ]}
                />
              </DataListItemRow>
            </DataListItem>
            <DataListItem aria-labelledby="compact-item2">
              <DataListItemRow className="gm_role_row">
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key="primary content">
                      <span id="compact-item1">
                        <strong>{t("invitationRoleSelection")}</strong>
                      </span>
                    </DataListCell>,
                    <DataListCell width={3} key="roles">
                      <table className="gm_roles-table">
                        <tbody>
                          {props.selectedEnrollment &&
                            props.selectedEnrollment?.groupRoles?.map(
                              (role: any, index: number) => {
                                return (
                                  <tr
                                    key={role || index}
                                    onClick={() => {
                                      roleHandler(role);
                                    }}
                                  >
                                    <td>{role}</td>
                                    <td>
                                      <Checkbox
                                        id="standalone-check"
                                        name="standlone-check"
                                        checked={props.selectedRoles.includes(
                                          role,
                                        )}
                                        aria-label="Standalone input"
                                      />
                                    </td>
                                  </tr>
                                );
                              },
                            )}
                        </tbody>
                      </table>
                    </DataListCell>,
                  ]}
                />
              </DataListItemRow>
            </DataListItem>
            {props.selectedEnrollment?.aup?.url && (
              <DataListItem aria-labelledby="aup-item">
                <DataListItemRow>
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell key="primary content">
                        <span id="aup-item">
                          <strong>
                            {t("enrollmentConfigurationAupTitle")}
                          </strong>
                        </span>
                      </DataListCell>,
                      <DataListCell width={3} key="aup-link-secondary">
                        <span>
                          <a
                            href={props.selectedEnrollment?.aup?.url}
                            target="_blank"
                            rel="noreferrer"
                          >
                            link <ExternalLinkAltIcon />{" "}
                          </a>
                        </span>
                      </DataListCell>,
                    ]}
                  />
                </DataListItemRow>
              </DataListItem>
            )}
          </DataList>
        </React.Fragment>
      )}
    </React.Fragment>
  );
};

interface CustomSelectOptionProps extends SelectOptionProps {
  user?: any;
  email?: string;
}

let initialSelectOptions: CustomSelectOptionProps[] = [];
const AddUserStep: React.FC<any> = (props) => {
  const { t } = useTranslation();
  const groupsService = useGroupsService();
  const [isOpen, setIsOpen] = useState(false);
  const [selected, setSelected] = useState<any>(null);
  const [focusedItemIndex, setFocusedItemIndex] = useState<number | null>(null);
  const textInputRef = React.useRef<HTMLInputElement>();
  const [inputValue, setInputValue] = React.useState<string>("");
  const [filterValue, setFilterValue] = React.useState<string>("");
  const [selectOptions, setSelectOptions] =
    React.useState<CustomSelectOptionProps[]>(initialSelectOptions);
  const [activeItemId, setActiveItemId] = React.useState<string | null>(null);
  const [isTouched, setIsTouched] = useState(false);

  const inviteEmail = (props.invitationEmail || "") as string;
  const isInviteMode = !props.addUserDirectly;

  const isInviteEmailValid = inviteEmail !== "" && ValidateEmail(inviteEmail);
  const shouldShowInviteError =
    isInviteMode && isTouched && !isInviteEmailValid;

  useEffect(() => {
    if (props.adminGroupIds.length > 0) {
      fetchGroupMembers();
    }
    setSelected(
      props.selectedUser?.email
        ? props.selectedUser.email
        : props.invitationEmail
          ? props.invitationEmail
          : null,
    );
  }, [props.adminGroupIds]);

  let fetchGroupMembers = async (searchString = "") => {
    groupsService!
      .doGet<any>("/group-admin/groups/members", {
        params: {
          max: 20,
          search: searchString,
          groups: props.adminGroupIds.join(","),
        },
      })
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          let users: any = [];
          response.data.results.forEach((user: any) => {
            users.push({
              value: user.id,
              email: user.email,
              children: user.email,
              user: user,
            });
          });
          if (searchString) {
            users.push({
              children: filterValue,
              value: `Send invite to this email address: "${filterValue}"`,
            });
          }
          setSelectOptions(users);
        }
      })
      .catch((err) => {
        console.log(err);
      });
  };
  let getUserIdentifier = (user: any) => {
    return user?.firstName || user?.lastName
      ? (user.firstName && user.firstName + " ") + user.lastName
      : user?.username
        ? user.username
        : user?.email
          ? user.email
          : user?.id
            ? user.id
            : "Info Not Available";
  };
  const CREATE_REG = /Send invite to this email address:\s*"([^"]+)"/;

  useEffect(() => {
    // let newSelectOptions: SelectOptionProps[] = initialSelectOptions;

    // Filter menu items based on the text input value when one exists
    if (filterValue) {
      props.setSelectedUser({});
      props.setInvitationEmail("");
      fetchGroupMembers(filterValue);
      // Open the menu when the input value changes and the new value is not empty
      if (!isOpen) {
        setIsOpen(true);
      }
    }

    // setSelectOptions(newSelectOptions);
  }, [filterValue]);

  const createItemId = (value: any) =>
    `select-typeahead-${value.replace(" ", "-")}`;

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
    if (!isOpen) {
      setIsOpen(true);
    } else if (!inputValue) {
      closeMenu();
    }
  };

  const selectOption = (value: string | number, content: string | number) => {
    // eslint-disable-next-line no-console
    setInputValue(String(content));
    setFilterValue("");
    setSelected(String(value));
    fetchGroupMembers();
    closeMenu();
  };

  const onSelect = (
    _event: React.MouseEvent<Element, MouseEvent> | undefined,
    value: any,
  ) => {
    if (value) {
      let email = value.match(CREATE_REG)?.[1];
      if (email) {
        props.setInvitationEmail(email);
        props.setSelectedUser({});
        props.setAddUserDirectly(false);
        setIsTouched(true); // ✅ now errors can display
        setSelected(value);
        setIsOpen(false);
        setSelected(filterValue);
        setFilterValue("");
        closeMenu();
        return;
      } else {
        const option = selectOptions.find((option) => option.value === value);
        props.setInvitationEmail("");
        if (option) {
          props.setSelectedUser(option.user);
          props.setInvitationEmail(option.children);
          selectOption(value, option.children as string);
        }
      }
    }
  };

  const onTextInputChange = (
    _event: React.FormEvent<HTMLInputElement>,
    value: string,
  ) => {
    setInputValue(value);
    setFilterValue(value);
    resetActiveAndFocusedItem();

    if (value !== selected) {
      setSelected("");
    }

    if (value === "") {
      props.setInvitationEmail("");
      setIsTouched(true);
      props.setSelectedUser({});
      props.setAddUserDirectly(false);
    }
  };

  const handleMenuArrowKeys = (key: string) => {
    let indexToFocus = 0;

    if (!isOpen) {
      setIsOpen(true);
    }

    if (selectOptions.every((option) => option.isDisabled)) {
      return;
    }

    if (key === "ArrowUp") {
      // When no index is set or at the first index, focus to the last, otherwise decrement focus index
      if (focusedItemIndex === null || focusedItemIndex === 0) {
        indexToFocus = selectOptions.length - 1;
      } else {
        indexToFocus = focusedItemIndex - 1;
      }

      // Skip disabled options
      while (selectOptions[indexToFocus].isDisabled) {
        indexToFocus--;
        if (indexToFocus === -1) {
          indexToFocus = selectOptions.length - 1;
        }
      }
    }

    if (key === "ArrowDown") {
      // When no index is set or at the last index, focus to the first, otherwise increment focus index
      if (
        focusedItemIndex === null ||
        focusedItemIndex === selectOptions.length - 1
      ) {
        indexToFocus = 0;
      } else {
        indexToFocus = focusedItemIndex + 1;
      }

      // Skip disabled options
      while (selectOptions[indexToFocus].isDisabled) {
        indexToFocus++;
        if (indexToFocus === selectOptions.length) {
          indexToFocus = 0;
        }
      }
    }

    setActiveAndFocusedItem(indexToFocus);
  };

  const onInputKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    const focusedItem =
      focusedItemIndex !== null ? selectOptions[focusedItemIndex] : null;

    switch (event.key) {
      case "Enter":
        if (isOpen && focusedItem && !focusedItem.isAriaDisabled) {
          onSelect(undefined, focusedItem.value as string);
        }

        if (!isOpen) {
          setIsOpen(true);
        }

        break;
      case "ArrowUp":
      case "ArrowDown":
        event.preventDefault();
        handleMenuArrowKeys(event.key);
        break;
    }
  };

  const onToggleClick = () => {
    setIsOpen(!isOpen);
    textInputRef?.current?.focus();
  };

  const onClearButtonClick = () => {
    setSelected("");
    setInputValue("");
    setFilterValue("");
    resetActiveAndFocusedItem();

    // ✅ clear parent state so Step 2 becomes incomplete
    props.setInvitationEmail("");
    props.setSelectedUser({});
    props.setAddUserDirectly(false);

    // ✅ mark touched so required error can show (see touched logic below)
    setIsTouched(true);

    textInputRef?.current?.focus();
  };

  const toggle = (toggleRef: React.Ref<MenuToggleElement>) => (
    <MenuToggle
      ref={toggleRef}
      variant="typeahead"
      aria-label="Typeahead creatable menu toggle"
      onClick={onToggleClick}
      isExpanded={isOpen}
      isFullWidth
    >
      <TextInputGroup isPlain>
        <TextInputGroupMain
          value={inputValue}
          onClick={onInputClick}
          onChange={onTextInputChange}
          onKeyDown={onInputKeyDown}
          onBlur={() => setIsTouched(true)} // ✅ touched only after leaving field
          id="create-typeahead-select-input"
          autoComplete="off"
          innerRef={textInputRef}
          placeholder="Select a state"
          {...(activeItemId && { "aria-activedescendant": activeItemId })}
          role="combobox"
          isExpanded={isOpen}
          aria-controls="select-create-typeahead-listbox"
        />
        <TextInputGroupUtilities
          {...(!inputValue ? { style: { display: "none" } } : {})}
        >
          <Button
            variant="plain"
            onClick={onClearButtonClick}
            aria-label="Clear input value"
          >
            <TimesIcon aria-hidden />
          </Button>
        </TextInputGroupUtilities>
      </TextInputGroup>
    </MenuToggle>
  );

  return (
    <React.Fragment>
      <strong>Search for a User or Enter an Email</strong>
      <Select
        id="create-typeahead-select"
        isOpen={isOpen}
        selected={selected}
        onSelect={onSelect}
        onOpenChange={(isOpen: boolean) => {
          !isOpen && closeMenu();
        }}
        toggle={toggle}
        shouldFocusFirstItemOnOpen={false}
      >
        <SelectList id="select-create-typeahead-listbox">
          {selectOptions.map((option: any, index: number) => {
            return (
              <SelectOption
                key={option.value || option.children}
                isFocused={focusedItemIndex === index}
                className={option.className}
                id={createItemId(option.value)}
                {...option}
                ref={null}
                description={option.email}
              >
                {option.user ? getUserIdentifier(option.user) : option.value}
              </SelectOption>
            );
          })}
        </SelectList>
      </Select>
      {shouldShowInviteError ? (
        <FormAlert>
          <Alert
            variant="danger"
            title={
              !inviteEmail
                ? t("invitationEmailRequired")
                : t("invitationEmailError")
            }
            aria-live="polite"
            isInline
          />
        </FormAlert>
      ) : null}
      <div className="gm_add-user-action-container">
        <strong>Choose Action</strong>
        <div className="gm_add-user-radio-container">
          <Radio
            isChecked={!props.addUserDirectly}
            name="radio-1"
            onClick={() => {
              props.setAddUserDirectly(false);
            }}
            label={t("invitationSend")}
            id="radio-invitation"
          ></Radio>
          {Object.keys(props.selectedUser).length === 0 ||
          props.selectedEnrollment?.aup?.url ? (
            <Tooltip
              distance={5}
              position="top-start"
              trigger={
                Object.keys(props.selectedUser).length === 0 ||
                props.selectedEnrollment?.aup?.url
                  ? "mouseenter"
                  : "manual"
              }
              isVisible={false}
              content={
                <div>
                  {t(
                    props.selectedEnrollment?.aup?.url
                      ? "addUserDisabledRadioTooltipAUP"
                      : "addUserDisabledRadioTooltipUser",
                  )}
                </div>
              }
            >
              <Radio
                isChecked={props.addUserDirectly}
                name="radio-1"
                isDisabled={
                  Object.keys(props.selectedUser).length === 0 ||
                  props.selectedEnrollment?.aup?.url
                }
                onClick={() => {
                  props.setAddUserDirectly(true);
                }}
                label={t("addMemberDirectly")}
                id="radio-direct-add"
              ></Radio>
            </Tooltip>
          ) : (
            <Radio
              isChecked={props.addUserDirectly}
              name="radio-1"
              isDisabled={
                Object.keys(props.selectedUser).length === 0 ||
                props.selectedEnrollment?.aup?.url
              }
              onClick={() => {
                props.setAddUserDirectly(true);
              }}
              label={t("addMemberDirectly")}
              id="radio-direct-add"
            ></Radio>
          )}
        </div>
      </div>
    </React.Fragment>
  );
};
