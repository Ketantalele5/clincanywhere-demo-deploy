import { useState } from 'react';
import { MessageCircle, Send, ShieldCheck } from 'lucide-react';
import { Button, Card, Label, ScrollArea, Select, Spinner, Textarea } from '../../components/ui/index.js';
import { formatDateTimeIST } from '../../lib/formatters.js';

export function MessagesPage({ clinicId, peer, contacts, onPeerChange, messages, draft, onDraftChange, onSend, username }) {
  const [sending, setSending] = useState(false);
  const contact = contacts.find(item => item.username === peer);

  async function submit(event) {
    setSending(true);
    try {
      await onSend(event);
    } finally {
      setSending(false);
    }
  }

  return <section className="messages-page">
    <div className="page-heading">
      <div><span className="eyebrow">SECURE CARE TEAM</span><h1>Messages</h1><p>Private conversations within your selected clinic.</p></div>
      <Label htmlFor="message-contact">Conversation
        <Select id="message-contact" aria-label="Conversation" value={peer} onChange={event => onPeerChange(event.target.value)} disabled={!contacts.length}>
          <option value="">{contacts.length ? 'Choose a colleague' : 'No contacts available'}</option>
          {contacts.map(item => <option key={item.username} value={item.username}>{item.username} · {item.role.toLowerCase()}</option>)}
        </Select>
      </Label>
    </div>
    <Card className="chat-card">
      <div className="chat-header">
        <span className="person-avatar"><MessageCircle size={17} /></span>
        <div><b>{contact?.username ?? 'Choose a colleague'}</b><small>{clinicId} · messages are saved to this clinic</small></div>
        <span className="chat-security"><ShieldCheck size={15} /> Clinic private</span>
      </div>
      <ScrollArea className="chat-history" aria-live="polite">
        {messages.length ? messages.map(message => <div key={message.id} className={`chat-message ${message.sender === username ? 'chat-own' : ''}`}>
          <small>{message.sender} · {formatDateTimeIST(message.sentAt)}</small>
          <p>{message.body}</p>
        </div>) : <div className="empty-inline">
          <div><MessageCircle size={20} /><b>{peer ? 'No messages yet' : 'Select a colleague to begin'}</b><p>{peer ? 'Your messages with this colleague will appear here.' : 'Conversations are limited to authorized members of this clinic.'}</p></div>
        </div>}
      </ScrollArea>
      <form className="chat-compose" onSubmit={submit}>
        <Textarea aria-label="Message" maxLength={2000} required value={draft} onChange={event => onDraftChange(event.target.value)} placeholder="Write a message to your care team…" disabled={!peer || sending} />
        <Button variant="primary" type="submit" disabled={!peer || !draft.trim() || sending}>
          {sending ? <Spinner /> : <Send size={15} />} {sending ? 'Sending…' : 'Send message'}
        </Button>
      </form>
    </Card>
  </section>;
}
