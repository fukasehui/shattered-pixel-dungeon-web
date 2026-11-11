package local.portal.pd;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;

public final class BrowserFile extends FileHandle {
    private final SaveAccess saves;
    public BrowserFile(String path, SaveAccess saves) { super(path, FileType.Local); this.saves = saves; }
    @Override public InputStream read() {
        String data = saves.read(path());
        if (data == null) throw new com.badlogic.gdx.utils.GdxRuntimeException("File missing: " + path());
        return new ByteArrayInputStream(Base64.getDecoder().decode(data));
    }
    @Override public OutputStream write(boolean append) {
        ByteArrayOutputStream out = new ByteArrayOutputStream() {
            private boolean closed;
            @Override public void close() {
                if (!closed) { saves.write(path(), Base64.getEncoder().encodeToString(toByteArray())); closed=true; }
            }
        };
        if (append && saves.read(path()) != null) {byte[] old=Base64.getDecoder().decode(saves.read(path())); out.write(old,0,old.length);}
        return out;
    }
    @Override public boolean exists() { return saves.read(path()) != null || isDirectory(); }
    @Override public long length() {String data=saves.read(path());return data==null?0:Base64.getDecoder().decode(data).length;}
    @Override public boolean isDirectory() {
        if (path().isEmpty()) return true;
        for(String item:saves.paths())if(item.startsWith(path()+"/"))return true;
        return false;
    }
    @Override public FileHandle child(String name) {return new BrowserFile(path().isEmpty()?name:path()+"/"+name,saves);}
    @Override public FileHandle parent() {int slash=path().lastIndexOf('/');return new BrowserFile(slash<0?"":path().substring(0,slash),saves);}
    @Override public FileHandle sibling(String name) {return parent().child(name);}
    @Override public void mkdirs() { /* Directories are inferred from native file paths. */ }
    @Override public boolean delete() {boolean existed=exists();saves.remove(path());return existed;}
    @Override public boolean deleteDirectory() {
        boolean existed=exists();for(String item:saves.paths())if(item.startsWith(path()+"/"))saves.remove(item);return existed;
    }
    @Override public FileHandle[] list() {
        LinkedHashSet<String> names=new LinkedHashSet<>();String prefix=path().isEmpty()?"":path()+"/";
        for(String item:saves.paths())if(item.startsWith(prefix)){String tail=item.substring(prefix.length());int slash=tail.indexOf('/');names.add(slash<0?tail:tail.substring(0,slash));}
        ArrayList<FileHandle> result=new ArrayList<>();for(String name:names)result.add(child(name));return result.toArray(new FileHandle[0]);
    }
    @Override public void moveTo(FileHandle destination) {saves.move(path(),destination.path());}
    @Override public void copyTo(FileHandle destination) {
        if(isDirectory()){for(FileHandle file:list())file.copyTo(destination.child(file.name()));}
        else destination.writeBytes(readBytes(),false);
    }
}
