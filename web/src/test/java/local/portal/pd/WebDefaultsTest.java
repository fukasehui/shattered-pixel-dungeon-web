package local.portal.pd;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;
import java.lang.reflect.Proxy;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;

public class WebDefaultsTest {
    @Test public void defaultsPersistWithoutReplacingExistingPreferences(){
        NativeSaveTest.Memory memory=new NativeSaveTest.Memory();BrowserPreferences prefs=new BrowserPreferences("settings.xml",memory);
        WebDefaults.preferences(prefs);
        BrowserPreferences restored=new BrowserPreferences("settings.xml",memory);
        assertEquals("zh",restored.getString("language"));assertTrue(restored.getBoolean("intro",true));assertFalse(restored.contains("version"));
        prefs.putString("language","fr").putInteger("version",896).putInteger("music_vol",3);
        WebDefaults.preferences(prefs);
        assertEquals("fr",prefs.getString("language"));assertEquals(896,prefs.getInteger("version"));assertEquals(3,prefs.getInteger("music_vol"));
    }
    @Test public void allHeroesAndRecipePagesUnlockWhileRunBytesStayUntouched(){
        Application previousApp=Gdx.app;Files previousFiles=Gdx.files;
        try{
            NativeSaveTest.Memory memory=new NativeSaveTest.Memory();memory.write("game1/game.dat","cHJlc2VydmU=");
            BrowserPreferences prefs=new BrowserPreferences("settings.xml",memory);
            Game.version="4.0.2-WEB";Game.versionCode=922;
            Gdx.app=(Application)Proxy.newProxyInstance(Application.class.getClassLoader(),new Class[]{Application.class},
                (o,m,a)->m.getName().equals("getType")?Application.ApplicationType.WebGL:m.getName().equals("getPreferences")?prefs:null);
            Files internal=(Files)Proxy.newProxyInstance(Files.class.getClassLoader(),new Class[]{Files.class},
                (o,m,a)->m.getName().equals("internal")?new FileHandle(new File("../core/src/main/assets",(String)a[0])):null);
            Gdx.files=new BrowserFiles(internal,memory);FileUtils.setDefaultFileProperties(Files.FileType.Local,"");
            com.watabou.utils.GameSettings.set(prefs);Messages.setup(Languages.ENGLISH);
            WebDefaults.preferences(prefs);Messages.setup(SPDSettings.language());WebDefaults.unlocks();
            for(HeroClass hero:HeroClass.values())assertTrue(hero.name(),hero.isUnlocked());
            for(String page:Document.ALCHEMY_GUIDE.pageNames())assertTrue(page,Document.ALCHEMY_GUIDE.isPageRead(page));
            assertEquals(Languages.CHI_SMPL,Messages.lang());assertEquals("cHJlc2VydmU=",memory.read("game1/game.dat"));
            assertTrue(SPDSettings.intro());assertFalse(Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_INTRO));
            assertNotNull(memory.read("badges.dat"));assertNotNull(memory.read("journal.dat"));
        }finally{Gdx.app=previousApp;Gdx.files=previousFiles;}
    }
}
