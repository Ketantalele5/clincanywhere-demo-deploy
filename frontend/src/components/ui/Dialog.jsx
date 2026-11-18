import { useEffect, useId } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';

export function Dialog({ open, onOpenChange, title, description, children, className = '' }) {
  const titleId = useId();
  const descriptionId = useId();

  useEffect(() => {
    if (!open) return undefined;
    const onKeyDown = event => {
      if (event.key === 'Escape') onOpenChange?.(false);
    };
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open, onOpenChange]);

  if (!open) return null;
  return createPortal(<div className="ui-dialog-backdrop" onMouseDown={event => {
    if (event.target === event.currentTarget) onOpenChange?.(false);
  }}>
    <section role="dialog" aria-modal="true" aria-labelledby={titleId} aria-describedby={description ? descriptionId : undefined} className={`ui-dialog ${className}`}>
      <header className="ui-dialog-header"><div><h2 id={titleId}>{title}</h2>{description && <p id={descriptionId}>{description}</p>}</div><button type="button" aria-label="Close dialog" onClick={() => onOpenChange?.(false)}><X size={18} /></button></header>
      {children}
    </section>
  </div>, document.body);
}
