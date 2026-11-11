package local.portal.pd;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Point;
import java.util.ArrayList;
import java.util.HashMap;

public final class WebPlatform extends PlatformSupport {
    private FreeTypeFontGenerator latin,asian;
    @Override public void updateDisplaySize(){
        int w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight();
        Point old=SPDSettings.windowResolution();
        if(w>0&&h>0&&(old.x!=w||old.y!=h))SPDSettings.windowResolution(new Point(w,h));
    }
    @Override public boolean supportsFullScreen(){return false;}
    @Override public void updateSystemUI(){ }
    @Override public boolean connectedToUnmeteredNetwork(){return true;}
    @Override public boolean supportsVibration(){return false;}
    @Override public void setupFontGenerators(int pageSize,boolean systemFont){
        if(fonts!=null&&this.pageSize==pageSize&&this.systemfont==systemFont)return;
        this.pageSize=pageSize;this.systemfont=systemFont;resetGenerators(false);
        fonts=new HashMap<>();
        latin=new FreeTypeFontGenerator(Gdx.files.internal(systemFont?"fonts/droid_sans.ttf":"fonts/pixel_font.ttf"));
        asian=systemFont?latin:new FreeTypeFontGenerator(Gdx.files.internal("fonts/droid_sans.ttf"));
        fonts.put(latin,new HashMap<>());fonts.put(asian,new HashMap<>());
        packer=new PixmapPacker(pageSize,pageSize,Pixmap.Format.RGBA8888,1,false);
    }
    @Override protected FreeTypeFontGenerator getGeneratorForString(String input){
        for(int i=0;i<input.length();i++)if(isAsian(input.charAt(i)))return asian;return latin;
    }
    static boolean isAsian(char c){return(c>=0x2E80&&c<=0x9FFF)||(c>=0xAC00&&c<=0xD7AF)||(c>=0xFF00&&c<=0xFFEF);}
    @Override public String[] splitforTextBlock(String text,boolean multiline){
        ArrayList<String> out=new ArrayList<>();StringBuilder part=new StringBuilder();
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);boolean pair=c=='*'&&i+1<text.length()&&text.charAt(i+1)=='*';
            if(pair||c=='\n'||c=='_'||isAsian(c)||(multiline&&c==' ')){
                if(part.length()>0){out.add(part.toString());part.setLength(0);}out.add(pair?"**":String.valueOf(c));if(pair)i++;
            }else part.append(c);
        }
        if(part.length()>0)out.add(part.toString());return out.toArray(new String[0]);
    }
}
