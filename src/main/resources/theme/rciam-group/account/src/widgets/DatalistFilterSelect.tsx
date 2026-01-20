import * as React from "react";
import {
  Badge,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  MenuToggleElement,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { CaretDownIcon, CaretUpIcon, CheckIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";

export const DatalistFilterSelect: React.FC<any> = (props) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const [selection, setSelection] = useState(props.default);
  const [initialRender, setInitialRender] = useState(true);
  const [options, setOptions] = useState(props.options);
  const { t } = useTranslation();

  useEffect(() => {
    setOptions(props.options);
  }, [props.options]);

  useEffect(() => {
    if (initialRender) {
      setInitialRender(false);
      return;
    }
    props.action(selection);
  }, [selection]);

  const onToggle = () => {
    console.log("Toggling dropdown:", !isOpen);
    setIsOpen(!isOpen);
  };

  const onFocus = () => {
    const element = document.getElementById("toggle-badge-" + props.name);
    element?.focus();
  };

  const onSelect = () => {
    setIsOpen(false);
    onFocus();
  };

  return (
    <Dropdown
      onSelect={() => {
        onSelect();
      }}
      onOpenChange={(open) => setIsOpen(open)}
      toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
        <MenuToggle
          ref={toggleRef}
          onClick={onToggle}
          isExpanded={isOpen}
          variant="plain"
          className="gm_badge_toggle"
        >
          <Badge isRead>{selection ? t(selection) : "all"}  <span className="gm_badge_caret">
          {isOpen ? <CaretUpIcon /> : <CaretDownIcon />}
        </span></Badge>
        </MenuToggle>
      )}
      className="gm_badge_dropdown"
      isOpen={isOpen}
    >
      <DropdownList>
        <DropdownItem
          key="All"
          component="button"
          onClick={() => {
            setSelection("");
          }}
          icon={!selection && <CheckIcon />}
        >
          {t("All")}
        </DropdownItem>

        {...options.map((option: any) => {
          return (
            <DropdownItem
              key={option}
              component="button"
              onClick={() => {
                setSelection(option);
              }}
              icon={selection === option && <CheckIcon />}
            >
              {props.optionsType === "raw" ? option : t(option)}
            </DropdownItem>
          );
        })}
      </DropdownList>
    </Dropdown>
  );
};
