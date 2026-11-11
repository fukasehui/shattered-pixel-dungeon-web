package local.portal.pd;

import com.badlogic.gdx.Preferences;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.watabou.utils.GameSettings;

/** Additive browser defaults. Never creates or replaces a character run. */
final class WebDefaults {
    static void preferences(Preferences prefs,int version){
        if(!prefs.contains(SPDSettings.KEY_LANG))prefs.putString(SPDSettings.KEY_LANG,Languages.CHI_SMPL.code());
        if(!prefs.contains(SPDSettings.KEY_VERSION))prefs.putInteger(SPDSettings.KEY_VERSION,version);
        prefs.putBoolean("intro",false).flush();
        GameSettings.set(prefs);
    }
    static void unlocks(){
        Badges.loadGlobal();Journal.loadGlobal();
        for(Badges.Badge badge:Badges.Badge.values()){
            if(badge.name().startsWith("UNLOCK_"))Badges.unlock(badge);
        }
        for(String page:Document.ALCHEMY_GUIDE.pageNames()){
            if(!Document.ALCHEMY_GUIDE.isPageRead(page))Document.ALCHEMY_GUIDE.readPage(page);
        }
        if(!Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_INTRO))Document.ADVENTURERS_GUIDE.readPage(Document.GUIDE_INTRO);
        Badges.saveGlobal();Journal.saveGlobal();
    }
}
