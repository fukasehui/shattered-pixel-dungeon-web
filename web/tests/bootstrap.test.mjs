import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';import vm from 'node:vm';
test('browser entry starts the compiled game after page load',()=>{
 const source=fs.readFileSync(new URL('../src/main/resources/webapp/index.html',import.meta.url),'utf8').replace('%MODE%','main()');
 const listeners=new Map();let calls=0;
 const context={window:{addEventListener:(name,fn)=>listeners.set(name,fn)},parent:{portalStore:{}},document:{addEventListener(){},getElementById(){return{focus(){}};}},main(){calls++;}};
 for(const [,script] of source.matchAll(/<script>([\s\S]*?)<\/script>/g))vm.runInNewContext(script,context);
 listeners.get('load')?.();assert.equal(calls,1);
});
