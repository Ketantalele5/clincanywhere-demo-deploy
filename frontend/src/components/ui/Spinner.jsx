export function Spinner({ label = 'Loading', className = '' }) {
  return <span role="status" aria-label={label} className={`ui-spinner ${className}`} />;
}
