import * as React from 'react';
import {FC,useState,useEffect, useRef} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, Tooltip, DataListAction, SelectVariant, Checkbox,Select,SelectOption, FormAlert, Alert, Form, FormGroup, TextInput, Modal, ModalVariant, Switch, FormFieldGroupHeader, FormFieldGroup, DatePicker, Popover, NumberInput, HelperTextItem, Breadcrumb, BreadcrumbItem} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import {dateParse,formatDateToString,isFutureDate} from '../js/utils.js'
import { Loading } from './LoadingModal';
import { Msg } from '../widgets/Msg';
import { HelpIcon } from '@patternfly/react-icons';
// @ts-ignore
import { ContentPage } from '../../content/ContentPage';



export const GroupRolesTable: FC<any> = (props) => {
    const [selectedRoles,setSelectedRoles] = useState(props.selectedRoles);

    useEffect(()=>{
       props.setSelectedRoles(selectedRoles); 
    },[selectedRoles])

    let roleHandler = (role)=>{
      if(selectedRoles.includes(role)){
        const index = selectedRoles.indexOf(role);
        if (index > -1) { // only splice array when item is found
          selectedRoles.splice(index, 1); // 2nd parameter means remove one item only
        }
      }
      else{
        selectedRoles.push(role);
      }
      setSelectedRoles([...selectedRoles]);
    }




    return (
      <React.Fragment>
      
        <table className="gm_roles-table">
            <tbody>
                {props.groupRoles?.map((role,index)=>{
                    return <tr onClick={()=>{roleHandler(role);}}>
                        <td>
                            {role}
                        </td>
                        <td>
                        <Checkbox id="standalone-check" name="standlone-check" checked={selectedRoles.includes(role)} aria-label="Standalone input" />
                        </td>   
                    </tr>                    
                })}
            </tbody>
        </table>
          
      </React.Fragment>     
   
    )
}

