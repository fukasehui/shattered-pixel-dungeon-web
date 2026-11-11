import test from 'node:test';
import assert from 'node:assert/strict';
import {createManagementView} from '../portal/management.mjs';

function page(href){
  const classes=new Set(),changes=[];
  const document={documentElement:{classList:{toggle(name,on){if(on)classes.add(name);else classes.delete(name);}}}};
  const location={href};
  const history={replaceState(state,title,url){changes.push(url);location.href=new URL(url,location.href).href;}};
  return {view:createManagementView({document,location,history}),classes,changes};
}
test('opening and closing management preserves URL parameters without navigating or reloading',()=>{
  const {view,classes,changes}=page('https://example.com/game/?theme=pixel#menu');
  assert.equal(classes.has('management'),false);
  view.open();assert.equal(classes.has('management'),true);
  assert.equal(changes.at(-1),'/game/?theme=pixel&manage=1#menu');
  view.close();assert.equal(classes.has('management'),false);
  assert.equal(changes.at(-1),'/game/?theme=pixel#menu');
});
test('direct management URL starts with controls visible and closes back to the game',()=>{
  const {view,classes,changes}=page('http://localhost:8082/?manage=1');
  assert.equal(classes.has('management'),true);
  assert.deepEqual(changes,[]);
  view.close();assert.equal(changes.at(-1),'/');
  view.open();assert.equal(changes.at(-1),'/?manage=1');
});
