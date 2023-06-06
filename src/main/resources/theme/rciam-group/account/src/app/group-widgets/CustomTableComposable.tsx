import * as React from 'react';

import {
    DataList, DataListItem, DataListItemRow, DataListItemCells, DataListCell, Divider
} from '@patternfly/react-core';

interface State {

}

interface Props {

}

export class CustomTableComposable extends React.Component<Props, State> {

  constructor(props : Props){
      super(props);

  }

  public componentDidMount(): void {

  }


  public render(): React.ReactNode {

      return (
        <>
          <Divider component="li" />
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
  }



}
