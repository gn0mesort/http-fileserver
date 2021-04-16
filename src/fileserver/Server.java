package fileserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final ConcurrentHashMap<Integer, Template> errors = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Path, Template> directories = new ConcurrentHashMap<>();

    private static void printUsage() {
        System.out.printf("Usage: Server <ROOT_PATH> <PORT>%n");
    }

    public static void main(String[] args) {
        Path root = Path.of("/var/fileserver/");
        int port = 80;

        try
        {
            root = Path.of(args[0]);
            port = Integer.parseInt(args[1]);
        }
        catch (final Exception err)
        {
            printUsage();
            System.exit(1);
        }
        root = root.toAbsolutePath();

        // Neither of the parameters can be unset here but Java will whine
        // nonetheless if I don't initialize them.

        try (ServerSocket server = new ServerSocket(port))
        {
            System.out.printf("Server connected on %s:%s%n", server.getInetAddress().getCanonicalHostName(), server.getLocalPort());
            while (true)
            {
                final Socket client = server.accept();
                (new RequestThread(errors, directories, root, client)).start();
            }
        }
        catch (final IOException err)
        {
            System.err.printf("Error starting server%n");
            err.printStackTrace();
            System.exit(1);
        }
    }
}
