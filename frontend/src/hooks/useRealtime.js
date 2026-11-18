import { useEffect } from 'react';
import { apiRequest as request, apiUrl, clearAccessToken, getAccessToken, webSocketUrl } from '../lib/apiClient.js';

export function useMessages({ clinicId, page, roleId, peer, setMessages, setContacts, setPeer, onError }) {
  useEffect(() => {
    if (page !== 'messages' || !roleId || !peer) return undefined;
    let active = true;
    request(`/api/${clinicId}/messages?with=${encodeURIComponent(peer)}`)
      .then(rows => { if (active) setMessages(rows); })
      .catch(error => { if (active) onError(error.message); });
    return () => { active = false; };
  }, [clinicId, page, roleId, peer, setMessages, onError]);

  useEffect(() => {
    if (page !== 'messages' || !roleId) return undefined;
    let active = true;
    request(`/api/${clinicId}/messages/contacts`).then(contacts => {
      if (!active) return;
      setContacts(contacts);
      if (contacts.length && !contacts.some(contact => contact.username === peer)) setPeer(contacts[0].username);
    }).catch(error => { if (active) onError(error.message); });
    return () => { active = false; };
  }, [clinicId, page, roleId, peer, setContacts, setPeer, onError]);

  useEffect(() => {
    if (page !== 'messages' || !getAccessToken()) return undefined;
    let socket;
    let subscription;
    let stopped = false;
    import('@stomp/stompjs').then(({ Client }) => {
      if (stopped) return;
      socket = new Client({
        brokerURL: webSocketUrl('/ws'),
        connectHeaders: { Authorization: `Bearer ${getAccessToken()}` },
        reconnectDelay: 3000,
        onConnect: () => {
          subscription = socket.subscribe('/user/queue/messages', frame => {
            try {
              const received = JSON.parse(frame.body);
              if (received.clinicId === clinicId && [received.sender, received.recipient].includes(peer)) {
                setMessages(previous => previous.some(item => item.id === received.id) ? previous : [...previous, received]);
              }
            } catch { /* Ignore malformed websocket frames. */ }
          });
        },
      });
      socket.activate();
    }).catch(() => { if (!stopped) onError('Real-time chat is unavailable. Refresh the conversation to see new messages.'); });
    return () => { stopped = true; subscription?.unsubscribe(); socket?.deactivate(); };
  }, [clinicId, page, roleId, peer, setMessages, onError]);
}

export function useNotifications({ clinicId, roleId, enabled = true, setItems, onNotification }) {
  useEffect(() => {
    if (!enabled || !roleId || !getAccessToken()) return undefined;
    let reader;
    let active = true;
    let lastEventId = '';
    let retryDelay = 1000;

    const connect = async () => {
      try {
        const recent = await request(`/api/${clinicId}/notifications?limit=20`);
        if (!active) return;
        setItems(recent);
        if (recent.length) lastEventId = recent[0].id;
      } catch { /* The SSE reconnect loop can recover when the API is available. */ }
      while (active) {
        try {
          const response = await fetch(apiUrl(`/api/${clinicId}/notifications/stream`), {
            headers: {
              Authorization: `Bearer ${getAccessToken()}`,
              Accept: 'text/event-stream',
              ...(lastEventId ? { 'Last-Event-ID': lastEventId } : {}),
            },
          });
          if (response.status === 401) {
            clearAccessToken();
            window.dispatchEvent(new Event('clincanywhere:session-expired'));
            active = false;
            break;
          }
          if (!response.ok || !response.body) throw new Error('Notification stream unavailable');
          retryDelay = 1000;
          reader = response.body.getReader();
          const decoder = new TextDecoder();
          let buffer = '';
          while (active) {
            const { value, done } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const frames = buffer.split(/\r?\n\r?\n/);
            buffer = frames.pop() ?? '';
            for (const frame of frames) {
              const eventName = frame.match(/^event:\s*(.+)$/m)?.[1];
              const data = frame.match(/^data:\s*(.+)$/m)?.[1];
              const eventId = frame.match(/^id:\s*(.+)$/m)?.[1];
              if (eventId) lastEventId = eventId;
              if (eventName !== 'notification' || !data) continue;
              try {
                const item = JSON.parse(data);
                setItems(previous => previous.some(existing => existing.id === item.id)
                  ? previous : [item, ...previous].slice(0, 20));
                onNotification(item.message);
              } catch { /* Ignore malformed server-sent events. */ }
            }
          }
        } catch { /* Reconnect while the signed-in session remains active. */ }
        finally { reader = undefined; }
        if (active) {
          await new Promise(resolve => window.setTimeout(resolve, retryDelay + Math.random() * retryDelay * 0.2));
          retryDelay = Math.min(retryDelay * 2, 30000);
        }
      }
    };
    connect();
    return () => { active = false; reader?.cancel(); };
  }, [clinicId, roleId, enabled, setItems, onNotification]);
}
