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
import { Msg } from '../../widgets/Msg';
import { RoutableTabs, useRoutableTab } from '../../widgets/RoutableTabs';


export interface AdminGroupPageProps {
  match:any;
  history:any;
  location:any;
}

type AdminGroupPageTabs = "details" | "members" | "admins" | "enrollments" | "attributes" | "subgroups";

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
    visibleToNotMembers: boolean;
    attributes: EnrollmentAttributes[];
    groupRoles: string[];
}
interface FederatedIdentity {
    identityProvider: string;
}

interface User {
    userId?: string;
    username: string;
    emailVerified: boolean;
    email: string;
    federatedIdentities: FederatedIdentity[];
}

interface UserGroupConfig {
  id?: string;
  username: string;
  emailVerified: boolean;
  email: string;
  federatedIdentities: FederatedIdentity[];
}

interface Admin {
    user: UserGroupConfig;
    direct: boolean;
}



interface GroupConfiguration {
    id?: string;
    name: string;
    path: string;
    attributes: any;
    groupRoles: any;
    enrollmentConfigurationList: EnrollmentConfiration[];
    status: string;
    membershipExpiresAt: string;
    validFrom: string;
    admins: Admin[];
    parents: any;
    extraSubGroups: Group[];
  }






// export class GroupPage extends React.Component<GroupsPageProps, GroupsPageState> {
export const AdminGroupPage: FC<AdminGroupPageProps> = (props)=> {

  // Get a specific query parameter
  // const myParam = new URLSearchParams(props.location.search).get('myParam');

  const [groupConfiguration,setGroupConfiguration] = useState({} as GroupConfiguration);
  const [groupId,setGroupId] = useState(props.match.params.id);
  const [activeTabKey, setActiveTabKey] = React.useState<string | number>(0);
  const [descriptionInput,setDescriptionInput] = useState<string>("");
  const [editDescription,setEditDescription] = useState<boolean>(false);
  const [user,setUser] = useState<User>({} as User);
  const [modalInfo,setModalInfo] = useState({});
  const [deleteGroup,setDeleteGroup] = useState(false);
  const [defaultConfiguration,setDefaultConfiguration] = useState("");
  const [initialRender,setInitialRender] = useState(true);
  const [userRoles,setUserRoles] = useState<String[]>([]);
  const [isGroupAdmin,setIsGroupAdmin] = useState<boolean>(false);

  let groupsService = new GroupsServiceClient();
  useEffect(()=>{
    fetchUser();
    fetchGroupConfiguration();
    setUserRoles(groupsService.getUserRoles());
  },[]);

  const useTab = (tab: AdminGroupPageTabs) => useRoutableTab(tab);

  const detailsTab = useTab("details");
  const membersTab = useTab("members");
  const attributesTab = useTab("attributes");
  const adminsTab = useTab("admins");
  const subgroupsTab = useTab("subgroups");
  const enrollmentsTab = useTab("enrollments");

  useEffect(()=>{
    setGroupId(props.match.params.id);
  },[props.match.params.id]);
  
  useEffect(()=>{
    if(initialRender){
      setInitialRender(false);
      return;
    }
    fetchGroupConfiguration();
    setActiveTabKey(0);
  },[groupId]);

  useEffect(()=>{    
    let isAdmin = false; 
    if(groupConfiguration?.admins?.length>0){
      groupConfiguration.admins.forEach((admin)=>{
        if(admin.user.id===user.userId){
          isAdmin = true;
        }
      });
    }
    setIsGroupAdmin(isAdmin);
  },[groupConfiguration,user])

  let fetchGroupConfiguration = ()=>{
    groupsService!.doGet<GroupConfiguration>("/group-admin/group/"+groupId+"/all")
    .then((response: HttpResponse<GroupConfiguration>) => {
      if(response.status===200&&response.data){
        if(response.data?.attributes?.description?.[0]!==descriptionInput){
          setDescriptionInput(response.data?.attributes?.description?.[0]);
        }
        if(response.data?.attributes?.defaultConfiguration?.[0]!==defaultConfiguration){
          setDefaultConfiguration(response.data?.attributes?.defaultConfiguration?.[0]);
        }
        setGroupConfiguration(response.data);
      }
    })
  }

  let updateAttributes = (attributes) =>{
    groupsService!.doPost<GroupConfiguration>("/group-admin/group/"+groupId+"/attributes",attributes?{...attributes}:{})
    .then((response: HttpResponse<GroupConfiguration>) => {
      if(response.status===200||response.status===204){
        setGroupConfiguration({...groupConfiguration});
        fetchGroupConfiguration();
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
          let user = response.data;           
            setUser(user);
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
            <Msg msgKey='accountConsole' />
          </BreadcrumbItem>
          <BreadcrumbItem to="#/groups/admingroups">
            <Msg msgKey='adminGroupLabel' />
          </BreadcrumbItem>
          {groupConfiguration?.parents?.map((group,index)=>{
            return (
              <BreadcrumbItem to={"#/groups/admingroups/"+group.id}>
                {group.name}
              </BreadcrumbItem>
            )
          })}
          <BreadcrumbItem isActive>
            {groupConfiguration?.name}
          </BreadcrumbItem>
        </Breadcrumb>
          <h1 className="pf-c-title pf-m-2xl pf-u-mb-xl gm_group-title gm_flex-center">{groupConfiguration?.name} {(isGroupAdmin||("/"+groupConfiguration?.name)!==groupConfiguration?.path) &&!(groupConfiguration?.extraSubGroups&&groupConfiguration?.extraSubGroups.length>0)&&<TrashIcon onClick={()=>{setDeleteGroup(true)}}/>}</h1>
          {editDescription?
            <div className="gm_description-input-container">
              <TextArea value={descriptionInput} onChange={value => setDescriptionInput(value)} aria-label="text area example" />
              <Button className={"gm_button-small"} 
                  onClick={()=>{
                    setModalInfo({
                      title:Msg.localize('confirmation'),
                      accept_message: Msg.localize('yes'),
                      cancel_message: Msg.localize('no'),
                      message: (Msg.localize('descriptionUpdateConfirmation')),
                      accept: function(){if(groupConfiguration.attributes){
                        groupConfiguration.attributes.description = [descriptionInput];
                        updateAttributes(groupConfiguration.attributes);
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
              {(groupConfiguration?.attributes?.description&&groupConfiguration?.attributes?.description[0])||Msg.localize('noDescription')}
              <div className="gm_edit-icon" onClick={()=>{setEditDescription(true)}}></div>

            </p>
          }
          <RoutableTabs className="gm_tabs"
            isBox={false}
            defaultTab={"details"}
          >                      
            <Tab {...detailsTab} id="details" title={<TabTitleText><Msg msgKey='adminGroupDetailsTab' /></TabTitleText>} aria-label="Default content - users">
              <GroupDetails groupConfiguration={groupConfiguration} groupId={groupId} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration}/>
            </Tab>    
            
            <Tab {...membersTab} id="members" title={<TabTitleText><Msg msgKey='adminGroupMembersTab' /></TabTitleText>} aria-label="Default content - members">
              <GroupMembers isGroupAdmin={isGroupAdmin} history={props.history} groupConfiguration={groupConfiguration} groupId={groupId} user={user}/>
            </Tab>
            <Tab {...adminsTab} id="admins" title={<TabTitleText><Msg msgKey='adminGroupAdminsTab' /></TabTitleText>} aria-label="Default content - admins">
              <GroupAdmins isGroupAdmin={isGroupAdmin} groupId={groupId} user={user} groupConfiguration={groupConfiguration} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration}/>
            </Tab>
            <Tab {...enrollmentsTab} id="enrollments" title={<TabTitleText><Msg msgKey='adminGroupEnrollmentTab' /></TabTitleText>} aria-label="Default content - attributes">   
              <GroupEnrollment isGroupAdmin={isGroupAdmin} groupConfiguration={groupConfiguration} defaultConfiguration={defaultConfiguration}  groupId={groupId} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration} updateAttributes={updateAttributes}/>
            </Tab>   
            <Tab {...attributesTab} id="attributes" title={<TabTitleText><Msg msgKey='adminGroupAttributesTab' /></TabTitleText>} aria-label="Default content - attributes">   
              <GroupAttributes isGroupAdmin={isGroupAdmin} groupConfiguration={groupConfiguration} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration} updateAttributes={updateAttributes}/>
            </Tab>
            <Tab {...subgroupsTab} id="subgroups" title={<TabTitleText><Msg msgKey='adminGroupSubgroupsTab' /></TabTitleText>} aria-label="Default content - attributes">   
              <GroupSubGroups isGroupAdmin={isGroupAdmin} groupConfiguration={groupConfiguration} groupId={groupId} setGroupConfiguration={setGroupConfiguration} fetchGroupConfiguration={fetchGroupConfiguration} />
            </Tab>
          </RoutableTabs>
      </div>
    </>  
  )
};
