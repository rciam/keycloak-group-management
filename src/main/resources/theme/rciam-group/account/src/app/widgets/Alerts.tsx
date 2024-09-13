import * as React from 'react';
import {
    Alert,
    AlertActionCloseButton,
    AlertGroup,
    AlertVariant,
  } from "@patternfly/react-core";
import { useEffect } from "react";

//   success = 'success',
//   danger = 'danger',
//   warning = 'warning',
//   info = 'info',
//   default = 'default  


  export const Alerts:React.FC<any> = (props) => {

    useEffect(()=>{
        setTimeout(() => props.close(), 18000);
    },[props.alert])

    return (
        <React.Fragment>
            {Object.keys(props.alert).length > 0&&
                <AlertGroup
                    data-testid="global-alerts"
                    isToast
                    style={{ whiteSpace: "pre-wrap" }}
                >
                    <Alert
                        isLiveRegion
                        variant={AlertVariant[props.alert.variant]}
                        variantLabel=""
                        title={props.alert.message}
                        actionClose={
                            <AlertActionCloseButton
                            title={props.alert.message}
                            onClose={() => props.close()}
                            />
                        }
                    >
                        {props.alert.description && <p>{props.alert.description}</p>}
                    </Alert>
                </AlertGroup>
            }  
        </React.Fragment>
          
    );
  }
  