import type {
  AccountEnvironment,
  LinkedAccountRepresentation,
} from "@keycloak/keycloak-account-ui";
import { unLinkAccount, useAccountAlerts } from "@keycloak/keycloak-account-ui";
import { IconMapper } from "@keycloak/keycloak-ui-shared";
import { useEnvironment } from "@keycloak/keycloak-account-ui";
import {
  Button,
  DataListAction,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Icon,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { LinkIcon, UnlinkIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";

type LinkedAccountWithUserId = LinkedAccountRepresentation & {
  linkedUserId?: string; // <-- your custom fork field
  linkedUsername?: string; // <-- keep for fallback
};

type AccountRowProps = {
  account: LinkedAccountWithUserId;
  isLinked?: boolean;
  canManageLinks: boolean;
  refresh: () => void;
};

export const AccountRow = ({
  account,
  isLinked = false,
  refresh,
  canManageLinks,
}: AccountRowProps) => {
  const { t } = useTranslation();

  // ✅ Use the Keycloak UI context provider (account-ui wraps your App)
  const context = useEnvironment<AccountEnvironment>();
  const { login } = context.keycloak;

  const { addAlert, addError } = useAccountAlerts();

  const unLink = async (acc: LinkedAccountRepresentation) => {
    try {
      await unLinkAccount(context, acc);
      addAlert(t("unLinkSuccess"));
      refresh();
    } catch (error) {
      addError("unLinkError", error);
    }
  };

  const idToShow = account.linkedUserId ?? account.linkedUsername ?? "";

  return (
    <DataListItem
      id={`${account.providerAlias}-idp`}
      key={account.providerName}
      aria-label={t("linkedAccounts")}
    >
      <DataListItemRow
        key={account.providerName}
        data-testid={`linked-accounts/${account.providerName}`}
      >
        <DataListItemCells
          dataListCells={[
            <DataListCell key="idp">
              <Split>
                <SplitItem className="pf-v5-u-mr-sm">
                  <IconMapper icon={account.providerName} />
                </SplitItem>
                <SplitItem className="pf-v5-u-my-xs" isFilled>
                  <span id={`${account.providerAlias}-idp-name`}>
                    {account.displayName}
                  </span>
                </SplitItem>
              </Split>
            </DataListCell>,
            <DataListCell key="username" width={5}>
              <Split>
                <SplitItem className="pf-v5-u-my-xs" isFilled>
                  <span id={`${account.providerAlias}-idp-username`}>
                    {idToShow}
                  </span>
                </SplitItem>
              </Split>
            </DataListCell>,
          ]}
        />
        {canManageLinks && (
          <DataListAction
            aria-labelledby={t("link")}
            aria-label={isLinked ? t("unLink") : t("link")}
            id={`${account.providerAlias}-idp-action`}
          >
            {isLinked ? (
              <Button
                id={`${account.providerAlias}-idp-unlink`}
                variant="link"
                onClick={() => unLink(account)}
              >
                <Icon size="sm">
                  <UnlinkIcon />
                </Icon>{" "}
                {t("unLink")}
              </Button>
            ) : (
              <Button
                id={`${account.providerAlias}-idp-link`}
                variant="link"
                onClick={async () => {
                  await login({
                    action: `idp_link:${account.providerAlias}`,
                  });
                }}
              >
                <Icon size="sm">
                  <LinkIcon />
                </Icon>{" "}
                {t("link")}
              </Button>
            )}
          </DataListAction>
        )}
      </DataListItemRow>
    </DataListItem>
  );
};
