import * as React from "react";
import { useState, useEffect } from "react";
import {
  Button,
  Tooltip,
  ModalVariant,
  Modal,
  Form,
  FormGroup,
  Popover,
  DatePicker,
  Switch,
  AlertVariant,
} from "@patternfly/react-core";
import {
  isPastDate,
  dateParse,
  addDays,
  isFirstDateBeforeSecond,
  dateFormat,
} from "../../widgets/Date";
import { HelpIcon } from "@patternfly/react-icons";
import { useGroupsService } from "../../groups-service/GroupsServiceContext";
import { useTranslation } from "react-i18next";
import { HttpResponse } from "../../groups-service/groups-service";
import { getError } from "../../js/utils";
import { useLoader } from "../../widgets/LoaderContext";
import { FormErrorText, useAlerts } from "@keycloak/keycloak-ui-shared";
import { GroupRolesTable } from "../../widgets/GroupRolesTable";

interface EditMembershipModalProps {
  membership: Membership;
  setMembership: any;
  fetchGroupMembers: any;
}

interface Membership {
  groupRoles: string[];
  validFrom: string;
  membershipExpiresAt?: string;
  id?: any;
  status?: string;
  user?: any;
  group?: any;
}

export const EditMembershipModal: React.FC<EditMembershipModalProps> = (
  props
) => {
  const touchDefault = {
    groupRoles: false,
    validFrom: false,
    membershipExpiresAt: false,
  };
  const groupsService = useGroupsService();
  const { t } = useTranslation();
  const [errors, setErrors] = useState<any>({});
  const { startLoader, stopLoader } = useLoader();
  const [membership, setMembership] = useState<Membership>({
    validFrom: "",
    membershipExpiresAt: "",
    groupRoles: [],
  });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [touched, setTouched] = useState<any>(touchDefault);
  const [enrollmentRules, setEnrollmentRules] = useState<any>({});
  const [groupConfiguration, setGroupConfiguration] = useState<any>({});
  const { addAlert, addError } = useAlerts();

  useEffect(() => {
    setIsModalOpen(Object.keys(props.membership).length > 0);
    if (Object.keys(props.membership).length > 0) {
      setMembership({
        validFrom: props.membership.validFrom,
        membershipExpiresAt: props.membership.membershipExpiresAt,
        groupRoles: [...props.membership.groupRoles],
      });
      fetchGroupEnrollmentRules();
      fetchGroupConfiguration();
    }
  }, [props.membership]);

  let fetchGroupConfiguration = () => {
    groupsService!
      .doGet<any>("/group-admin/group/" + props.membership.group.id + "/all")
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          setGroupConfiguration(response.data);
        }
      });
  };

  let fetchGroupEnrollmentRules = () => {
    groupsService!
      .doGet<any>("/group-admin/configuration-rules", {
        params: {
          type:
            "/" + props.membership.group?.name !== props.membership.group?.path
              ? "SUBGROUP"
              : "TOP_LEVEL",
        },
      })
      .then((response: HttpResponse<any>) => {
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

  useEffect(() => {
    validateMembership();
  }, [membership]);

  const validateValidFrom = (date: Date): string => {
    if (dateFormat(date) !== props.membership.validFrom) {
      const selectedDateWithoutTime = new Date(
        date.getFullYear(),
        date.getMonth(),
        date.getDate()
      );
      if (
        props.membership["validFrom"] !== dateFormat(selectedDateWithoutTime)
      ) {
        let pastDateError = isPastDate(date);
        if (pastDateError) {
          return pastDateError;
        }
      }
      // Now check if membership.membershipExpiresAt exists, and run the comparison
      if (membership.membershipExpiresAt) {
        let expirationError = isFirstDateBeforeSecond(
          dateParse(membership.membershipExpiresAt),
          date,
          t("validateFromMembershipError1")
        );
        return expirationError || "";
      }
    }
    return "";
  };

  const validateMembershipExpiresAt = (date: Date | null): string => {
    if (date) {
      const selectedDateWithoutTime = new Date(
        date.getFullYear(),
        date.getMonth(),
        date.getDate()
      );
      if (
        props.membership["membershipExpiresAt"] !==
        dateFormat(selectedDateWithoutTime)
      ) {
        let pastDateError = isPastDate(date);
        if (pastDateError) {
          return t(pastDateError);
        }
      }

      // Now check if membership.membershipExpiresAt exists, and run the comparison
      if (membership.validFrom) {
        let expirationError = isFirstDateBeforeSecond(
          date,
          dateParse(membership.validFrom),
          "You cannot set the membership expiration date before the date the membership starts."
        );
        if (expirationError) {
          return expirationError;
        }
      }
      if (enrollmentRules?.membershipExpirationDays?.max) {
        const currentDate = new Date();
        // Normalize both dates to remove the time part for an accurate comparison
        const currentDateWithoutTime = new Date(
          currentDate.getFullYear(),
          currentDate.getMonth(),
          currentDate.getDate()
        );
        const rulesValidationError = isFirstDateBeforeSecond(
          isPastDate(dateParse(membership.validFrom))
            ? addDays(
                currentDateWithoutTime,
                parseInt(enrollmentRules.membershipExpirationDays.max)
              )
            : addDays(
                dateParse(membership.validFrom),
                parseInt(enrollmentRules.membershipExpirationDays.max)
              ),
          date,
          t("validateMembershipExpiresErrorMax", {
            param_0: enrollmentRules.membershipExpirationDays.max,
          })
        );
        if (rulesValidationError) {
          return rulesValidationError;
        }
      }
    } else {
      if (
        enrollmentRules?.membershipExpirationDays?.required ||
        enrollmentRules?.membershipExpirationDays?.max
      ) {
        return t("validateMembershipExpiratErrorRequired");
      }
    }
    return "";
  };

  const validatorValidFrom: ((date: Date) => string)[] = [validateValidFrom];
  const validatorMembershipExpiresAt: ((date: Date) => string)[] = [
    validateMembershipExpiresAt,
  ];

  const submit = () => {
    touchFields();
    if (Object.keys(errors).length === 0) {
      updateMembership();
    } else {
      addError("enrollmentConfigurationModalSubmitError", getError("Please fix the errors in the form."));
    }
  };

  const handleModalToggle = () => {
    props?.setMembership({});
  };

  let updateMembership = () => {
    setIsModalOpen(false);
    startLoader();
    groupsService!
      .doPut<any>(
        "/group-admin/group/" +
          props.membership.group.id +
          "/member/" +
          props.membership?.id,
        { ...membership }
      )
      .then((response: HttpResponse<any>) => {
        // Fetch updated group members after the request
        props.fetchGroupMembers();
        // Stop the loader
        stopLoader();
        // Show success or error alert based on the response
        if (response.status === 200 || response.status === 204) {
          addAlert(t("updateMembershipSuccess"), AlertVariant.success);
        } else {
          addError("updateMembershipError", getError(response));
        }
        // Clear the membership only after the request is completed
        props?.setMembership({});
      })
      .catch((error) => {
        // Handle errors
        stopLoader();
        addError("updateMembershipError", getError(error));
        // Clear the membership even if there is an error
        props?.setMembership({});
      });
  };

  const validateMembership = () => {
    let errors: Record<string, string> = {};
    let validFromError = validateValidFrom(dateParse(membership.validFrom));
    let membershipExpiresAtError = validateMembershipExpiresAt(
      membership.membershipExpiresAt
        ? dateParse(membership.membershipExpiresAt)
        : null
    );
    !(membership?.groupRoles?.length > 0) &&
      (errors.groupRoles = t("groupRolesFormError"));
    validFromError && (errors.validFrom = validFromError);
    membershipExpiresAtError &&
      (errors.membershipExpiresAt = membershipExpiresAtError);
    setErrors(errors);
  };

  const touchFields = () => {
    for (const property in touched) {
      touched[property] = true;
    }
    setTouched({ ...touched });
  };

  return (
    <React.Fragment>
      <Modal
        variant={ModalVariant.medium}
        title={t("adminGroupEditMembeship")}
        isOpen={isModalOpen}
        onClose={handleModalToggle}
        actions={[
          <Button
            key="save"
            variant="primary"
            onClick={() => {
              submit();
            }}
          >
            Save
          </Button>,
          <Button
            key="cancel"
            variant="link"
            onClick={() => {
              props?.setMembership({});
            }}
          >
            Cancel
          </Button>,
        ]}
      >
        <Form>
          <FormGroup label={t("groupPath") + ":"} fieldId="simple-form-name-01">
            <div>
              {props.membership?.group?.path
                ? props.membership.group.path
                : "Not Available"}
            </div>
          </FormGroup>
          <FormGroup
            label={t("enrollmentFullNameLabel")}
            fieldId="simple-form-name-01"
          >
            <div>
              {props.membership?.user?.firstName ||
              props.membership?.user?.lastName
                ? props.membership.user.firstName +
                  " " +
                  props.membership.user.lastName
                : "Not Available"}
            </div>
          </FormGroup>
          <FormGroup
            label={t("enrollmentEmailLabel")}
            fieldId="simple-form-name-02"
          >
            <div>
              {props.membership?.user?.email
                ? props.membership.user.email
                : t("notAvailable")}
            </div>
          </FormGroup>
          <FormGroup label={"Username:"} fieldId="simple-form-name-02">
            <div>
              {props.membership?.user?.username
                ? props.membership.user.username
                : t("notAvailable")}
            </div>
          </FormGroup>
          <FormGroup
            label={t("groupDatalistCellRoles")}
            isRequired
            fieldId="simple-form-name-01"
            onBlur={() => {
              touched.groupRoles = true;
              setTouched({ ...touched });
            }}
          >
            <GroupRolesTable
              groupRoles={
                groupConfiguration.groupRoles
                  ? Object.keys(groupConfiguration.groupRoles)
                  : []
              }
              selectedRoles={membership.groupRoles}
              setSelectedRoles={(roles: string[]) => {
                membership.groupRoles = roles;
                setMembership({ ...membership });
              }}
            />
            {errors.groupRoles && touched.groupRoles && (
              <FormErrorText message={t("required")} />
            )}
          </FormGroup>
          <FormGroup
            label={t("memberSince")}
            isRequired
            fieldId="simple-form-name-01"
            onBlur={() => {
              touched.validFrom = true;
              setTouched({ ...touched });
            }}
          >
            <DatePicker
              isDisabled={props.membership.status === "ENABLED"}
              value={membership?.validFrom}
              placeholder="DD-MM-YYYY"
              dateFormat={dateFormat}
              dateParse={dateParse}
              validators={validatorValidFrom}
              onChange={(value: any) => {
                if (membership?.validFrom) {
                  membership.validFrom = value;
                  setMembership({ ...membership });
                }
              }}
            />
            {errors.validFrom && touched.validFrom && (
              <FormErrorText message={t("required")} />
            )}
          </FormGroup>
          <FormGroup
            label={t("groupDatalistCellMembershipExp")}
            isRequired={!!enrollmentRules?.membershipExpirationDays?.required}
            fieldId="simple-form-name-01"
            onBlur={() => {
              touched.membershipExpiresAt = true;
              setTouched({ ...touched });
            }}
            labelIcon={
              <Popover
                bodyContent={
                  <div>
                    {t("enrollmentConfigurationTooltipExpirationDate")} .
                  </div>
                }
              >
                <button
                  type="button"
                  aria-label="More info for name field"
                  onClick={(e) => e.preventDefault()}
                  aria-describedby="simple-form-name-01"
                  className="pf-c-form__group-label-help"
                >
                  <HelpIcon />
                </button>
              </Popover>
            }
          >
            <div className="gm_switch_container">
              <Tooltip
                {...(!enrollmentRules?.membershipExpirationDays?.required
                  ? { trigger: "manual", isVisible: false }
                  : { trigger: "mouseenter" })}
                content={
                  <div>
                    {t(
                      "enrollmentConfigurationExpirationDateSwitchDisabledTooltip"
                    )}
                  </div>
                }
              >
                <Switch
                  aria-label="simple-switch-membershipExpirationDays"
                  isChecked={!!membership?.membershipExpiresAt}
                  onChange={(_event: any) => {
                    if (membership.membershipExpiresAt) {
                      delete membership.membershipExpiresAt;
                      setMembership({ ...membership });
                    } else {
                      membership.membershipExpiresAt = dateFormat(new Date());
                      setMembership({ ...membership });
                    }
                  }}
                />
              </Tooltip>
            </div>
            {membership?.membershipExpiresAt && (
              <DatePicker
                value={membership?.membershipExpiresAt}
                placeholder="DD-MM-YYYY"
                dateFormat={dateFormat}
                dateParse={dateParse}
                validators={validatorMembershipExpiresAt}
                onChange={(_value: any, date: any) => {
                  if (membership?.membershipExpiresAt) {
                    membership.membershipExpiresAt = date;
                    setMembership({ ...membership });
                  }
                }}
              />
            )}
          </FormGroup>
        </Form>
      </Modal>
    </React.Fragment>
  );
};
