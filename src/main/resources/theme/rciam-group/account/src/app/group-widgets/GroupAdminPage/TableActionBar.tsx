import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import {Button, TextInput, InputGroup, Tooltip} from '@patternfly/react-core';
import { CreateGroupModal } from '../Modals';
// @ts-ignore


export const TableActionBar: FC<any> = (props) => {

    useEffect(()=>{
        console.log(props.createButton);
    },[props.createButton])
    const [searchString,setSearchString] = useState<string>("");
    const [createGroupModalOpen,setCreateGroupModalOpen] = useState<boolean>(false);


    return(
        <div className="gm_search-input-container">
            {props.createButton&&
            <div className="gm_search-input-action">
                <CreateGroupModal active={createGroupModalOpen} afterSuccess={()=>{
                    props.afterCreate();}} close={()=>{setCreateGroupModalOpen(false);}}/>
                <Button onClick={()=>{setCreateGroupModalOpen(true);}}>
                    Create Group
                </Button>
            </div>}
            
            <InputGroup className="gm_search-input">
                <TextInput
                name="searchInput"
                id="searchInput1"
                type="text"
                value={searchString}
                onChange={(e)=>{setSearchString(e)}}
                placeholder="Search..."
                aria-label="Search Input"
                onKeyDown={(e)=>{e.key=== 'Enter'&&props.search(searchString);}}
                />
                <Tooltip content={<div>{props.searchText}</div>}>
                    <Button variant="control" aria-label="popover for input" onClick={()=>{props.search(searchString);}}>
                        <div className='gm_search-icon-container'></div>
                    </Button>
                </Tooltip>
                <Tooltip content={<div>{props.cancelText}</div>}>
                    <Button variant="control" aria-label="popover for input" onClick={()=>{
                        setSearchString('');
                        props.cancel(searchString);}}>
                        <div className='gm_cancel-icon-container'></div>
                    </Button>
                </Tooltip>
            </InputGroup>
            {props.childComponent||""}
        </div>


    )
}



