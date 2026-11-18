import { useEffect, useState } from 'react';
import { Download, X } from 'lucide-react';

function isIOSDevice() {
  return /iPad|iPhone|iPod/.test(navigator.userAgent)
    || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
}

function isInstalled() {
  return window.matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;
}

export function InstallAppControl() {
  const [installPrompt, setInstallPrompt] = useState(null);
  const [showIOSHelp, setShowIOSHelp] = useState(false);
  const [iosDevice, setIOSDevice] = useState(false);
  const [installed, setInstalled] = useState(false);

  useEffect(() => {
    setIOSDevice(isIOSDevice());
    setInstalled(isInstalled());
    const onBeforeInstall = event => {
      event.preventDefault();
      setInstallPrompt(event);
    };
    const onInstalled = () => {
      setInstalled(true);
      setInstallPrompt(null);
    };
    window.addEventListener('beforeinstallprompt', onBeforeInstall);
    window.addEventListener('appinstalled', onInstalled);
    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstall);
      window.removeEventListener('appinstalled', onInstalled);
    };
  }, []);

  if (installed || (!installPrompt && !iosDevice)) return null;

  async function install() {
    if (!installPrompt) {
      setShowIOSHelp(true);
      return;
    }
    await installPrompt.prompt();
    const choice = await installPrompt.userChoice;
    if (choice.outcome === 'accepted') setInstalled(true);
    setInstallPrompt(null);
  }

  return <>
    <button className="pwa-install-button" type="button" onClick={install}>
      <Download size={16} /> Install app
    </button>
    {showIOSHelp && <div className="pwa-help-backdrop" onClick={() => setShowIOSHelp(false)}>
      <section className="pwa-help-dialog" role="dialog" aria-modal="true" aria-labelledby="pwa-help-title" onClick={event => event.stopPropagation()}>
        <button className="pwa-help-close" type="button" aria-label="Close install instructions" onClick={() => setShowIOSHelp(false)}><X size={18} /></button>
        <h2 id="pwa-help-title">Add ClincyAnywhere to your Home Screen</h2>
        <ol>
          <li>Open this page in Safari.</li>
          <li>Tap the <b>Share</b> button in the browser toolbar.</li>
          <li>Choose <b>Add to Home Screen</b>, then tap <b>Add</b>.</li>
        </ol>
      </section>
    </div>}
  </>;
}
