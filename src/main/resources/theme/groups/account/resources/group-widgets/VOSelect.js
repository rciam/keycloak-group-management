function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from "../../../common/keycloak/web_modules/react.js";
import { GroupsServiceClient } from "../groups-mngnt-service/groups.service.js";
import { DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell, Divider } from "../../../common/keycloak/web_modules/@patternfly/react-core.js";
export class VOSelect extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "groupsService", new GroupsServiceClient());

    _defineProperty(this, "onSelectDataListItem", selectedDataListItemId => {
      this.setState({
        selectedDataListItemId: selectedDataListItemId
      });
      this.props.getVOSelection(selectedDataListItemId);
    });

    _defineProperty(this, "onChange", event => {
      console.log("changed: ", event);
    });

    this.state = {
      data: [],
      selectedDataListItemId: ''
    };
    this.fetchData();
  }

  fetchData() {
    this.groupsService.doGet("/groups/user/vo").then(resp => {
      if (resp.ok) this.setState({
        data: resp.data
      });
    }).catch(err => {
      console.log(err);
    });
  }

  render() {
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("p", null, "Please select the virtual organisation you would like to join"), /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement(DataList, {
      "aria-label": "Compact data list example",
      isCompact: true,
      onSelectDataListItem: this.onSelectDataListItem,
      onChange: this.onChange
    }, this.state.data && this.state.data.map(item => /*#__PURE__*/React.createElement(DataListItem, {
      "aria-labelledby": "compact-item1",
      id: item
    }, /*#__PURE__*/React.createElement(DataListItemRow, null, /*#__PURE__*/React.createElement(DataListItemCells, {
      dataListCells: [/*#__PURE__*/React.createElement(DataListCell, {
        key: "primary content"
      }, /*#__PURE__*/React.createElement("span", {
        id: "compact-item1"
      }, item.group.name)), /*#__PURE__*/React.createElement(DataListCell, {
        key: "secondary content"
      }, item.description)]
    }))))));
  }

}
//# sourceMappingURL=VOSelect.js.map