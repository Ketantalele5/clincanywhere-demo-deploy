import React from 'react';
import { createRoot } from 'react-dom/client';
import ClincyAnywhere from './app/ClincyAnywhere.jsx';
import { ToastProvider } from './components/ui/index.js';
import { InstallAppControl } from './components/shared/InstallAppControl.jsx';
import './styles.css';

if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/service-worker.js').catch(error => {
      console.error('ClincyAnywhere offline support could not be enabled.', error);
    });
  }, { once: true });
}

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ToastProvider>
      <ClincyAnywhere />
      <InstallAppControl />
    </ToastProvider>
  </React.StrictMode>,
);
