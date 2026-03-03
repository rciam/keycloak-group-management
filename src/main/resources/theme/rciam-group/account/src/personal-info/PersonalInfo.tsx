import {
  useEnvironment,
  usePromise,
  getPersonalInfo,
  getSupportedLocales,
  savePersonalInfo,
  Page,
  useAccountAlerts,
} from "@keycloak/keycloak-account-ui";
import {
  ActionGroup,
  Alert,
  AlertVariant,
  Button,
  ExpandableSection,
  Form,
  Spinner,
} from "@patternfly/react-core";
import { ExternalLinkSquareAltIcon } from "@patternfly/react-icons";
import type { ErrorOption } from "react-hook-form";
import { useForm } from "react-hook-form";
import type { TFunction } from "i18next";
import { useTranslation } from "react-i18next";
import { useState } from "react";

import { AccountEnvironmentExtended } from "../environment";
import { i18n } from "../i18n";
import {
  beerify,
  debeerify,
  setUserProfileServerError,
  UserFormFields,
  UserProfileFields,
} from "@keycloak/keycloak-ui-shared";

export interface UserProfileAttributeMetadata {
  name: string;
  displayName: string;
  required: boolean;
  readOnly: boolean;
  annotations?: { [index: string]: any };
  validators: { [index: string]: { [index: string]: any } };
  multivalued: boolean;
  defaultValue: string;
}

export interface UserProfileMetadata {
  attributes: UserProfileAttributeMetadata[];
}

export const PersonalInfo = () => {
  const { t } = useTranslation();
  const context = useEnvironment<AccountEnvironmentExtended>();

  const [userProfileMetadata, setUserProfileMetadata] =
    useState<UserProfileMetadata>();
  const [supportedLocales, setSupportedLocales] = useState<string[]>([]);

  // Form typed as UserFormFields – that's what UserProfileFields expects
  const form = useForm<UserFormFields>({ mode: "onChange" });
  const { handleSubmit, reset, setValue, setError } = form;
  const { addAlert } = useAccountAlerts();

  usePromise(
    (signal) =>
      Promise.all([
        getPersonalInfo({ signal, context }),
        getSupportedLocales({ signal, context }),
      ]),
    ([personalInfo, locales]) => {
      const filteredUserProfileMetadata: UserProfileMetadata = {
        ...personalInfo.userProfileMetadata,
        attributes: personalInfo.userProfileMetadata.attributes
          .filter((attr: any) => attr.name !== "username")
          .map((attr: any) =>
            attr.validators?.["up-no-editable-attribute"]
              ? { ...attr, readOnly: true }
              : attr,
          ),
      };

      setUserProfileMetadata(filteredUserProfileMetadata);
      setSupportedLocales(locales);
      // personalInfo is coming from the account API; shape is compatible with UserFormFields
      reset(personalInfo as UserFormFields);

      Object.entries(personalInfo.attributes || {}).forEach(([k, v]) =>
        setValue(
          // attribute keys are stored beerified in the form
          `attributes[${beerify(k)}]` as keyof UserFormFields,
          v as any,
        ),
      );
    },
    [],
  );

  const onSubmit = async (user: UserFormFields) => {
    try {
      const attributes = Object.fromEntries(
        Object.entries(user.attributes || {}).map(([k, v]) => [
          debeerify(k),
          v,
        ]),
      );

      // savePersonalInfo expects a UserRepresentation from account-ui;
      // our UserFormFields is structurally compatible, so cast.
      await savePersonalInfo(context, {
        ...(user as any),
        attributes,
      });

      const locale = attributes["locale"]?.toString();
      if (locale) {
        await i18n.changeLanguage(locale, (error) => {
          if (error) {
            // eslint-disable-next-line no-console
            console.warn("Error(s) loading locale", locale, error);
          }
        });
      }

      await context.keycloak.updateToken();
      addAlert(t("accountUpdatedMessage"));
    } catch (error) {
      addAlert(t("accountUpdatedError"), AlertVariant.danger);

      setUserProfileServerError(
        { responseData: { errors: error as any } },
        (name: string | number, err: unknown) =>
          setError(name as keyof UserFormFields, err as ErrorOption),
        ((key: string, param?: object) =>
          t(key, param as any)) as unknown as TFunction,
      );
    }
  };

  if (!userProfileMetadata) {
    return <Spinner />;
  }

  const allFieldsReadOnly = () =>
    userProfileMetadata?.attributes
      ?.map((a) => a.readOnly)
      .reduce((p, c) => p && c, true);

  const {
    updateEmailFeatureEnabled,
    updateEmailActionEnabled,
    isRegistrationEmailAsUsername,
    isEditUserNameAllowed,
    deleteAccountAllowed,
  } = context.environment.features;

  return (
    <Page title={t("personalInfo")} description={t("personalInfoDescription")}>
      <Form isHorizontal onSubmit={handleSubmit(onSubmit)}>
        <UserProfileFields
          form={form as any}
          userProfileMetadata={userProfileMetadata}
          supportedLocales={supportedLocales}
          currentLocale={context.environment.locale}
          t={
            ((key: unknown, params: any) =>
              t(key as string, params as any)) as unknown as TFunction
          }
          renderer={(attribute) => {
            const annotations = attribute.annotations ?? {};
            const isEmailField = attribute.name === "email";

            const emailUpdateSupported =
              updateEmailFeatureEnabled &&
              updateEmailActionEnabled &&
              annotations["kc.required.action.supported"] &&
              (!isRegistrationEmailAsUsername || isEditUserNameAllowed);
            if (isEmailField && emailUpdateSupported) {
              return (
                <Button
                  id="update-email-btn"
                  variant="link"
                  onClick={() =>
                    context.keycloak.login({ action: "UPDATE_EMAIL" })
                  }
                  icon={<ExternalLinkSquareAltIcon />}
                  iconPosition="right"
                >
                  {t("updateEmail")}
                </Button>
              );
            }

            return undefined;
          }}
        />

        {!allFieldsReadOnly() && (
          <ActionGroup>
            <Button
              data-testid="save"
              type="submit"
              id="save-btn"
              variant="primary"
            >
              {t("save")}
            </Button>
            <Button
              data-testid="cancel"
              id="cancel-btn"
              variant="link"
              onClick={() => reset()}
            >
              {t("cancel")}
            </Button>
          </ActionGroup>
        )}

        {deleteAccountAllowed && (
          <ExpandableSection
            data-testid="delete-account"
            toggleText={t("deleteAccount")}
          >
            <Alert
              isInline
              title={t("deleteAccount")}
              variant="danger"
              actionLinks={
                <Button
                  id="delete-account-btn"
                  variant="danger"
                  onClick={() =>
                    context.keycloak.login({
                      action: "delete_account",
                    })
                  }
                  className="delete-button"
                >
                  {t("delete")}
                </Button>
              }
            >
              {t("deleteAccountWarning")}
            </Alert>
          </ExpandableSection>
        )}
      </Form>
    </Page>
  );
};

export default PersonalInfo;
