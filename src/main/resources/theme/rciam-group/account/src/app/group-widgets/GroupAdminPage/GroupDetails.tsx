import * as React from 'react';
import { FC, useState, useRef } from 'react';
import { DataList, DataListItem, DataListItemCells, DataListItemRow, DataListCell, Button, TextInput, InputGroup, Chip, Tooltip, ExpandableSection, ClipboardCopy, Popover } from '@patternfly/react-core';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import { GroupsServiceClient, HttpResponse } from '../../groups-mngnt-service/groups.service';
import { MinusIcon, PlusIcon, EyeIcon, TimesIcon, CopyIcon } from '@patternfly/react-icons';
import { Msg } from '../../widgets/Msg';
import { Alerts } from '../../widgets/Alerts';
import { Link } from 'react-router-dom';


export const GroupDetails: FC<any> = (props) => {
    let groupsService = new GroupsServiceClient();
    const roleRef = useRef<any>(null);
    const [roleInput, setRoleInput] = useState<string>("");
    const [modalInfo, setModalInfo] = useState({});
    const [alert, setAlert] = useState({});
    const [isExpanded, setIsExpanded] = useState(false);

    const onToggle = (isExpanded: boolean) => {
        setIsExpanded(isExpanded);
    };

    const addGroupRole = (role) => {
        groupsService!.doPost<any>("/group-admin/group/" + props.groupId + "/roles", {}, { params: { name: role } })
            .then((response: HttpResponse<any>) => {
                if (response.status === 200 || response.status === 204) {
                    props.fetchGroupConfiguration();
                    setRoleInput("");
                    setModalInfo({});
                }
            })
    }

    const removeGroupRole = (role) => {
        groupsService!.doDelete<any>("/group-admin/group/" + props.groupId + "/role/" + role)
            .then((response: HttpResponse<any>) => {
                if (response.status === 200 || response.status === 204) {
                    props.fetchGroupConfiguration();
                }
                setModalInfo({});
            }).catch(err => {
                setAlert({ message: Msg.localize('deleteRoleErrorTitle'), variant: "danger", description: Msg.localize('deleteRoleErrorMessage') });
                setModalInfo({});
            })

    }


    return (
        <React.Fragment>
            <Alerts alert={alert} close={() => { setAlert({}) }} />
            <ConfirmationModal modalInfo={modalInfo} />
            <DataList aria-label="Compact data list example" isCompact wrapModifier={"breakWord"}>
                <DataListItem aria-labelledby="compact-item1">
                    <DataListItemRow>
                        <DataListItemCells
                            dataListCells={[
                                <DataListCell key="primary content">
                                    <span id="compact-item1"><strong><Msg msgKey='Path' /></strong></span>
                                </DataListCell>,
                                <DataListCell width={3} key="secondary content ">
                                    <span>/{props.groupConfiguration?.parents?.map((group) => {
                                        return (<React.Fragment>
                                            <Link to={"/groups/admingroups/" + group.id}>{group.name}</Link>{'/'}
                                        </React.Fragment>)
                                    })}{props.groupConfiguration.name}</span>
                                </DataListCell>
                            ]}
                        />
                    </DataListItemRow>
                </DataListItem>
                <DataListItem aria-labelledby="compact-item1">
                    <DataListItemRow>
                        <DataListItemCells
                            dataListCells={[
                                <DataListCell key="primary content">
                                    <span id="compact-item1"><strong><Msg msgKey='enrollmentDiscoveryLink' /></strong></span>
                                </DataListCell>,
                                <DataListCell width={3} key="secondary content ">
                                    <ClipboardCopy isReadOnly hoverTip="Copy" clickTip="Copied" className="gm_copy-text-input">
                                        {groupsService.getBaseUrl() + '/account/#/enroll?groupPath=' + encodeURI(props.groupConfiguration.path)}
                                    </ClipboardCopy>
                                </DataListCell>
                            ]}
                        />
                    </DataListItemRow>
                </DataListItem>

                <DataListItem aria-labelledby="compact-item2">
                    <DataListItemRow className="gm_role_row">
                        <DataListItemCells
                            dataListCells={[
                                <DataListCell key="primary content">
                                    <span id="compact-item1"><strong><Msg msgKey='adminGroupRoles' /></strong></span>
                                </DataListCell>,
                                <DataListCell width={3} key="roles">
                                    <div className="gm_role_add_container">
                                        <InputGroup>
                                            <TextInput id="textInput-basic-1" value={roleInput} placeholder={Msg.localize('adminGroupRolesAddPlaceholder')} onChange={(e) => { setRoleInput(e.trim()); }} onKeyDown={(e) => { e.key === 'Enter' && roleRef?.current?.click(); }} type="email" aria-label="email input field" />
                                        </InputGroup>
                                        <Tooltip content={<div><Msg msgKey='adminGroupRolesAdd' /></div>}>
                                            <Button ref={roleRef} onClick={() => {
                                                if (props?.groupConfiguration?.groupRoles && Object.keys(props.groupConfiguration.groupRoles).includes(roleInput)) {
                                                    setModalInfo({
                                                        title: (Msg.localize('adminGroupRoleExistsTitle')),
                                                        accept_message: Msg.localize('OK'),
                                                        message: (Msg.localize('adminGroupRoleExistsMessage1') + " (" + roleInput + ") " + Msg.localize('adminGroupRoleExistsMessage2')),
                                                        accept: function () { setModalInfo({}) },
                                                        cancel: function () { setModalInfo({}) }
                                                    });
                                                }
                                                if (!roleInput) {
                                                    setModalInfo({
                                                        title: (Msg.localize('adminGroupRoleEmptyTitle')),
                                                        accept_message: Msg.localize('OK'),
                                                        accept: function () { setModalInfo({}) },
                                                        cancel: function () { setModalInfo({}) }
                                                    });
                                                }
                                                else {
                                                    setModalInfo({
                                                        title: (Msg.localize('Confirmation')),
                                                        accept_message: (Msg.localize('Yes')),
                                                        cancel_message: Msg.localize('No'),
                                                        message: (Msg.localize('adminGroupRoleAddConfirmation1') + " " + roleInput + " " + Msg.localize('adminGroupRoleAddConfirmation2')),
                                                        accept: function () { addGroupRole(roleInput) },
                                                        cancel: function () { setModalInfo({}) }
                                                    });
                                                }
                                            }}><PlusIcon /></Button>
                                        </Tooltip>
                                    </div>
                                    <table className="gm_roles-table">
                                        <thead>
                                            <tr>
                                                <th>
                                                    Role Name
                                                </th>
                                                <th>
                                                    Role Entitlement
                                                </th>
                                                <th>

                                                </th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {props?.groupConfiguration?.groupRoles && Object.keys(props.groupConfiguration.groupRoles).map((role, index) => {
                                                return <tr>
                                                    <td>
                                                        {role}
                                                    </td>
                                                    <td>
                                                        <ClipboardCopy isReadOnly hoverTip="Copy" clickTip="Copied">
                                                            {props.groupConfiguration.groupRoles[role]}
                                                        </ClipboardCopy>
                                                    </td>
                                                    <td>
                                                        <Tooltip content={<div><Msg msgKey='adminGroupRoleRemove' /></div>}>
                                                            <Button className="gm_roles-delete-button" variant="danger" onClick={() => {
                                                                setModalInfo({
                                                                    title: (Msg.localize('Confirmation')),
                                                                    accept_message: (Msg.localize('Yes')),
                                                                    cancel_message: (Msg.localize('No')),
                                                                    message: (Msg.localize('adminGroupRoleRemoveConfirmation1') + " " + role + " " + Msg.localize('adminGroupRoleRemoveConfirmation2')),
                                                                    accept: function () { removeGroupRole(role) },
                                                                    cancel: function () { setModalInfo({}) }
                                                                });
                                                            }}>
                                                                <MinusIcon />
                                                            </Button>
                                                        </Tooltip>
                                                    </td>
                                                </tr>
                                            })}
                                        </tbody>
                                    </table>
                                </DataListCell>
                            ]}
                        />
                    </DataListItemRow>
                </DataListItem>
            </DataList>
        </React.Fragment>

    )

}

