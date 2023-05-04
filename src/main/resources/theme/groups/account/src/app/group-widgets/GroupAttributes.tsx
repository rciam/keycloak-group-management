import * as React from 'react';
import {FC,useState,useEffect,useRef} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, Tooltip, DataListAction, Pagination, TextInput} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from './Modal';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '



export const GroupAttributes: FC<any> = (props) => {
    const attributeRef = useRef<any>(null);
    const [attributeKeyInput,setAttributeKeyInput] = useState<string>("");
    const [attributeValueInput,setAttributeValueInput] = useState<string>("");
    const [attributes,setAttributes] = useState<any>(props.groupConfiguration.attributes||{});
    const [modalInfo,setModalInfo] = useState({});


    useEffect(()=>{
        setAttributes(props.groupConfiguration.attributes||{})
      },[props.groupConfiguration])

    return (
        <React.Fragment>
            <ConfirmationModal modalInfo={modalInfo}/>
            <DataList aria-label="Compact data list example" isCompact>
                <DataListItem aria-labelledby="compact-item1">
                    <DataListItemRow>
                        <DataListItemCells dataListCells={[
                            <DataListCell width={2} key="key-hd"><strong>Key</strong></DataListCell>,
                            <DataListCell width={3} key="value-hd"><strong>Value</strong></DataListCell>
                        ]}>
                        </DataListItemCells>
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
                {attributes&&Object.keys(attributes).map(attribute=>{
                    return <DataListItem aria-labelledby={attribute}>
                        <DataListItemRow>
                            <DataListItemCells
                                dataListCells={[
                                    <DataListCell width={2} key="primary content"><strong>{attribute}</strong></DataListCell>,
                                    <DataListCell width={3} key="secondary content "><TextInput value={attributes[attribute]} onChange={(e)=>{
                                        attributes[attribute] = [e];
                                        setAttributes({...attributes})
                                        }}/>    
                                    </DataListCell>
                                ]}
                            />
                            <DataListAction
                                className="gm_cell-center"
                                aria-labelledby="check-action-item1 check-action-action1"
                                id="check-action-action1"
                                aria-label="Actions"
                                isPlainButtonAction
                            >
                                <Tooltip content={<div>Remove Attribute</div>}>
                                    <Button variant="danger" className={"gm_x-button-small"} 
                                        onClick={()=>{
                                            delete attributes[attribute];
                                            setAttributes({...attributes});
                                        }}
                                    >
                                        <div className={"gm_x-button"}></div>
                                    </Button>
                                </Tooltip>
                            </DataListAction>
                        </DataListItemRow>
                    </DataListItem>
                })}
                <DataListItem aria-labelledby='attribute-input'>
                    <DataListItemRow>
                        <DataListItemCells
                            dataListCells={[
                            <DataListCell width={2} key="key-input">
                                <span id="compact-item1">                              
                                    <TextInput id="textInput-basic-1" value={attributeKeyInput} placeholder='Add Attribute Key' onKeyDown={(e)=>{e.key=== 'Enter'&&attributeRef?.current?.click();}} type="text" aria-label="text input field" onChange={(e)=>{setAttributeKeyInput(e);}} />
                                </span>
                            </DataListCell>,
                            <DataListCell width={3} key="value-input">
                                <span id="item2">
                                    <TextInput id="textInput-basic-2" value={attributeValueInput} placeholder='Add Attribute Value' onKeyDown={(e)=>{e.key=== 'Enter'&&attributeRef?.current?.click();}} type="text" aria-label="text input field" onChange={(e)=>{setAttributeValueInput(e);}} />                                
                                </span>
                            </DataListCell>
                            ]}

                        />
                        <DataListAction
                            className="gm_cell-center"
                            aria-labelledby="check-action-item1 check-action-action1"
                            id="check-action-action1"
                            aria-label="Actions"
                            isPlainButtonAction
                        >
                            <Tooltip content={<div>Add Attribute</div>}>
                                <Button className={"gm_plus-button-small"} ref={attributeRef} 
                                    onClick={()=>{
                                        if(attributeKeyInput){
                                            attributes[attributeKeyInput] = [attributeValueInput];
                                            setAttributes({...attributes})
                                            setAttributeKeyInput("");
                                            setAttributeValueInput("");
                                        }
                                    }}
                                >
                                    <div className={"gm_plus-button"}></div>
                                </Button>
                            </Tooltip>
                        </DataListAction>
                    </DataListItemRow>
                </DataListItem>
            </DataList>
                <div className={"gm_attribute-controls"}>
                <Button  className={""} onClick={()=>{
                     setModalInfo({
                        title:"Confirmation",
                        accept_message: "YES",
                        cancel_message: "NO",
                        message: ("Save changes made to the service attributes?"),
                        accept: function(){
                            props.groupConfiguration.attributes = attributes;
                            props.updateAttributes(props.groupConfiguration)
                            setModalInfo({})},
                        cancel: function(){
                            props.fetchGroupConfiguration()
                            setModalInfo({})}
                    });
                }}>Save</Button>
                <Button variant={"tertiary"} className={""} onClick={()=>{props.fetchGroupConfiguration()}}>Cancel</Button>
                </div>
        </React.Fragment>

    );
    }
