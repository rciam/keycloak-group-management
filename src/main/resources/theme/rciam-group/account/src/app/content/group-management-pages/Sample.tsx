
import * as React from 'react';

// @ts-ignore
import { KeycloakClient, KeycloakService } from '../../keycloak-service/keycloak.service';
// @ts-ignore
import { KeycloakContext } from '../../keycloak-service/KeycloakContext';

import {
    Card,
    CardBody,
    EmptyState,
    EmptyStateBody,
    EmptyStateVariant,
    Grid,
    GridItem,
    Title,
    Banner
} from '@patternfly/react-core';

import { CustomTableComposable } from '../../group-widgets/CustomTableComposable';

import { AccordionSample } from '../../group-widgets/AccordionSample';

declare const resourceUrl: string;

declare const keycloak: KeycloakClient;
const keycloakService = new KeycloakService(keycloak);

declare let isReactLoading: boolean;

interface State {
  accessToken: any,
  s: any
}


interface Props {
  value: string,
  children: React.ReactNode
}

export class Sample extends React.Component<Props, State> {

    constructor(props : Props){
        super(props);
        this.state = {
            accessToken: null,
            s: null
        };
    }

    public componentDidMount(): void {
        isReactLoading = false;
        console.log("componentDidMount");
        this.testServices();
    }


    private testServices(){

        //get token
        keycloakService.getToken()
            .then( (token: any) => {
                console.log("AccessToken: ", token);
                this.setState({accessToken: token });
            })
            .catch((err: any) => {
                console.log("Error: ", err);
            });

    }


    public render(): React.ReactNode {
        console.log("rendering GroupsManagementPage");
        return (
            <>
            <Banner variant="info">
                  There are pending requests
            </Banner>

            <CustomTableComposable></CustomTableComposable>
            <AccordionSample></AccordionSample>

            </>
        );
    }
};
