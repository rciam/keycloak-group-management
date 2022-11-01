# UI TODOs

In the file **src/main/resources/theme/groups/account/src/app/groups-mngnt-service/groups.service.ts** update the **groupsUrl** value in the constructor to match the base path of the extension's REST endpoints!!!

Use the **groups.service.ts** to perform requests (GET,POST,PUT,DELETE) to the extension's REST endpoints (src/main/java -> org.keycloak.plugins.groups.services.* classes) 
See the **GroupSelect.tsx** file, function **fetchData()** for an example of how to call the service.