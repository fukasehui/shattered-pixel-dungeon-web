package local.portal.pd;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;
import com.badlogic.gdx.Gdx;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.badlogic.gdx.Files;

public class NativeSaveTest {
    static class Memory implements SaveAccess {
        Map<String,String> data=new HashMap<>();
        public String read(String p){return data.get(p);}public void write(String p,String b){data.put(p,b);}
        public void remove(String p){data.remove(p);}public String[] paths(){return data.keySet().toArray(new String[0]);}
        public void move(String f,String t){data.put(t,data.remove(f));}
    }
    @Test public void officialBundleRoundTripsAndAtomicTempRenameWorks() throws Exception {
        Memory saves=new Memory();Gdx.files=new BrowserFiles(null,saves);FileUtils.setDefaultFileProperties(Files.FileType.Local,"");
        Bundle first=new Bundle();first.put("version",896);first.put("depth",7);FileUtils.bundleToFile("game1/game.dat",first);
        assertEquals(896,FileUtils.bundleFromFile("game1/game.dat").getInt("version"));
        Bundle next=new Bundle();next.put("version",922);next.put("depth",8);FileUtils.bundleToFile("game1/game.dat",next);
        assertEquals(8,FileUtils.bundleFromFile("game1/game.dat").getInt("depth"));
        assertNull(saves.read("game1/game.dat.spdtmp"));
    }
    @Test public void preferencesSurviveAReopenWithNativeTypes(){
        Memory saves=new Memory();BrowserPreferences prefs=new BrowserPreferences("settings.xml",saves);
        prefs.putBoolean("intro",false).putInteger("version",922).putString("language","zh").flush();
        BrowserPreferences restored=new BrowserPreferences("settings.xml",saves);
        assertFalse(restored.getBoolean("intro",true));assertEquals(922,restored.getInteger("version"));assertEquals("zh",restored.getString("language"));
    }
}
