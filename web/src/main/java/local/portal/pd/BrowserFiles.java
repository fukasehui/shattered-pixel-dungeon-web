package local.portal.pd;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;

public final class BrowserFiles implements Files {
    private final Files assets; private final SaveAccess saves;
    public BrowserFiles(Files assets, SaveAccess saves){this.assets=assets;this.saves=saves;}
    @Override public FileHandle getFileHandle(String path, FileType type){return type==FileType.Local||type==FileType.External?local(path):assets.getFileHandle(path,type);}
    @Override public FileHandle classpath(String path){return assets.classpath(path);}
    @Override public FileHandle internal(String path){return assets.internal(path);}
    @Override public FileHandle external(String path){return local(path);}
    @Override public FileHandle absolute(String path){throw new UnsupportedOperationException("Absolute browser file access");}
    @Override public FileHandle local(String path){return new BrowserFile(path,saves);}
    @Override public String getExternalStoragePath(){return "";}
    @Override public boolean isExternalStorageAvailable(){return true;}
    @Override public String getLocalStoragePath(){return "";}
    @Override public boolean isLocalStorageAvailable(){return true;}
}
