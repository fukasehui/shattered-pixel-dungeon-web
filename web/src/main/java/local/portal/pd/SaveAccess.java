package local.portal.pd;

public interface SaveAccess {
    String read(String path);
    void write(String path, String base64);
    void remove(String path);
    String[] paths();
    void move(String from, String to);
}
