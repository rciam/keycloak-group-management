import * as React from 'react';
import { GroupsServiceClient } from './groups.service';

export const GroupsServiceContext = React.createContext<GroupsServiceClient | undefined>(undefined);