import * as React from 'react';

import { GroupsServiceClient } from '../../groups-mngnt-service/groups.service';

import { DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell, Divider, Wizard, WizardStep } from '@patternfly/react-core';


interface State {
  currentStep: number
}


interface Props {

}


export class EnrollmentRequest extends React.Component<Props, State> {

    groupsService = new GroupsServiceClient();


    constructor(props : Props){
        super(props);
        this.state = {
          currentStep: 1
        };
    }

    public componentDidMount(): void {

    }

    onNext = ( step: WizardStep) => {
      console.log("initial value:", this.state.currentStep);
      this.setState({
        currentStep: this.state.currentStep < Number(step.id) ? Number(step.id) : this.state.currentStep
      });
      console.log("setting value:", step);
      console.log("new value:", this.state.currentStep);
    };



    closeWizard = () => {
      console.log('close wizard');
    };

    public render(): React.ReactNode {



        const steps = [
          { id: '1', name: 'First step', component: <p>Step 1 content</p> },
          {
            id: '2',
            name: 'Second step',
            component: <p>Step 2 content</p>,
            canJumpTo: this.state.currentStep >= 2
          },
          {
            id: '3',
            name: 'Third step',
            component: <p>Step 3 content</p>,
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

        const title = 'Incrementally enabled wizard';


        return (
          <Wizard
            navAriaLabel={`${title} steps`}
            mainAriaLabel={`${title} content`}
            onClose={this.closeWizard}
            steps={steps}
            onNext={this.onNext}

          />
        );
    }
};
