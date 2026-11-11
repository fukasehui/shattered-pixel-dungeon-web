package local.portal.pd;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

public final class WebGame extends ShatteredPixelDungeon {
    @JSFunctor interface SaveNow extends JSObject {void run();}
    @JSBody(params="save",script="window.portalSaveNow=()=>save();") private static native void exposeSave(SaveNow save);
    private long nextSave;
    private final CooperativeFrame frames=new CooperativeFrame(task->new Thread(task,"SHPD Web Render").start());
    public WebGame(){super(new WebPlatform());}
    @Override public void create(){
        Gdx.files=new BrowserFiles(Gdx.files,new WebSaves());
        WebDefaults.preferences(Gdx.app.getPreferences("settings.xml"),WebVersion.CODE);
        // Messages can initialize before create(); reload bundles after binding browser preferences.
        com.shatteredpixel.shatteredpixeldungeon.messages.Messages.setup(SPDSettings.language());
        WebDefaults.unlocks();
        super.create();
        exposeSave(()->{try{if(Dungeon.hero!=null&&Dungeon.hero.ready)Dungeon.saveAll();WebSaves.flush();}catch(Exception error){reportException(error);}});
    }
    @Override public void render(){
        // WebApplication invokes render directly from JavaScript. A Java Thread
        // supplies the TeaVM coroutine needed by official scene teardown waits.
        frames.run(this::renderFrame);
    }
    private void renderFrame(){
        super.render();
        if(Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.ready){
            WebSaves.acceptUpgrade();
            long now=System.currentTimeMillis();
            if(now>=nextSave){nextSave=now+3000;try{Dungeon.saveAll();}catch(Exception error){reportException(error);}}
        }
        WebSaves.flush();
    }
    @Override public void pause(){new Thread(()->{super.pause();WebSaves.flush();},"SHPD Web Pause").start();}
    @Override public void finish(){Game.switchScene(com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene.class);}
}
