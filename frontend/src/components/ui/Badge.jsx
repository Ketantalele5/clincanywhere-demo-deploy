import { cn } from '../../lib/cn.js';

export function Badge({ variant = 'default', className, ...props }) {
  return <span className={cn('status-badge', variant !== 'default' && `status-${variant}`, className)} {...props} />;
}
