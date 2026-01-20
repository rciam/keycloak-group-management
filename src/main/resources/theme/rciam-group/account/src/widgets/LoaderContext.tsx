import * as React from 'react';
import { createContext, useContext, useState, ReactNode } from 'react';
import { Modal, ModalVariant, Spinner } from "@patternfly/react-core";

interface LoaderContextType {
  startLoader: () => void;
  stopLoader: () => void;
}

const LoaderContext = createContext<LoaderContextType | undefined>(undefined);

export const LoaderProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [isModalOpen, setIsModalOpen] = useState(false);

  const startLoader = () => setIsModalOpen(true);
  const stopLoader = () => setIsModalOpen(false);

  return (
    <LoaderContext.Provider value={{ startLoader, stopLoader }}>
      {children}
      <Modal
        variant={ModalVariant.large}
        width="19rem"
        isOpen={isModalOpen}
        header=""
        showClose={false}
        onEscapePress={() => { }}
        aria-labelledby="modal-custom-header-label"
        aria-describedby="modal-custom-header-description"
        footer=""
      >
        <div tabIndex={0} id="modal-no-header-description" className="gm_loader-modal-container">
          <Spinner diameter="100px" aria-label="Contents of the custom size example" />
        </div>
      </Modal>
    </LoaderContext.Provider>
  );
};

export const useLoader = (): LoaderContextType => {
  const context = useContext(LoaderContext);
  if (!context) {
    throw new Error('useLoader must be used within a LoaderProvider');
  }
  return context;
};