import { useEffect, useRef, useState } from 'react';
import { Check, Mic, RotateCcw, Square } from 'lucide-react';
import { Button, Card, Input, Select } from '../../components/ui/index.js';
import { addDaysToISODate, todayISO } from '../../lib/formatters.js';

const COPY = {
  en: {
    title: 'Voice-to-clinic assistant',
    description: 'Speak or type an instruction. Review every field and confirm before saving.',
    start: 'Start listening',
    stop: 'Stop',
    save: 'Confirm and save',
    unsupported: 'Speech recognition is unavailable here. Type the instruction instead.',
    review: 'Review instruction',
    draftTitle: 'Review before saving',
    instructionPlaceholder: 'For example: appointment for Patil tomorrow at 5 pm',
    speakFirst: 'Speak or type an instruction first',
    fillMissing: 'Please fill',
    missingFields: 'Missing fields',
    readyToReview: 'Fields extracted; please verify each detail.',
    appointment: 'Appointment',
    followUp: 'Follow-up',
    clear: 'Clear',
    recognitionError: 'Voice recognition failed. Type the instruction instead.',
  },
  hi: {
    title: 'हिंदी वॉइस असिस्टेंट',
    description: 'निर्देश बोलें या लिखें। सेव करने से पहले हर जानकारी जाँचें।',
    start: 'सुनना शुरू करें',
    stop: 'रोकें',
    save: 'पुष्टि करके सेव करें',
    unsupported: 'इस ब्राउज़र में आवाज़ पहचान उपलब्ध नहीं है। निर्देश लिखें।',
    review: 'निर्देश जाँचें',
    draftTitle: 'सेव करने से पहले जाँचें',
    instructionPlaceholder: 'उदाहरण: कल शाम 5 बजे पाटिल की अपॉइंटमेंट रखें',
    speakFirst: 'पहले निर्देश बोलें या लिखें',
    fillMissing: 'कृपया भरें',
    missingFields: 'ज़रूरी जानकारी बाकी है',
    readyToReview: 'जानकारी निकाली गई है; सेव करने से पहले जाँचें।',
    appointment: 'अपॉइंटमेंट',
    followUp: 'फॉलो-अप',
    clear: 'साफ़ करें',
    recognitionError: 'आवाज़ पहचान काम नहीं कर पाई। निर्देश लिखकर देखें।',
  },
  mr: {
    title: 'मराठी आवाज सहाय्यक',
    description: 'सूचना बोला किंवा लिहा. सेव करण्यापूर्वी प्रत्येक तपशील तपासा.',
    start: 'ऐकायला सुरू करा',
    stop: 'थांबवा',
    save: 'पुष्टी करून सेव करा',
    unsupported: 'या ब्राउझरमध्ये आवाज ओळख उपलब्ध नाही. सूचना लिहा.',
    review: 'सूचना तपासा',
    draftTitle: 'सेव करण्यापूर्वी तपासा',
    instructionPlaceholder: 'उदाहरण: उद्या संध्याकाळी ५ वाजता पाटील यांची अपॉइंटमेंट ठेवा',
    speakFirst: 'आधी सूचना बोला किंवा लिहा',
    fillMissing: 'कृपया भरा',
    missingFields: 'आवश्यक तपशील बाकी आहेत',
    readyToReview: 'तपशील मिळाले आहेत; सेव करण्यापूर्वी तपासा.',
    appointment: 'अपॉइंटमेंट',
    followUp: 'फॉलो-अप',
    clear: 'पुसून टाका',
    recognitionError: 'आवाज ओळखता आला नाही. सूचना लिहून पहा.',
  },
};

function findPatient(text, patients) {
  const normalizedText = text.toLocaleLowerCase();
  return patients.find(patient => normalizedText.includes(String(patient.name).toLocaleLowerCase()));
}

function findDoctor(text, doctors) {
  const normalizedText = text.toLocaleLowerCase();
  return doctors.find(doctor => normalizedText.includes(String(doctor.name).toLocaleLowerCase()));
}

function extractTime(text) {
  const match = text.match(/(\d{1,2})(?::(\d{2}))?\s*(am|pm)?/i);
  if (!match) return '';

  let hour = Number(match[1]);
  const minute = match[2] ?? '00';
  const saysEvening = /pm|संध्या|शाम|सायंकाळ|संध्याकाळ|evening/i.test(text);
  const saysMorning = /am|सुबह|सकाळ|morning/i.test(text);

  if (saysEvening && hour < 12) hour += 12;
  if (saysMorning && hour === 12) hour = 0;
  if (hour > 23 || Number(minute) > 59) return '';

  return `${String(hour).padStart(2, '0')}:${minute}`;
}

function extractDate(text) {
  const daysAhead = (/tomorrow|उद्या|कल/i.test(text)) ? 1 : 0;
  return addDaysToISODate(todayISO(), daysAhead);
}

function isAppointmentInstruction(text) {
  return /appointment|अपॉइंटमेंट|भेंट/i.test(text);
}

/** Turns a short instruction into a draft. The user reviews every field before it is saved. */
function parseInstruction(text, data) {
  const patients = data.patients ?? [];
  const doctors = data.doctors ?? [];
  const patient = findPatient(text, patients);
  const doctor = findDoctor(text, doctors);
  const appointment = isAppointmentInstruction(text);
  const date = extractDate(text);
  const time = appointment ? extractTime(text) : '';

  const fields = appointment
    ? {
        patient: patient?.name ?? '',
        doctor: doctor?.name ?? '',
        department: doctor?.specialty ?? 'General Medicine',
        date,
        time,
        type: /follow-up|फॉलो.?अप|पुनःतपासणी/i.test(text) ? 'Follow-up' : 'Consultation',
        status: 'Confirmed',
      }
    : {
        patient: patient?.name ?? '',
        doctor: doctor?.name ?? '',
        dueDate: date,
        reminderDays: 0,
        channel: 'Call task',
        status: 'Scheduled',
        appointmentStatus: 'Pending',
        contactStatus: 'Not contacted',
        returnStatus: 'Not returned',
      };

  const missing = [
    !patient && 'patient',
    appointment && !time && 'time',
    appointment && !doctor && 'doctor',
  ].filter(Boolean);

  return { category: appointment ? 'appointments' : 'followups', fields, missing };
}

function getInitialLanguage() {
  try {
    const savedLanguage = window.localStorage.getItem('clinic-language');
    return COPY[savedLanguage] ? savedLanguage : 'mr';
  } catch {
    return 'mr';
  }
}

export function VoiceAssistantPage({ data, onSave, onNotify }) {
  const [language, setLanguage] = useState(getInitialLanguage);
  const [instruction, setInstruction] = useState('');
  const [listening, setListening] = useState(false);
  const [draft, setDraft] = useState(null);
  const [saving, setSaving] = useState(false);
  const recognitionRef = useRef(null);
  const strings = COPY[language];
  const speechRecognitionAvailable = Boolean(
    typeof window !== 'undefined' && (window.SpeechRecognition || window.webkitSpeechRecognition),
  );

  useEffect(() => () => {
    recognitionRef.current?.abort();
    recognitionRef.current = null;
  }, []);

  function changeLanguage(event) {
    const nextLanguage = event.target.value;
    setLanguage(nextLanguage);
    try {
      window.localStorage.setItem('clinic-language', nextLanguage);
    } catch {
      // The assistant still works if the browser blocks local storage.
    }
  }

  function startListening() {
    if (!speechRecognitionAvailable) {
      onNotify(strings.unsupported);
      return;
    }

    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Recognition) {
      onNotify(strings.unsupported);
      return;
    }
    const recognition = new Recognition();
    recognitionRef.current = recognition;
    recognition.lang = { en: 'en-IN', hi: 'hi-IN', mr: 'mr-IN' }[language];
    recognition.interimResults = false;
    recognition.onresult = event => {
      const spokenInstruction = event.results?.[0]?.[0]?.transcript;
      if (typeof spokenInstruction !== 'string' || !spokenInstruction.trim()) {
        onNotify(strings.recognitionError);
        return;
      }
      setInstruction(spokenInstruction);
      setDraft(parseInstruction(spokenInstruction, data));
    };
    recognition.onerror = () => onNotify(strings.recognitionError);
    recognition.onend = () => {
      if (recognitionRef.current === recognition) recognitionRef.current = null;
      setListening(false);
    };

    try {
      recognition.start();
      setListening(true);
    } catch {
      recognitionRef.current = null;
      setListening(false);
      onNotify(strings.recognitionError);
    }
  }

  function stopListening() {
    const recognition = recognitionRef.current;
    recognitionRef.current = null;
    setListening(false);
    recognition?.stop();
  }

  function reviewInstruction() {
    if (!instruction.trim()) {
      onNotify(strings.speakFirst);
      return;
    }
    setDraft(parseInstruction(instruction, data));
  }

  async function confirmDraft() {
    if (!draft) return;
    if (draft.missing.length > 0) {
      onNotify(`${strings.fillMissing}: ${draft.missing.join(', ')}`);
      return;
    }

    setSaving(true);
    try {
      await onSave(draft.category, draft.fields, true);
      setDraft(null);
      setInstruction('');
    } catch (error) {
      onNotify(error.message);
    } finally {
      setSaving(false);
    }
  }

  function clearDraft() {
    setInstruction('');
    setDraft(null);
  }

  function updateDraftField(field, value) {
    setDraft(current => ({
      ...current,
      fields: { ...current.fields, [field]: value },
      missing: current.missing.filter(missingField => missingField !== field),
    }));
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>{strings.title}</h1>
          <p>{strings.description}</p>
        </div>
        <Select aria-label="Assistant language" value={language} onChange={changeLanguage}>
          <option value="mr">मराठी</option>
          <option value="hi">हिन्दी</option>
          <option value="en">English</option>
        </Select>
      </div>

      <Card style={{ padding: 22, maxWidth: 800 }}>
        <div style={{ display: 'flex', gap: 9, marginBottom: 14 }}>
          <Button onClick={startListening} disabled={listening}>
            <Mic size={16} />{listening ? strings.stop : strings.start}
          </Button>
          {listening && (
            <Button variant="outline" onClick={stopListening} aria-label={strings.stop}>
              <Square size={15} />
            </Button>
          )}
        </div>

        {!speechRecognitionAvailable && <p>{strings.unsupported}</p>}
        <Input
          aria-label={strings.title}
          value={instruction}
          onChange={event => setInstruction(event.target.value)}
          placeholder={strings.instructionPlaceholder}
        />
        <div style={{ display: 'flex', gap: 9, marginTop: 12 }}>
          <Button variant="outline" onClick={reviewInstruction}>{strings.review}</Button>
          <Button variant="ghost" onClick={clearDraft}>
            <RotateCcw size={15} />{strings.clear}
          </Button>
        </div>
      </Card>

      {draft && (
        <Card style={{ padding: 22, maxWidth: 800, marginTop: 16 }}>
          <h3>{strings.draftTitle}</h3>
          <p>
            {draft.category === 'appointments' ? strings.appointment : strings.followUp}
            {' · '}
            {draft.missing.length > 0
              ? `${strings.missingFields}: ${draft.missing.join(', ')}`
              : strings.readyToReview}
          </p>
          {Object.entries(draft.fields).map(([field, value]) => (
            <label key={field} style={{ display: 'grid', gap: 5, marginBottom: 10 }}>
              {field}
              <Input value={value ?? ''} onChange={event => updateDraftField(field, event.target.value)} />
            </label>
          ))}
          <Button disabled={saving || draft.missing.length > 0} onClick={confirmDraft}>
            <Check size={15} />{strings.save}
          </Button>
        </Card>
      )}
    </>
  );
}
