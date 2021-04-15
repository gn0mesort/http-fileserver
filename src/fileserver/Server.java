package fileserver;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final ConcurrentHashMap<Integer, TemplatedPage> errorPages = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Path, TemplatedPage> directoryPages = new ConcurrentHashMap<>();

    public static void main(String[] args) {

    }
}
