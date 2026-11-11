// randomUUID is secure-context-only; getRandomValues is available on this HTTP game origin.
// Preserve the native implementation on HTTPS and other supporting contexts.
(function(){
  if(typeof crypto.randomUUID==='function')return;
  crypto.randomUUID=function(){
    const bytes=new Uint8Array(16);crypto.getRandomValues(bytes);
    bytes[6]=(bytes[6]&15)|64;bytes[8]=(bytes[8]&63)|128;
    const hex=Array.from(bytes,b=>b.toString(16).padStart(2,'0'));
    return hex.slice(0,4).join('')+'-'+hex.slice(4,6).join('')+'-'+hex.slice(6,8).join('')+'-'+hex.slice(8,10).join('')+'-'+hex.slice(10).join('');
  };
})();
