import { cn } from '../../lib/cn.js';

export function Card({ className, ...props }) {
  return <section className={cn('card', className)} {...props} />;
}

export function CardHeader({ className, ...props }) {
  return <div className={cn('card-heading', className)} {...props} />;
}

export function CardContent({ className, ...props }) {
  return <div className={cn(className)} {...props} />;
}
