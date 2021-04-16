package fileserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Supplier;

public class DirectorySupplier implements Supplier<String> {
    private final Path root;
    private final Path metaDirectory;
    private final Path directory;
    private final boolean showHidden;

    public DirectorySupplier(final Path root, final Path metaDirectory, final boolean showHidden,
                             final Path directory) {
        this.root = root;
        this.metaDirectory = metaDirectory;
        this.showHidden = showHidden;
        this.directory = directory;
    }

    @Override
    public String get() {
        StringBuilder builder = new StringBuilder();
        final String opening = "<hr /><a href=\"/%s\">Up one level</a><br /><br /><table><tr><th>Name</th><th>Size" +
                "</th><th>Date Modified</th></tr>";
        builder.append(String.format(opening, this.root.relativize(this.directory.getParent())));
        try (final DirectoryStream<Path> dir = Files.newDirectoryStream(this.directory))
        {
            long count = 0;
            for (final Path entry : dir)
            {
                final File file = entry.toFile();
                if (!file.isHidden() || this.showHidden)
                {

                    final String mimetype = Files.probeContentType(entry);
                    final String icon;
                    final String length;
                    if (file.isDirectory())
                    {
                        icon = "icons/places/folder.svg";
                        length = String.format("%d Items", Objects.requireNonNull(file.listFiles()).length);
                    }
                    else
                    {
                        icon = String.format("icons/mimetypes/%s.svg", mimetype.replaceAll("/", "-"));
                        length = String.format("%d Bytes", file.length());
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
