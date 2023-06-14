import * as React from 'react';
import {FC,useState,useRef} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, TextInput, InputGroup, Chip, Tooltip} from '@patternfly/react-core';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import { GroupsServiceClient, HttpResponse } from '../../groups-mngnt-service/groups.service';
import {MinusIcon,PlusIcon } from '@patternfly/react-icons';


export const GroupDetails: FC<any> = (props) => {
    let groupsService = new GroupsServiceClient();
    const roleRef= useRef<any>(null);
    const [roleInput, setRoleInput] = useState<string>("");
    const [modalInfo,setModalInfo] = useState({});


    const addGroupRole = (role) =>{  
        groupsService!.doPost<any>("/group-admin/group/"+props.groupId+"/roles",{},{params:{name:role}})
        .then((response: HttpResponse<any>) => {
          if(response.status===200||response.status===204){
            props.groupConfiguration.groupRoles.push(role);
            props.setGroupConfiguration({...props.groupConfiguration});
            setRoleInput("");
            setModalInfo({});
          }
        })      
    }

    const removeGroupRole = (role) => {
        groupsService!.doDelete<any>("/group-admin/group/"+props.groupId+"/role/"+role)
        .then((response: HttpResponse<any>) => {
          if(response.status===200||response.status===204){
            const index = props.groupConfiguration.groupRoles.indexOf(role);
            if (index > -1) { // only splice array when item is found
                props.groupConfiguration.groupRoles.splice(index, 1); // 2nd parameter means remove one item only
            }
            props.setGroupConfiguration({...props.groupConfiguration});
            
          }
          setModalInfo({});
        }).catch(err=>{
            setModalInfo({});
        })    
        
    }


    return(
        <React.Fragment>

        <ConfirmationModal modalInfo={modalInfo}/>
            <DataList aria-label="Compact data list example" isCompact>
                <DataListItem aria-labelledby="compact-item1">
                    <DataListItemRow>
                        <DataListItemCells
                            dataListCells={[
                            <DataListCell key="primary content">
                                <span id="compact-item1"><strong>Path</strong></span>
                            </DataListCell>,
                            <DataListCell width={3} key="secondary content ">
                                <span>{props.groupConfiguration?.path}</span>  
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
                                    <span id="compact-item1"><strong>Group Roles</strong></span>
                                </DataListCell>,
                                <DataListCell width={3} key="roles">
                                <table className="gm_roles-table">
                                <thead>
                                    <tr>
                                    <th>
                                        <InputGroup>
                                            <TextInput id="textInput-basic-1" value={roleInput} placeholder='Add new role' onChange={(e)=>{setRoleInput(e.trim());}} onKeyDown={(e)=>{e.key=== 'Enter'&&roleRef?.current?.click(); }} type="email" aria-label="email input field" />
                                        </InputGroup>
                                    </th>
                                    <th>
                                        <Tooltip content={<div>Add Role</div>}>

                                            <Button ref={roleRef} onClick={()=>{
                                                if(props.groupConfiguration?.groupRoles.includes(roleInput)){
                                                    setModalInfo({
                                                    title:"Invalid Role",
                                                    accept_message: "OK",
                                                    message: ("The role ("+ roleInput + ") cannot be added because it already exists in this group."),
                                                    accept: function(){setModalInfo({})},
                                                    cancel: function(){setModalInfo({})}
                                                    });
                                                }
                                                else{
                                                    setModalInfo({
                                                        title:"Confirmation",
                                                        accept_message: "Yes",
                                                        cancel_message: "No",
                                                        message: ("Are you sure you want to add the role "+ roleInput + " to this group."),
                                                        accept: function(){addGroupRole(roleInput)},
                                                        cancel: function(){setModalInfo({})}
                                                        });
                                                }
                                            }}><PlusIcon/></Button>
                                        </Tooltip>
                                    </th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {props.groupConfiguration?.groupRoles?.map((role,index)=>{
                                        return <tr>
                                            <td>
                                                {role}
                                            </td>
                                            <td>
                                                <Tooltip content={<div>Remove Role</div>}>
                                                    <Button variant="danger" onClick={()=>{
                                                        setModalInfo({
                                                            title:"Confirmation",
                                                            accept_message: "Yes",
                                                            cancel_message: "No",
                                                            message: ("Are you sure you want to remove the role " + role + " from this group."),
                                                            accept: function(){removeGroupRole(role)},
                                                            cancel: function(){setModalInfo({})}
                                                        });
                                                    }}>
                                                        <MinusIcon/>
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

