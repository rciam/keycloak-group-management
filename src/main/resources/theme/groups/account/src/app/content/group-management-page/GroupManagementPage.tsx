/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

export class GroupsManagementPage extends React.Component<Props, State> {

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
                  There are pending
            </Banner>
            <Card>
                <CardBody>
                    <EmptyState variant={EmptyStateVariant.small}>
                        <Title headingLevel="h4" size="lg">
                        Keycloak Man Loves JSX, React, and PatternFly ETC ----aaaa
                        </Title>
                        <EmptyStateBody>
                            Token is: {this.state.accessToken}
                        </EmptyStateBody>
                        <Title headingLevel="h4" size="lg">
                        But you can use whatever you want as long as you wrap it in a React Component.
                        </Title>
                    </EmptyState>
                </CardBody>
            </Card>
            </>
        );
    }
};