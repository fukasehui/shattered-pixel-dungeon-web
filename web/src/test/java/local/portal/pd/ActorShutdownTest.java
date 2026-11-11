package local.portal.pd;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.watabou.noosa.Game;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class ActorShutdownTest {
    @Test public void parkedBrowserActorStopsWithoutInterruptAndClearsProcessing() throws Exception {
        Application previous=Gdx.app;CountDownLatch acted=new CountDownLatch(1);
        final boolean[] interrupted={false};Thread actor=null;
        try{
            Gdx.app=(Application)Proxy.newProxyInstance(Application.class.getClassLoader(),new Class[]{Application.class},
                (o,m,a)->m.getName().equals("getType")?Application.ApplicationType.WebGL:null);
            new Game(null,null){{requestedReset=false;}};Actor.clear();Actor.keepActorThreadAlive=true;
            Actor.add(new Actor(){protected boolean act(){acted.countDown();return false;}});
            actor=new Thread(Actor::process){@Override public void interrupt(){interrupted[0]=true;super.interrupt();}};
            actor.setDaemon(true);actor.start();assertTrue(acted.await(2,TimeUnit.SECONDS));
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
            while(actor.getState()!=Thread.State.WAITING&&System.nanoTime()<until)Thread.sleep(1);
            assertTrue(Actor.processing());Actor.stopCooperatively(actor);actor.join(1000);
            assertFalse(actor.isAlive());assertFalse(interrupted[0]);assertFalse(Actor.processing());
        }finally{if(actor!=null&&actor.isAlive()){Actor.keepActorThreadAlive=false;actor.interrupt();actor.join(1000);}Actor.clear();Gdx.app=previous;}
    }
}
