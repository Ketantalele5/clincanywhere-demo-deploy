export function ScrollArea({ className = '', children, ...props }) {
  return <div className={`ui-scroll-area ${className}`} {...props}>{children}</div>;
}
