// src/services/groups-service.ts
import type { AccountEnvironment } from "@keycloak/keycloak-account-ui";
import type Keycloak from "keycloak-js";

/**
 * The context object we expect from useEnvironment().
 * In v26, useEnvironment() returns { environment, keycloak }.
 */
export interface AccountContext {
  environment: AccountEnvironment;
  keycloak: Keycloak;
}

export interface HttpResponse<T = unknown> extends Response {
  data?: T;
}

export interface RequestInitWithParams extends RequestInit {
  params?: Record<string, string | number>;
  /** if target === "base_account", send to /account instead of /agm/account */
  target?: "base_account";
}

export class GroupsServiceError<T = unknown> extends Error {
  constructor(public response: HttpResponse<T>) {
    super(response.statusText || "GroupsServiceError");
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

export class GroupsServiceClient {
  private readonly ctx: AccountContext;
  private readonly baseRealmUrl: string;
  private readonly groupsUrl: string;
  private readonly baseAccountUrl: string;

  constructor(context: AccountContext) {
    this.ctx = context;

    const { serverBaseUrl, realm } = this.ctx.environment;
    const base = serverBaseUrl.replace(/\/$/, "");

    this.baseRealmUrl = `${base}/realms/${realm}`;
    this.groupsUrl = `${this.baseRealmUrl}/agm/account`;
    this.baseAccountUrl = `${this.baseRealmUrl}/account`;
  }

  // --- Helpers ---

  /** Roles from the 'account' client, if present */
  public getUserRoles(): string[] {
    try {
      const ra = this.ctx.keycloak.resourceAccess || {};
      return ra["account"]?.roles ?? [];
    } catch (err) {
      console.warn("GroupsService: error reading user roles", err);
      return [];
    }
  }

  public getBaseUrl(): string {
    return this.baseRealmUrl;
  }

  public async doGet<T>(
    endpoint: string,
    config?: RequestInitWithParams
  ): Promise<HttpResponse<T>> {
    return this.doRequest<T>(endpoint, { ...config, method: "GET" });
  }

  public async doDelete<T>(
    endpoint: string,
    config?: RequestInitWithParams
  ): Promise<HttpResponse<T>> {
    return this.doRequest<T>(endpoint, { ...config, method: "DELETE" });
  }

  public async doPost<T>(
    endpoint: string,
    body: unknown,
    config?: RequestInitWithParams
  ): Promise<HttpResponse<T>> {
    return this.doRequest<T>(endpoint, {
      ...config,
      method: "POST",
      body: JSON.stringify(body),
    });
  }

  public async doPut<T>(
    endpoint: string,
    body: unknown,
    config?: RequestInitWithParams
  ): Promise<HttpResponse<T>> {
    return this.doRequest<T>(endpoint, {
      ...config,
      method: "PUT",
      body: JSON.stringify(body),
    });
  }

  /**
   * Core request method. It:
   *  - builds the URL (agm/account vs /account)
   *  - attaches the Bearer token
   *  - parses JSON into response.data
   *  - throws GroupsServiceError on non-2xx
   */
  public async doRequest<T>(
    endpoint: string,
    config: RequestInitWithParams = {}
  ): Promise<HttpResponse<T>> {
    const url = this.makeUrl(endpoint, config);
    const requestConfig = await this.makeConfig(config);

    const rawResponse = await fetch(url.toString(), requestConfig);
    const response = rawResponse as HttpResponse<T>;

    let data: T | undefined = undefined;
    try {
      // might be empty body
      data = (await response.json()) as T;
    } catch {
      // ignore parse errors – treat as no JSON
    }
    response.data = data;

    if (!response.ok) {
      // Let callers handle the error – we just wrap it.
      throw new GroupsServiceError<T>(response);
    }

    return response;
  }

  // --- internals ---

  private makeUrl(endpoint: string, config?: RequestInitWithParams): URL {
    if (endpoint.startsWith("http")) {
      return new URL(endpoint);
    }

    const base =
      config?.target === "base_account" ? this.baseAccountUrl : this.groupsUrl;

    const url = new URL(base + endpoint);

    if (config?.params) {
      Object.entries(config.params).forEach(([key, value]) =>
        url.searchParams.append(key, String(value))
      );
    }

    return url;
  }

  private async makeConfig(
    init: RequestInitWithParams = {}
  ): Promise<RequestInit> {
    const kc = this.ctx.keycloak;

    // Ensure the token is fresh enough (e.g. 30 seconds)
    await kc.updateToken(30).catch(() => kc.login());

    const token = kc.token;
    if (!token) {
      // login() above will redirect; this is mostly to satisfy TS
      throw new Error("No Keycloak token available");
    }

    return {
      ...init,
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        ...(init.headers || {}),
        Authorization: `Bearer ${token}`,
      },
    };
  }
}
