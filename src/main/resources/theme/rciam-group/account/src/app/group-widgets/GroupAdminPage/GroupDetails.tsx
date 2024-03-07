import * as React from 'react';
import {FC,useState,useRef} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, TextInput, InputGroup, Chip, Tooltip} from '@patternfly/react-core';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import { GroupsServiceClient, HttpResponse } from '../../groups-mngnt-service/groups.service';
import {MinusIcon,PlusIcon } from '@patternfly/react-icons';
import { Msg } from '../../widgets/Msg';
import { Alerts } from '../../widgets/Alerts';


export const GroupDetails: FC<any> = (props) => {
    let groupsService = new GroupsServiceClient();
    const roleRef= useRef<any>(null);
    const [roleInput, setRoleInput] = useState<string>("");
    const [modalInfo,setModalInfo] = useState({});
    const [alert,setAlert] = useState({});

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
            setAlert({message:Msg.localize('deleteRoleErrorTitle'),variant:"danger",description:Msg.localize('deleteRoleErrorMessage')});
            setModalInfo({});
        })    
        
    }


    return(
        <React.Fragment>
        <Alerts alert={alert} close={()=>{setAlert({})}}/>
        <ConfirmationModal modalInfo={modalInfo}/>
            <DataList aria-label="Compact data list example" isCompact wrapModifier={"breakWord"}>
                <DataListItem aria-labelledby="compact-item1">
                    <DataListItemRow>
                        <DataListItemCells
                            dataListCells={[
                            <DataListCell key="primary content">
                                <span id="compact-item1"><strong><Msg msgKey='Path' /></strong></span>
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
                                    <span id="compact-item1"><strong><Msg msgKey='adminGroupRoles' /></strong></span>
                                </DataListCell>,
                                <DataListCell width={3} key="roles">
                                <table className="gm_roles-table">
                                <thead>
                                    <tr>
                                    <th>
                                        <InputGroup>
                                            <TextInput id="textInput-basic-1" value={roleInput} placeholder={Msg.localize('adminGroupRolesAddPlaceholder')} onChange={(e)=>{setRoleInput(e.trim());}} onKeyDown={(e)=>{e.key=== 'Enter'&&roleRef?.current?.click(); }} type="email" aria-label="email input field" />
                                        </InputGroup>
                                    </th>
                                    <th>
                                        <Tooltip content={<div><Msg msgKey='adminGroupRolesAdd' /></div>}>

                                            <Button ref={roleRef} onClick={()=>{
                                                if(props.groupConfiguration?.groupRoles.includes(roleInput)){
                                                    setModalInfo({
                                                    title:(Msg.localize('adminGroupRoleExistsTitle')),
                                                    accept_message: Msg.localize('OK'),
                                                    message: (Msg.localize('adminGroupRoleExistsMessage1')+" ("+ roleInput + ") "+Msg.localize('adminGroupRoleExistsMessage2')),
                                                    accept: function(){setModalInfo({})},
                                                    cancel: function(){setModalInfo({})}
                                                    });
                                                }
                                                else{
                                                    setModalInfo({
                                                        title:(Msg.localize('Confirmation')),
                                                        accept_message: (Msg.localize('Yes')),
                                                        cancel_message: Msg.localize('No'),
                                                        message: (Msg.localize('adminGroupRoleAddConfirmation1')+" "+ roleInput + Msg.localize('adminGroupRoleAddConfirmation2')),
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
                                                <Tooltip content={<div><Msg msgKey='adminGroupRoleRemove' /></div>}>
                                                    <Button variant="danger" onClick={()=>{
                                                        setModalInfo({
                                                            title:(Msg.localize('Confirmation')),
                                                            accept_message: (Msg.localize('Yes')),
                                                            cancel_message: (Msg.localize('No')),
                                                            message: (Msg.localize('adminGroupRoleRemoveConfirmation1')+" " + role + " "+Msg.localize('adminGroupRoleRemoveConfirmation2')),
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

