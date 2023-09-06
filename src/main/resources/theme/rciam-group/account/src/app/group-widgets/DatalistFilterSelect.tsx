import * as React from 'react';
import { BadgeToggle, Dropdown, DropdownItem, Modal, ModalVariant, Spinner } from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Msg } from '../widgets/Msg';
import { CheckIcon } from '@patternfly/react-icons';

export const DatalistFilterSelect: React.FC<any> = (props) => {
    const [isOpen, setIsOpen] = React.useState(false);
    const [selection,setSelection] = useState(props.default);
    const [initialRender,setInitialRender] = useState(true);
    const [options,setOptions] = useState(props.options)


    useEffect(()=>{
        if(initialRender){
            setInitialRender(false);
            return;
          }
        props.action(selection);
    },[selection]);

    const onToggle = (open: boolean) => {
        setIsOpen(open);
      };
  
      const onFocus = () => {
        const element = document.getElementById('toggle-badge-'+props.name);
        element?.focus();
      };
  
      const onSelect = () => {
        
        setIsOpen(false);
        onFocus();
      };
  
    return(
        <Dropdown
        onSelect={()=>{onSelect()}}
        toggle={
          <BadgeToggle id={"toggle-badge-"+props.name} onToggle={(e)=>{onToggle(e)}}>
            {selection?<Msg msgKey={selection} />:"all"}
          </BadgeToggle>
        }
        className="gm_badge_dropdown"
        isOpen={isOpen}
        dropdownItems={[
          <DropdownItem key="All" component="button" onClick={()=>{setSelection('')}} icon={!selection&&<CheckIcon />}>
                <Msg msgKey='All' />
          </DropdownItem>,
          
          ...(options.map((option)=>{
            return (
              <DropdownItem key={option} component="button" onClick={()=>{setSelection(option)}} icon={selection===option&&<CheckIcon />}>
                {props.optionsType==='raw'?option:<Msg msgKey={option} />}
              </DropdownItem>
            )
          }))
        ]}
      />
    )
  }