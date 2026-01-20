import * as React from 'react';
import {FC,useState,useEffect} from 'react';
import {  DataList,DataListItem,DataListItemCells,DataListItemRow,DataListCell, Button, Tooltip, DataListAction} from '@patternfly/react-core';
import { GroupListItem } from '../AdminGroupsPage';
import { CreateGroupModal } from '../components/Modals';
import { useGroupsService } from '../../groups-service/GroupsServiceContext';
import { useTranslation } from 'react-i18next';

interface AdminGroup{
  id? : string;
  name: string;
  path: string;
  extraSubGroups: AdminGroup[];
}

export const GroupSubGroups: FC<any> = (props) => {
  
  const [createSubgroup,setCreateSubgroup] = useState(false);
  const [userRoles,setUserRoles] = useState<String[]>([]);
  const groupsService = useGroupsService();
  const { t } = useTranslation();

  useEffect(()=>{
    setUserRoles(groupsService.getUserRoles());
  },[])

  const emptyGroup= ()=>{
    return (
      <DataListItem key='emptyItem' aria-labelledby="empty-item">
        <DataListItemRow key='emptyRow'>
          <DataListItemCells dataListCells={[
            <DataListCell key='empty'><strong>{t('adminGroupSubgroupNo')}</strong></DataListCell>
          ]} />
        </DataListItemRow>
      </DataListItem>
    )
  }


    return (
      <React.Fragment>
        <CreateGroupModal groupId={props.groupId} active={createSubgroup} afterSuccess={()=>{
          props.fetchGroupConfiguration();}} close={()=>{setCreateSubgroup(false);}}/> 
         <DataList id="groups-list" aria-label={t('groupLabel')} isCompact wrapModifier={"breakWord"}>
            
            <DataListItem id="groups-list-header" aria-labelledby="Columns names">
            
              <DataListItemRow className="gm_datalist-header">
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key='group-name-header' width={2}>
                      <strong>{t('Name')}</strong>
                    </DataListCell>,
                    <DataListCell key='group-path-header' width={2}>
                      <strong>{t('Path')}</strong>
                    </DataListCell>
                  ]}
                />
                <DataListAction
                      className="gm_cell-center"
                      aria-labelledby="check-action-item1 check-action-action2"
                      id="check-action-action1"
                      aria-label="Actions"
                      isPlainButtonAction
                >
                  <Tooltip content={<div>{t('createSubgroup')}</div>}>
                    <Button className={"gm_plus-button-small"} onClick={()=>{setCreateSubgroup(true)}}>
                        <div className={"gm_plus-button"}></div>
                    </Button>
                  </Tooltip>
                </DataListAction>
                  
              </DataListItemRow>
            </DataListItem>
            {props.groupConfiguration?.extraSubGroups&&props.groupConfiguration?.extraSubGroups.length>0 ?
              props.groupConfiguration?.extraSubGroups.map((group:AdminGroup,appIndex:number)=>{
                return(
                <GroupListItem  group={group as AdminGroup} isGroupAdmin={props.isGroupAdmin} userRoles={userRoles} fetchAdminGroups={props.fetchGroupConfiguration} appIndex={appIndex} depth={0} />
                )
              }):
              emptyGroup()
              }
          </DataList>
      </React.Fragment> 
    )
}
