import * as React from "../../../common/keycloak/web_modules/react.js";
import { Accordion, AccordionContent, AccordionItem, AccordionToggle } from "../../../common/keycloak/web_modules/@patternfly/react-core.js";
export class AccordionSample extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      expanded: 'def-list-toggle2'
    };
  }

  onToggle(id) {
    if (id === this.state.expanded) {
      this.setExpanded('');
    } else {
      this.setExpanded(id);
    }
  }

  setExpanded(exp) {
    this.setState({
      expanded: exp
    });
  }

  render() {
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(Accordion, {
      asDefinitionList: true
    }, /*#__PURE__*/React.createElement(AccordionItem, null, /*#__PURE__*/React.createElement(AccordionToggle, {
      onClick: () => {
        this.onToggle('def-list-toggle1');
      },
      isExpanded: this.state.expanded === 'def-list-toggle1',
      id: "def-list-toggle1"
    }, "Item one"), /*#__PURE__*/React.createElement(AccordionContent, {
      id: "def-list-expand1",
      isHidden: this.state.expanded !== 'def-list-toggle1'
    }, /*#__PURE__*/React.createElement("p", null, "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."))), /*#__PURE__*/React.createElement(AccordionItem, null, /*#__PURE__*/React.createElement(AccordionToggle, {
      onClick: () => {
        this.onToggle('def-list-toggle2');
      },
      isExpanded: this.state.expanded === 'def-list-toggle2',
      id: "def-list-toggle2"
    }, "Item two"), /*#__PURE__*/React.createElement(AccordionContent, {
      id: "def-list-expand2",
      isHidden: this.state.expanded !== 'def-list-toggle2'
    }, /*#__PURE__*/React.createElement("p", null, "Vivamus et tortor sed arcu congue vehicula eget et diam. Praesent nec dictum lorem. Aliquam id diam ultrices, faucibus erat id, maximus nunc."))), /*#__PURE__*/React.createElement(AccordionItem, null, /*#__PURE__*/React.createElement(AccordionToggle, {
      onClick: () => {
        this.onToggle('def-list-toggle3');
      },
      isExpanded: this.state.expanded === 'def-list-toggle3',
      id: "def-list-toggle3"
    }, "Item three"), /*#__PURE__*/React.createElement(AccordionContent, {
      id: "def-list-expand3",
      isHidden: this.state.expanded !== 'def-list-toggle3'
    }, /*#__PURE__*/React.createElement("p", null, "Morbi vitae urna quis nunc convallis hendrerit. Aliquam congue orci quis ultricies tempus."))), /*#__PURE__*/React.createElement(AccordionItem, null, /*#__PURE__*/React.createElement(AccordionToggle, {
      onClick: () => {
        this.onToggle('def-list-toggle4');
      },
      isExpanded: this.state.expanded === 'def-list-toggle4',
      id: "def-list-toggle4"
    }, "Item four"), /*#__PURE__*/React.createElement(AccordionContent, {
      id: "def-list-expand4",
      isHidden: this.state.expanded !== 'def-list-toggle4'
    }, /*#__PURE__*/React.createElement("p", null, "Donec vel posuere orci. Phasellus quis tortor a ex hendrerit efficitur. Aliquam lacinia ligula pharetra, sagittis ex ut, pellentesque diam. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Vestibulum ultricies nulla nibh. Etiam vel dui fermentum ligula ullamcorper eleifend non quis tortor. Morbi tempus ornare tempus. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Mauris et velit neque. Donec ultricies condimentum mauris, pellentesque imperdiet libero convallis convallis. Aliquam erat volutpat. Donec rutrum semper tempus. Proin dictum imperdiet nibh, quis dapibus nulla. Integer sed tincidunt lectus, sit amet auctor eros."))), /*#__PURE__*/React.createElement(AccordionItem, null, /*#__PURE__*/React.createElement(AccordionToggle, {
      onClick: () => {
        this.onToggle('def-list-toggle5');
      },
      isExpanded: this.state.expanded === 'def-list-toggle5',
      id: "def-list-toggle5"
    }, "Item five"), /*#__PURE__*/React.createElement(AccordionContent, {
      id: "def-list-expand5",
      isHidden: this.state.expanded !== 'def-list-toggle5'
    }, /*#__PURE__*/React.createElement("p", null, "Vivamus finibus dictum ex id ultrices. Mauris dictum neque a iaculis blandit.")))));
  }

}
//# sourceMappingURL=AccordionSample.js.map