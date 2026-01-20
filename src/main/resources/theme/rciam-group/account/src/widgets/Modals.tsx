import * as React from "react";
import {
  Modal,
  ModalVariant,
  Button,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";

interface ConfirmationModalProps {
  modalInfo: any;
}

export const ConfirmationModal: React.FC<ConfirmationModalProps> = (props) => {
    const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    setIsModalOpen(Object.keys(props.modalInfo).length > 0);
  }, [props.modalInfo]);
  const hasInfo = props.modalInfo && Object.keys(props.modalInfo).length > 0;
  if (!hasInfo) return null;

  const title = props.modalInfo.title ?? "Confirmation";
  const handleModalToggle = () => {
    props?.modalInfo?.cancel();
  };

  return (
    <>
      <Modal
        variant={
          props.modalInfo?.variant === "medium"
            ? ModalVariant.medium
            : ModalVariant.small
        }
        title={title}
        isOpen={isModalOpen}
        onClose={handleModalToggle}
        actions={[
          <Button
            key="confirm"
            variant={props?.modalInfo?.button_variant || "primary"}
            onClick={() => {
              props?.modalInfo?.accept();
            }}
          >
            {props?.modalInfo?.accept_message}
          </Button>,
          props?.modalInfo?.cancel_message && (
            <Button
              key="cancel"
              variant="link"
              onClick={() => {
                props?.modalInfo?.cancel();
              }}
            >
              {props?.modalInfo?.cancel_message}
            </Button>
          ),
        ]}
      >
        {props?.modalInfo?.message && props?.modalInfo?.message}
      </Modal>
    </>
  );
};