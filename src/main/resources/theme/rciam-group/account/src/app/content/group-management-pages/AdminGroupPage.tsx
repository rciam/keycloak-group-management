import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import { Tabs, Tab,TabTitleText,Breadcrumb, BreadcrumbItem, TextArea, Button} from '@patternfly/react-core';
// @ts-ignore
import { HttpResponse, GroupsServiceClient } from '../../groups-mngnt-service/groups.service';
import {GroupMembers} from '../../group-widgets/GroupAdminPage/GroupMembers';
//import { TableComposable, Caption, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import {GroupAttributes} from '../../group-widgets/GroupAdminPage/GroupAttributes';
import { GroupDetails } from '../../group-widgets/GroupAdminPage/GroupDetails';
import { ConfirmationModal, DeleteSubgroupModal } from '../../group-widgets/Modals';
import { GroupAdmins } from '../../group-widgets/GroupAdminPage/GroupAdmins';
import { GroupSubGroups } from '../../group-widgets/GroupAdminPage/GroupSubgroups';
import { GroupEnrollment } from '../../group-widgets/GroupAdminPage/GroupEnrollment';
import {TrashIcon } from '@patternfly/react-icons';


export interface AdminGroupPageProps {
  match:any;
  history:any;
}



interface Group {
  id: string;
  name: string;
  path: string;
  extraSubGroups: Group[];
}

interface AUP {
    id: string;
    type: string;
    url: string;
}

interface EnrollmentAttributes {
    id?: string;
    attribute: string;
    label: string;
    order: number;
    defaultValue: string;
    hidden: boolean;
    modifiable: boolean;
}

interface EnrollmentConfiration {
    id?: string;
    group: Group;
    name: string;
    active: boolean;
    requiredAupAcceptance: boolean;
    reaquireApproval: boolean;
    aupExpiryDays: number;
    membershipExpirationDays: number;
    aup: AUP;
    hideConfiguration: boolean;
    attributes: EnrollmentAttributes[];
    groupRoles: string[];
}
interface FederatedIdentity {
    identityProvider: string;
}

interface User {
    id?: string;
    username: string;
    emailVerified: boolean;
    email: string;
    federatedIdentities: FederatedIdentity[];
}

interface Admin {
    user: User;
    direct: boolean;
}



interface GroupConfiguration {
    id?: string;
    name: string;
    path: string;
    attributes: any;
    groupRoles: string[];
    enrollmentConfigurationList: EnrollmentConfiration[];
    status: string;
    membershipExpiresAt: string;
    validFrom: string;
    admins: Admin[];
    extraSubGroups: Group[];
  }






// export class GroupPage extends React.Component<GroupsPageProps, GroupsPageState> {
export const AdminGroupPage: FC<AdminGroupPageProps> = (props)=> {


  const [groupConfiguration,setGroupConfiguration] = useState({} as GroupConfiguration);
  const [groupId,setGroupId] = useState(props.match.params.id);
  const [activeTabKey, setActiveTabKey] = React.useState<string | number>(0);
  const [descriptionInput,setDescriptionInput] = useState<string>("");
  const [editDescription,setEditDescription] = useState<boolean>(false);
  const [user,setUser] = useState<User>({} as User);
  const [modalInfo,setModalInfo] = useState({});
  const [deleteGroup,setDeleteGroup] = useState(false);

  let groupsService = new GroupsServiceClient();
  useEffect(()=>{
    fetchUser();
    fetchGroupConfiguration();
  },[]);


  useEffect(()=>{
    setGroupId(props.match.params.id);
  },[props.match.params.id]);
  
  useEffect(()=>{
    fetchGroupConfiguration();
    setActiveTabKey(0);
  },[groupId]);

  

  const handleTabClick = (
    event: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent,
    tabIndex: string | number
  ) => {
    setActiveTabKey(tabIndex);
  };


  let fetchGroupConfiguration = ()=>{
    groupsService!.doGet<GroupConfiguration>("/group-admin/group/"+groupId+"/all")
    .then((response: HttpResponse<GroupConfiguration>) => {
      if(response.status===200&&response.data){
        if(response.data?.attributes?.description?.[0]!==descriptionInput){
          setDescriptionInput(response.data?.attributes?.description?.[0]);
        }
        setGroupConfiguration(response.data);
      }
    })
  }

  let updateAttributes = (groupConfiguration) =>{
    
    groupsService!.doPost<GroupConfiguration>("/group-admin/group/"+groupId+"/attributes",groupConfiguration?.attributes?{...groupConfiguration.attributes}:{})
    .then((response: HttpResponse<GroupConfiguration>) => {
      if(response.status===200||response.status===204){
        setGroupConfiguration({...groupConfiguration})
      }
      else{
        fetchGroupConfiguration();
      }
    })
  }

  let fetchUser= ()=>{
    groupsService!.doGet<User>("/whoami",{target:"base"})
      .then((response: HttpResponse<User>)=>{
        if(response.status===200&&response.data){
          setUser(response.data);
        }
      }).catch(err=>{
        console.log(err);
      })
  }

  return (
    <>
      <div className="gm_content">
        <ConfirmationModal modalInfo={modalInfo}/>
        <DeleteSubgroupModal groupId={groupId} active={deleteGroup} afterSuccess={()=>{props.history.push('/groups/admingroups');}} close={()=>{setDeleteGroup(false);}}/>  
        <Breadcrumb className="gm_breadcumb">
          <BreadcrumbItem to="#">
            Account Console
          </BreadcrumbItem>
          <BreadcrumbItem to="#/groups/admingroups">
            Manage Groups
          </BreadcrumbItem>
          <BreadcrumbItem isActive>
            {groupConfiguration?.name}
          </BreadcrumbItem>
        </Breadcrumb>
          <h1 className="pf-c-title pf-m-2xl pf-u-mb-xl gm_group-title">{groupConfiguration?.name} {("/"+groupConfiguration?.name)!==groupConfiguration?.path&&!(groupConfiguration?.extraSubGroups&&groupConfiguration?.extraSubGroups.length>0)&&<TrashIcon onClick={()=>{setDeleteGroup(true)}}/>}</h1>
          {editDescription?
            <div className="gm_description-input-container">
              <TextArea value={descriptionInput} onChange={value => setDescriptionInput(value)} aria-label="text area example" />
              <Button className={"gm_button-small"} 
                  onClick={()=>{
                    setModalInfo({
                      title:"Confirmation",
                      accept_message: "Yes",
                      cancel_message: "No",
                      message: ("Are you sure you want to update group's description?"),
                      accept: function(){if(groupConfiguration.attributes){
                        groupConfiguration.attributes.description = [descriptionInput];
                        updateAttributes(groupConfiguration);
                        setEditDescription(false);
                        setModalInfo({})
                      }},
                      cancel: function(){
                        setEditDescription(false);
                        setModalInfo({})}
                      });
                  }}
              >
                  <div className={"gm_check-button"}></div>
              </Button>
              <Button variant="tertiary" className={"gm_button-small"} onClick={()=>{setEditDescription(false);}}>
                  <div className={"gm_cancel-button"}></div>
              </Button>            
            </div>
            :<p className="gm_group_desc">
              {(groupConfiguration?.attributes?.description&&groupConfiguration?.attributes?.description[0])||"No descritption available."}
              <div className="gm_edit-icon" onClick={()=>{setEditDescription(true)}}></div>

            </p>
          }
          
          <Tabs
          className="gm_tabs"
          activeKey={activeTabKey}
          onSelect={handleTabClick}
          isBox={false}
          aria-label="Tabs in the default example"
          role="region"
          >
            <Tab eventKey={0} title={<TabTitleText>Group Details</TabTitleText>} aria-label="Default content - users">
              <GroupDetails groupConfiguration={groupConfiguration} groupId={groupId} setGroupConfiguration={setGroupConfiguration}/>
            </Tab>    
            
            <Tab eventKey={1} title={<TabTitleText>Group Members</TabTitleText>} aria-label="Default content - members">
              <GroupMembers groupConfiguration={groupConfiguration} groupId={groupId} user={user}/>
            </Tab>
            <Tab eventKey={2} title={<TabTitleText>Group Admins</TabTitleText>} aria-label="Default content - admins">
              <GroupAdmins groupId={groupId} user={user} groupConfiguration={groupConfiguration} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration}/>
            </Tab>
            <Tab eventKey={3} title={<TabTitleText>Group Enrollment Configuration</TabTitleText>} aria-label="Default content - attributes">   
              <GroupEnrollment groupConfiguration={groupConfiguration}  groupId={groupId} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration} updateAttributes={updateAttributes}/>
            </Tab>   
            <Tab eventKey={4} title={<TabTitleText>Group Attributes</TabTitleText>} aria-label="Default content - attributes">   
              <GroupAttributes groupConfiguration={groupConfiguration} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration} updateAttributes={updateAttributes}/>
            </Tab>
            <Tab eventKey={5} title={<TabTitleText>Sub Groups</TabTitleText>} aria-label="Default content - attributes">   
              <GroupSubGroups groupConfiguration={groupConfiguration} groupId={groupId} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration} updateAttributes={updateAttributes}/>
            </Tab>
            
          </Tabs>
      </div>
    </>  
  )
};
