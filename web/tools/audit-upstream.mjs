import fs from 'node:fs';import path from 'node:path';import {fileURLToPath} from 'node:url';import {execFileSync} from 'node:child_process';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const meta=JSON.parse(fs.readFileSync(path.join(root,'web/upstream.json')));
const git=(...args)=>execFileSync('git',args,{cwd:root,encoding:'utf8'}).trim();
if(!/^v\d+\.\d+\.\d+$/.test(meta.tag)||git('rev-parse',meta.tag+'^{}')!==meta.upstreamCommit)throw new Error('Official tag/commit mismatch');
const patches={
 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/GamesInProgress.java':[
  ['public static final int MAX_SLOTS = HeroClass.values().length;','public static final int MAX_SLOTS = 1; // Web portal: one current run; native save serialization unchanged.']],
 'SPD-classes/src/main/java/com/watabou/utils/DeviceCompat.java':[
  ['return SharedLibraryLoader.os == Os.Android;','return Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android;'],
  ['return SharedLibraryLoader.os == Os.IOS;','return Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.iOS;'],
  ['return SharedLibraryLoader.os == Os.Windows || SharedLibraryLoader.os == Os.MacOsX || SharedLibraryLoader.os == Os.Linux;','return Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop\n\t\t\t\t|| Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.HeadlessDesktop;']],
};
const changed=git('diff','--name-only',meta.tag,'--','core','SPD-classes','services','desktop','build.gradle','settings.gradle').split('\n').filter(Boolean);
for(const f of changed){
 const actual=fs.readFileSync(path.join(root,f),'utf8').replaceAll('\r\n','\n');
 const original=git('show',meta.tag+':'+f).replaceAll('\r\n','\n')+'\n';
 if(actual===original)continue;
 if(!patches[f])throw new Error('Unexpected official-source change: '+f);
 let expected=original;
 for(const [from,to] of patches[f]){if(!expected.includes(from))throw new Error('Review changed upstream patch: '+f);expected=expected.replace(from,to);}
 if(actual!==expected)throw new Error('Patch differs from allowlist: '+f);
}
console.log(JSON.stringify({officialTag:meta.tag,officialCommit:meta.upstreamCommit,compatibilityPatches:changed}));
