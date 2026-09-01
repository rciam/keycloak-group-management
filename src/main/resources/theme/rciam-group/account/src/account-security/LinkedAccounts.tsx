import {
  Page,
  EmptyRow,
  usePromise,
  getLinkedAccounts,
} from "@keycloak/keycloak-account-ui";
import type { LinkedAccountRepresentation } from "@keycloak/keycloak-account-ui";
import { useEnvironment } from "@keycloak/keycloak-account-ui";
import { DataList, Stack, StackItem, Title } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { AlertVariant } from "@patternfly/react-core";
import { LinkedAccountsToolbar } from "./LinkedAccountsToolbar";
import { AccountRow } from "./AccountRow";
import { AccountEnvironmentExtended } from "../environment";

type PaginationParams = {
  first: number;
  max: number;
};
type LinkedAccountQueryParams = PaginationParams & {
  search?: string;
  linked?: boolean;
};
export const LinkedAccounts = () => {
  const { t } = useTranslation();
  const context = useEnvironment<AccountEnvironmentExtended>();
  const { addAlert } = useAlerts();
  const [linkedAccounts, setLinkedAccounts] = useState<
    LinkedAccountRepresentation[]
  >([]);
  const [unlinkedAccounts, setUnlinkedAccounts] = useState<
    LinkedAccountRepresentation[]
  >([]);

  // ✅ Typed params include `search?: string`
  const [paramsLinked, setParamsLinked] = useState<LinkedAccountQueryParams>({
    first: 0,
    max: 11,
    linked: true,
    search: "",
  });

  const [paramsUnlinked, setParamsUnlinked] =
    useState<LinkedAccountQueryParams>({
      first: 0,
      max: 11,
      linked: false,
      search: "",
    });

  const [key, setKey] = useState(0);
  const refresh = () => setKey((k) => k + 1);
  const canManageLinks =
    context.keycloak.resourceAccess?.account?.roles?.includes(
      "manage-account-links",
    ) ?? false;

  usePromise(
    async (signal) => {
      try {
        return await getLinkedAccounts({ signal, context }, paramsLinked);
      } catch {
        if (signal.aborted) {
          return [];
        }
        console.log("this iss");
        addAlert(t("failedToFetchLinkedAccounts"), AlertVariant.danger);

        return [];
      }
    },
    setLinkedAccounts,
    [paramsLinked, key],
  );

  usePromise(
    async (signal) => {
      if (!canManageLinks) {
        return [];
      }
      try {
        return await getLinkedAccounts({ signal, context }, paramsUnlinked);
      } catch {
        if (signal.aborted) {
          return [];
        }
        addAlert(t("failedToFetchUnlinkedAccounts"), AlertVariant.danger);
        return [];
      }
    },
    setUnlinkedAccounts,
    [canManageLinks, paramsUnlinked, key],
  );

  return (
    <Page
      title={t("linkedAccounts")}
      description={t("linkedAccountsIntroMessage")}
    >
      <Stack hasGutter>
        <StackItem>
          <Title headingLevel="h2" className="pf-v5-u-mb-lg" size="xl">
            {t("linkedLoginProviders")}
          </Title>
          <LinkedAccountsToolbar
            onFilter={(search) =>
              setParamsLinked({ ...paramsLinked, first: 0, search })
            }
            count={linkedAccounts.length}
            first={paramsLinked.first}
            max={paramsLinked.max}
            onNextClick={() =>
              setParamsLinked({
                ...paramsLinked,
                first: paramsLinked.first + paramsLinked.max - 1,
              })
            }
            onPreviousClick={() =>
              setParamsLinked({
                ...paramsLinked,
                first: paramsLinked.first - paramsLinked.max + 1,
              })
            }
            // IMPORTANT: toolbar currently calls onPerPageSelect(first, max)
            onPerPageSelect={(first, max) =>
              setParamsLinked({ ...paramsLinked, first, max })
            }
            hasNext={linkedAccounts.length > paramsLinked.max - 1}
          />

          <DataList id="linked-idps" aria-label={t("linkedLoginProviders")}>
            {linkedAccounts.length > 0 ? (
              linkedAccounts.map(
                (account, index) =>
                  index !== paramsLinked.max - 1 && (
                    <AccountRow
                      key={account.providerName}
                      account={account}
                      isLinked
                      canManageLinks={canManageLinks}
                      refresh={refresh}
                    />
                  ),
              )
            ) : (
              <EmptyRow message={t("linkedEmpty")} />
            )}
          </DataList>
        </StackItem>

        {canManageLinks && (
          <StackItem>
            <Title
              headingLevel="h2"
              className="pf-v5-u-mt-xl pf-v5-u-mb-lg"
              size="xl"
            >
              {t("unlinkedLoginProviders")}
            </Title>

            <LinkedAccountsToolbar
              onFilter={(search) =>
                setParamsUnlinked({ ...paramsUnlinked, first: 0, search })
              }
              count={unlinkedAccounts.length}
              first={paramsUnlinked.first}
              max={paramsUnlinked.max}
              onNextClick={() =>
                setParamsUnlinked({
                  ...paramsUnlinked,
                  first: paramsUnlinked.first + paramsUnlinked.max - 1,
                })
              }
              onPreviousClick={() =>
                setParamsUnlinked({
                  ...paramsUnlinked,
                  first: paramsUnlinked.first - paramsUnlinked.max + 1,
                })
              }
              onPerPageSelect={(first, max) =>
                setParamsUnlinked({ ...paramsUnlinked, first, max })
              }
              hasNext={unlinkedAccounts.length > paramsUnlinked.max - 1}
            />

            <DataList
              id="unlinked-idps"
              aria-label={t("unlinkedLoginProviders")}
            >
              {unlinkedAccounts.length > 0 ? (
                unlinkedAccounts.map(
                  (account, index) =>
                    index !== paramsUnlinked.max - 1 && (
                      <AccountRow
                        key={account.providerName}
                        account={account}
                        canManageLinks={canManageLinks}
                        refresh={refresh}
                      />
                    ),
                )
              ) : (
                <EmptyRow message={t("unlinkedEmpty")} />
              )}
            </DataList>
          </StackItem>
        )}
      </Stack>
    </Page>
  );
};

export default LinkedAccounts;
