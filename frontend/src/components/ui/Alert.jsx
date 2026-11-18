import { cn } from '../../lib/cn.js';

export function Alert({ variant = 'default', className, ...props }) {
  return <div role="alert" className={cn('ui-alert', `ui-alert-${variant}`, className)} {...props} />;
}
