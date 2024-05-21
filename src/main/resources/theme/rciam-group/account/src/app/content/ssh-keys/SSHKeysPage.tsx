import * as React from 'react';
import { FC, useState, useEffect } from 'react';
import { DataListContent, DataList, DataListItem, DataListItemRow, Spinner, Split, SplitItem, Button, Grid, GridItem, Badge, Modal, ModalVariant, Form, FormGroup, TextArea } from '@patternfly/react-core';
// @ts-ignore
import { ContentAlert } from '../ContentAlert';
// @ts-ignore
import { ContentPage } from '../ContentPage';
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { Msg } from '../../widgets/Msg';
import { ExclamationCircleIcon, KeyIcon, PlusIcon } from '@patternfly/react-icons';

export interface SSHKeysPageProps {
}

export interface AdminGroupsPageState {
    groups: AdminGroup[];
    directGroups: AdminGroup[];
    isDirectMembership: boolean;
}

interface AdminGroup {
    id?: string;
    name: string;
    path: string;
    extraSubGroups: AdminGroup[];
}


interface Response {
    results: AdminGroup[],
    count: BigInteger;
}


export const SSHKeysPage: FC<SSHKeysPageProps> = () => {

    let groupsService = new GroupsServiceClient();
    const [sshKeys, setSshKeys] = useState<any>();
    const [openModal, setOpenModal] = useState(false);
    const [key, setKey] = useState(0);
    const refresh = () => setKey(key + 1);
    const [deleteSSHKeyIndex, setDeleteSSHKeyIndex] = useState(-1);

    let getSSHKeys = () => {
        groupsService!.doGet<Response>("/ssh-public-keys", { target: "sshKeys" })
            .then((response: HttpResponse<Response>) => {
                if (response.status === 200 && response?.data) {
                    setSshKeys(response.data);
                }
            });
    }

    useEffect(() => {
        getSSHKeys();
    }, [key])
    if (!sshKeys) {
        return <Spinner />;
    }

    const saveSSHKeys = async (sshKeys: string[], type: string) => {
        try {
            await updateSHHKeys(sshKeys);
            ContentAlert.success(Msg.localize(type === "add" ? "sshKeyAddSuccess" : "sshKeyDeleteSuccess"));
        } catch (error) {
            console.log(error);
            ContentAlert.danger(Msg.localize(type === "add" ? "sshKeyAddError" : "sshKeyAddSuccess"));
        }
    };


    let updateSHHKeys = (sshKeys: string[]) => {
        groupsService!.doPut<Response>("/ssh-public-keys", sshKeys, { target: "sshKeys" })
            .then((response: HttpResponse<Response>) => {
                refresh();
            });
    }

    return (
        <div className="gm_content">
            <ContentPage title={Msg.localize("sshKeys")} introMessage={Msg.localize("sshKeysDescrption")}>
                <ConfirmationModal
                    deleteSSHKeyIndex={deleteSSHKeyIndex}
                    close={() => {
                        setDeleteSSHKeyIndex(-1);
                    }}
                    deleteSSHKey={(index: number) => {
                        const array = [...sshKeys];
                        array.splice(index, 1);
                        saveSSHKeys(array, "delete");
                        setDeleteSSHKeyIndex(-1);
                    }}
                />
                <SSHKeyModal
                    openModal={openModal}
                    sshKeys={sshKeys}
                    close={() => {
                        setOpenModal(false);
                    }}
                    save={(keyValue: string) => {
                        sshKeys.push(keyValue);
                        saveSSHKeys([...sshKeys], "add");
                        setOpenModal(false);
                    }}
                />
                <Split hasGutter className="pf-u-mb-lg gm_padding-right">
                    <SplitItem isFilled></SplitItem>
                    <SplitItem>
                        <Button
                            variant="primary"
                            icon={<PlusIcon />}
                            iconPosition="right"
                            onClick={() => setOpenModal(true)}
                        >
                            {Msg.localize("addNew")}
                        </Button>
                    </SplitItem>
                </Split>
                <DataList
                    className="signed-in-device-list"
                    aria-label={Msg.localize("signedInDevices")}
                >
                    <DataListItem className="test-classname">
                        {sshKeys.map((key, index) => (
                            <DataListItemRow key={key}>
                                <DataListContent
                                    aria-label="device-sessions-content"
                                    className="pf-u-flex-grow-1"
                                >
                                    <Grid hasGutter>
                                        <GridItem span={1}>
                                            <KeyIcon />
                                            <Badge isRead>SSH</Badge>
                                        </GridItem>
                                        <GridItem sm={8} md={9} span={10}>
                                            <span className="pf-u-mr-md session-title">{key}</span>
                                        </GridItem>
                                        <GridItem
                                            className="pf-u-text-align-right gm_padding-right"
                                            sm={3}
                                            md={2}
                                            span={1}
                                        >
                                            <Button
                                                variant="danger"
                                                onClick={() => {
                                                    setDeleteSSHKeyIndex(index);
                                                }}
                                            >
                                                Delete
                                            </Button>
                                        </GridItem>
                                    </Grid>
                                </DataListContent>
                            </DataListItemRow>
                        ))}
                    </DataListItem>
                </DataList>
            </ContentPage>
        </div>
    );
}

const ConfirmationModal = (props: any) => {
    return (
        <Modal
            variant={ModalVariant.medium}
            header={
                <h1 className="pf-c-modal-box__title gm_modal-title gm_flex-center">
                    {Msg.localize("sshKeyDeleteConfirmationTitle")}
                </h1>
            }
            isOpen={props.deleteSSHKeyIndex >= 0}
            onClose={() => {
                props.close();
            }}
            actions={[
                <Button
                    key="cancel"
                    variant="secondary"
                    isDanger
                    onClick={() => {
                        console.log(props.deleteSSHKeyIndex);
                        props.deleteSSHKey(props.deleteSSHKeyIndex);
                    }}
                >
                    {Msg.localize("sshKeyDeleteConfirmationButton")}
                </Button>,
            ]}
        >
            <div dangerouslySetInnerHTML={{ __html: Msg.localize("sshKeyDeleteConfirmationMessage") }} />
        </Modal>
    );
};

const SSHKeyModal: FC<any> = (props) => {
    const [error, setError] = useState("");
    const [keyValue, setKeyValue] = useState("");

    function validateSSHKey(input: string) {
        const regexArray = [
            /^ssh-dss AAAAB3NzaC1kc3[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
            /^ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNT[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
            /^ecdsa-sha2-nistp384 AAAA[0-9A-Za-z+/]+[=]{0,3}( [^\s]+)*$/,
            /^ecdsa-sha2-nistp521 AAAA[0-9A-Za-z+/]+[=]{0,3}( [^\s]+)*$/,
            /^sk-ecdsa-sha2-nistp256@openssh.com AAAAInNrLWVjZHNhLXNoYTItbmlzdHAyNTZAb3BlbnNzaC5jb2[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
            /^ssh-ed25519 AAAAC3NzaC1lZDI1NTE5[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
            /^sk-ssh-ed25519@openssh.com AAAAGnNrLXNzaC1lZDI1NTE5QG9wZW5zc2guY29t[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
            /^ssh-rsa AAAAB3NzaC1yc2[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
        ];
        if (props.sshKeys.includes(input)) {
            return "dublicate";
        }
        for (let i = 0; i < regexArray.length; i++) {
            if (regexArray[i].test(input)) {
                return "";
            }
        }
        return "invalid";
    }

    useEffect(() => {
        setError(validateSSHKey(keyValue));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [keyValue]);

    const close = () => {
        props.close();
        setKeyValue("");
    };

    return (
        <Modal
            variant={ModalVariant.large}
            header={
                <h1 className="pf-c-modal-box__title gm_modal-title gm_flex-center">
                    {Msg.localize("addNewShhKey")}
                </h1>
            }
            isOpen={props.openModal}
            onClose={() => {
                close();
            }}
            actions={[
                <Button
                    key="confirm"
                    variant="primary"
                    isDisabled={!keyValue || !!error}
                    onClick={() => {
                        setKeyValue("");
                        props.save(keyValue);
                    }}
                >
                    {Msg.localize("addShhKey")}
                </Button>,
                <Button
                    key="cancel"
                    variant="secondary"
                    isDanger
                    onClick={() => {
                        close();
                    }}
                >
                    {Msg.localize("cancel")}
                </Button>,
            ]}
        >
            <Form>
                <FormGroup
                    label={"Key"}
                    isRequired
                    fieldId="simple-form-name-01"
                    helperTextInvalid={Msg.localize(
                        error === "dublicate"
                            ? "sshKeyErrorDublicate"
                            : "sshKeyErrorInvalid",
                    )}
                    helperTextInvalidIcon={<ExclamationCircleIcon />}
                    validated={keyValue && error ? "error" : "default"}
                >
                    <TextArea
                        isRequired
                        type="text"
                        autoResize={true}
                        placeholder={Msg.localize("sshKeyInputPlaceholder")}
                        id="simple-form-name-01"
                        name="simple-form-name-01"
                        aria-describedby="simple-form-name-01-helper"
                        value={keyValue}
                        validated={keyValue && error ? "error" : "default"}
                        onChange={(value) => {
                            setKeyValue(value.trim());
                        }}
                    />
                </FormGroup>
            </Form>
        </Modal>
    );
};
