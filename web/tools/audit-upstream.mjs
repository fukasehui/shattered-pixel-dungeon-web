import fs from 'node:fs';import path from 'node:path';import {fileURLToPath} from 'node:url';import {execFileSync} from 'node:child_process';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const meta=JSON.parse(fs.readFileSync(path.join(root,'web/upstream.json')));
const git=(...args)=>execFileSync('git',args,{cwd:root,encoding:'utf8'}).trim();
if(!/^v\d+\.\d+\.\d+$/.test(meta.tag)||git('rev-parse',meta.tag+'^{}')!==meta.upstreamCommit)throw new Error('Official tag/commit mismatch');
const scopes=['core','SPD-classes','services','desktop','build.gradle','settings.gradle'];
const actual=git('diff','--no-ext-diff',meta.tag,'--',...scopes),approved=fs.readFileSync(path.join(root,'web/patches/compatibility.patch'),'utf8');
// Ignore line offsets and object hashes only; changed compatibility code requires review.
const normalize=text=>text.replaceAll('\r\n','\n').replace(/^index .*\n/gm,'').replace(/^@@.*@@.*$/gm,'@@').trim();
if(normalize(actual)!==normalize(approved))throw new Error('Official-source compatibility patch changed; review web/patches/compatibility.patch before release.');
console.log(JSON.stringify({officialTag:meta.tag,officialCommit:meta.upstreamCommit,compatibilityPatches:git('diff','--name-only',meta.tag,'--',...scopes).split('\n').filter(Boolean)}));