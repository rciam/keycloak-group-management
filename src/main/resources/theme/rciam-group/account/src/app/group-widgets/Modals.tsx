import * as React from 'react';
import { Modal, ModalVariant, Button, Tooltip, Form, FormGroup, TextInput } from '@patternfly/react-core';
import {useEffect, useState} from 'react';
import { GroupsServiceClient, HttpResponse } from '../groups-mngnt-service/groups.service';
import { Loading } from './LoadingModal';
// import parse from '../../node_modules/react-html-parser';
import { Msg } from '../widgets/Msg';


interface ConfirmationModalProps {
    modalInfo: any;
};


export const ConfirmationModal: React.FC<ConfirmationModalProps> = (props) =>{
    
    useEffect(()=>{
        setIsModalOpen(Object.keys(props.modalInfo).length > 0);
    },[props.modalInfo])

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [type,setType] = useState('small');
    const handleModalToggle = () => {
        props?.modalInfo?.cancel();
    };
    
    return (
        <React.Fragment>
            <Modal
            variant={props.modalInfo?.variant==='medium'?ModalVariant.medium:ModalVariant.small}
            title={props?.modalInfo?.title}
            isOpen={isModalOpen}
            onClose={handleModalToggle}
            actions={[
                <Button key="confirm" variant="primary" onClick={()=>{props?.modalInfo?.accept()}}>
                    {props?.modalInfo?.accept_message}
                </Button>,
                props?.modalInfo?.cancel_message&&
                    <Button key="cancel" variant="link" onClick={()=>{props?.modalInfo?.cancel();}}>
                        {props?.modalInfo?.cancel_message}
                    </Button>
                
            ]}
            >
                {props?.modalInfo?.message&&props?.modalInfo?.message}
            </Modal>
        </React.Fragment>   );
}



export const DeleteSubgroupModal:React.FC<any> = (props) => {
    const [loading,setLoading] = useState(false);
    let groupsService = new GroupsServiceClient();
    const [modalInfo,setModalInfo] = useState({});

    useEffect(()=>{
        if(props.active){
            setModalInfo({
                message:(Msg.localize('deleteGroupConfirmation')),
                accept_message: (Msg.localize('YES')),
                cancel_message: (Msg.localize('NO')),
                accept: function(){
                    setModalInfo({});
                    deleteGroup();},
                cancel: function(){
                  setModalInfo({});
                  props.close();
                }
            });
        }
    },[props.active])

    const deleteGroup = () =>{
        setLoading(true);
        groupsService!.doDelete<any>("/group-admin/group/"+props.groupId)
        .then((response: HttpResponse<any>) => {
          setLoading(false);
          props.close();
          if(response.status===200||response.status===204){
            setModalInfo({
                message:Msg.localize('deleteGroupSuccess'),
                accept_message: Msg.localize('OK'),
                accept: function(){
                    props.afterSuccess();
                    setModalInfo({})},
                cancel: function(){
                    props.afterSuccess();
                    setModalInfo({})}
              });
            // setGroupMembers(response.data.results);
          }
        }).catch((err)=>{
            props.close();
            setLoading(false);
            console.log(err)})    
    }


    return (
        <React.Fragment>
            <Loading active={loading}/>
            <ConfirmationModal modalInfo={modalInfo}/>
        </React.Fragment>
    )
}



export const CreateSubgroupModal:React.FC<any> = (props) => {

    useEffect(()=>{
        setIsModalOpen(props.active);
        setSubGroup(subgroupDefault);
    },[props.active])

    const [isModalOpen, setIsModalOpen] = React.useState(false);
    let subgroupDefault = {
        name: "",
        attributes: {
            description: [""]
        }
    }
    let groupsService = new GroupsServiceClient();
    const [modalInfo,setModalInfo] = useState({});
    const [loading,setLoading] = useState(false);
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
          props.close()
          if(response.status===200||response.status===204){
            setModalInfo({
                title:(Msg.localize('createSubgroupSuccess')),
                accept_message: (Msg.localize('OK')),
                accept: function(){
                  props.afterSuccess();
                  setModalInfo({})},
                cancel: function(){
                    props.afterSuccess();
                    setModalInfo({})}
              });
            // setGroupMembers(response.data.results);
          }
        }).catch((err)=>{
            props.close()
          setLoading(false);
          console.log(err)})    
    }



    return (
        <React.Fragment>
            <Loading active={loading}/>
            <ConfirmationModal modalInfo={modalInfo}/>
            <Modal
                variant={ModalVariant.medium}
                title={Msg.localize('createSubgroup')}
                isOpen={isModalOpen}
                onClose={()=>{props.close()}}
                actions={[
                    <Tooltip {...(!!validSubGroup ? { trigger:'manual', isVisible:false }:{trigger:'mouseenter'})}
                        content={
                            <div>
                                <Msg msgKey='createSubgroupFormError' />
                            </div>
                        }
                    >
                    <div>
                        <Button key="confirm" variant="primary" isDisabled={!validSubGroup} onClick={()=>{createSubgroup();}}>
                            <Msg msgKey='Create' />
                        </Button>
                    </div>

                    </Tooltip>
                    ,
                    
                    <Button key="cancel" variant="link" onClick={()=>{props.close()}}>
                        <Msg msgKey='Cancel' />
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
                            onChange={(value)=>{subGroup.name=value; setSubGroup({...subGroup})}}
                            />
                        </FormGroup>
                        <FormGroup label="Description" isRequired fieldId="simple-form-desription-01">
                            <TextInput
                            isRequired
                            type="text"
                            id="simple-form-email-01"
                            name="simple-form-email-01"
                            value={subGroup.attributes.description[0]}
                            onChange={(value)=>{ subGroup.attributes.description[0]=value; setSubGroup({...subGroup})}}
                            />
                        </FormGroup>
                    </Form>
            </Modal>
        </React.Fragment>
    )
}


