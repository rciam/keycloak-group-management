import * as React from "react";
import { FC, useState, useEffect } from "react";
import {
  Button,
  Tooltip,
  Checkbox,
  Form,
  FormGroup,
  TextInput,
  Modal,
  ModalVariant,
  Switch,
  FormFieldGroupHeader,
  FormFieldGroup,
  DatePicker,
  Popover,
  NumberInput,
  HelperTextItem,
  AlertVariant,
  FormAlert,
  Alert,
} from "@patternfly/react-core";
import { HttpResponse } from "../../groups-service/groups-service";
import { useGroupsService } from "../../groups-service/GroupsServiceContext";
import { ConfirmationModal } from "../../widgets/Modals";
import { isIntegerOrNumericString, getError } from "../../js/utils";
import { useTranslation } from "react-i18next";
import { HelpIcon, TrashIcon } from "@patternfly/react-icons";
import { useLoader } from "../../widgets/LoaderContext";
import { FormErrorText, useAlerts } from "@keycloak/keycloak-ui-shared";

const reg_url = /^(https?|chrome):\/\/[^\s$.?#].[^\s]*$/;

export const EnrollmentModal: FC<any> = (props) => {
  const getCurrentDate = () => {
    const now = new Date();
    return `${now.getFullYear().toString().padStart(4, "0")}-${(
      now.getMonth() + 1
    )
      .toString()
      .padStart(2, "0")}-${now.getDate().toString().padStart(2, "0")}`;
  };
  let currentDate = getCurrentDate();

  const touchDefault = {
    name: false,
    groupRoles: false,
    aup_url: false,
    membershipExpirationDays: false,
    validFrom: false,
    commentsLabel: false,
    commentsDescription: false,
  };

  const dateFormat = (date: Date) =>
    date
      .toLocaleDateString("en-CA", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
      })
      .replace(/\//g, "-");

  const dateParse = (date: string) => {
    const split = date.split("-");
    if (split.length !== 3) {
      return new Date();
    }
    const month = split[1];
    const day = split[2];
    const year = split[0];
    return new Date(
      `${year.padStart(4, "0")}-${month.padStart(2, "0")}-${day.padStart(
        2,
        "0"
      )}T00:00:00`
    );
  };

  const groupsService = useGroupsService();
  const [modalInfo, setModalInfo] = useState({});
  const [enrollment, setEnrollment] = useState<any>({});
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [errors, setErrors] = useState<any>({});
  const [touched, setTouched] = useState<any>(touchDefault);
  const { startLoader, stopLoader } = useLoader();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  useEffect(() => {
    const hasEnrollment =
      props.enrollment && Object.keys(props.enrollment).length !== 0;
    setIsModalOpen(!!hasEnrollment);

    setEnrollment({
      ...(props.enrollment || {}),
      groupRoles: props.enrollment?.groupRoles
        ? [...props.enrollment.groupRoles]
        : [],
    });
  }, [props.enrollment]);

  useEffect(() => {
    if (Object.keys(enrollment).length !== 0) {
      validateEnrollment();
    }
  }, [enrollment]);

  const validateEnrollment = () => {
    let errors: Record<string, string> = {};
    !(enrollment?.name?.length > 0) && (errors.name = t("requredFormError"));
    enrollment?.aup?.url?.length > 0 &&
      !reg_url.test(enrollment.aup.url) &&
      (errors.aup_url = t("invalidUrlFormError"));
    !(enrollment?.aup?.url?.length > 0) &&
      props.validationRules?.aupEntity?.required === true &&
      (errors.aup_url = t("requredFormError"));
    !(enrollment?.groupRoles?.length > 0) &&
      (errors.groupRoles = t("groupRolesFormError"));
    enrollment?.membershipExpirationDays === 0 &&
      props.validationRules?.membershipExpirationDays?.max &&
      (errors.membershipExpirationDays = t("fieldMaxZeroFormError"));
    enrollment?.membershipExpirationDays &&
      !(enrollment?.membershipExpirationDays > 0) &&
      (errors.membershipExpirationDays = t("expirationDaysPositiveFormError"));
    typeof enrollment?.membershipExpirationDays !== "number" &&
      (errors.membershipExpirationDays = t("expirationDaysNumberFormError"));
    enrollment?.commentsNeeded &&
      (!enrollment?.commentsLabel || enrollment?.commentsLabel.length < 1) &&
      (errors.commentsLabel = t("requredFormError"));
    enrollment?.commentsNeeded &&
      (!enrollment?.commentsDescription ||
        enrollment?.commentsDescription.length < 1) &&
      (errors.commentsDescription = t("requredFormError"));
    if (enrollment?.validFrom) {
      let parsedDate = dateParse(enrollment?.validFrom);
      if (parsedDate instanceof Date && isFinite(parsedDate.getTime())) {
        isPastDate(parsedDate) &&
          props.enrollment.validFrom !== enrollment.validFrom &&
          (errors.validFrom = t("pastDateError"));
      } else {
        !(parsedDate instanceof Date && isFinite(parsedDate.getTime())) &&
          (errors.validFrom = t("validFromInvalidFormError"));
      }
    }
    if (
      props.validationRules &&
      Object.keys(props.validationRules).length !== 0
    ) {
      for (const field in props.validationRules) {
        field === "validFrom" &&
          props.validationRules[field]?.required &&
          !enrollment?.validFrom &&
          (errors.validFrom = t("validFromRequiredFormError"));
        props.validationRules[field]?.max &&
          parseInt(props.validationRules[field].max) &&
          enrollment[field] > parseInt(props.validationRules[field]?.max) &&
          (errors[field] =
            t("fieldMaxFormError") +
            " (" +
            props.validationRules[field]?.max +
            ")");
        // validationRules[field]?.max && parseInt(validationRules[field].max) && enrollment[field]===0 && (errors[field]=  t('fieldMaxZeroFormError')+ " (" + validationRules[field]?.max + ")" )
      }
    }
    setErrors(errors);
  };

  const touchFields = () => {
    for (const property in touched) {
      touched[property] = true;
    }
    setTouched({ ...touched });
  };

  const close = () => {
    setTouched(touchDefault);
    props.close();
  };

  const updateEnrollment = (attribute: string, value: any) => {
    enrollment[attribute] = value;
    setEnrollment({ ...enrollment });
  };

  let roleHandler = (role: string) => {
    const currentRoles = enrollment.groupRoles
      ? [...enrollment.groupRoles]
      : [];
    const idx = currentRoles.indexOf(role);
    if (idx > -1) {
      currentRoles.splice(idx, 1);
    } else {
      currentRoles.push(role);
    }
    setEnrollment({ ...enrollment, groupRoles: currentRoles });
  };

  const createEnrollment = () => {
    close();
    startLoader();
    if (enrollment.membershipExpirationDays === 0) {
      enrollment.membershipExpirationDays = null;
    }
    if (!enrollment?.aup?.url) {
      delete enrollment.aup;
    }
    groupsService!
      .doPost<any>("/group-admin/group/" + props.groupId + "/configuration", {
        ...enrollment,
      })
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          addAlert(
            t(
              enrollment?.id
                ? "updateDefaultEnrollmentSuccess"
                : "createEnrollmentSuccess"
            ),
            AlertVariant.success
          );
          props.refresh();
        } else {
          addError(
            t(
              enrollment?.id
                ? "updateDefaultEnrollmentError"
                : "createEnrollmentError"
            ),
            getError(response)
          );
        }
      })
      .catch(() => {
        addError(
          t(
            enrollment?.id
              ? "updateDefaultEnrollmentError"
              : "createEnrollmentError"
          ),
          getError(t("unexpectedError"))
        );
      })
      .finally(() => {
        stopLoader();
      });
  };

  const deleteEnrollment = (id: any) => {
    close();
    startLoader();
    groupsService!
      .doDelete<any>(
        "/group-admin/group/" + props.groupId + "/configuration/" + id
      )
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 || response.status === 204) {
          addAlert(t("deleteEnrollmentSuccess"), AlertVariant.success);
          props.refresh();
          // setGroupMembers(response.data.results);
        } else {
          addError(t("deleteEnrollmentError"), getError(response));
        }
      })
      .catch(() => {
        addError(t("deleteEnrollmentError"), getError(t("unexpectedError")));
      })
      .finally(() => {
        stopLoader();
      });
  };

  const onMinus = () => {
    enrollment.membershipExpirationDays =
      (enrollment.membershipExpirationDays || 0) - 1;
    setEnrollment({ ...enrollment });
  };

  const onChange = (event: React.FormEvent<HTMLInputElement>) => {
    touched.membershipExpirationDays = true;
    enrollment.membershipExpirationDays = (
      event.target as HTMLInputElement
    ).value;
    enrollment.membershipExpirationDays =
      enrollment.membershipExpirationDays === ""
        ? enrollment.membershipExpirationDays
        : +enrollment.membershipExpirationDays;
    setEnrollment({ ...enrollment });
    setTouched({ ...touched });
  };

  const onPlus = () => {
    enrollment.membershipExpirationDays =
      (enrollment?.membershipExpirationDays || 0) + 1;
    setEnrollment({ ...enrollment });
  };

  const isPastDate = (date: Date): string => {
    const currentDate = new Date();

    // Normalize both dates to remove the time part for an accurate comparison
    const currentDateWithoutTime = new Date(
      currentDate.getFullYear(),
      currentDate.getMonth(),
      currentDate.getDate()
    );
    const selectedDateWithoutTime = new Date(
      date.getFullYear(),
      date.getMonth(),
      date.getDate()
    );

    // Check if the selected date is in the past and not the same as the validFrom date
    if (
      selectedDateWithoutTime < currentDateWithoutTime &&
      props.enrollment.validFrom !== dateFormat(selectedDateWithoutTime)
    ) {
      return t("pastDateError");
    } else {
      return "";
    }
  };

  const validators: ((date: Date) => string)[] = [isPastDate];
  return (
    <React.Fragment>
      <Modal
        variant={ModalVariant.large}
        header={
          <React.Fragment>
            <h1 className="pf-c-modal-box__title gm_modal-title gm_flex-center">
              {enrollment?.id
                ? t("enrollmentConfigurationModalTitleEdit")
                : t("enrollmentConfigurationModalTitleCreate")}
              {enrollment?.id && (
                <Tooltip content={<div>{t("deleteEnrollmentTooltip")}</div>}>
                  <TrashIcon
                    className={"gm_trash-icon"}
                    onClick={() => {
                      setModalInfo({
                        variant: "medium",
                        title: t("deleteEnrollmentConfirmationTitle"),
                        message: t("deleteEnrollmentConfirmationMessage"),
                        accept_message: t("yes"),
                        cancel_message: t("no"),
                        accept: function () {
                          deleteEnrollment(enrollment?.id);
                          setModalInfo({});
                        },
                        cancel: function () {
                          setModalInfo({});
                        },
                      });
                    }}
                  />
                </Tooltip>
              )}
            </h1>
          </React.Fragment>
        }
        isOpen={isModalOpen}
        onClose={() => {
          close();
        }}
        actions={[
          <Tooltip
            {...(!!(Object.keys(errors).length !== 0)
              ? { trigger: "manual", isVisible: false }
              : { trigger: "mouseenter" })}
            content={<div>{t("createGroupFormError")}</div>}
          >
            <div>
              <Button
                key="confirm"
                variant="primary"
                onClick={() => {
                  touchFields();
                  if (Object.keys(errors).length !== 0) {
                    setModalInfo({
                      message: t("enrollmentConfigurationModalSubmitError"),
                      accept_message: t("OK"),
                      accept: function () {
                        setModalInfo({});
                      },
                      cancel: function () {
                        setModalInfo({});
                      },
                    });
                  } else {
                    createEnrollment();
                  }
                }}
              >
                {enrollment?.id ? t("Submit") : t("Create")}
              </Button>
            </div>
          </Tooltip>,
          <Button
            key="cancel"
            variant="link"
            onClick={() => {
              close();
            }}
          >
            {t("Cancel")}
          </Button>,
        ]}
      >
        <ConfirmationModal modalInfo={modalInfo} />
        {Object.keys(enrollment).length !== 0 ? (
          <Form>
            <FormGroup
              label={t("enrollmentConfigurationNameTitle")}
              isRequired
              fieldId="simple-form-name-01"
              onBlur={() => {
                touched.name = true;
                setTouched({ ...touched });
              }}
            >
              <TextInput
                isRequired
                type="text"
                id="simple-form-name-01"
                name="simple-form-name-01"
                aria-describedby="simple-form-name-01-helper"
                value={enrollment?.name}
                validated={errors.name && touched.name ? "error" : "default"}
                onChange={(_event, value) => {
                  updateEnrollment("name", value);
                }}
              />
              {errors.name && touched.name ? (
                <FormAlert>
                  <Alert
                    variant="danger"
                    title={errors.name}
                    aria-live="polite"
                    isInline
                  />
                </FormAlert>
              ) : null}
            </FormGroup>
            <FormGroup
              label={t("enrollmentConfigurationMembExpTitle")}
              fieldId="simple-form-name-01"
              onBlur={() => {
                touched.membershipExpirationDays = true;
                setTouched({ ...touched });
              }}
            >
              <Tooltip
                {...(!props.validationRules?.membershipExpirationDays?.max
                  ? { trigger: "manual", isVisible: false }
                  : { trigger: "mouseenter" })}
                content={
                  <div>
                    {t(
                      "enrollmentConfigurationExpirationSwitchDisabledTooltip"
                    )}
                  </div>
                }
              >
                <Switch
                  id="simple-switch-membershipExpirationDays"
                  aria-label="simple-switch-membershipExpirationDays"
                  isDisabled={
                    props.validationRules?.membershipExpirationDays?.max &&
                    isIntegerOrNumericString(
                      enrollment?.membershipExpirationDays
                    ) &&
                    enrollment.membershipExpirationDays !== 0
                  }
                  isChecked={
                    isIntegerOrNumericString(
                      enrollment?.membershipExpirationDays
                    ) && enrollment.membershipExpirationDays !== 0
                  }
                  onChange={() => {
                    if (
                      isIntegerOrNumericString(
                        enrollment?.membershipExpirationDays
                      ) &&
                      enrollment.membershipExpirationDays !== 0
                    ) {
                      enrollment.membershipExpirationDays = 0;
                      setEnrollment({ ...enrollment });
                    } else {
                      enrollment.membershipExpirationDays =
                        props.validationRules?.membershipExpirationDays
                          ?.defaultValue &&
                        isIntegerOrNumericString(
                          props.validationRules?.membershipExpirationDays
                            ?.defaultValue
                        )
                          ? parseInt(
                              props.validationRules.membershipExpirationDays
                                .defaultValue
                            )
                          : 32;
                      setEnrollment({ ...enrollment });
                    }
                  }}
                />
              </Tooltip>

              {enrollment.membershipExpirationDays === 0 ? (
                <HelperTextItem
                  className="gm_expiration-warning-label"
                  variant="warning"
                  hasIcon
                >
                  {t("enrollmentConfigurationExpirationWaring")}
                </HelperTextItem>
              ) : null}

              {enrollment.membershipExpirationDays !== 0 ? (
                <div className="gm_number-input-container">
                  <NumberInput
                    value={enrollment?.membershipExpirationDays}
                    onMinus={onMinus}
                    onBlur={() => {
                      touched.membershipExpirationDays = true;
                      setTouched({ ...touched });
                    }}
                    onChange={onChange}
                    onPlus={onPlus}
                    inputName="input"
                    inputAriaLabel="number input"
                    minusBtnAriaLabel="minus"
                    plusBtnAriaLabel="plus"
                  />
                </div>
              ) : null}
              {errors.membershipExpirationDays &&
              touched.membershipExpirationDays ? (
                <FormAlert>
                  <Alert
                    variant="danger"
                    title={errors.membershipExpirationDays}
                    aria-live="polite"
                    isInline
                  />
                </FormAlert>
              ) : null}
            </FormGroup>
            <FormGroup
              label={t("enrollmentConfigurationValidFromTitle")}
              fieldId="simple-form-name-09"
              labelIcon={
                <Popover
                  bodyContent={
                    <div>{t("enrollmentConfigurationTooltipValidFrom")} .</div>
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
              <Switch
                id="simple-switch-requireApproval"
                aria-label="simple-switch-requireApproval"
                isChecked={enrollment?.validFrom}
                onChange={() => {
                  touched.validFrom = true;
                  setTouched({ ...touched });

                  if (enrollment?.validFrom) {
                    enrollment.validFrom = null;
                    setEnrollment({ ...enrollment });
                  } else {
                    enrollment.validFrom = currentDate;
                    setEnrollment({ ...enrollment });
                  }
                }}
              />
              {errors.validFrom && touched.validFrom ? (
                <FormAlert>
                  <Alert
                    variant="danger"
                    title={errors.validFrom}
                    aria-live="polite"
                    isInline
                  />
                </FormAlert>
              ) : null}
            </FormGroup>
            {enrollment?.validFrom ? (
              <DatePicker
                value={enrollment?.validFrom}
                placeholder="DD-MM-YYYY"
                dateFormat={dateFormat}
                dateParse={dateParse}
                validators={validators}
                onChange={(_value, date) => {
                  if (enrollment?.validFrom) {
                    enrollment.validFrom = date;
                    setEnrollment({ ...enrollment });
                  }
                }}
              />
            ) : null}
            {errors.validFrom && touched.validFrom ? (
              <FormAlert>
                <Alert
                  variant="danger"
                  title={errors.validFrom}
                  aria-live="polite"
                  isInline
                />
              </FormAlert>
            ) : null}

            <FormGroup
              label={t("enrollmentConfigurationApprovalTitle")}
              fieldId="simple-form-name-01"
              // helperText=""
            >
              <Switch
                id="simple-switch-requireApproval1"
                aria-label="simple-switch-requireApproval1"
                isChecked={enrollment?.requireApproval}
                onChange={(_event, value) => {
                  updateEnrollment("requireApproval", value);
                }}
              />
            </FormGroup>
            <FormFieldGroup
              header={
                <FormFieldGroupHeader
                  titleText={{
                    text: t("enrollmentConfigurationCommentSectionTitle"),
                    id: "field-group4-non-expandable-titleText-id",
                  }}
                />
              }
            >
              <FormGroup
                label={t("enrollmentConfigurationCommentEnableLabel")}
                isRequired
                fieldId="simple-form-name-01"
                labelIcon={
                  <Popover
                    bodyContent={
                      <div>
                        {t("enrollmentConfigurationCommentEnableTooltip")}
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
                <Switch
                  id="simple-switch-comments"
                  aria-label="simple-switch-membershipExpirationDays"
                  isChecked={enrollment.commentsNeeded}
                  onChange={() => {
                    enrollment.commentsNeeded = !enrollment.commentsNeeded;
                    setEnrollment({ ...enrollment });
                  }}
                />
              </FormGroup>
              {enrollment.commentsNeeded ? (
                <React.Fragment>
                  <FormGroup
                    label={t("enrollmentConfigurationCommentLabelLabel")}
                    isRequired
                    fieldId="simple-form-name-01"
                    onBlur={() => {
                      touched.commentsLabel = true;
                      setTouched({ ...touched });
                    }}
                  >
                    <TextInput
                      isRequired
                      type="url"
                      id="simple-form-name-01"
                      name="simple-form-name-01"
                      aria-describedby="simple-form-name-01-helper"
                      value={enrollment?.commentsLabel}
                      onBlur={() => {
                        touched.commentsLabel = true;
                        setTouched({ ...touched });
                      }}
                      validated={
                        errors.commentsLabel && touched.commentsLabel
                          ? "error"
                          : "default"
                      }
                      onChange={(_event, value) => {
                        enrollment.commentsLabel = value;
                        setEnrollment({ ...enrollment });
                      }}
                    />
                    {errors.commentsLabel && touched.commentsLabel ? (
                      <FormAlert>
                        <Alert
                          variant="danger"
                          title={errors.commentsLabel}
                          aria-live="polite"
                          isInline
                        />
                      </FormAlert>
                    ) : null}
                  </FormGroup>
                  <FormGroup
                    label={t("enrollmentConfigurationCommentDescriptionLabel")}
                    isRequired
                    fieldId="simple-form-name-01"
                    onBlur={() => {
                      touched.commentsDescription = true;
                      setTouched({ ...touched });
                    }}
                  >
                    <TextInput
                      isRequired
                      type="url"
                      id="simple-form-name-01"
                      name="simple-form-name-01"
                      aria-describedby="simple-form-name-01-helper"
                      value={enrollment?.commentsDescription}
                      onBlur={() => {
                        touched.commentsDescription = true;
                        setTouched({ ...touched });
                      }}
                      validated={
                        errors.commentsDescription &&
                        touched.commentsDescription
                          ? "error"
                          : "default"
                      }
                      onChange={(_event, value) => {
                        enrollment.commentsDescription = value;
                        setEnrollment({ ...enrollment });
                      }}
                    />
                    {errors.commentsDescription &&
                    touched.commentsDescription ? (
                      <FormAlert>
                        <Alert
                          variant="danger"
                          title={errors.commentsDescription}
                          aria-live="polite"
                          isInline
                        />
                      </FormAlert>
                    ) : null}
                  </FormGroup>
                </React.Fragment>
              ) : null}
            </FormFieldGroup>
            <FormFieldGroup
              header={
                <FormFieldGroupHeader
                  titleText={{
                    text: t("enrollmentConfigurationAupTitle"),
                    id: "field-group4-non-expandable-titleText-id",
                  }}
                />
              }
            >
              <FormGroup
                label={t("URL")}
                isRequired={props.validationRules?.aupEntity?.required === true}
                fieldId="simple-form-name-01"
              >
                <TextInput
                  isRequired={
                    props.validationRules?.aupEntity?.required === true
                  }
                  type="url"
                  id="simple-form-name-01"
                  name="simple-form-name-01"
                  aria-describedby="simple-form-name-01-helper"
                  value={enrollment?.aup?.url}
                  onBlur={() => {
                    touched.aup_url = true;
                    setTouched({ ...touched });
                  }}
                  validated={
                    errors.aup_url && touched.aup_url ? "error" : "default"
                  }
                  onChange={(_event,value) => {
                    enrollment.aup.url = value;
                    setEnrollment({ ...enrollment });
                  }}
                />
                {errors.aup_url && touched.aup_url ? (
                  <FormAlert>
                    <Alert
                      variant="danger"
                      title={errors.aup_url}
                      aria-live="polite"
                      isInline
                    />
                  </FormAlert>
                ) : null}
              </FormGroup>
            </FormFieldGroup>
            <FormFieldGroup
              header={
                <FormFieldGroupHeader
                  titleText={{
                    text: t("enrollmentConfigurationGroupRolesTitle"),
                    id: "field-group4-non-expandable-titleText-id",
                  }}
                />
              }
            >
              <FormGroup
                label={t("enrollmentConfigurationTooltipGroupRoles")}
                fieldId="simple-form-name-01"
              >
                <table className="gm_roles-table">
                  <tbody>
                    {props.groupRoles &&
                      (Array.isArray(props.groupRoles)
                        ? props.groupRoles
                        : Object.keys(props.groupRoles)
                      ).map((role: any, index: number) => {
                        const key = `role-${index}-${role}`;
                        return (
                          <tr key={key} onClick={() => roleHandler(role)}>
                            <td>{role}</td>
                            <td>
                              <Checkbox
                                id={`role-${index}`}
                                name={`role-${index}`}
                                isChecked={enrollment?.groupRoles?.includes(
                                  role
                                )}
                                aria-label={`checkbox-role-${role}`}
                                onChange={() => roleHandler(role)}
                              />
                            </td>
                          </tr>
                        );
                      })}
                  </tbody>
                </table>
                {errors.groupRoles && touched.groupRoles && (
                  <FormErrorText message={t("required")} />
                )}
              </FormGroup>
              <FormGroup
                label={t("enrollmentConfigurationMultiSelectTitle")}
                fieldId="simple-form-name-01"
                labelIcon={
                  <Popover
                    bodyContent={
                      <div>
                        {t("enrollmentConfigurationMultiSelectTooltip")}
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
                <Switch
                  aria-label="simple-switch-multiselectRole"
                  isChecked={enrollment?.multiselectRole}
                  onChange={(_event, value) => {
                    updateEnrollment("multiselectRole", value);
                  }}
                />
              </FormGroup>
            </FormFieldGroup>
            <FormGroup
              label={t("enrollmentConfigurationHideConfTitle")}
              fieldId="simple-form-name-01"
              labelIcon={
                <Popover
                  bodyContent={
                    <div>{t("enrollmentConfigurationHideConfTooltip")}</div>
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
              // helperText=""
            >
              <Switch
                id="simple-switch-visibleToNotMembers"
                aria-label="simple-switch-visibleToNotMembers"
                isChecked={enrollment?.visibleToNotMembers}
                onChange={(_event, value) => {
                  updateEnrollment("visibleToNotMembers", value);
                }}
              />
            </FormGroup>
            <FormGroup
              label={t("enrollmentConfigurationActiveTitle")}
              fieldId="simple-form-name-01"
              // helperText=""
            >
              <Switch
                aria-label="simple-switch-active"
                isChecked={enrollment?.active}
                onChange={(_event, value) => {
                  updateEnrollment("active", value);
                }}
              />
            </FormGroup>
          </Form>
        ) : null}
      </Modal>
    </React.Fragment>
  );
};
