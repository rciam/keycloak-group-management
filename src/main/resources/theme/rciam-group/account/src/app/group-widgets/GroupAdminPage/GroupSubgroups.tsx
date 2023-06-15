import * as React from 'react';
import {FC,useState,useEffect,useRef} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, Tooltip, DataListAction, SelectVariant, Checkbox,Select,SelectOption, FormAlert, Alert, Modal, ModalVariant, Form, FormGroup, TextInput} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import {ValidateEmail} from '../../js/utils.js'
import { Loading } from '../LoadingModal';

export const GroupSubGroups: FC<any> = (props) => {

    const titleId = 'typeahead-select-id-1';
    let subgroupDefault = {
        name: "",
        attributes: {
            description: [""]
        }
    }
    let groupsService = new GroupsServiceClient();
    const [modalInfo,setModalInfo] = useState({});
    const [loading,setLoading] = useState(false);
    const [createModal,setCreateModal] = useState(false);
    const [subGroup,setSubGroup] = useState(subgroupDefault);
    const [validSubGroup,setValidSubGroup]= useState(false);
    
    useEffect(()=>{
        setValidSubGroup(subGroup.name.length>0&&subGroup.attributes.description[0].length>0);
      },[subGroup]);

    const createSubgroup = () =>{
        setLoading(true);
        groupsService!.doPost<any>("/group-admin/group/"+props.groupId+"/children",{...subGroup})
        .then((response: HttpResponse<any>) => {
          setLoading(false);
          if(response.status===200||response.status===204){
            setModalInfo({
                title:"Sub Group Created",
                accept_message: "OK",
                accept: function(){
                  setModalInfo({})},
                cancel: function(){
                  setModalInfo({})}
              });
            // setGroupMembers(response.data.results);
          }
        }).catch((err)=>{
          setLoading(false);
          console.log(err)})    
    }

  
    return (
      <React.Fragment>
        <Button onClick={()=>{setCreateModal(true);}}>Create Subgroup</Button>
        <Modal
            variant={ModalVariant.medium}
            title={"Create Subgroup"}
            isOpen={createModal}
            onClose={()=>{setCreateModal(false)}}
            actions={[
                <Tooltip {...(!!validSubGroup ? { trigger:'manual', isVisible:false }:{trigger:'mouseenter'})}
                    content={
                        <div>
                          Make sure you have provided all the required information
                        </div>
                    }
                >
                  <div>
                    <Button key="confirm" variant="primary" isDisabled={!validSubGroup} onClick={()=>{createSubgroup();}}>
                        Create
                    </Button>
                  </div>

                </Tooltip>
                ,
                
                <Button key="cancel" variant="link" onClick={()=>{setCreateModal(false);}}>
                    Cancel
                </Button>
                
            ]}
            >
                <Form>
                    <FormGroup
                        label="Group Name"
                        isRequired
                        fieldId="simple-form-name-01"
                        // helperText=""
                    >
                        <TextInput
                        isRequired
                        type="text"
                        id="simple-form-name-01"
                        name="simple-form-name-01"
                        aria-describedby="simple-form-name-01-helper"
                        value={subGroup.name}
                        onChange={(value)=>{console.log("This is the thing " + value); subGroup.name=value; setSubGroup({...subGroup})}}
                        />
                    </FormGroup>
                    <FormGroup label="Description" isRequired fieldId="simple-form-desription-01">
                        <TextInput
                        isRequired
                        type="text"
                        id="simple-form-email-01"
                        name="simple-form-email-01"
                        value={subGroup.attributes.description[0]}
                        onChange={(value)=>{console.log("This is the thing " + value); subGroup.attributes.description[0]=value; setSubGroup({...subGroup})}}
                        />
                    </FormGroup>
                </Form>
        </Modal>
        <Loading active={loading}/>
        <ConfirmationModal modalInfo={modalInfo}/>
      </React.Fragment> 
    )
}
