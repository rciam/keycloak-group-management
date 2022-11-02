import * as React from 'react';

import { ToggleGroup, ToggleGroupItem, ToggleGroupItemProps } from '@patternfly/react-core';

import { Accordion, AccordionContent, AccordionItem, AccordionToggle } from '@patternfly/react-core';


interface State {
  expanded: string;
}

interface Props {

}



export class AccordionSample extends React.Component<Props, State> {


  constructor(props : Props){
      super(props);
      this.state = { expanded: 'def-list-toggle2'};
  }

  onToggle(id: string){
    if (id === this.state.expanded) {
      this.setExpanded('');
    } else {
      this.setExpanded(id);
    }
  }

  setExpanded(exp: string){
    this.setState({expanded: exp});
  }

  public render(): React.ReactNode {
    return (
      <>
        <Accordion asDefinitionList>
          <AccordionItem>
            <AccordionToggle
              onClick={() => {
                this.onToggle('def-list-toggle1');
              }}
              isExpanded={this.state.expanded === 'def-list-toggle1'}
              id="def-list-toggle1"
            >
              Item one
            </AccordionToggle>
            <AccordionContent id="def-list-expand1" isHidden={this.state.expanded !== 'def-list-toggle1'}>
              <p>
                Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et
                dolore magna aliqua.
              </p>
            </AccordionContent>
          </AccordionItem>

          <AccordionItem>
            <AccordionToggle
              onClick={() => {
                this.onToggle('def-list-toggle2');
              }}
              isExpanded={this.state.expanded === 'def-list-toggle2'}
              id="def-list-toggle2"
            >
              Item two
            </AccordionToggle>
            <AccordionContent id="def-list-expand2" isHidden={this.state.expanded !== 'def-list-toggle2'}>
              <p>
                Vivamus et tortor sed arcu congue vehicula eget et diam. Praesent nec dictum lorem. Aliquam id diam
                ultrices, faucibus erat id, maximus nunc.
              </p>
            </AccordionContent>
          </AccordionItem>

          <AccordionItem>
            <AccordionToggle
              onClick={() => {
                this.onToggle('def-list-toggle3');
              }}
              isExpanded={this.state.expanded === 'def-list-toggle3'}
              id="def-list-toggle3"
            >
              Item three
            </AccordionToggle>
            <AccordionContent id="def-list-expand3" isHidden={this.state.expanded !== 'def-list-toggle3'}>
              <p>Morbi vitae urna quis nunc convallis hendrerit. Aliquam congue orci quis ultricies tempus.</p>
            </AccordionContent>
          </AccordionItem>

          <AccordionItem>
            <AccordionToggle
              onClick={() => {
                this.onToggle('def-list-toggle4');
              }}
              isExpanded={this.state.expanded === 'def-list-toggle4'}
              id="def-list-toggle4"
            >
              Item four
            </AccordionToggle>
            <AccordionContent id="def-list-expand4" isHidden={this.state.expanded !== 'def-list-toggle4'}>
              <p>
                Donec vel posuere orci. Phasellus quis tortor a ex hendrerit efficitur. Aliquam lacinia ligula pharetra,
                sagittis ex ut, pellentesque diam. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere
                cubilia Curae; Vestibulum ultricies nulla nibh. Etiam vel dui fermentum ligula ullamcorper eleifend non quis
                tortor. Morbi tempus ornare tempus. Orci varius natoque penatibus et magnis dis parturient montes, nascetur
                ridiculus mus. Mauris et velit neque. Donec ultricies condimentum mauris, pellentesque imperdiet libero
                convallis convallis. Aliquam erat volutpat. Donec rutrum semper tempus. Proin dictum imperdiet nibh, quis
                dapibus nulla. Integer sed tincidunt lectus, sit amet auctor eros.
              </p>
            </AccordionContent>
          </AccordionItem>

          <AccordionItem>
            <AccordionToggle
              onClick={() => {
                this.onToggle('def-list-toggle5');
              }}
              isExpanded={this.state.expanded === 'def-list-toggle5'}
              id="def-list-toggle5"
            >
              Item five
            </AccordionToggle>
            <AccordionContent id="def-list-expand5" isHidden={this.state.expanded !== 'def-list-toggle5'}>
              <p>Vivamus finibus dictum ex id ultrices. Mauris dictum neque a iaculis blandit.</p>
            </AccordionContent>
          </AccordionItem>
        </Accordion>
      </>
    );
  }


}
