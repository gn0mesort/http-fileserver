package fileserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final ConcurrentHashMap<Integer, Template> errors = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Path, Template> directories = new ConcurrentHashMap<>();
    private static final ExecutorService threadpool = Executors.newWorkStealingPool();

    private static void printUsage() {
        System.out.printf("Usage: Server [CONFIG_PATH]%n");
    }

    public static void main(String[] args) {
        Configuration config = new Configuration();
        try
        {
            if (args.length > 0)
            {
                config = Configuration.from(Path.of(args[0]).toAbsolutePath());
                System.out.printf("Read config from %s%n", Path.of(args[0]).toAbsolutePath());
            }
        }
        catch (final Exception err)
        {
            System.err.printf("Failed to read configuration file.%n");
            printUsage();
            System.exit(1);
        }
        System.out.printf("Server config:%n%s%n", config);


        try (ServerSocket server = new ServerSocket(config.getPort()))
        {
            System.out.printf("Server connected on %s:%s%n", server.getInetAddress().getCanonicalHostName(),
                    server.getLocalPort());
            while (true)
            {
                final Socket client = server.accept();
                threadpool.submit(new RequestHandler(errors, directories, config, client));
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
