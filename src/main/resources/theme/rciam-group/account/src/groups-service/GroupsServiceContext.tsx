// src/services/GroupsServiceContext.tsx
import React, { createContext, useContext, useMemo } from "react";
import { useEnvironment, AccountEnvironment } from "@keycloak/keycloak-account-ui";
import { GroupsServiceClient, AccountContext } from "./groups-service";

export const GroupsServiceContext = createContext<GroupsServiceClient | undefined>(
  undefined
);

export const GroupsServiceProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  // v26: useEnvironment returns { environment, keycloak }
  const ctx = useEnvironment<AccountEnvironment>() as AccountContext;

  const client = useMemo(() => new GroupsServiceClient(ctx), [ctx]);

  return (
    <GroupsServiceContext.Provider value={client}>
      {children}
    </GroupsServiceContext.Provider>
  );
};

export const useGroupsService = (): GroupsServiceClient => {
  const svc = useContext(GroupsServiceContext);
  if (!svc) {
    throw new Error("useGroupsService must be used within a GroupsServiceProvider");
  }
  return svc;
};
