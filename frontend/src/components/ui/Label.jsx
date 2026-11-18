export function Label({ htmlFor, className = '', children, ...props }) {
  return <label htmlFor={htmlFor} className={`field-label ${className}`} {...props}>{children}</label>;
}
