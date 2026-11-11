package local.portal.pd;

import java.util.function.Consumer;

/** Run one complete frame in a TeaVM coroutine; never overlap suspended frames. */
final class CooperativeFrame {
    private final Consumer<Runnable> start;
    private boolean pending;
    CooperativeFrame(Consumer<Runnable> start){this.start=start;}
    void run(Runnable frame){
        if(pending)return;
        pending=true;
        try{start.accept(()->{try{frame.run();}finally{pending=false;}});}
        catch(RuntimeException failure){pending=false;throw failure;}
    }
}
