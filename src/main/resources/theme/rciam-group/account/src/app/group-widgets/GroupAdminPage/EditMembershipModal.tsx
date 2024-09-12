import * as React from 'react';
import { useState, useEffect } from 'react';
import { Button, Tooltip, ModalVariant, Modal, Form, FormGroup, Popover, DatePicker, Switch } from '@patternfly/react-core';
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
import { Msg } from '../../widgets/Msg';
import { HelpIcon } from '@patternfly/react-icons';
import { Loading } from '../LoadingModal';
import { Alerts } from '../../widgets/Alerts';
import { GroupRolesTable } from '../GroupRolesTable';


interface EditMembershipModalProps {
    membership: Membership;
    setMembership: any;
    groupRoles: any;
    groupId: any;
    fetchGroupMembers: any;
    enrollmentRules: any;
};

interface Membership {
    groupRoles: string[];
    validFrom: string;
    membershipExpiresAt?: string;
    id?: any;
    status?: string;
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
    const [loading, setLoading] = useState(false);
    const [alert, setAlert] = useState({});
    const [membership, setMembership] = useState<Membership>({
        "validFrom": "",
        "membershipExpiresAt": "",
        "groupRoles": []
    });
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [touched, setTouched] = useState<any>(touchDefault);

    useEffect(() => {
        setIsModalOpen(Object.keys(props.membership).length > 0);
        if (Object.keys(props.membership).length > 0) {
            setMembership({
                "validFrom": props.membership.validFrom,
                "membershipExpiresAt": props.membership.membershipExpiresAt,
                "groupRoles": [...props.membership.groupRoles]
            })
        }
    }, [props.membership])


    useEffect(() => {
        validateMembership();
    }, [membership])

    const isPastDate = (date: Date, field: string): string => {
        const currentDate = new Date();
        // Normalize both dates to remove the time part for an accurate comparison
        const currentDateWithoutTime = new Date(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate());
        const selectedDateWithoutTime = new Date(date.getFullYear(), date.getMonth(), date.getDate());
        // Check if the selected date is in the past and not the same as the validFrom date
        if (
            selectedDateWithoutTime < currentDateWithoutTime &&
            props.membership[field] !== dateFormat(selectedDateWithoutTime)
        ) {
            return Msg.localize('validFromPastFormError');
        } else {
            return "";
        }
    };

    const addDays = (date: Date, days: number): Date => {
        const result = new Date(date);
        result.setDate(result.getDate() + days);
        return result;
    };
    const dateParse = (date: string) => {
        const split = date.split('-');
        if (split.length !== 3) {
            return new Date();
        }
        const month = split[1];
        const day = split[2];
        const year = split[0];
        return new Date(`${year.padStart(4, '0')}-${month.padStart(2, '0')}-${day.padStart(2, '0')}T00:00:00`);
    };

    const isFirstDateBeforeSecond = (firstDate: Date | null, secondDate: Date, errorMessage: string) => {
        // Normalize both dates to remove the time part for an accurate comparison    
        if (firstDate) {
            const firstDateWithoutTime = new Date(firstDate.getFullYear(), firstDate.getMonth(), firstDate.getDate());
            const secondDateWithoutTime = new Date(secondDate.getFullYear(), secondDate.getMonth(), secondDate.getDate());

            // Check if the first date is before the second date
            if (firstDateWithoutTime < secondDateWithoutTime) {
                return errorMessage;
            } else {
                return "";
            }
        }
        else {
            return "";
        }
    };

    const validateValidFrom = (date: Date): string => {
        if (dateFormat(date) !== props.membership.validFrom) {
            let pastDateError = isPastDate(date, 'validFrom');
            if (pastDateError) {
                return pastDateError;
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
            let pastDateError = isPastDate(date, 'membershipExpiresAt');
            if (pastDateError) {
                return pastDateError;
            }

            // Now check if membership.membershipExpiresAt exists, and run the comparison
            if (membership.validFrom) {
                let expirationError = isFirstDateBeforeSecond(date, dateParse(membership.validFrom), "You cannot set the membership expiration date before the date the membership starts.");
                if (expirationError) {
                    return expirationError;
                }
            }
            if (props.enrollmentRules?.membershipExpirationDays?.max) {
                const currentDate = new Date();
                // Normalize both dates to remove the time part for an accurate comparison
                const currentDateWithoutTime = new Date(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate());
                const rulesValidationError = isFirstDateBeforeSecond(
                    isPastDate(dateParse(membership.validFrom), 'validFrom') ?
                        addDays(currentDateWithoutTime, parseInt(props.enrollmentRules.membershipExpirationDays.max))
                        :
                        addDays(dateParse(membership.validFrom), parseInt(props.enrollmentRules.membershipExpirationDays.max))
                    ,
                    date
                    ,
                    Msg.localize("validateMembershipExpiresErrorMax",[props.enrollmentRules.membershipExpirationDays.max])
                )
                if (rulesValidationError) {
                    return rulesValidationError
                }
            }
        }
        else {
            if (props.enrollmentRules?.membershipExpirationDays?.required || props.enrollmentRules?.membershipExpirationDays?.max) {
                return Msg.localize("validateMembershipExpiratErrorRequired");
            }
        }
        return "";
    }

    const validatorValidFrom: ((date: Date) => string)[] = [validateValidFrom];
    const validatorMembershipExpiresAt: ((date: Date) => string)[] = [validateMembershipExpiresAt];

    const dateFormat = (date: Date) =>
        date.toLocaleDateString('en-CA', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-');

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
        setLoading(true);
        groupsService!.doPut<any>("/group-admin/group/" + props.groupId + "/member/" + props.membership?.id, { ...membership })
            .then((response: HttpResponse<any>) => {
                props.fetchGroupMembers();
                setLoading(false);
                props?.setMembership({});
                if (response.status === 200 || response.status === 204) {
                    setAlert({ message: Msg.localize('updateMembershipMessage'), variant: "success", description: Msg.localize('updateMembershipSuccessMessage') })
                }
                else {
                    if(response.data.error){
                        setAlert({ message: Msg.localize('updateMembershipMessage'), variant: "danger", description: Msg.localize('updateMembershipErrorMessage',[response.data.error]) })
                    }
                }
            })
    }

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
                title={Msg.localize('adminGroupMemberEditRole')}
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
                <Alerts alert={alert} close={() => { setAlert({}) }} />
                <Loading active={loading} />
                <Form>
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
                        <GroupRolesTable groupRoles={props.groupRoles ? Object.keys(props.groupRoles) : []} selectedRoles={membership.groupRoles} setSelectedRoles={(roles) => {
                            membership.groupRoles = roles;
                            setMembership({ ...membership });
                        }} />
                    </FormGroup>
                    <FormGroup
                        label={Msg.localize('groupDatalistCellMembershipSince')}
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
                        isRequired={!!props.enrollmentRules?.membershipExpirationDays?.required}
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
                                {...(!(props.enrollmentRules?.membershipExpirationDays?.required) ? { trigger: 'manual', isVisible: false } : { trigger: 'mouseenter' })} content={<div><Msg msgKey='enrollmentConfigurationExpirationDateSwitchDisabledTooltip' /></div>}
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

