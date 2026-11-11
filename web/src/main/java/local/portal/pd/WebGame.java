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
    public WebGame(){super(new WebPlatform());}
    @Override public void create(){
        Gdx.files=new BrowserFiles(Gdx.files,new WebSaves());
        if(SPDSettings.intro()&&GamesInProgress.gameExists(1))SPDSettings.intro(false);
        if(!Gdx.app.getPreferences("settings.xml").contains("language"))SPDSettings.language(Languages.CHI_SMPL);
        super.create();
        exposeSave(()->{try{if(Dungeon.hero!=null&&Dungeon.hero.ready)Dungeon.saveAll();WebSaves.flush();}catch(Exception error){reportException(error);}});
    }
    @Override public void render(){
        super.render();
        if(Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.ready){
            WebSaves.acceptUpgrade();
            long now=System.currentTimeMillis();
            if(now>=nextSave){nextSave=now+3000;try{Dungeon.saveAll();}catch(Exception error){reportException(error);}}
        }
        WebSaves.flush();
    }
    @Override public void pause(){super.pause();WebSaves.flush();}
    @Override public void finish(){Game.switchScene(com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene.class);}
}
