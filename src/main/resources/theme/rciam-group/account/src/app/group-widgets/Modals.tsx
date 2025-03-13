import * as React from 'react';
import { Modal, ModalVariant, Button, Tooltip, Form, FormGroup, TextInput } from '@patternfly/react-core';
import {useEffect, useState} from 'react';
import { GroupsServiceClient, HttpResponse } from '../groups-mngnt-service/groups.service';
import { Loading } from './LoadingModal';
// import parse from '../../node_modules/react-html-parser';
import { Msg } from '../widgets/Msg';
import { getError } from '../js/utils.js';


interface ConfirmationModalProps {
    modalInfo: any;
};


export const ConfirmationModal: React.FC<ConfirmationModalProps> = (props) =>{
    
    useEffect(()=>{
        setIsModalOpen(Object.keys(props.modalInfo).length > 0);
    },[props.modalInfo])

    const [isModalOpen, setIsModalOpen] = useState(false);
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
                <Button key="confirm" variant={props?.modalInfo?.button_variant||"primary"} onClick={()=>{props?.modalInfo?.accept()}}>
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
          }
          else{
            setModalInfo({
                message:Msg.localize('deleteGroupError',[getError(response)]),
                accept_message: Msg.localize('OK'),
                accept: function(){
                    props.afterSuccess();
                    setModalInfo({})},
                cancel: function(){
                    props.afterSuccess();
                    setModalInfo({})}
              });
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



export const CreateGroupModal:React.FC<any> = (props) => {

    useEffect(()=>{
        setIsModalOpen(props.active);
        setGroupConfig(groupConfigDefault);
    },[props.active])

    const [isModalOpen, setIsModalOpen] = React.useState(false);
    let groupConfigDefault = {
        name: "",
        attributes: {
            description: [""]
        }
    }
    let groupsService = new GroupsServiceClient();
    const [modalInfo,setModalInfo] = useState({});
    const [loading,setLoading] = useState(false);
    const [groupConfig,setGroupConfig] = useState(groupConfigDefault);
    const [isValid,setIsValid]= useState(false);
    
    useEffect(()=>{
        setIsValid(groupConfig.name.length>0&&groupConfig.attributes.description[0].length>0);
      },[groupConfig]);

    const createGroup = () =>{
        setLoading(true);
        groupsService!.doPost<any>("/group-admin/group"+(props.groupId?("/"+props.groupId+"/children"):""),{...groupConfig})
        .then((response: HttpResponse<any>) => {
          setLoading(false);
          props.close()
          if(response.status===200||response.status===204||response.status===201){
            setModalInfo({
                title:(props.groupId?Msg.localize('createSubgroupSuccess'):Msg.localize('createGroupSuccess')),
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
          else{
            setModalInfo({
                title:(props.groupId?Msg.localize('createSubgroupError',[getError(response)]):Msg.localize('createGroupError',[getError(response)])),
                accept_message: (Msg.localize('OK')),
                accept: function(){
                  props.afterSuccess();
                  setModalInfo({})},
                cancel: function(){
                    props.afterSuccess();
                    setModalInfo({})}
              });
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
                title={props.groupId?Msg.localize('createSubgroup'):Msg.localize('createGroup')}
                isOpen={isModalOpen}
                onClose={()=>{props.close()}}
                actions={[
                    <Tooltip {...(!!isValid ? { trigger:'manual', isVisible:false }:{trigger:'mouseenter'})}
                        content={
                            <div>
                                <Msg msgKey='createGroupFormError' />
                            </div>
                        }
                    >
                    <div>
                        <Button key="confirm" variant="primary" isDisabled={!isValid} onClick={()=>{createGroup();}}>
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
                            value={groupConfig.name}
                            onChange={(value)=>{groupConfig.name=value; setGroupConfig({...groupConfig})}}
                            />
                        </FormGroup>
                        <FormGroup label="Description" isRequired fieldId="simple-form-desription-01">
                            <TextInput
                            isRequired
                            type="text"
                            id="simple-form-email-01"
                            name="simple-form-email-01"
                            value={groupConfig.attributes.description[0]}
                            onChange={(value)=>{ groupConfig.attributes.description[0]=value; setGroupConfig({...groupConfig})}}
                            />
                        </FormGroup>
                    </Form>
            </Modal>
        </React.Fragment>
    )
}


