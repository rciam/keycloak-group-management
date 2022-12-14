import * as React from "../../../../common/keycloak/web_modules/react.js"; // @ts-ignore

import { KeycloakService } from "../../keycloak-service/keycloak.service.js"; // @ts-ignore

import { Banner } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { CustomTableComposable } from "../../group-widgets/CustomTableComposable.js";
import { AccordionSample } from "../../group-widgets/AccordionSample.js";
const keycloakService = new KeycloakService(keycloak);
export class Sample extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      accessToken: null,
      s: null
    };
  }

  componentDidMount() {
    isReactLoading = false;
    console.log("componentDidMount");
    this.testServices();
  }

  testServices() {
    //get token
    keycloakService.getToken().then(token => {
      console.log("AccessToken: ", token);
      this.setState({
        accessToken: token
      });
    }).catch(err => {
      console.log("Error: ", err);
    });
  }

  render() {
    console.log("rendering GroupsManagementPage");
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(Banner, {
      variant: "info"
    }, "There are pending requests"), /*#__PURE__*/React.createElement(CustomTableComposable, null), /*#__PURE__*/React.createElement(AccordionSample, null));
  }

}
;
//# sourceMappingURL=Sample.js.map