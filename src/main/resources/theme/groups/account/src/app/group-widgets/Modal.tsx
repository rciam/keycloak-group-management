import * as React from 'react';
import { Modal, ModalVariant, Button } from '@patternfly/react-core';
import {useEffect} from 'react';
// import parse from '../../node_modules/react-html-parser';


interface ConfirmationModalProps {
    modalInfo: any;
};


export const ConfirmationModal: React.FC<ConfirmationModalProps> = (props) =>{
    
    useEffect(()=>{
        setIsModalOpen(Object.keys(props.modalInfo).length > 0);
    },[props.modalInfo])

    const [isModalOpen, setIsModalOpen] = React.useState(false);

    const handleModalToggle = () => {
        props?.modalInfo?.cancel();
    };
    
    return (
        <React.Fragment>
            <Modal
            variant={ModalVariant.small}
            title={props?.modalInfo?.title}
            isOpen={isModalOpen}
            onClose={handleModalToggle}
            actions={[
                <Button key="confirm" variant="primary" onClick={()=>{props?.modalInfo?.accept()}}>
                    {props?.modalInfo?.accept_message}
                </Button>,
                props?.modalInfo?.cancel_message&&
                    <Button key="cancel" variant="link" onClick={()=>{props?.modalInfo?.cancel();}}>
                        {props?.modalInfo?.cancel_message}
                    </Button>
                
            ]}
            >
                {props?.modalInfo?.message&&props?.modalInfo?.message}
            </Modal>
        </React.Fragment>   );
}


