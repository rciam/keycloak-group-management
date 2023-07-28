import * as React from 'react';

import { GroupsServiceClient } from '../../groups-mngnt-service/groups.service';

import { DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell, Divider, Wizard, WizardStep } from '@patternfly/react-core';

import { VOSelect } from '../../group-widgets/VOSelect';
import { GroupSelect } from '../../group-widgets/GroupSelect';
import { Msg } from '../../widgets/Msg';



interface State {
  currentStep: number,
  selectedVO: any,
  selectedGroup: any
}


interface Props {
  goToMainMenu: any
}


export class EnrollmentRequest extends React.Component<Props, State> {

    groupsService = new GroupsServiceClient();

    constructor(props : Props){
        super(props);
        this.state = {
          currentStep: 1,
          selectedVO: null,
          selectedGroup: null
        };
    }

    public componentDidMount(): void {

    }

    onNext = ( step: WizardStep) => {
      this.setState({
        currentStep: this.state.currentStep < Number(step.id) ? Number(step.id) : this.state.currentStep
      });
    };

    getVOSelection = (selection: any) => {
      console.log("selected VO: ", selection);
      this.setState({
        selectedVO: selection.group.id
      });
    }

    getGroupSelection = (selection: any) => {
      console.log("selected group: ", selection);
    }

    public render(): React.ReactNode {



        const steps = [
          {
            id: '1',
            name: 'Select VO',
            component: <VOSelect getVOSelection={this.getVOSelection}></VOSelect>
          },
          {
            id: '2',
            name: 'Select group(s)',
            component: <GroupSelect getGroupSelection={this.getGroupSelection} vo_id={this.state.selectedVO}> </GroupSelect>,
            canJumpTo: this.state.currentStep >= 2
          },
          {
            id: '3',
            name: 'Acceptable use policy',
            component: <p>Has no special use policies</p>,
            canJumpTo: this.state.currentStep >= 3
          },
          {
            id: '4',
            name: 'Fourth step',
            component: <p>Step 4 content</p>,
            canJumpTo: this.state.currentStep >= 4
          },
          {
            id: '5',
            name: 'Review',
            component: <p>Review step content</p>,
            nextButtonText: 'Finish',
            canJumpTo: this.state.currentStep >= 5
          }
        ];

        const title = 'VO/group enrollment wizard';

        return (
          <Wizard
            navAriaLabel={`${title} steps`}
            mainAriaLabel={`${title} content`}
            onClose={this.props.goToMainMenu}
            steps={steps}
            onNext={this.onNext}

          />
        );
    }
};
