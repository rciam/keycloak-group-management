function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from "../../../../common/keycloak/web_modules/react.js";
import { GroupsServiceClient } from "../../groups-mngnt-service/groups.service.js";
import { DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell, Label } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
export class EnrollmentProgress extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "groupsService", new GroupsServiceClient());

    this.state = {
      data: []
    };
    this.fetchData();
  }

  componentDidMount() {}

  fetchData() {
    this.groupsService.doGet("/groups/user/enroll/request").then(resp => {
      if (resp.ok) this.setState({
        data: resp.data
      });
    }).catch(err => {
      console.log(err);
    });
  }

  toDate(timestamp) {
    let dateTimeFormat = new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'long',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
    return dateTimeFormat.format(timestamp);
  }

  render() {
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(DataList, {
      "aria-label": "Simple data list example",
      isCompact: true
    }, this.state.data.map(d => /*#__PURE__*/React.createElement(DataListItem, null, /*#__PURE__*/React.createElement(DataListItemRow, null, /*#__PURE__*/React.createElement(DataListItemCells, {
      dataListCells: [/*#__PURE__*/React.createElement(DataListCell, null, /*#__PURE__*/React.createElement("span", null, d.group.name)), /*#__PURE__*/React.createElement(DataListCell, null, /*#__PURE__*/React.createElement(DataList, {
        "aria-label": "internal",
        isCompact: true
      }, d.enrollmentStates.map(es => /*#__PURE__*/React.createElement(DataListItem, null, /*#__PURE__*/React.createElement(DataListItemRow, null, /*#__PURE__*/React.createElement(DataListItemCells, {
        dataListCells: [/*#__PURE__*/React.createElement(DataListCell, null, /*#__PURE__*/React.createElement(Label, null, es.state)), /*#__PURE__*/React.createElement(DataListCell, null, /*#__PURE__*/React.createElement("span", null, this.toDate(es.timestamp))), /*#__PURE__*/React.createElement(DataListCell, null, /*#__PURE__*/React.createElement("span", null, es.justification))]
      }))))))]
    }))))));
    /*
            return (
                <>
                  <DataList aria-label="Simple data list example" isCompact>
                    <DataListItem aria-labelledby="compact-item1">
                      <DataListItemRow>
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell key="primary content">
                              <span id="simple-item1">Primary content</span>
                            </DataListCell>,
                            <DataListCell key="secondary content">Secondary content</DataListCell>
                          ]}
                        />
                      </DataListItemRow>
                    </DataListItem>
                    <DataListItem aria-labelledby="compact-item2">
                      <DataListItemRow>
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell isFilled={false} key="secondary content fill">
                              <span id="simple-item2">Secondary content (pf-m-no-fill)</span>
                            </DataListCell>,
                            <DataListCell isFilled={false} alignRight key="secondary content align">
                              Secondary content (pf-m-align-right pf-m-no-fill)
                            </DataListCell>
                          ]}
                        />
                      </DataListItemRow>
                    </DataListItem>
                  </DataList>
                </>
            );
    */
  }

}
;
//# sourceMappingURL=EnrollmentProgress.js.map