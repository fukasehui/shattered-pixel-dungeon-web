import test from 'node:test';
import assert from 'node:assert/strict';
import { createStore, importLegacy, acquireWriter, CURRENT_KEY, UPGRADE_KEY, RECOVERY_KEY } from '../portal/store.mjs';

class Storage {
  data = new Map(); fail = false;
  getItem(key) { return this.data.get(key) ?? null; }
  setItem(key, value) { if (this.fail) throw new Error('quota'); this.data.set(key, value); }
  removeItem(key) { this.data.delete(key); }
}
const current = { version: '4.0.2', code: 922, upstreamCommit: 'official', adapter: 1 };
const encoded = text => Buffer.from(text).toString('base64');
const profile = (code = 922) => ({ format: 'portal-pd-save', schema: 1, engine: { ...current, code }, files: { 'game1/game.dat': encoded('{"version":896}') } });

test('one profile persists native file bytes and file rename atomically', () => {
  const storage = new Storage(); const store = createStore(storage, current);
  store.write('game1/game.dat.spdtmp', encoded('native bytes'));
  store.move('game1/game.dat.spdtmp', 'game1/game.dat'); store.flush();
  assert.equal(store.read('game1/game.dat'), encoded('native bytes'));
  assert.equal(store.read('game1/game.dat.spdtmp'), null);
  assert.equal(JSON.parse(storage.getItem(CURRENT_KEY)).files['game1/game.dat'], encoded('native bytes'));
});
test('reopening preserves the one run and rejects a second run directory', () => {
  const storage = new Storage(); const store = createStore(storage, current);
  store.write('game1/game.dat', encoded('save')); store.flush();
  assert.equal(createStore(storage, current).read('game1/game.dat'), encoded('save'));
  assert.throws(() => store.write('game2/game.dat', encoded('other')), /one|slot/);
});
test('quota failure leaves the durable profile unchanged', () => {
  const storage = new Storage(); storage.setItem(CURRENT_KEY, JSON.stringify(profile()));
  const original = storage.getItem(CURRENT_KEY); const store = createStore(storage, current);
  store.write('game1/game.dat', encoded('changed')); storage.fail = true;
  assert.throws(() => store.flush(), /quota/);
  assert.equal(storage.getItem(CURRENT_KEY), original);
});
test('upgrade uses a copy; cannot overwrite old data before successful native load', () => {
  const storage = new Storage(); storage.setItem(CURRENT_KEY, JSON.stringify(profile(896)));
  const original = storage.getItem(CURRENT_KEY); const store = createStore(storage, current);
  store.write('preferences/settings.xml.json', encoded('{}')); store.flush();
  assert.equal(storage.getItem(CURRENT_KEY), original);
  store.acceptUpgrade(); store.flush();
  assert.equal(storage.getItem(UPGRADE_KEY), original);
  assert.equal(JSON.parse(storage.getItem(CURRENT_KEY)).engine.code, 922);
});
test('older engine cannot open a newer profile; corrupted data is preserved', () => {
  const storage = new Storage(); storage.setItem(CURRENT_KEY, JSON.stringify(profile(999)));
  assert.throws(() => createStore(storage, current), /newer|新版本/);
  storage.setItem(CURRENT_KEY, 'broken');
  assert.throws(() => createStore(storage, current), /corrupt|损坏/);
  assert.equal(storage.getItem(CURRENT_KEY), 'broken');
});
test('backup validation rejects traversal and invalid base64', () => {
  const storage = new Storage(); const store = createStore(storage, current);
  assert.throws(() => store.write('../private', encoded('x')), /path/);
  assert.throws(() => store.write('game1/game.dat', 'not base64!'), /base64/);
});
test('legacy export imports native files and settings without changing source', async () => {
  const source = { format:'shattered-pixel-dungeon-web-browser-data',formatVersion:1,gameVersion:'3.3.8-WEB',localStorage:[
    {key:'pref:app:web-save-mirror:file.' + encoded('game1/game.dat') + 's',value:encoded('{"version":896}')},
    {key:'pref:app:settings.xml:introb',value:'false'},
  ],indexedDb:[] };
  const before = JSON.stringify(source); const imported = await importLegacy(source);
  assert.equal(imported.engine.code,896);
  assert.equal(imported.files['game1/game.dat'],encoded('{"version":896}'));
  assert.equal(JSON.parse(Buffer.from(imported.files['preferences/settings.xml.json'],'base64')).intro,false);
  assert.equal(JSON.stringify(source),before);
});
test('old callbacks cannot overwrite an installed replacement during page unload',()=>{
 const storage=new Storage(),store=createStore(storage,current);
 store.write('game1/game.dat',encoded('old'));store.flush();
 const replacement=profile();replacement.files['game1/game.dat']=encoded('imported');store.install(replacement);
 store.write('game1/game.dat',encoded('old unload callback'));store.flush();
 assert.equal(JSON.parse(storage.getItem(CURRENT_KEY)).files['game1/game.dat'],encoded('imported'));
});
test('blocked second tab cannot replace saves',()=>{
 const storage=new Storage(),a=acquireWriter(storage,'a',()=>0),b=acquireWriter(storage,'b',()=>0);
 assert.equal(a.acquired,true);assert.equal(b.acquired,false);
 const blocked=createStore(storage,current);blocked.setWriteGuard(b.owns);
 assert.throws(()=>blocked.install(profile()),/owns|Another/);assert.equal(storage.getItem(CURRENT_KEY),null);
});
test('expired writer can never reacquire its stale in-memory profile',()=>{
 const storage=new Storage();let now=0;const a=acquireWriter(storage,'a',()=>now);
 now=11000;const b=acquireWriter(storage,'b',()=>now);assert.equal(b.acquired,true);b.release();
 assert.equal(a.renew(),false);assert.equal(a.owns(),false);
});
test('replaced progress survives a subsequent older-save migration',()=>{
 const storage=new Storage(),currentStore=createStore(storage,current);
 currentStore.write('game1/game.dat',encoded('current progress'));currentStore.flush();const original=storage.getItem(CURRENT_KEY);
 currentStore.install(profile(896));const migrated=createStore(storage,current);migrated.acceptUpgrade();migrated.flush();
 assert.equal(storage.getItem(RECOVERY_KEY),original);
 assert.equal(JSON.parse(storage.getItem(UPGRADE_KEY)).engine.code,896);
});
