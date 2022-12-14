function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from "../../../../common/keycloak/web_modules/react.js";
import { GroupsServiceClient } from "../../groups-mngnt-service/groups.service.js";
import { Wizard } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { VOSelect } from "../../group-widgets/VOSelect.js";
import { GroupSelect } from "../../group-widgets/GroupSelect.js";
export class EnrollmentRequest extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "groupsService", new GroupsServiceClient());

    _defineProperty(this, "onNext", step => {
      this.setState({
        currentStep: this.state.currentStep < Number(step.id) ? Number(step.id) : this.state.currentStep
      });
    });

    _defineProperty(this, "getVOSelection", selection => {
      console.log("selected VO: ", selection);
      this.setState({
        selectedVO: selection.group.id
      });
    });

    _defineProperty(this, "getGroupSelection", selection => {
      console.log("selected group: ", selection);
    });

    this.state = {
      currentStep: 1,
      selectedVO: null,
      selectedGroup: null
    };
  }

  componentDidMount() {}

  render() {
    const steps = [{
      id: '1',
      name: 'Select VO',
      component: /*#__PURE__*/React.createElement(VOSelect, {
        getVOSelection: this.getVOSelection
      })
    }, {
      id: '2',
      name: 'Select group(s)',
      component: /*#__PURE__*/React.createElement(GroupSelect, {
        getGroupSelection: this.getGroupSelection,
        vo_id: this.state.selectedVO
      }, " "),
      canJumpTo: this.state.currentStep >= 2
    }, {
      id: '3',
      name: 'Acceptable use policy',
      component: /*#__PURE__*/React.createElement("p", null, "Has no special use policies"),
      canJumpTo: this.state.currentStep >= 3
    }, {
      id: '4',
      name: 'Fourth step',
      component: /*#__PURE__*/React.createElement("p", null, "Step 4 content"),
      canJumpTo: this.state.currentStep >= 4
    }, {
      id: '5',
      name: 'Review',
      component: /*#__PURE__*/React.createElement("p", null, "Review step content"),
      nextButtonText: 'Finish',
      canJumpTo: this.state.currentStep >= 5
    }];
    const title = 'VO/group enrollment wizard';
    return /*#__PURE__*/React.createElement(Wizard, {
      navAriaLabel: `${title} steps`,
      mainAriaLabel: `${title} content`,
      onClose: this.props.goToMainMenu,
      steps: steps,
      onNext: this.onNext
    });
  }

}
;
//# sourceMappingURL=EnrollmentRequest.js.map