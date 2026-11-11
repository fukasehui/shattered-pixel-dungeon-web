package local.portal.pd;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.utils.DeviceCompat;
import java.lang.reflect.Proxy;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlatformDetectionTest {
    @Test public void browserRenderCallbackAllowsCooperativeActorThreadName(){
        Application previous=Gdx.app;String name=Thread.currentThread().getName();
        try{
            Gdx.app=(Application)Proxy.newProxyInstance(Application.class.getClassLoader(),new Class[]{Application.class},
                (object,method,args)->method.getName().equals("getType")?Application.ApplicationType.WebGL:null);
            Thread.currentThread().setName("SHPD Actor Thread");
            assertEquals("",new com.watabou.noosa.RenderedText("",8).text());
        }finally{Gdx.app=previous;Thread.currentThread().setName(name);}
    }
    @Test public void browserDoesNotUseHostNativeOperatingSystem(){
        Application previous=Gdx.app;
        try{
            Gdx.app=(Application)Proxy.newProxyInstance(Application.class.getClassLoader(),new Class[]{Application.class},
                (object,method,args)->method.getName().equals("getType")?Application.ApplicationType.WebGL:null);
            assertFalse(DeviceCompat.isDesktop());assertFalse(DeviceCompat.isAndroid());assertFalse(DeviceCompat.isiOS());
        }finally{Gdx.app=previous;}
    }
}
