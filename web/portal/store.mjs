// Browser-only persistence adapter. Native game files remain opaque base64 bytes.
export const CURRENT_KEY = 'portal-pd.current.v1';
export const UPGRADE_KEY = 'portal-pd.upgrade.v1';
export const RECOVERY_KEY = 'portal-pd.replacement.v1';
export const LOCK_KEY = 'portal-pd.writer.v1';
const MAX_PROFILE = 2 * 1024 * 1024;
const textEncoder = new TextEncoder();
const textDecoder = new TextDecoder();
const clone = value => JSON.parse(JSON.stringify(value));
function pathCheck(path) {
  if (typeof path !== 'string' || !path || path.length > 240 || path.includes('\\') || path.startsWith('/')
      || path.split('/').some(part => !part || part === '.' || part === '..')) throw new Error('Invalid save path');
  if (/^game\d+\//.test(path) && !path.startsWith('game1/')) throw new Error('Only one active save slot is supported');
}
function base64Check(value) {
  if (typeof value !== 'string' || value.length > MAX_PROFILE || !/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(value)) throw new Error('Invalid base64 file');
}
function validate(profile) {
  if (!profile || profile.format !== 'portal-pd-save' || profile.schema !== 1 || !Number.isInteger(profile.engine?.code)
      || !profile.files || typeof profile.files !== 'object' || Array.isArray(profile.files)) throw new Error('Invalid or corrupt save profile / 存档格式无效');
  const entries = Object.entries(profile.files);
  if (entries.length > 200) throw new Error('Too many save files');
  for (const [path, data] of entries) { pathCheck(path); base64Check(data); }
  if (profile.dead !== undefined && typeof profile.dead !== 'boolean') throw new Error('Invalid death state');
  if (profile.floorCheckpoint) {
    const cp=profile.floorCheckpoint;
    if(typeof cp.key!=='string'||cp.key.length>160||!Number.isInteger(cp.code)||!Number.isInteger(cp.depth)
      ||typeof cp.levelPath!=='string'||!/^game1\/depth[^/]*\.dat$/.test(cp.levelPath)
      ||!cp.files||Object.keys(cp.files).length!==2||!cp.files['game1/game.dat']||!cp.files[cp.levelPath])throw new Error('Invalid floor checkpoint');
    for(const [p,b] of Object.entries(cp.files)){pathCheck(p);if(p!=='game1/game.dat'&&p!==cp.levelPath)throw new Error('Invalid checkpoint path');base64Check(b);}
  }
  if (JSON.stringify(profile).length > MAX_PROFILE) throw new Error('Save profile too large');
  return clone(profile);
}
export function createStore(storage, engine) {
  const original = storage.getItem(CURRENT_KEY);
  let profile;
  try { profile = original ? validate(JSON.parse(original)) : {format:'portal-pd-save',schema:1,engine:clone(engine),files:{}}; }
  catch { throw new Error('存档损坏 / corrupt profile：请先导出保留，未覆盖原数据。'); }
  if (profile.engine.code > engine.code) throw new Error('存档来自新版本 / newer engine，当前旧版不能读取。');
  let migrationPending = profile.engine.code < engine.code && !!profile.files['game1/game.dat']
    && profile.files['game1/game.dat'].length > 4;
  let dirty = false;
  let retired = false;
  let upgradeBackedUp = false;
  let canWrite = () => true;
  let activeFloorKey=profile.dead?profile.deathFloorKey:null;
  const mutable=()=>{if(retired||!canWrite())throw new Error('当前页面已停止写入，请重新打开游戏。');};
  function replaceAndRetire(next,inPlace=false){mutable();const raw=JSON.stringify(validate(next));storage.setItem(CURRENT_KEY,raw);profile=next;dirty=false;retired=!inPlace;activeFloorKey=next.floorCheckpoint?.key??null;}
  const store = {
    get dead(){return !!profile.dead;},
    get checkpointDepth(){return profile.floorCheckpoint?.depth??null;},
    get canRetry(){return !!profile.floorCheckpoint&&profile.floorCheckpoint.key===activeFloorKey&&profile.floorCheckpoint.code<=engine.code;},
    get migrationPending() { return migrationPending; },
    read(path) { pathCheck(path); return profile.files[path] ?? null; },
    write(path, value) { if(retired||profile.dead&&path.startsWith('game1/'))return;pathCheck(path); base64Check(value); profile.files[path] = value;if(path==='game1/game.dat'&&value.length<=4)delete profile.floorCheckpoint; dirty = true; },
    remove(path) { if(retired||profile.dead&&path.startsWith('game1/'))return;pathCheck(path); delete profile.files[path];if(path==='game1/game.dat'&&!profile.files[path+'.spdtmp'])delete profile.floorCheckpoint; dirty = true; },
    paths() { return Object.keys(profile.files); },
    move(from, to) { if(retired||profile.dead&&to.startsWith('game1/'))return;pathCheck(from); pathCheck(to); if (!(from in profile.files)) throw new Error('Missing source file'); profile.files[to] = profile.files[from]; delete profile.files[from]; dirty = true; },
    acceptUpgrade() { migrationPending = false; dirty = true; },
    setWriteGuard(guard) { canWrite = guard; },
    flush() {
      if (retired || !dirty || migrationPending) return false;
      if (!canWrite()) throw new Error('另一个页面正在使用此存档，已暂停写入。');
      const next = {...profile,engine:clone(engine),savedAt:new Date().toISOString()};
      const raw = JSON.stringify(validate(next));
      if (original && profile.engine.code < engine.code && !upgradeBackedUp) {
        storage.setItem(UPGRADE_KEY, original); upgradeBackedUp = true;
      }
      storage.setItem(CURRENT_KEY, raw);
      profile = next; dirty = false; return true;
    },
    export() { return clone(profile); },
    captureFloor(key,gamePath,levelPath,depth){
      mutable();activeFloorKey=key;
      if(profile.dead)return false;
      if(profile.floorCheckpoint?.key===key)return true;
      if(!profile.files[gamePath]||!profile.files[levelPath])throw new Error('本层存档尚未写入，无法建立检查点。');
      const previous=profile,wasDirty=dirty;
      const next={...profile,floorCheckpoint:{key,code:engine.code,depth,levelPath,files:{[gamePath]:profile.files[gamePath],[levelPath]:profile.files[levelPath]}}};
      validate(next);profile=next;dirty=true;
      try{store.flush();return true;}catch(error){profile=previous;dirty=wasDirty;throw error;}
    },
    markDeath(){
      mutable();profile.dead=true;profile.deathFloorKey=activeFloorKey;dirty=true;
      // A real death also establishes that the native engine restored the run.
      migrationPending=false;store.flush();
    },
    retryFloor({inPlace=false}={}){
      if(!store.canRetry)throw new Error('没有可用的本层检查点，请先导出存档。');
      const next={...clone(profile),files:{...profile.files,...profile.floorCheckpoint.files},dead:false};
      delete next.deathFloorKey;replaceAndRetire(next,inPlace);
    },
    endRun({inPlace=false}={}){
      const next=clone(profile);for(const p of Object.keys(next.files))if(p.startsWith('game1/'))delete next.files[p];
      delete next.floorCheckpoint;delete next.deathFloorKey;delete next.dead;replaceAndRetire(next,inPlace);
    },
    install(input) {
      const imported = validate(input);
      if (imported.engine.code > engine.code) throw new Error('Cannot import a newer engine save');
      if (!canWrite()) throw new Error('Another page owns this save');
      const previous = storage.getItem(CURRENT_KEY);
      if (previous) storage.setItem(RECOVERY_KEY, previous);
      storage.setItem(CURRENT_KEY, JSON.stringify(imported));
      profile=imported;dirty=false;retired=true;
    },
  };
  return store;
}
function bytesFromBase64(value) { const binary=atob(value); return Uint8Array.from(binary,c=>c.charCodeAt(0)); }
function toBase64(bytes) { let out=''; for(let i=0;i<bytes.length;i+=8192)out+=String.fromCharCode(...bytes.subarray(i,i+8192)); return btoa(out); }
export async function importLegacy(input) {
  if (input?.format === 'portal-pd-save') return validate(input);
  if (input?.format !== 'shattered-pixel-dungeon-web-browser-data' || input.formatVersion !== 1
      || !Array.isArray(input.localStorage)) throw new Error('Unsupported backup format');
  const files = {}; const settings = {};
  // The community sample's durable mirror contains native game bytes, not a new gameplay format.
  for (const item of input.localStorage) {
    const mirror = item.key?.match(/^pref:app:web-save-mirror:file\.(.+)s$/);
    if (mirror) {
      const path = textDecoder.decode(bytesFromBase64(mirror[1]));
      if (path.startsWith('game1/')) {pathCheck(path);base64Check(item.value);files[path]=item.value;}
    }
    const pref = item.key?.match(/^pref:app:settings\.xml:(.+)([bilfs])$/);
    if (pref) settings[pref[1]] = pref[2] === 'b' ? item.value === 'true' : pref[2] === 's' ? item.value : Number(item.value);
  }
  // Other native files can also be recovered from the exported IndexedDB inventory.
  const records = Array.isArray(input.indexedDb) ? input.indexedDb : input.indexedDb?.records || [];
  for (const record of records) {
    const path = String(record.key || '').replace(/^.*shattered-pixel-dungeon\//,'').replace(/^\//,'');
    const data = record.contents ?? record.data;
    if (path.startsWith('game1/') && typeof data === 'string' && !(path in files)) {pathCheck(path);base64Check(data);files[path]=data;}
  }
  if (!files['game1/game.dat']) throw new Error('Backup has no current game1 save');
  let bytes = bytesFromBase64(files['game1/game.dat']);
  if (bytes[0] === 31 && bytes[1] === 139) bytes = new Uint8Array(await new Response(new Blob([bytes]).stream().pipeThrough(new DecompressionStream('gzip'))).arrayBuffer());
  const native = JSON.parse(textDecoder.decode(bytes));
  if (!Number.isInteger(native.version)) throw new Error('Native game version missing');
  settings.intro = false;
  files['preferences/settings.xml.json'] = toBase64(textEncoder.encode(JSON.stringify(settings)));
  return validate({format:'portal-pd-save',schema:1,engine:{version:input.gameVersion,code:native.version},files});
}
export function acquireWriter(storage, owner, now = () => Date.now()) {
  let started=false,lost=false;
  function current() {try{return JSON.parse(storage.getItem(LOCK_KEY)||'null');}catch{return null;}}
  function renew() {
    const lock=current();
    if(lost)return false;
    if(started&&(!lock||lock.owner!==owner)){lost=true;return false;}
    if(lock&&lock.owner!==owner&&lock.expires>now())return false;
    storage.setItem(LOCK_KEY,JSON.stringify({owner,expires:now()+10000}));
    started=true;return current()?.owner===owner;
  }
  const acquired=renew();
  if(!acquired)lost=true;
  return {acquired, owns:()=>!lost&&current()?.owner===owner&&current()?.expires>now(), renew,release(){if(current()?.owner===owner)storage.removeItem(LOCK_KEY);lost=true;}};
}
