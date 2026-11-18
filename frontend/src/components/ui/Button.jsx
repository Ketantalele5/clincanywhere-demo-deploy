import { forwardRef } from 'react';
import { cn } from '../../lib/cn.js';

const variants = {
  default: 'button button-primary',
  primary: 'button button-primary',
  secondary: 'button button-outline',
  outline: 'button button-outline',
  danger: 'button button-danger',
  ghost: 'icon-button',
  link: 'text-link',
};

export const Button = forwardRef(function Button({ variant = 'default', size, className, type = 'button', ...props }, ref) {
  return <button ref={ref} type={type} className={cn(variants[variant] ?? variants.default, size && `button-${size}`, className)} {...props} />;
});
