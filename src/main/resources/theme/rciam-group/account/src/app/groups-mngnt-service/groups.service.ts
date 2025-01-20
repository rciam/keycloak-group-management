

// @ts-ignore
import { KeycloakClient, KeycloakService } from '../keycloak-service/keycloak.service';
// @ts-ignore
import { ContentAlert } from '../content/ContentAlert';


declare const keycloak: KeycloakClient;
const keycloakService = new KeycloakService(keycloak);

declare const baseUrl: string;

type ConfigResolve = (config: RequestInit) => void;

export interface HttpResponse<T = {}> extends Response {
    data?: T;
}

export interface RequestInitWithParams extends RequestInit {
    params?: {[name: string]: string | number};
    target?: any; 
}

export class GroupsServiceError extends Error {
    constructor(public response: HttpResponse<any>) {
        super(response.statusText);
    }
}


export class GroupsServiceClient {
    private kcSvc: KeycloakService;
    private groupsUrl: string;
    private baseUrl: string;
    private sshKeysUrl: string;
    //TODO: UPDATE the groupsUrl value in the constructor to match the base path of the extension's REST endpoints!!!
    public constructor() {
        this.kcSvc = keycloakService;
        this.groupsUrl = this.kcSvc.authServerUrl() + 'realms/' + this.kcSvc.realm() + '/agm/account';
        this.sshKeysUrl= this.kcSvc.authServerUrl() + 'realms/' + this.kcSvc.realm() + '/account';
        this.baseUrl = this.kcSvc.authServerUrl() + 'admin/' + this.kcSvc.realm() + '/console';
    }

    public getUserRoles = ()=> {
        let userRoles = [];
        try{
            userRoles = this.kcSvc.keycloakAuth.resourceAccess.account.roles;
        }
        catch(err){
            console.log(err);
        }
        return userRoles;
    }

    public getBaseUrl(){
        return this.kcSvc.authServerUrl() + 'realms/' + this.kcSvc.realm();
    }

    public async doGet<T>(endpoint: string,
                          config?: RequestInitWithParams): Promise<HttpResponse<T>> {
        return this.doRequest(endpoint, {...config, method: 'get'});
    }

    public async doDelete<T>(endpoint: string,
                            config?: RequestInitWithParams): Promise<HttpResponse<T>> {
        return this.doRequest(endpoint, {...config, method: 'delete'});
    }


    public async doPost<T>(endpoint: string,
                          body: string | {},
                          config?: RequestInitWithParams): Promise<HttpResponse<T>> {
        return this.doRequest(endpoint, {...config, body: JSON.stringify(body), method: 'post'});
    }

    public async doPut<T>(endpoint: string,
                         body: string | {},
                         config?: RequestInitWithParams): Promise<HttpResponse<T>> {
        return this.doRequest(endpoint, {...config, body: JSON.stringify(body), method: 'put'});
    }

    public async doRequest<T>(endpoint: string,
                              config?: RequestInitWithParams): Promise<HttpResponse<T>> {
        const url = this.makeUrl(endpoint, config);
        const requestConfig = await this.makeConfig(config);
        const rawResponse = await fetch(url.toString(), requestConfig);
        const response: HttpResponse<T> = rawResponse as HttpResponse<T>;
        try {
            response.data = await response.json();
        } catch (e) {
            response.data = undefined; // Handle empty or invalid JSON
        } // ignore.  Might be empty

        return response;
    }

    private handleError(response: HttpResponse<any>): void {
        if (response.status === 401) {
            if (this.kcSvc.authenticated() && !this.kcSvc.audiencePresent()) {
                window.location.href = baseUrl + '#/forbidden';
            } else {
                this.kcSvc.login();
            }
        }

        if (response.status === 403) {
            window.location.href = baseUrl + '#/forbidden';
        }

        if (response.data) {
            if (response.data['errors']) {
                for (const err of response.data['errors']) {
                    ContentAlert.danger(err['errorMessage'], err['params']);
                }
            } else {
                ContentAlert.danger(
                    `${response.statusText}: ${response.data['errorMessage'] ? response.data['errorMessage'] : ''} ${response.data['error'] ? response.data['error'] : ''}`
                );
            }
        } else {
            ContentAlert.danger(response.statusText);
        }
    }

    private makeUrl(endpoint: string, config?: RequestInitWithParams): URL {
        if (endpoint.startsWith('http')) return new URL(endpoint);
        const url = new URL((config?.target==='base'?this.baseUrl:config?.target==='sshKeys'?this.sshKeysUrl:this.groupsUrl) + endpoint);
        // add request params
        if (config && config.hasOwnProperty('params')) {
            const params: {[name: string]: string} = config.params as {} || {};
            Object.keys(params).forEach(key => url.searchParams.append(key, params[key]))
        }

        return url;
    }

    private makeConfig(config: RequestInit = {}): Promise<RequestInit> {
        return new Promise( (resolve: ConfigResolve) => {
            this.kcSvc.getToken()
                .then( (token: string) => {
                    resolve( {
                        ...config,
                        headers: {'Content-Type': 'application/json',
                                 ...config.headers,
                                  Authorization: 'Bearer ' + token}
                    });
                }).catch(() => {
                    this.kcSvc.login();
                });
        });
    }

}

window.addEventListener("unhandledrejection", (event: PromiseRejectionEvent) => {
    event.promise.catch(error => {
        if (error instanceof GroupsServiceError) {
            // We already handled the error. Ignore unhandled rejection.
            event.preventDefault();
        }
    });
});
