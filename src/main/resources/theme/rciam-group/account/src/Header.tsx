import { KeycloakMasthead, label } from "@keycloak/keycloak-ui-shared";
import { useEnvironment } from "@keycloak/keycloak-account-ui";
import { useTranslation } from "react-i18next";
import style from "./header.module.css";
import { useEffect, useState } from "react";
import { useGroupsService } from "./groups-service/GroupsServiceContext";
import { HttpResponse } from "./groups-service/groups-service";
import { AccountEnvironmentExtended } from "./environment";
import { Button } from "@patternfly/react-core";
import { ExternalLinkSquareAltIcon } from "@patternfly/react-icons";
import { isLocalUrl, joinPath } from "./js/utils";

const ReferrerLink = () => {
  const { t } = useTranslation();
  const { environment } = useEnvironment<AccountEnvironmentExtended>();

  return environment.referrerUrl ? (
    <Button
      data-testid="referrer-link"
      component="a"
      href={environment.referrerUrl.replace("_hash_", "#")}
      variant="link"
      icon={<ExternalLinkSquareAltIcon />}
      iconPosition="right"
      isInline
    >
      {t("backTo", {
        app: label(t, environment.referrerName, environment.referrerUrl),
      })}
    </Button>
  ) : null;
};

export const Header = () => {
  const { environment, keycloak } =
    useEnvironment<AccountEnvironmentExtended>();
  const [logo, setLogo] = useState<string>();
  const { t } = useTranslation();
  let defaultLogo = environment.resourceUrl + "/additional/logo.png";
  useEffect(() => {
    getThemeConfig();
  }, []);

  const groupsService = useGroupsService();

  let getThemeConfig = () => {
    groupsService!
      .doGet<any>("/theme-info/theme-config", { target: "base_realm" })
      .then((response: HttpResponse<any>) => {
        if (response.status === 200 && response.data) {
          if (response.data?.projectLogoIconUrl?.[0]) {
            let logoUrl = response.data.projectLogoIconUrl[0];
            setLogo(
              isLocalUrl(logoUrl)
                ? joinPath(environment.resourceUrl, logoUrl)
                : logoUrl,
            );
          }
        } else {
          setLogo(defaultLogo);
        }
      })
      .catch(() => {
        setLogo(defaultLogo);
      });
  };

  return (
    <KeycloakMasthead
      data-testid="page-header"
      keycloak={keycloak}
      features={{ hasManageAccount: false }}
      brand={{
        href: environment.baseUrl,
        src: logo,
        alt: t("logo"),
        className: style.brand,
      }}
      toolbarItems={[<ReferrerLink key="link" />]}
    />
  );
};
