import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { Check, X } from 'lucide-react';

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const dismiss = useCallback(id => setItems(current => current.filter(item => item.id !== id)), []);
  const toast = useCallback(({ title, description, variant = 'default', duration = 3500 }) => {
    const id = crypto.randomUUID();
    setItems(current => [...current, { id, title, description, variant }]);
    if (duration > 0) window.setTimeout(() => dismiss(id), duration);
    return id;
  }, [dismiss]);
  const value = useMemo(() => ({ toast, dismiss }), [toast, dismiss]);

  return <ToastContext.Provider value={value}>
    {children}
    <div className="ui-toaster" aria-live="polite" aria-relevant="additions">
      {items.map(item => <div key={item.id} role="status" className={`ui-toast ui-toast-${item.variant}`}>
        <Check size={16} aria-hidden="true" />
        <div className="ui-toast-copy">
          {item.title && <b>{item.title}</b>}
          {item.description && <span>{item.description}</span>}
        </div>
        <button type="button" aria-label="Dismiss notification" onClick={() => dismiss(item.id)}><X size={15} /></button>
      </div>)}
    </div>
  </ToastContext.Provider>;
}

export function useToast() {
  const value = useContext(ToastContext);
  if (!value) throw new Error('useToast must be used inside ToastProvider');
  return value;
}
