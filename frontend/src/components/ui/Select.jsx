import { forwardRef } from 'react';

export const Select = forwardRef(function Select({ className = '', children, ...props }, ref) {
  return <select ref={ref} className={className} {...props}>{children}</select>;
});
