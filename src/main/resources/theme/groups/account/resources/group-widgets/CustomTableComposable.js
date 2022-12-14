import * as React from "../../../common/keycloak/web_modules/react.js";
import { DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell, Divider } from "../../../common/keycloak/web_modules/@patternfly/react-core.js";
export class CustomTableComposable extends React.Component {
  constructor(props) {
    super(props);
  }

  componentDidMount() {}

  render() {
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(Divider, {
      component: "li"
    }), /*#__PURE__*/React.createElement(DataList, {
      "aria-label": "Simple data list example",
      isCompact: true
    }, /*#__PURE__*/React.createElement(DataListItem, {
      "aria-labelledby": "compact-item1"
    }, /*#__PURE__*/React.createElement(DataListItemRow, null, /*#__PURE__*/React.createElement(DataListItemCells, {
      dataListCells: [/*#__PURE__*/React.createElement(DataListCell, {
        key: "primary content"
      }, /*#__PURE__*/React.createElement("span", {
        id: "simple-item1"
      }, "Primary content")), /*#__PURE__*/React.createElement(DataListCell, {
        key: "secondary content"
      }, "Secondary content")]
    }))), /*#__PURE__*/React.createElement(DataListItem, {
      "aria-labelledby": "compact-item2"
    }, /*#__PURE__*/React.createElement(DataListItemRow, null, /*#__PURE__*/React.createElement(DataListItemCells, {
      dataListCells: [/*#__PURE__*/React.createElement(DataListCell, {
        isFilled: false,
        key: "secondary content fill"
      }, /*#__PURE__*/React.createElement("span", {
        id: "simple-item2"
      }, "Secondary content (pf-m-no-fill)")), /*#__PURE__*/React.createElement(DataListCell, {
        isFilled: false,
        alignRight: true,
        key: "secondary content align"
      }, "Secondary content (pf-m-align-right pf-m-no-fill)")]
    })))));
  }

}
//# sourceMappingURL=CustomTableComposable.js.map