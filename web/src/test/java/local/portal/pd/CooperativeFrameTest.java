package local.portal.pd;
import java.util.ArrayDeque;
import org.junit.Test;
import static org.junit.Assert.*;

public class CooperativeFrameTest {
    @Test public void pendingFrameCannotOverlapSceneDestructionAndFollowingFrames(){
        ArrayDeque<Runnable> queue=new ArrayDeque<>();int[] frames={0};
        CooperativeFrame dispatcher=new CooperativeFrame(queue::add);
        dispatcher.run(()->frames[0]++);
        dispatcher.run(()->frames[0]+=100);
        assertEquals(1,queue.size());assertEquals(0,frames[0]);
        queue.remove().run();assertEquals(1,frames[0]);
        dispatcher.run(()->frames[0]++);queue.remove().run();assertEquals(2,frames[0]);
    }
    @Test public void failedFrameReleasesTheSchedulingGate(){
        ArrayDeque<Runnable> queue=new ArrayDeque<>();CooperativeFrame dispatcher=new CooperativeFrame(queue::add);
        dispatcher.run(()->{throw new IllegalStateException("test failure");});
        try{queue.remove().run();fail();}catch(IllegalStateException expected){}
        dispatcher.run(()->{});assertEquals(1,queue.size());
    }
}
