import { AccountEnvironment } from "@keycloak/keycloak-account-ui";
import { getInjectedEnvironment } from "@keycloak/keycloak-ui-shared";

export const environment = getInjectedEnvironment<AccountEnvironment>();


export type ExtendedFeatures = AccountEnvironment["features"] & {
  manageAccountLinkAllowed?: boolean;
  manageAccountBasicAuthAllowed?: boolean;
  manageAccount2faAllowed?: boolean;
};

export type AccountEnvironmentExtended = Omit<AccountEnvironment, "features"> & {
  features: ExtendedFeatures;
};
