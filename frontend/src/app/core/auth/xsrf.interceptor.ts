import { HttpInterceptorFn } from '@angular/common/http';

function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop()?.split(';').shift() ?? null;
  }
  return null;
}

export const xsrfInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.method === 'GET' || req.method === 'HEAD' || req.method === 'OPTIONS') {
    return next(req);
  }
  const token = getCookie('XSRF-TOKEN');
  if (token) {
    const cloned = req.clone({
      setHeaders: {
        'X-XSRF-TOKEN': token,
      },
      withCredentials: req.withCredentials || true,
    });
    return next(cloned);
  }
  const cloned = req.clone({ withCredentials: req.withCredentials || true });
  return next(cloned);
};
