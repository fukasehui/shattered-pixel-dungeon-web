package local.portal.pd;

import org.teavm.jso.JSBody;
import org.json.JSONArray;

public final class WebSaves implements SaveAccess {
    @JSBody(params="path",script="return window.portalStore.read(path);") private static native String readNative(String path);
    @JSBody(params={"path","base64"},script="window.portalStore.write(path,base64);") private static native void writeNative(String path,String base64);
    @JSBody(params="path",script="window.portalStore.remove(path);") private static native void removeNative(String path);
    @JSBody(params={"from","to"},script="window.portalStore.move(from,to);") private static native void moveNative(String from,String to);
    @Override public String read(String path){return readNative(path);}
    @Override public void write(String path,String base64){writeNative(path,base64);}
    @Override public void remove(String path){removeNative(path);}
    @Override public void move(String from,String to){moveNative(from,to);}
    @JSBody(script="return JSON.stringify(window.portalStore.paths());") private static native String pathsJson();
    @Override public String[] paths(){JSONArray array=new JSONArray(pathsJson());String[] out=new String[array.length()];for(int i=0;i<out.length;i++)out[i]=array.getString(i);return out;}
    @JSBody(script="window.portalStore.flush();") public static native void flush();
    @JSBody(script="if(window.portalStore.migrationPending) window.portalStore.acceptUpgrade();") public static native void acceptUpgrade();
}
