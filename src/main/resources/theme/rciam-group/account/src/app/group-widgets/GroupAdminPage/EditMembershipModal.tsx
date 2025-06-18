import * as React from 'react';
import { useState, useEffect } from 'react';
import { Button, Tooltip, ModalVariant, Modal, Form, FormGroup, Popover, DatePicker, Switch } from '@patternfly/react-core';
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
import { Msg } from '../../widgets/Msg';
import { isPastDate, dateParse, addDays, isFirstDateBeforeSecond, dateFormat } from '../../widgets/Date';
import { HelpIcon } from '@patternfly/react-icons';
import { GroupRolesTable } from '../GroupRolesTable';
import { ContentAlert } from '../../content/ContentAlert';
import { getError } from '../../js/utils.js'
import { useLoader } from '../LoaderContext';


interface EditMembershipModalProps {
    membership: Membership;
    setMembership: any;
    fetchGroupMembers: any;
};

interface Membership {
    groupRoles: string[];
    validFrom: string;
    membershipExpiresAt?: string;
    id?: any;
    status?: string;
    user?:any;
    group?:any;

}

export const EditMembershipModal: React.FC<EditMembershipModalProps> = (props) => {
    const touchDefault = {
        groupRoles: false,
        validFrom: false,
        membershipExpiresAt: false
    };
    let groupsService = new GroupsServiceClient();
    const [errors, setErrors] = useState<any>({});
    const [modalInfo, setModalInfo] = useState({});
    const { startLoader, stopLoader } = useLoader();    
    const [membership, setMembership] = useState<Membership>({
        "validFrom": "",
        "membershipExpiresAt": "",
        "groupRoles": []
    });
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [touched, setTouched] = useState<any>(touchDefault);
    const [enrollmentRules,setEnrollmentRules] = useState<any>({});
    const [groupConfiguration,setGroupConfiguration] = useState<any>({});

    useEffect(() => {
        setIsModalOpen(Object.keys(props.membership).length > 0);
        if (Object.keys(props.membership).length > 0) {
            setMembership({
                "validFrom": props.membership.validFrom,
                "membershipExpiresAt": props.membership.membershipExpiresAt,
                "groupRoles": [...props.membership.groupRoles]
            });
            fetchGroupEnrollmentRules();
            fetchGroupConfiguration();
        }

    }, [props.membership]);

    let fetchGroupConfiguration = () => {
        groupsService!.doGet<any>("/group-admin/group/" + props.membership.group.id + "/all")
          .then((response: HttpResponse<any>) => {
            if (response.status === 200 && response.data) {
              setGroupConfiguration(response.data);
            }
          })
      }


    let fetchGroupEnrollmentRules = () => {
        groupsService!.doGet<any>("/group-admin/configuration-rules", { params: { type: (("/" + props.membership.group?.name) !== props.membership.group?.path ? 'SUBGROUP' : 'TOP_LEVEL') } })
          .then((response: HttpResponse<any>) => {
            if (response.status === 200 && response.data) {
              if (response.data.length > 0) {
                let rules = {};
                response.data.forEach(field_rules => {
                  rules[field_rules.field] = {
                    "max": parseInt(field_rules.max),
                    "required": field_rules.required,
                    ...(field_rules.defaultValue && { "defaultValue": field_rules.defaultValue })
                  }
                });
                setEnrollmentRules(rules);
              }
              else {
                setEnrollmentRules({});
              }
            }
          })
      }

    useEffect(() => {
        validateMembership();
    }, [membership])


    const validateValidFrom = (date: Date): string => {
        if (dateFormat(date) !== props.membership.validFrom) {
            const selectedDateWithoutTime = new Date(date.getFullYear(), date.getMonth(), date.getDate());
            if (props.membership['validFrom'] !== dateFormat(selectedDateWithoutTime)) {
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
                    Msg.localize("validateFromMembershipError1")
                );
                return expirationError || "";
            }
        }
        return "";
    }


    const validateMembershipExpiresAt = (date: Date | null): string => {
        if (date) {
            const selectedDateWithoutTime = new Date(date.getFullYear(), date.getMonth(), date.getDate());
            if (props.membership['membershipExpiresAt'] !== dateFormat(selectedDateWithoutTime)) {
                let pastDateError = isPastDate(date);
                if (pastDateError) {
                    return pastDateError;
                }
            }

            // Now check if membership.membershipExpiresAt exists, and run the comparison
            if (membership.validFrom) {
                let expirationError = isFirstDateBeforeSecond(date, dateParse(membership.validFrom), "You cannot set the membership expiration date before the date the membership starts.");
                if (expirationError) {
                    return expirationError;
                }
            }
            if (enrollmentRules?.membershipExpirationDays?.max) {
                const currentDate = new Date();
                // Normalize both dates to remove the time part for an accurate comparison
                const currentDateWithoutTime = new Date(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate());
                const rulesValidationError = isFirstDateBeforeSecond(
                    isPastDate(dateParse(membership.validFrom)) ?
                        addDays(currentDateWithoutTime, parseInt(enrollmentRules.membershipExpirationDays.max))
                        :
                        addDays(dateParse(membership.validFrom), parseInt(enrollmentRules.membershipExpirationDays.max))
                    ,
                    date
                    ,
                    Msg.localize("validateMembershipExpiresErrorMax", [JSON.stringify(enrollmentRules.membershipExpirationDays.max)])
                )
                if (rulesValidationError) {
                    return rulesValidationError
                }
            }
        }
        else {
            if (enrollmentRules?.membershipExpirationDays?.required || enrollmentRules?.membershipExpirationDays?.max) {
                return Msg.localize("validateMembershipExpiratErrorRequired");
            }
        }
        return "";
    }

    const validatorValidFrom: ((date: Date) => string)[] = [validateValidFrom];
    const validatorMembershipExpiresAt: ((date: Date) => string)[] = [validateMembershipExpiresAt];

    const submit = () => {
        touchFields();
        if (Object.keys(errors).length !== 0) {
            setModalInfo({
                message: Msg.localize('enrollmentConfigurationModalSubmitError'),
                accept_message: Msg.localize('OK'),
                accept: function () {
                    setModalInfo({})
                },
                cancel: function () {
                    setModalInfo({})
                }
            });
        }
        else {
            updateMembership();
        }
    }

    const handleModalToggle = () => {
        props?.setMembership({});
    };

    let updateMembership = () => {
        setIsModalOpen(false);
        startLoader();
        groupsService!.doPut<any>(
            "/group-admin/group/" + props.membership.group.id + "/member/" + props.membership?.id,
            { ...membership }
        )
            .then((response: HttpResponse<any>) => {
                // Fetch updated group members after the request
                props.fetchGroupMembers();
                // Stop the loader
                stopLoader();
                // Show success or error alert based on the response
                if (response.status === 200 || response.status === 204) {
                    ContentAlert.success(Msg.localize('updateMembershipSuccess'));
                } else {
                    ContentAlert.danger(Msg.localize('updateMembershipError', [getError(response)]));
                }
                // Clear the membership only after the request is completed
                props?.setMembership({});
            })
            .catch((error) => {
                // Handle errors
                stopLoader();
                ContentAlert.danger(Msg.localize('updateMembershipError', [getError(error)]));

                // Clear the membership even if there is an error
                props?.setMembership({});
            });
    };

    const validateMembership = () => {
        let errors: Record<string, string> = {};
        let validFromError = validateValidFrom(dateParse(membership.validFrom));
        let membershipExpiresAtError = validateMembershipExpiresAt(membership.membershipExpiresAt ? dateParse(membership.membershipExpiresAt) : null);
        !(membership?.groupRoles?.length > 0) && (errors.groupRoles = Msg.localize('groupRolesFormError'));
        validFromError && (errors.validFrom = validFromError);
        membershipExpiresAtError && (errors.membershipExpiresAt = membershipExpiresAtError);
        setErrors(errors);
    }

    const touchFields = () => {
        for (const property in touched) {
            touched[property] = true;
        }
        setTouched({ ...touched });
    }


    return (
        <React.Fragment>
            <Modal
                variant={ModalVariant.medium}
                title={Msg.localize('adminGroupEditMembeship')}
                isOpen={isModalOpen}
                onClose={handleModalToggle}
                actions={[
                    <Button key="save" variant="primary" onClick={() => {
                        submit();
                    }}>
                        Save
                    </Button>,
                    <Button key="cancel" variant="link" onClick={() => {
                        props?.setMembership({});
                    }}>
                        Cancel
                    </Button>
                ]}
            >  
                <Form>
                <FormGroup
                        label={Msg.localize('groupPath')+":"}
                        fieldId="simple-form-name-01"
                    // helperText=""
                    >
                        <div>
                            {props.membership?.group?.path  ? props.membership.group.path : "Not Available"}
                        </div>
                    </FormGroup>
                    <FormGroup
                        label={Msg.localize('enrollmentFullNameLabel')}
                        fieldId="simple-form-name-01"
                    // helperText=""
                    >
                        <div>
                            {props.membership?.user?.firstName || props.membership?.user?.lastName ? props.membership.user.firstName + " " + props.membership.user.lastName : "Not Available"}
                        </div>
                    </FormGroup>
                    <FormGroup
                        label={Msg.localize('enrollmentEmailLabel')}
                        fieldId="simple-form-name-02"
                    >
                        <div>{props.membership?.user?.email ? props.membership.user.email : Msg.localize('notAvailable')}</div>
                    </FormGroup>
                    <FormGroup
                        label={"Username:"}
                        fieldId="simple-form-name-02"
                    >
                        <div>{props.membership?.user?.username ? props.membership.user.username : Msg.localize('notAvailable')}</div>
                    </FormGroup>
                    <FormGroup
                        label={Msg.localize('groupDatalistCellRoles')}
                        isRequired
                        fieldId="simple-form-name-01"
                        helperTextInvalid={touched.groupRoles && errors.groupRoles}
                        onBlur={() => {
                            touched.groupRoles = true;
                            setTouched({ ...touched });
                        }}
                        validated={errors.groupRoles && touched.groupRoles ? 'error' : 'default'}
                    >
                        <GroupRolesTable groupRoles={groupConfiguration.groupRoles ? Object.keys(groupConfiguration.groupRoles) : []} selectedRoles={membership.groupRoles} setSelectedRoles={(roles) => {
                            membership.groupRoles = roles;
                            setMembership({ ...membership });
                        }} />
                    </FormGroup>
                    <FormGroup
                        label={Msg.localize('memberSince')}
                        isRequired
                        fieldId="simple-form-name-01"
                        helperTextInvalid={touched.validFrom && errors.validFrom}
                        onBlur={() => {
                            touched.validFrom = true;
                            setTouched({ ...touched });
                        }}
                        validated={errors.validFrom && touched.validFrom ? 'error' : 'default'}
                    // helperText=""
                    >
                        <DatePicker isDisabled={props.membership.status === 'ENABLED'} value={membership?.validFrom} placeholder="DD-MM-YYYY" dateFormat={dateFormat} dateParse={dateParse} validators={validatorValidFrom} onChange={(value, date) => {
                            if (membership?.validFrom) {
                                membership.validFrom = value;
                                setMembership({ ...membership });
                            }
                        }}
                        />
                    </FormGroup>
                    <FormGroup
                        label={Msg.localize('groupDatalistCellMembershipExp')}
                        isRequired={!!enrollmentRules?.membershipExpirationDays?.required}
                        fieldId="simple-form-name-01"
                        helperTextInvalid={(touched.membershipExpiresAt && !membership?.membershipExpiresAt) && errors.membershipExpiresAt}
                        onBlur={() => {
                            touched.membershipExpiresAt = true;
                            setTouched({ ...touched });
                        }}
                        validated={errors.membershipExpiresAt && touched.membershipExpiresAt ? 'error' : 'default'}
                        labelIcon={
                            <Popover
                                bodyContent={
                                    <div>
                                        <Msg msgKey='enrollmentConfigurationTooltipExpirationDate' />    .
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
                                    <HelpIcon noVerticalAlign />
                                </button>
                            </Popover>
                        }
                    >
                        <div className="gm_switch_container">
                            <Tooltip
                                {...(!(enrollmentRules?.membershipExpirationDays?.required) ? { trigger: 'manual', isVisible: false } : { trigger: 'mouseenter' })} content={<div><Msg msgKey='enrollmentConfigurationExpirationDateSwitchDisabledTooltip' /></div>}
                            >
                                <Switch
                                    aria-label="simple-switch-membershipExpirationDays"
                                    isChecked={!!membership?.membershipExpiresAt}
                                    onChange={(value) => {
                                        if (membership.membershipExpiresAt) {
                                            delete membership.membershipExpiresAt;
                                            setMembership({ ...membership });
                                        }
                                        else {
                                            membership.membershipExpiresAt = dateFormat(new Date())
                                            setMembership({ ...membership });
                                        }
                                    }}
                                />
                            </Tooltip>
                        </div>
                        {membership?.membershipExpiresAt &&
                            <DatePicker value={membership?.membershipExpiresAt} placeholder="DD-MM-YYYY" dateFormat={dateFormat} dateParse={dateParse} validators={validatorMembershipExpiresAt} onChange={(value, date) => {
                                if (membership?.membershipExpiresAt) {
                                    membership.membershipExpiresAt = value;
                                    setMembership({ ...membership });
                                }
                            }}
                            />
                        }
                    </FormGroup>
                </Form>
            </Modal>
        </React.Fragment>);
}

