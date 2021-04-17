package fileserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Represents a server configuration file.
 *
 * Configuration files are basically traditional *.conf files. Empty lines or lines beginning with # are ignored.
 * Otherwise the the lines are interpreted as key-value pairs. Sections are not supported.
 *
 * @author Alexander Rothman #714145 <alexanderpaul.rothman@calbaptist.edu>
 * @since April 16, 2021
 */
public class Configuration {
    /**
     * Parse a configuration file.
     *
     * @param config A valid Path to a configuration file.
     * @return A new Configuration object representing the parsed data.
     * @throws IOException If the input file cannot be read.
     */
    public static Configuration from(final Path config) throws IOException {
        final List<String> lines = Files.readAllLines(config);
        Path root = null;
        Path metaRoot = null;
        Path theme = null;
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
                else if (key.equalsIgnoreCase("theme"))
                {
                    theme = Path.of(value);
                }
                else if (key.equalsIgnoreCase("show-hidden"))
                {
                    showHidden = Boolean.parseBoolean(value);
                }
            }
        }
        return new Configuration(root, metaRoot, theme, port, showHidden);
    }

    private final Path root;
    private final Path metaRoot;
    private final Path theme;
    private final short port;
    private final boolean showHidden;

    /**
     * Constructs a default Configuration.
     */
    public Configuration() {
        this(null, null, null, (short) 0, false);
    }

    /**
     * Constructs a Configuration from the input parameters.
     *
     * @param root The root path for the server. Defaults to /
     * @param metaRoot The root path for server meta files. This is relative to root. Defaults to .meta
     * @param theme The path to the theme CSS file. This is relative to metaRoot. Defaults to css/theme.css
     * @param port The port to bind the server to. Defaults to 80.
     * @param showHidden Whether or not the server should display hidden files. Defaults to false.
     */
    public Configuration(final Path root, final Path metaRoot, final Path theme, final short port,
                         final boolean showHidden) {
        this.root = root != null ? root.toAbsolutePath() : Path.of("/");
        this.metaRoot =
                this.root.relativize(Path.of(this.root.toString(), metaRoot != null ? metaRoot.toString() : ".meta"));
        this.theme =
                this.metaRoot.relativize(Path.of(this.metaRoot.toString(), theme != null ? theme.toString() : "css/" +
                        "theme.css"));
        this.port = port != 0 ? port : 80;
        this.showHidden = showHidden;
    }

    /**
     * @return The root path for the Configuration.
     */
    public Path getRoot() {
        return this.root;
    }

    /**
     * @return The meta root path for the Configuration.
     */
    public Path getMetaRoot() {
        return this.metaRoot;
    }

    /**
     * @return The path to the theme CSS file for the Configuration.
     */
    public Path getTheme() {
        return this.theme;
    }

    /**
     * @return The port for the Configuration.
     */
    public short getPort() {
        return this.port;
    }

    /**
     * @return True if the server should display hidden files. Otherwise false.
     */
    public boolean shouldShowHidden() {
        return this.showHidden;
    }

    /**
     * Convert a Configuration to a String representation.
     *
     * This will result in a valid minimal configuration file if written to a file.
     *
     * @return A String representing the Configuration.
     */
    @Override
    public String toString() {
        return "root=" +
                this.root +
                System.lineSeparator() +
                "meta-root=" +
                this.metaRoot +
                System.lineSeparator() +
                "theme=" +
                this.theme +
                System.lineSeparator() +
                "port=" +
                this.port +
                System.lineSeparator() +
                "show-hidden=" +
                this.showHidden;
    }
}
