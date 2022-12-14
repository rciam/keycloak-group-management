function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from "../../../common/keycloak/web_modules/react.js";
import { GroupsServiceClient } from "../groups-mngnt-service/groups.service.js";
import { Divider, DualListSelector } from "../../../common/keycloak/web_modules/@patternfly/react-core.js";
export class GroupSelect extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "groupsService", new GroupsServiceClient());

    _defineProperty(this, "availableOptions", []);

    _defineProperty(this, "chosenOptions", []);

    _defineProperty(this, "toTreeListData", data => {
      return data.subGroups.map(group => {
        let translated = {
          id: group.id,
          text: group.name,
          checkProps: {
            'aria-label': group.name
          },
          isChecked: false
        };
        let children = this.toTreeListData(group);
        if (children.length != 0) translated['children'] = children;
        return translated;
      });
    });

    _defineProperty(this, "onListChange", (newAvailableOptions, newChosenOptions) => {
      this.availableOptions = newAvailableOptions;
      this.chosenOptions = newChosenOptions;
    });

    this.availableOptions = [{
      id: "1",
      text: "Option 1",
      isChecked: false
    }, {
      id: "2",
      text: "Option 2",
      isChecked: false
    }];
    this.state = {
      reloadTrigger: false
    };
    this.fetchData();
  }

  fetchData() {
    this.groupsService.doGet("/groups/user/vo/" + this.props.vo_id + "/groups").then(resp => {
      if (resp.ok) {
        //this.availableOptions = this.toTreeListData(resp.data);
        this.setState({
          reloadTrigger: !this.state.reloadTrigger
        });
      }
    }).catch(err => {
      console.log(err);
    });
  }

  render() {
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("p", null, "Please select the groups you would like to join"), /*#__PURE__*/React.createElement("br", null), /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement(DualListSelector, {
      isSearchable: true,
      isTree: true,
      availableOptions: this.availableOptions,
      chosenOptions: this.chosenOptions,
      onListChange: this.onListChange,
      id: "dual-list-selector-tree"
    }), /*#__PURE__*/React.createElement("div", null, this.state.reloadTrigger));
  }

}
//# sourceMappingURL=GroupSelect.js.map