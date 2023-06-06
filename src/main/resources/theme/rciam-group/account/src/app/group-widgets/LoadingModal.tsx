import * as React from 'react';
import { Modal, ModalVariant, Spinner } from "@patternfly/react-core";
import { useEffect, useState } from "react";

export const Loading: React.FC<any> = (props) => {
    const [isModalOpen,setIsModalOpen] = useState(false);
    useEffect(()=>{    
      setIsModalOpen(props.active)
    },[props.active])
  
    return(
      <Modal
      variant={ModalVariant.large}
      width="19rem"
      isOpen={isModalOpen}
      header=""
      showClose={false}
      onEscapePress={()=>{}}
      aria-labelledby="modal-custom-header-label"
      aria-describedby="modal-custom-header-description"
      footer=""
    >
      <div tabIndex={0} id="modal-no-header-description" className="gm_loader-modal-container">
        <Spinner isSVG diameter="100px" aria-label="Contents of the custom size example" />
      </div>
    </Modal>
    )
  }