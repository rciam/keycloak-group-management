import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import { Checkbox} from '@patternfly/react-core';

export const GroupRolesTable: FC<any> = (props) => {
    const [selectedRoles,setSelectedRoles] = useState(props.selectedRoles);

    useEffect(()=>{
       props.setSelectedRoles(selectedRoles); 
    },[selectedRoles])

    useEffect(()=>{
      setSelectedRoles(props.selectedRoles);
    },[props.selectedRoles]);

    let roleHandler = (role: string)=>{
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
                {props.groupRoles?.map((role: string)=>{
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

