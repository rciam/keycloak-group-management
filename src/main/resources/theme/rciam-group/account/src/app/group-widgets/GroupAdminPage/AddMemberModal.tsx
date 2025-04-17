import * as React from "react";
import { useState, useEffect, useRef } from "react";
// @ts-ignore
import {
  HttpResponse,
  GroupsServiceClient,
} from "../../groups-mngnt-service/groups.service";
// @ts-ignore
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '
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
  SelectVariant,
  Spinner,
  Wizard,
  WizardStep,
  Popover,
  TextArea,
} from "@patternfly/react-core";
import { Msg } from "../../widgets/Msg";
import { HelpIcon, ExternalLinkAltIcon } from "@patternfly/react-icons";
import { Tooltip } from "@patternfly/react-core";
import { useLoader } from "../LoaderContext";
import { ContentAlert } from "../../content/ContentAlert";
import { getError } from "../../js/utils";

export const AddMemberModal: React.FC<any> = (props) => {
  let groupsService = new GroupsServiceClient();
  const [stepIdReached, setStepIdReached] = useState(1);
  const [isStep1Complete, setIsStep1Complete] = useState<boolean>(false);
  const [isStep2Complete, setIsStep2Complete] = useState<boolean>(false);
  const [adminGroupIds, setAdminGroupIds] = useState<String[]>([]);
  const [selectedRoles, setSelectedRoles] = useState<String[]>([]);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [selectedUser, setSelectedUser] = useState<any>({});
  const [addUserDirectly, setAddUserDirectly] = useState<Boolean>(false);
  const [selectedEnrollment, setSelectedEnrollment] = useState<any>({});
  const [enrollmentConfigurations, setEnrollmentConfigurations] = useState<any>(
    []
  );
  const [invitationEmail, setInvitationEmail] = useState<String>("");
  const { startLoader, stopLoader } = useLoader();

  useEffect(() => {
    setIsModalOpen(props.active);
  }, [props.active]);

  useEffect(() => {
    fetchAdminGroupIds();
  }, []);

  useEffect(() => {
    const searchParams = new URLSearchParams(location.hash.split("?")[1]);
    const activeTab = searchParams.get("tab");
    if (activeTab === "members") {
      fetchGroupEnrollments();
    }
  }, [location.hash]);

  useEffect(() => {
    setIsStep1Complete(!!(selectedEnrollment?.id && selectedRoles.length > 0));
  }, [selectedRoles, selectedEnrollment]);

  useEffect(() => {
    setIsStep2Complete(
      !!(
        !addUserDirectly &&
        invitationEmail &&
        ValidateEmail(invitationEmail)
      ) ||
        !!(selectedUser?.id && addUserDirectly && !selectedEnrollment?.aup?.url)
    );
  }, [invitationEmail, selectedUser, addUserDirectly, selectedEnrollment]);

  useEffect(() => {
    if (!isStep1Complete) {
      setSelectedUser({});
      setInvitationEmail("");
      setAddUserDirectly(false);
    }
  }, [isStep1Complete]);

  const onNext = ({ id }: WizardStep) => {
    if (id) {
      if (typeof id === "string") {
        const [, orderIndex] = id.split("-");
        id = parseInt(orderIndex);
      }
      setStepIdReached(stepIdReached < id ? id : stepIdReached);
    }
  };

  const onSave = () => {
    if (addUserDirectly) {
      addNewMember();
    } else {
      sendInvitation();
    }
  };

  function getExpirationDate(days) {
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
        selectedEnrollment.membershipExpirationDays
      );
    }
    startLoader();
    closeWizard();
    groupsService!
      .doPost<any>(
        "/group-admin/group/" + props.groupId + "/members",
        requestBody
      )
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          ContentAlert.success(Msg.localize("addUserSuccess"));
          props.fetchGroupMembers();
          // setGroupMembers(response.data.results);
        } else {
          ContentAlert.danger(Msg.localize("addUserError"), [
            getError(response),
          ]);
        }
        stopLoader();
      })
      .catch((err) => {
        stopLoader();
        ContentAlert.danger(Msg.localize("addUserError"), [
          Msg.localize("unexpectedError"),
        ]);
      });
  };

  const sendInvitation = () => {
    startLoader();
    let requestBody= {
      email: invitationEmail,
      groupEnrollmentConfiguration: {
        id: selectedEnrollment?.id,
      },
      groupRoles: selectedRoles,
      withoutAcceptance: true,
    }
    closeWizard();
    groupsService!
      .doPost<any>(
        "/group-admin/group/" + props.groupId + "/members/invitation",
        requestBody
      )
      .then((response: HttpResponse<any>) => {
        stopLoader();
        if (response.status === 200 || response.status === 204) {
          ContentAlert.success(Msg.localize("userInvitationSuccess"));
          // setGroupMembers(response.data.results);
        } else {
          ContentAlert.danger(Msg.localize("userInvitationError"), [
            getError(response),
          ]);
        }
      })
      .catch((err) => {
        stopLoader();
        ContentAlert.danger(Msg.localize("userInvitationError"), [
          Msg.localize("unexpectedError"),
        ]);
        console.log(err);
      });
  };

  let fetchGroupEnrollments = () => {
    groupsService!
      .doGet<any>("/group-admin/group/" + props.groupId + "/configuration/all")
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setEnrollmentConfigurations(response.data);
        }
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

  let steps = [
    {
      id: "incrementallyEnabled-1",
      name: Msg.localize("invitationStep1"),
      component: (
        <EnrollmentStep
          setIsStep1Complete={setIsStep1Complete}
          setSelectedEnrollment={setSelectedEnrollment}
          setStepIdReached={setStepIdReached}
          selectedEnrollment={selectedEnrollment}
          selectedRoles={selectedRoles}
          setSelectedRoles={setSelectedRoles}
          enrollmentConfigurations={enrollmentConfigurations}
        />
      ),
      enableNext: isStep1Complete,
    },
    {
      id: "incrementallyEnabled-2",
      name: Msg.localize("addOrInviteMember"),
      component: (
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
      ),
      enableNext: isStep2Complete,
      nextButtonText: Msg.localize("confirm"),
      canJumpTo: stepIdReached >= 2,
    },
  ];

  const title = Msg.localize("invitationSend");

  return (
    <React.Fragment>
      <Modal
        variant={ModalVariant.medium}
        title={
          Msg.localize("addMemberGroup") +
          ' "' +
          props.groupConfiguration.path +
          '"'
        }
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
          mainAriaLabel={`${title} content`}
          onClose={closeWizard}
          steps={steps}
          onNext={onNext}
          height={400}
          onSave={onSave}
        />
      </Modal>
    </React.Fragment>
  );
};

const EnrollmentStep: React.FC<any> = (props) => {
  // const toggleRef = useRef<any>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [selections, setSelections] = useState(
    props.selectedEnrollment?.name ? props.selectedEnrollment.name : ""
  );

  const onToggle = (isOpen) => {
    setIsOpen(isOpen);
  };

  const clearSelection = () => {
    setSelections("");
    props.setIsStep1Complete(false);
    props.setSelectedEnrollment({});
    props.setStepIdReached(1);
    setIsOpen(false);
  };

  const onSelect = (event, selection, isPlaceholder) => {
    if (isPlaceholder) clearSelection();
    else {
      setSelections(selection);
      setIsOpen(false);
      // toggleRef.current.focus();
    }
  };

  const roleHandler = (role) => {
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
        variant={SelectVariant.single}
        aria-label="Select Input"
        onToggle={onToggle}
        onSelect={onSelect}
        selections={selections}
        isOpen={isOpen}
      >
        <SelectOption
          key="placeholder"
          value={Msg.localize("invitationEnrollmentSelectPlaceholder")}
          isPlaceholder
        />
        {props.enrollmentConfigurations
          ? props.enrollmentConfigurations.map((enrollment, index) => {
              return (
                <SelectOption
                  key={index}
                  value={enrollment?.name}
                  isDisabled={!enrollment.active}
                  onClick={() => {
                    props.setSelectedRoles([]);
                    props.setSelectedEnrollment(enrollment);
                  }}
                />
              );
            })
          : []}
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
                        <strong>
                          <Msg msgKey="invitationMemberhipDuration" />
                        </strong>
                        <Popover
                          bodyContent={
                            <div>
                              <Msg msgKey="helpTextInvitationExpiration" />
                            </div>
                          }
                        >
                          <button
                            type="button"
                            aria-label="More info for name field"
                            onClick={(e) => e.preventDefault()}
                            aria-describedby="simple-form-name-01"
                            className="pf-c-form__group-label-help gm_popover-info"
                          >
                            <HelpIcon noVerticalAlign />
                          </button>
                        </Popover>
                      </span>
                    </DataListCell>,
                    <DataListCell width={3} key="secondary content ">
                      <span>
                        {props.selectedEnrollment?.membershipExpirationDays
                          ? props.selectedEnrollment?.membershipExpirationDays +
                            " " +
                            Msg.localize("Days")
                          : Msg.localize("Permanent")}{" "}
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
                        <strong>
                          <Msg msgKey="invitationRoleSelection" />
                        </strong>
                      </span>
                    </DataListCell>,
                    <DataListCell width={3} key="roles">
                      <table className="gm_roles-table">
                        <tbody>
                          {props.selectedEnrollment &&
                            props.selectedEnrollment?.groupRoles?.map(
                              (role, index) => {
                                return (
                                  <tr
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
                                          role
                                        )}
                                        aria-label="Standalone input"
                                      />
                                    </td>
                                  </tr>
                                );
                              }
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
                            <Msg msgKey="enrollmentConfigurationAupTitle" />
                          </strong>
                        </span>
                      </DataListCell>,
                      <DataListCell width={3} key="secondary content ">
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

const AddUserStep: React.FC<any> = (props) => {
  let groupsService = new GroupsServiceClient();
  const [emailError, setEmailError] = useState(true);
  const [showEmailError, setShowEmailError] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [selected, setSelected] = useState<any>(null);
  const [options, setOptions] = useState<any>([]);
  const [isFirstLoad, setIsFirstLoad] = useState(true);

  useEffect(() => {
    if (props.adminGroupIds.length > 0) {
      fetchGroupMembers();
    }
    setSelected(
      props.selectedUser?.email
        ? props.selectedUser.email
        : props.invitationEmail
        ? props.invitationEmail
        : null
    );
  }, [props.adminGroupIds]);

  const clearSelection = () => {
    props.setInvitationEmail("");
    props.setSelectedUser({});
    props.setAddUserDirectly(false);
    setSelected(null);
    setIsOpen(false);
    setEmailError(false);
    fetchGroupMembers();
  };

  const onToggle = (open) => {
    setIsOpen(open);
  };

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
          response.data.results.forEach((user) => {
            users.push({
              value: getUserIdentifier(user),
              description: user.email,
              id: user.id,
              user: user,
            });
          });
          setOptions(users);
        }
      })
      .catch((err) => {
        console.log(err);
      });
  };

  let getUserIdentifier = (user) => {
    return user.firstName || user.lastName
      ? (user.firstName && user.firstName + " ") + user.lastName
      : user.username
      ? user.username
      : user.email
      ? user.email
      : user.id
      ? user.id
      : "Info Not Available";
  };

  return (
    <React.Fragment>
      <strong>Search for a User or Enter an Email</strong>
      <div className="gm_invitation-email-input">
        <Select
          variant={SelectVariant.typeahead}
          typeAheadAriaLabel="Select a state"
          onToggle={onToggle}
          onSelect={() => {}}
          onClear={clearSelection}
          selections={selected}
          createText={Msg.localize("invitationEmailInputTypeahead")}
          onCreateOption={(value) => {
            props.setInvitationEmail(value);
            setEmailError(!ValidateEmail(value));
            setShowEmailError(true);
            props.setSelectedUser({});
            props.setAddUserDirectly(false);
            setSelected(value);
            setIsOpen(false);
          }}
          onFilter={(e, searchString) => {
            if (!isFirstLoad) {
              props.setSelectedUser({});
              props.setInvitationEmail("");
              setEmailError(false);
              setShowEmailError(false);
              let filterOptions: any = [];
              fetchGroupMembers(searchString);
              options.forEach((option, index) =>
                filterOptions.push(
                  <SelectOption
                    isDisabled={option.disabled}
                    key={index}
                    value={option.value}
                    onClick={() => {
                      props.setInvitationEmail("");
                      if (option.description) {
                        setSelected(option.description);
                        props.setSelectedUser(option.user);
                        props.setInvitationEmail(option.description);
                      }
                      setIsOpen(false);
                    }}
                    {...(option.description && {
                      description: option.description,
                    })}
                  />
                )
              );
              return filterOptions;
            }
            setIsFirstLoad(false);
          }}
          isOpen={isOpen}
          aria-labelledby={"titleId"}
          isInputValuePersisted={true}
          placeholderText="Start typing a user's name or email address"
          isCreatable={true}
        >
          {options.map((option, index) => (
            <SelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              onClick={() => {
                props.setInvitationEmail("");
                if (option.description) {
                  setSelected(option.description);
                  props.setSelectedUser(option.user);
                  props.setInvitationEmail(option.description);
                }
                setIsOpen(false);
              }}
              {...(option.description && { description: option.description })}
            />
          ))}
        </Select>
        {emailError && showEmailError ? (
          <FormAlert>
            <Alert
              variant="danger"
              title={
                !props.invitationEmail
                  ? Msg.localize("invitationEmailRequired")
                  : Msg.localize("invitationEmailError")
              }
              aria-live="polite"
              isInline
            />
          </FormAlert>
        ) : null}
      </div>
      <div className="gm_add-user-action-container">
        <strong>Choose Action</strong>
        <div className="gm_add-user-radio-container">
          <Radio
            isChecked={!props.addUserDirectly}
            name="radio-1"
            onClick={() => {
              props.setAddUserDirectly(false);
            }}
            label={Msg.localize("invitationSend")}
            id="radio-invitation"
          ></Radio>
          {Object.keys(props.selectedUser).length === 0 ||
              props.selectedEnrollment?.aup?.url?
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
                  <Msg
                    msgKey={
                      props.selectedEnrollment?.aup?.url
                        ? "addUserDisabledRadioTooltipAUP"
                        : "addUserDisabledRadioTooltipUser"
                    }
                  />
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
                label={Msg.localize("addMemberDirectly")}
                id="radio-direct-add"
              ></Radio>
            </Tooltip>:
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
              label={Msg.localize("addMemberDirectly")}
              id="radio-direct-add"
            ></Radio>
            }
          
        </div>
      </div>
    </React.Fragment>
  );
};


