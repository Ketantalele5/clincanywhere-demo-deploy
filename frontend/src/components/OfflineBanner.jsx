import { useEffect, useState } from 'react';
export function OfflineBanner(){
  const [online,setOnline]=useState(() => typeof navigator === 'undefined' || navigator.onLine);
  useEffect(()=>{
    const up=()=>setOnline(true),down=()=>setOnline(false);
    window.addEventListener('online',up);window.addEventListener('offline',down);
    return()=>{window.removeEventListener('online',up);window.removeEventListener('offline',down)};
  },[]);
  return online?null:<div role="status" style={{position:'sticky',top:78,zIndex:7,padding:'9px 16px',background:'#fff1d7',color:'#68450b',borderBottom:'1px solid #ead6a6'}}>Your device is offline. Clinic data is unavailable and changes are not saved locally. Reconnect before continuing.</div>;
}
