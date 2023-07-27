import * as React from 'react';
import {FC,useState,useEffect,useRef} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, Tooltip, DataListAction, Pagination, InputGroup, TextInput, Dropdown, BadgeToggle, DropdownItem, Badge, Modal, Checkbox} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
// @ts-ignore
import { ConfirmationModal } from '../Modals';
import { SearchInput } from './SearchInput';
import {ExternalLinkAltIcon } from '@patternfly/react-icons';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '
import { Msg } from '../../widgets/Msg';
import { EnrollmentModal } from '../GroupEnrollment/EnrollmentModal';
import { Link } from 'react-router-dom';
import {isIntegerOrNumericString} from '../../js/utils.js'


interface FederatedIdentity {
    identityProvider: string;
}

interface User {
    id?: string;
    username: string;
    emailVerified: boolean;
    email: string;
    federatedIdentities: FederatedIdentity[];
    firstName: string;
    lastName: string;
    attributes: any;
}

interface Memberships {
    id?: string;
    user: User;
    status: string;
    membershipExpiresAt: string;
    groupRoles: string[];
  }



export const GroupEnrollment: FC<any> = (props) => {

    const [modalInfo,setModalInfo] = useState({});
    const [groupEnrollments,setGroupEnrollments] = useState<any>([]);
    const [enrollmentModal,setEnrollmentModal] = useState({});
    const [enrollmentRules, setEnrollmentRules] = useState({});
    const [defaultEnrollmentConfiguration,setDefaultEnrollmentConfiguration] = useState({
      group: {id:""},
      membershipExpirationDays : 3,
      name: "",
      active: true,
      requireApproval: true,
      aup: {
          type: "URL",
          url: ""
      },
      requireApprovalForExtension:false,
      visibleToNotMembers: false,
      validFrom: null,
      commentsNeeded:true,
      commentsLabel: Msg.localize('enrollmentConfigurationCommentsDefaultLabel'),
      commentsDescription: Msg.localize('enrollmentConfigurationCommentsDefaultDescription'),
      groupRoles : []
    })

    let groupsService = new GroupsServiceClient();
    


    useEffect(()=>{
      if(Object.keys(props.groupConfiguration).length !== 0){
        fetchGroupEnrollmentRules();
      }
    },[props.groupConfiguration])

    useEffect(()=>{
      if(props.groupId){
        fetchGroupEnrollments();
      }
    },[props.groupId]);

    let fetchGroupEnrollments = ()=>{
      groupsService!.doGet<any>("/group-admin/group/"+props.groupId+"/configuration/all")
      .then((response: HttpResponse<any>) => {
        if(response.status===200&&response.data){
          setGroupEnrollments(response.data);
        }
      })
    }

    let fetchGroupEnrollmentRules = ()=>{
      groupsService!.doGet<any>("/group-admin/configuration-rules",{params:{type:(("/"+props.groupConfiguration?.name)!==props.groupConfiguration?.path?'SUBGROUP':'TOP_LEVEL')}})
      .then((response: HttpResponse<any>) => {
        if(response.status===200&&response.data){
          if(response.data.length>0){
            let rules = {};
            response.data.forEach(field_rules=>{
              rules[field_rules.field] = {
                "max": parseInt(field_rules.max),
                "required": field_rules.required,
                ...(field_rules.defaultValue&&{"defaultValue":field_rules.defaultValue}) 
              }
              if(field_rules.defaultValue){
                  if(isIntegerOrNumericString(field_rules.defaultValue)){
                    defaultEnrollmentConfiguration[field_rules.field] = parseInt(field_rules.defaultValue); 
                  }
                  else{
                    defaultEnrollmentConfiguration[field_rules.field] = field_rules.defaultValue; 

                  }
              }
            })
            setDefaultEnrollmentConfiguration({...defaultEnrollmentConfiguration});
            setEnrollmentRules(rules);
          }
          else{
            setEnrollmentRules({});
          }
        }
      })
    }

    const noGroupEnrollments= ()=>{
        return (
          <DataListItem key='emptyItem' aria-labelledby="empty-item">
            <DataListItemRow key='emptyRow'>
              <DataListItemCells dataListCells={[
                <DataListCell key='empty'><strong><Msg msgKey='adminGroupNoEnrollments' /></strong></DataListCell>
              ]} />
            </DataListItemRow>
          </DataListItem>
        )
      }
    
  

  
  
    return (
      <React.Fragment>
        <ConfirmationModal modalInfo={modalInfo}/>
        <EnrollmentModal enrollment={enrollmentModal} validationRules={enrollmentRules}  groupRoles={props.groupConfiguration.groupRoles} close={()=>{setEnrollmentModal({}); fetchGroupEnrollments();}} groupId={props.groupId}/>
        <DataList aria-label="Group Member Datalist" isCompact>
            <DataListItem aria-labelledby="compact-item1">
              <DataListItemRow>
                <DataListItemCells dataListCells={[
                  <DataListCell className="gm_vertical_center_cell" width={3} key="id-hd">
                    <strong><Msg msgKey='Name' /></strong>
                  </DataListCell>,
                  <DataListCell className="gm_vertical_center_cell" width={3} key="username-hd">
                    <strong><Msg msgKey='Status' /></strong>
                  </DataListCell>,
                  <DataListCell className="gm_vertical_center_cell" width={3} key="email-hd">
                    <strong><Msg msgKey='Aup' /></strong>
                  </DataListCell>,
                ]}>
                </DataListItemCells>
                <DataListAction
                      className="gm_cell-center"
                      aria-labelledby="check-action-item1 check-action-action2"
                      id="check-action-action1"
                      aria-label="Actions"
                      isPlainButtonAction
                >
                  <Tooltip content={<div><Msg msgKey='createEnrollmentButton'/></div>}>
                    <Button className={"gm_plus-button-small"} onClick={()=>{defaultEnrollmentConfiguration.group.id=props.groupId;  setEnrollmentModal(defaultEnrollmentConfiguration);}}>
                        <div className={"gm_plus-button"}></div>
                    </Button>
                  </Tooltip>
                </DataListAction>
              </DataListItemRow>
            </DataListItem>
            {groupEnrollments.length>0?groupEnrollments.map((enrollment,index)=>{
              return <DataListItem aria-labelledby={"enrollment-"+index}>
                <DataListItemRow>
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell width={3} key="primary content" onClick={()=>{                        
                        enrollment?.aup?.id && delete enrollment.aup.id;
                        if(!enrollment.validFrom){
                          enrollment.validFrom=null;
                        }
                        if(!enrollment.aup){
                          enrollment.aup=  {
                            type: "URL",
                            url: ""
                          }
                        }
                        setEnrollmentModal(enrollment)}}>
                        <Link to={"/groups/admingroups/"+props.groupId}>{enrollment.name||Msg.localize('notAvailable')}</Link>
                      </DataListCell>,
                      <DataListCell width={3} className={enrollment.active?"gm_group-enrollment-active":"gm_group-enrollment-inactive"} key="secondary content ">
                        <strong>{enrollment.active?Msg.localize('Active'):Msg.localize('Inactive')}</strong>
                      </DataListCell>,
                      <DataListCell width={3} key="secondary content ">
                        {enrollment?.aup?.url?<a href={enrollment?.aup?.url} target="_blank" rel="noreferrer">link <ExternalLinkAltIcon/> </a>:Msg.localize('notAvailable')}
                      </DataListCell>,                    
                    ]}
                  />
                  <DataListAction
                    className="gm_cell-center"
                    aria-labelledby="check-action-item1 check-action-action2"
                    id="check-action-action1"
                    aria-label="Actions"
                    isPlainButtonAction
                  ><div className="gm_cell-placeholder"></div></DataListAction>
                </DataListItemRow>
              </DataListItem>
            }):noGroupEnrollments()}
          </DataList>
               
        </React.Fragment>         
   
    )
  }


