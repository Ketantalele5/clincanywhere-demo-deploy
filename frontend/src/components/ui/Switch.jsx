export function Switch({ checked = false, onCheckedChange, disabled = false, id, label, ...props }) {
  return <button {...props} id={id} type="button" role="switch" aria-checked={checked} aria-label={label} disabled={disabled} className={`ui-switch ${checked ? 'ui-switch-checked' : ''}`} onClick={() => onCheckedChange?.(!checked)}>
    <span />
  </button>;
}
