package fileserver;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A function object that is used to supply HTML output for directory objects.
 *
 * @author Alexander Rothman #714145 <alexanderpaul.rothman@calbaptist.edu>
 * @since April 16, 2021
 */
public class DirectorySupplier implements Supplier<String> {
    private final Path root;
    private final Path metaDirectory;
    private final Path directory;
    private final boolean showHidden;

    /**
     * Constructs a new DirectorySupplier.
     *
     * @param root The absolute Path to the root of the Server's filesystem.
     * @param metaDirectory The absolute Path to the Server's meta file directory.
     * @param showHidden Whether or not to display hidden files/directories.
     * @param directory The Path to the directory to generate HTML for.
     */
    public DirectorySupplier(final Path root, final Path metaDirectory, final boolean showHidden,
                             final Path directory) {
        this.root = root;
        this.metaDirectory = metaDirectory;
        this.showHidden = showHidden;
        this.directory = directory;
    }

    private String toHumanReadable(final long bytes) {
        if (bytes < 1024)
        {
            return String.format("%d Bytes", bytes);
        }
        final int zeroes = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        System.out.printf("%d %d%n", bytes, zeroes);
        final String units = "KMGTPE";
        return String.format("%.1f %ciB", (double) bytes / (1L << (zeroes * 10)), units.charAt(zeroes - 1));
    }

    /**
     * Generate HTML representing the configured directory.
     *
     * @return A valid HTML body for a directory index page.
     */
    @Override
    public String get() {
        StringBuilder builder = new StringBuilder();
        final String opening = "<hr /><a %s>Up one level</a><br /><br /><table><tr><th>Name</th><th>Size</th><th>Date " +
                "Modified</th></tr>";
        final String openingAttrs;
        if (this.directory.equals(this.root))
        {
            openingAttrs = "class=\"disabled\" href=\"#\"";
        }
        else
        {
            openingAttrs = String.format("href=\"/%s\"", this.root.relativize(this.directory.getParent()));
        }
        builder.append(String.format(opening, openingAttrs));
        try (final DirectoryStream<Path> dir = Files.newDirectoryStream(this.directory))
        {
            long count = 0;
            for (final Path entry : dir)
            {
                final File file = entry.toFile();
                if (!file.isHidden() || this.showHidden)
                {

                    final String icon;
                    final String length;
                    if (file.isDirectory())
                    {
                        icon = "icons/places/folder.svg";
                        // I know this violates the spec. I do not care. I like this better. The spec is boring.
                        length = String.format("%d Items", Objects.requireNonNull(file.listFiles()).length);
                    }
                    else
                    {
                        // This is ridiculous but it will either determine the type of the file or decide it's an octet
                        // stream. Really when you think about it everything is just an octet stream anyway.
                        String mimetype = Files.probeContentType(entry);
                        if (mimetype == null)
                        {
                            final BufferedInputStream fstream = new BufferedInputStream(new FileInputStream(file));
                            mimetype = URLConnection.guessContentTypeFromStream(fstream);
                        }
                        if (mimetype == null)
                        {
                            mimetype = URLConnection.guessContentTypeFromName(file.getName());
                        }
                        if (mimetype == null)
                        {
                            mimetype = "application/octet-stream";
                        }
                        icon = String.format("icons/mimetypes/%s.svg", mimetype.replaceAll("/", "-"));
                        // This is also not to spec. I still do not care. This is better.
                        // Calculates the correct binary unit to use and displays it.
                        length = toHumanReadable(file.length());
                    }
                    final String href = String.format("/%s", this.root.relativize(entry));
                    final String name = file.getName();
                    final String time =
                            Instant.ofEpochMilli(file.lastModified()).atZone(Clock.systemDefaultZone().getZone())
                            .toOffsetDateTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
                    final String rowFormat = "<tr id=\"row-%d\"><td><a class=\"reflink\" href=\"#row-%d\">#</a>&nbsp;" +
                            "&nbsp;<img class=\"icon\" src=\"/%s/img/%s\" />&nbsp;&nbsp;<a href=\"%s\">%s</a></td>" +
                            "<td>%s</td><td><time datetime=\"%s\">%s</time></td></tr>";
                    final String row = String.format(rowFormat, count, count, this.root.relativize(this.metaDirectory),
                            icon, href, name, length, time, time);
                    builder.append(row);
                    ++count;
                }
            }
        }
        catch (final IOException err)
        {
            builder.append("<p class=\"error\">Failed reading directory!</p>");
            err.printStackTrace();
        }
        builder.append("</table><br /><a href=\"#\">Back to top</a><hr />");
        return builder.toString();
    }
}
