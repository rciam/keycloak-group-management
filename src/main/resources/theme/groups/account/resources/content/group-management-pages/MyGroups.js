function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from "../../../../common/keycloak/web_modules/react.js";
import { GroupsServiceClient } from "../../groups-mngnt-service/groups.service.js";
import "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { CustomTableComposable } from "../../group-widgets/CustomTableComposable.js";
export class MyGroups extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "groupsService", new GroupsServiceClient());

    this.state = {
      data: null
    };
    this.fetchData();
  }

  componentDidMount() {}

  fetchData() {
    this.groupsService.doGet("/groups/user/test/get-all").then(resp => {
      if (resp.ok) this.showData(resp.data);
    }).catch(err => {
      console.log(err);
    });
  }

  showData(data) {
    console.log(data); //transform data here (if needed before fitting them into the state)

    this.setState({
      data: data
    });
  }

  render() {
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(CustomTableComposable, null));
  }

}
;
//# sourceMappingURL=MyGroups.js.map