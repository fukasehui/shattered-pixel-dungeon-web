package local.portal.pd;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.StartScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Scene;
import com.watabou.noosa.Game;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

public final class WebGame extends ShatteredPixelDungeon {
    @JSFunctor interface SaveNow extends JSObject {void run();}
    @JSBody(params="save",script="window.portalSaveNow=()=>save();") private static native void exposeSave(SaveNow save);
    private long nextSave;
    private Scene lastDeathDialogScene;
    private Scene saveManagementScene;
    private boolean deathMenu;
    private final CooperativeFrame frames=new CooperativeFrame(task->new Thread(task,"SHPD Web Render").start());
    public WebGame(){super(new WebPlatform());}
    @Override public void create(){
        Gdx.files=new BrowserFiles(Gdx.files,new WebSaves());
        WebDefaults.preferences(Gdx.app.getPreferences("settings.xml"));
        // Messages can initialize before create(); reload bundles after binding browser preferences.
        com.shatteredpixel.shatteredpixeldungeon.messages.Messages.setup(SPDSettings.language());
        WebDefaults.unlocks();
        super.create();
        exposeSave(()->{try{if(Dungeon.hero!=null&&Dungeon.hero.isAlive()&&Dungeon.hero.ready)Dungeon.saveAll();WebSaves.flush();}catch(Exception error){reportException(error);}});
    }
    @Override public void render(){
        // WebApplication invokes render directly from JavaScript. A Java Thread
        // supplies the TeaVM coroutine needed by official scene teardown waits.
        frames.run(this::renderFrame);
    }
    private void renderFrame(){
        super.render();
        if(Game.scene() instanceof TitleScene&&Game.scene()!=saveManagementScene){
            saveManagementScene=Game.scene();
            StyledButton saves=new StyledButton(Chrome.Type.GREY_BUTTON_TR,"存档管理",8){
                @Override protected void onClick(){WebSaves.openManagement();}
            };
            saves.setRect(Camera.main.width-62,4,58,18);
            PixelScene.align(saves);
            Game.scene().add(saves);
        }
        if(WebSaves.isDead()&&!Game.switchingScene()&&Game.scene()!=lastDeathDialogScene
                &&(Game.scene() instanceof GameScene||Game.scene() instanceof TitleScene&&!deathMenu))showDeathOptions(null);
        if(Game.scene() instanceof GameScene&&Dungeon.hero!=null&&Dungeon.hero.isAlive()&&Dungeon.hero.ready){
            WebSaves.acceptUpgrade();
            long now=System.currentTimeMillis();
            if(now>=nextSave){nextSave=now+3000;try{Dungeon.saveAll();}catch(Exception error){reportException(error);}}
        }
        WebSaves.flush();
    }
    @Override protected void step(){
        // A pending death may be inspected from the title screen, but must not
        // resume the last alive autosave until the player explicitly retries.
        if(WebSaves.isDead()&&Game.switchingScene()&&(sceneClass==StartScene.class
                ||sceneClass==HeroSelectScene.class||sceneClass==InterlevelScene.class)){
            deathMenu=false;lastDeathDialogScene=null;Game.switchScene(TitleScene.class);
        }
        super.step();
    }
    private void stopActors(){
        if(Game.scene() instanceof GameScene&&!((GameScene)Game.scene()).waitForActorThread(4500,true))
            throw new IllegalStateException("角色行动尚未结束，请稍后重试。");
    }
    private void showDeathOptions(String error){
        lastDeathDialogScene=Game.scene();
        String message=WebSaves.canRetry()?"存档已保留。重试将恢复第 "+WebSaves.checkpointDepth()+" 层进入时的生命、背包和地图。":"存档已保留，但当前没有可用的本层检查点。";
        if(error!=null)message="操作失败："+error+"\n\n"+message;
        Game.scene().add(new WndOptions("冒险结束",message,"从本层开头重试","导出存档","返回主菜单","结束本局"){
            @Override protected boolean enabled(int index){return index!=0||WebSaves.canRetry();}
            @Override protected void onSelect(int index){
                try{
                    if(index==1){WebSaves.exportSave();showDeathOptions(null);return;}
                    stopActors();
                    if(index==0){
                        WebSaves.retryFloor();deathMenu=false;lastDeathDialogScene=null;
                        GamesInProgress.curSlot=1;GamesInProgress.setUnknown(1);
                        InterlevelScene.mode=InterlevelScene.Mode.CONTINUE;Game.switchScene(InterlevelScene.class);
                    }else{
                        if(index==3){WebSaves.endRun();GamesInProgress.delete(1);}
                        deathMenu=true;Game.switchScene(TitleScene.class);
                    }
                }catch(Throwable failure){showDeathOptions(failure.getMessage());}
            }
            @Override public void onBackPressed(){hide();onSelect(2);}
        });
    }
    @Override public void pause(){new Thread(()->{super.pause();WebSaves.flush();},"SHPD Web Pause").start();}
    @Override public void finish(){Game.switchScene(com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene.class);}
}
