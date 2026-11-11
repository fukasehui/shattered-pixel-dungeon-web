package local.portal.pd;
import com.badlogic.gdx.Application;import com.badlogic.gdx.Gdx;import com.badlogic.gdx.graphics.GL20;
import com.watabou.glwrap.Attribute;import java.lang.reflect.Proxy;import java.nio.Buffer;import java.nio.FloatBuffer;
import org.junit.Test;import static org.junit.Assert.*;
public class WebGlBufferTest {
 @Test public void browserAttributeUsesGpuBufferOffsetsInsteadOfClientPointers(){
  Application app=Gdx.app;GL20 gl=Gdx.gl;final Object[][] pointer={null};final int[] uploaded={0};
  try{
   Gdx.app=(Application)Proxy.newProxyInstance(Application.class.getClassLoader(),new Class[]{Application.class},(o,m,a)->m.getName().equals("getType")?Application.ApplicationType.WebGL:null);
   Gdx.gl=(GL20)Proxy.newProxyInstance(GL20.class.getClassLoader(),new Class[]{GL20.class},(o,m,a)->{if(m.getName().equals("glVertexAttribPointer"))pointer[0]=a;if(m.getName().equals("glBufferData"))uploaded[0]=(Integer)a[1];return m.getReturnType()==int.class?1:null;});
   FloatBuffer vertices=FloatBuffer.allocate(16);((Buffer)vertices).position(2);new Attribute(3).vertexPointer(2,4,vertices);
   assertEquals(64,uploaded[0]);assertTrue(pointer[0][5] instanceof Integer);assertEquals(8,pointer[0][5]);
  }finally{Gdx.app=app;Gdx.gl=gl;}
 }
}
