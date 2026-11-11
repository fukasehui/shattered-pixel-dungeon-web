package local.portal.pd;

import com.badlogic.gdx.Preferences;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public final class BrowserPreferences implements Preferences {
    private final SaveAccess saves; private final String path; private final Map<String,Object> values=new HashMap<>();
    public BrowserPreferences(String name,SaveAccess saves){
        this.saves=saves;this.path="preferences/"+name+".json";
        String raw=saves.read(path);
        if(raw!=null){JSONObject json=new JSONObject(new String(Base64.getDecoder().decode(raw),StandardCharsets.UTF_8));for(String key:json.keySet())values.put(key,json.get(key));}
    }
    @Override public Preferences putBoolean(String k,boolean v){values.put(k,v);return this;}
    @Override public Preferences putInteger(String k,int v){values.put(k,v);return this;}
    @Override public Preferences putLong(String k,long v){values.put(k,v);return this;}
    @Override public Preferences putFloat(String k,float v){values.put(k,v);return this;}
    @Override public Preferences putString(String k,String v){values.put(k,v);return this;}
    @Override public Preferences put(Map<String,?> v){values.putAll(v);return this;}
    @Override public boolean getBoolean(String k){return getBoolean(k,false);}
    @Override public int getInteger(String k){return getInteger(k,0);}
    @Override public long getLong(String k){return getLong(k,0);}
    @Override public float getFloat(String k){return getFloat(k,0);}
    @Override public String getString(String k){return getString(k,"");}
    @Override public boolean getBoolean(String k,boolean d){Object v=values.get(k);return v instanceof Boolean?(Boolean)v:d;}
    @Override public int getInteger(String k,int d){Object v=values.get(k);return v instanceof Number?((Number)v).intValue():d;}
    @Override public long getLong(String k,long d){Object v=values.get(k);return v instanceof Number?((Number)v).longValue():d;}
    @Override public float getFloat(String k,float d){Object v=values.get(k);return v instanceof Number?((Number)v).floatValue():d;}
    @Override public String getString(String k,String d){Object v=values.get(k);return v instanceof String?(String)v:d;}
    @Override public Map<String,?> get(){return new HashMap<>(values);}
    @Override public boolean contains(String k){return values.containsKey(k);}
    @Override public void clear(){values.clear();}
    @Override public void remove(String k){values.remove(k);}
    @Override public void flush(){saves.write(path,Base64.getEncoder().encodeToString(new JSONObject(values).toString().getBytes(StandardCharsets.UTF_8)));}
}
