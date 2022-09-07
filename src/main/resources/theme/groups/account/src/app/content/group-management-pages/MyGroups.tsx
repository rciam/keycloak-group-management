
import * as React from 'react';

// @ts-ignore
import { KeycloakClient, KeycloakService } from '../../keycloak-service/keycloak.service';
// @ts-ignore
import { KeycloakContext } from '../../keycloak-service/KeycloakContext';

import { Button } from '@patternfly/react-core';

import { CustomTableComposable } from '../../group-widgets/CustomTableComposable';


declare const keycloak: KeycloakClient;
const keycloakService = new KeycloakService(keycloak);

interface State {
  //accessToken: any
}


interface Props {

}

export class MyGroups extends React.Component<Props, State> {

    constructor(props : Props){
        super(props);
        /*
        this.state = {
            accessToken: null
        };
        */
    }

    public componentDidMount(): void {

    }

/*
    private testServices(){

        //get token
        keycloakService.getToken()
            .then( (token: any) => {
                this.setState({accessToken: token });
            })
            .catch((err: any) => {
                console.log("Error: ", err);
            });

    }
*/

    public render(): React.ReactNode {

        return (
            <>
              <CustomTableComposable></CustomTableComposable>
            </>
        );
    }
};
