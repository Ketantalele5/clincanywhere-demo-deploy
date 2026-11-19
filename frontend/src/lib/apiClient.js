const API_BASE_URL = import.meta.env.VITE_API_URL ?? '';
const REALTIME_BASE_URL = import.meta.env.VITE_REALTIME_URL ?? API_BASE_URL;
let accessToken = null;
let authenticatedUsername = '';

function validateApiPath(path) {
  const pathname = String(path).split(/[?#]/, 1)[0];
  if (pathname.startsWith('/api/') && pathname.split('/').slice(2).some(segment => segment.length === 0)) {
    throw new Error('Select a clinic workspace before loading clinic data.');
  }
}

export function setAccessToken(token) {
  accessToken = token;
}

export function setAuthenticatedUsername(username) {
  authenticatedUsername = username;
}

export function getAuthenticatedUsername() {
  return authenticatedUsername;
}

export function getAccessToken() {
  return accessToken;
}

export function clearAccessToken() {
  accessToken = null;
  authenticatedUsername = '';
}

export async function apiRequest(path, options = {}) {
  validateApiPath(path);
  const headers = new Headers(options.headers ?? {});
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
  if (!headers.has('Content-Type') && options.body != null && !isFormData) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);

  let response;
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), options.timeoutMs ?? 20000);
  const { timeoutMs: _timeoutMs, signal, ...requestOptions } = options;
  try {
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      throw new Error('Your device is offline. No changes were saved. Reconnect and retry.');
    }
    response = await fetch(`${API_BASE_URL}${path}`, { ...requestOptions, headers, signal: signal ?? controller.signal });
  } catch (error) {
    if (error?.name === 'AbortError') throw new Error('The request took too long. Check your connection and retry.');
    if (error instanceof Error && error.message.startsWith('Your device is offline.')) throw error;
    throw new Error('Could not reach the clinic API. No changes were saved. Check the clinic connection and retry.');
  } finally {
    window.clearTimeout(timeout);
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      if (body && typeof body === 'object') {
        const serverMessage = [body.message, body.detail, body.error]
          .find(value => typeof value === 'string' && value.trim().length > 0);
        if (serverMessage) message = serverMessage;
      }
    } catch {
      // Keep the HTTP status when the response has no JSON body.
    }
    if (response.status === 401 && accessToken) {
      clearAccessToken();
      window.dispatchEvent(new Event('clincanywhere:session-expired'));
    }
    throw new Error(message);
  }

  return response.status === 204 ? null : response.json();
}

export function apiUrl(path) {
  validateApiPath(path);
  return `${API_BASE_URL}${path}`;
}

export function webSocketUrl(path) {
  validateApiPath(path);
  const url = new URL(`${REALTIME_BASE_URL}${path}`, window.location.href);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  return url.toString();
}
