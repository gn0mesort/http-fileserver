package fileserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.StringTokenizer;

public class Configuration {
    public static Configuration from(final Path config) throws IOException {
        final List<String> lines = Files.readAllLines(config);
        Path root = null;
        Path metaRoot = null;
        short port = 0;
        boolean showHidden = false;
        for (final String line : lines)
        {
            if (!line.startsWith("#") && !line.isEmpty())
            {
                final StringTokenizer tokens = new StringTokenizer(line, "= \f\t\r\n");
                final String key = tokens.nextToken();
                final String value = tokens.nextToken();
                if (key.equalsIgnoreCase("root"))
                {
                    root = Path.of(value);
                }
                else if (key.equalsIgnoreCase("meta-root"))
                {
                    metaRoot = Path.of(value);
                }
                else if (key.equalsIgnoreCase("port"))
                {
                    port = Short.parseShort(value);
                }
                else if (key.equalsIgnoreCase("show-hidden"))
                {
                    showHidden = Boolean.parseBoolean(value);
                }
            }
        }
        return new Configuration(root, metaRoot, port, showHidden);
    }

    private final Path root;
    private final Path metaRoot;
    private final short port;
    private final boolean showHidden;

    public Configuration() {
        this(null, null, (short) 0, false);
    }

    public Configuration(final Path root, final Path metaRoot, final short port, final boolean showHidden) {
        this.root = root != null ? root.toAbsolutePath() : Path.of("/");
        this.metaRoot =
                this.root.relativize(Path.of(this.root.toString(), metaRoot != null ? metaRoot.toString() : ".meta"));
        this.port = port != 0 ? port : 80;
        this.showHidden = showHidden;
    }

    public Path getRoot() {
        return this.root;
    }

    public Path getMetaRoot() {
        return this.metaRoot;
    }

    public short getPort() {
        return this.port;
    }

    public boolean shouldShowHidden() {
        return this.showHidden;
    }

    @Override
    public String toString() {
        return "root=" +
                this.root +
                System.lineSeparator() +
                "meta-root=" +
                this.metaRoot +
                System.lineSeparator() +
                "port=" +
                this.port +
                System.lineSeparator() +
                "show-hidden=" +
                this.showHidden;
    }
}
