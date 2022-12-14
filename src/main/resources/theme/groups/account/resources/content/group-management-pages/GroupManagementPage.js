function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from "../../../../common/keycloak/web_modules/react.js";
import { Flex, FlexItem, Card, CardTitle, CardBody, Grid, GridItem } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { MyGroups } from "./MyGroups.js";
import { EnrollmentProgress } from "./EnrollmentProgress.js";
import { EnrollmentRequest } from "./EnrollmentRequest.js";
var Menus;

(function (Menus) {
  Menus[Menus["main"] = 0] = "main";
  Menus[Menus["show_groups"] = 1] = "show_groups";
  Menus[Menus["join_groups"] = 2] = "join_groups";
  Menus[Menus["enrollment_progress"] = 3] = "enrollment_progress";
})(Menus || (Menus = {}));

export class GroupsManagementPage extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "goToMainMenu", () => {
      this.setState({
        menu: Menus.main
      });
    });

    this.state = {
      menu: Menus.main
    };
  }

  componentDidMount() {
    let navItem = document.getElementById('nav-link-group-management');
    if (navItem == null) return;

    navItem.onclick = event => {
      this.goToMainMenu();
    };
  }

  render() {
    return /*#__PURE__*/React.createElement(React.Fragment, null, this.state.menu == Menus.main && this.renderMainMenu(), this.state.menu == Menus.show_groups && this.renderGroupsMenu(), this.state.menu == Menus.join_groups && this.renderJoinGroupMenu(), this.state.menu == Menus.enrollment_progress && this.renderEnrollmentProgressMenu());
  }

  renderMainMenu() {
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(Grid, {
      className: "top-bottom-margin-10 centered-text"
    }, /*#__PURE__*/React.createElement(GridItem, {
      span: 12
    }, "What would you like to do?")), /*#__PURE__*/React.createElement(Flex, null, /*#__PURE__*/React.createElement(FlexItem, null, /*#__PURE__*/React.createElement(Card, {
      id: "show-groups",
      onClick: () => this.setState({
        menu: Menus.show_groups
      }),
      isRounded: true,
      isSelectable: true
    }, /*#__PURE__*/React.createElement(CardTitle, null, "Show my groups"), /*#__PURE__*/React.createElement(CardBody, null, "Here you can see which groups you have already joined into"))), /*#__PURE__*/React.createElement(FlexItem, null, /*#__PURE__*/React.createElement(Card, {
      id: "enroll-groups",
      onClick: () => this.setState({
        menu: Menus.join_groups
      }),
      isRounded: true,
      isSelectable: true
    }, /*#__PURE__*/React.createElement(CardTitle, null, "Join group(s)"), /*#__PURE__*/React.createElement(CardBody, null, "Here you can ask to join a new group"))), /*#__PURE__*/React.createElement(FlexItem, null, /*#__PURE__*/React.createElement(Card, {
      id: "enroll-groups",
      onClick: () => this.setState({
        menu: Menus.enrollment_progress
      }),
      isRounded: true,
      isSelectable: true
    }, /*#__PURE__*/React.createElement(CardTitle, null, "View enrollment progress"), /*#__PURE__*/React.createElement(CardBody, null, "Here you can view the progress of your group enrollment requests")))));
  }

  renderGroupsMenu() {
    return /*#__PURE__*/React.createElement(MyGroups, null);
  }

  renderJoinGroupMenu() {
    return /*#__PURE__*/React.createElement(EnrollmentRequest, {
      goToMainMenu: this.goToMainMenu
    });
  }

  renderEnrollmentProgressMenu() {
    return /*#__PURE__*/React.createElement(EnrollmentProgress, null);
  }

}
;
//# sourceMappingURL=GroupManagementPage.js.map