import { createContext, useContext } from 'react';
import { cn } from '../../lib/cn.js';

const SidebarContext = createContext({ open: true, onOpenChange() {} });

export function SidebarProvider({ open = true, onOpenChange, children }) {
  return <SidebarContext.Provider value={{ open, onOpenChange }}>{children}</SidebarContext.Provider>;
}

export function Sidebar({ className, children, ...props }) {
  const { open } = useContext(SidebarContext);
  return <aside data-state={open ? 'expanded' : 'collapsed'} className={cn('sidebar', !open && 'sidebar-collapsed', className)} {...props}>{children}</aside>;
}

export function SidebarTrigger({ className, children, ...props }) {
  const { open, onOpenChange } = useContext(SidebarContext);
  return <button type="button" aria-label={open ? 'Collapse sidebar' : 'Expand sidebar'} aria-expanded={open} className={cn('icon-button', className)} onClick={() => onOpenChange?.(!open)} {...props}>{children}</button>;
}
