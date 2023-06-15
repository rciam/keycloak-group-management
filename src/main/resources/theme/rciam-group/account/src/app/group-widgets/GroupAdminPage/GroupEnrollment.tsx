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



    let groupsService = new GroupsServiceClient();
    useEffect(()=>{
      fetchGroupEnrollments();
    },[]);

    useEffect(()=>{
      fetchGroupEnrollments();
    },[props.groupId]);

    let fetchGroupEnrollments = ()=>{
      groupsService!.doGet<any>("/group-admin/group/"+props.groupId+"/configuration/all")
      .then((response: HttpResponse<any>) => {
        if(response.status===200&&response.data){
          setGroupEnrollments(response.data);
        }
      })
    }

    const noGroupEnrollments= ()=>{
        return (
          <DataListItem key='emptyItem' aria-labelledby="empty-item">
            <DataListItemRow key='emptyRow'>
              <DataListItemCells dataListCells={[
                <DataListCell key='empty'><strong>No group enrollments found</strong></DataListCell>
              ]} />
            </DataListItemRow>
          </DataListItem>
        )
      }
    
  

  
  
    return (
      <React.Fragment>
        <ConfirmationModal modalInfo={modalInfo}/>
        <DataList aria-label="Group Member Datalist" isCompact>
            <DataListItem aria-labelledby="compact-item1">
              <DataListItemRow>
                <DataListItemCells dataListCells={[
                  <DataListCell className="gm_vertical_center_cell" width={3} key="id-hd">
                    <strong>Name</strong>
                  </DataListCell>,
                  <DataListCell className="gm_vertical_center_cell" width={3} key="username-hd">
                    <strong>Status</strong>
                  </DataListCell>,
                  <DataListCell className="gm_vertical_center_cell" width={3} key="email-hd">
                    <strong>Aup</strong>
                  </DataListCell>,
                ]}>
                </DataListItemCells>
              </DataListItemRow>
            </DataListItem>
            {groupEnrollments.length>0?groupEnrollments.map((enrollment,index)=>{
              return <DataListItem aria-labelledby={"enrollment-"+index}>
                <DataListItemRow>
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell width={3} key="primary content">
                        {enrollment.name||"Not Available"}
                      </DataListCell>,
                      <DataListCell width={3} className={enrollment.active?"gm_group-enrollment-active":"gm_group-enrollment-inactive"} key="secondary content ">
                        <strong>{enrollment.active?"Active":"Inactive"}</strong>
                      </DataListCell>,
                      <DataListCell width={3} key="secondary content ">
                        {enrollment?.aup?.url?<a href={enrollment?.aup?.url} target="_blank" rel="noreferrer">link <ExternalLinkAltIcon/> </a>:"Not Available"}
                      </DataListCell>,                    
                    ]}
                  />
                  
                </DataListItemRow>
              </DataListItem>
            }):noGroupEnrollments()}
          </DataList>
               
        </React.Fragment>         
   
    )
  }

