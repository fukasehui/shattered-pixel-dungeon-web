package local.portal.pd;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Preferences;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;
import java.util.HashMap;
import java.util.Map;

public final class Launcher {
    private static final Map<String,Preferences> prefs=new HashMap<>();
    public static void main(String[] args){
        Game.version=WebVersion.NAME+"-WEB";Game.versionCode=WebVersion.CODE;
        FileUtils.setDefaultFileProperties(Files.FileType.Local,"");
        WebApplicationConfiguration config=new WebApplicationConfiguration("canvas");
        config.width=0;config.height=0;config.useGL30=true;
        config.storagePrefix="portal-pd-engine";config.localStoragePrefix="portal-pd-engine";
        config.showDownloadLogs=false;
        config.preloadListener=loader->loader.loadScript("freetype.js");
        new WebApplication(new WebGame(),config){
            @Override public Preferences getPreferences(String name){
                Preferences result=prefs.get(name);
                if(result==null){result=new BrowserPreferences(name,new WebSaves());prefs.put(name,result);}return result;
            }
        };
    }
}
