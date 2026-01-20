import { Page, Spinner } from "@patternfly/react-core";
import style from "./App.module.css";
import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { PageNav } from "./PageNav";
import {
  AccountEnvironment,
  Header,
  useEnvironment,
} from "@keycloak/keycloak-account-ui";
import { GroupsServiceProvider } from "./groups-service/GroupsServiceContext";
import { LoaderProvider } from "./widgets/LoaderContext";
import { AlertProvider } from "@keycloak/keycloak-ui-shared";

function App() {
  useEnvironment<AccountEnvironment>();
  return (
    <LoaderProvider>
      <AlertProvider>
      <GroupsServiceProvider>
        <Page
          className={style.headerLogo}
          header={<Header />}
          sidebar={<PageNav />}
          isManagedSidebar
        >
          <Suspense fallback={<Spinner />}>
            <Outlet />
          </Suspense>
        </Page>
      </GroupsServiceProvider>
      </AlertProvider>
    </LoaderProvider>
  );
}

export default App;
