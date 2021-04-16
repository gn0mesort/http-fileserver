package fileserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class DirectorySupplier implements Supplier<String> {
    private Path root;
    private Path directory;

    public DirectorySupplier(final Path root, final Path directory) {
        this.root = root;
        this.directory = directory;
    }

    @Override
    public String get() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("<hr /><a href=\"/%s\">Up one level</a><br /><br /><table><tr><th>Name</th><th>Size</th><th>Date Modified</th></tr>", this.root.relativize(this.directory.getParent())));
        try (final DirectoryStream<Path> dir = Files.newDirectoryStream(this.directory))
        {
            long count = 0;
            for (final Path entry : dir)
            {
                final File file = entry.toFile();
                final String mimetype = Files.probeContentType(entry);

                final String icon = file.isDirectory() ? "icons/places/folder.svg" : String.format("icons/mimetypes/%s.svg", mimetype.replaceAll("\\/", "-"));
                final String href = String.format("/%s", this.root.relativize(entry));
                final String name = file.getName();
                final String length = file.isDirectory() ? String.format("%d Items", file.listFiles().length) : String.format("%d Bytes", file.length());
                final String time = Instant.ofEpochMilli(file.lastModified()).atZone(Clock.systemDefaultZone().getZone()).toOffsetDateTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
                final String row = String.format("<tr id=\"row-%d\"><td><a class=\"reflink\" href=\"#row-%d\">#</a>&nbsp;&nbsp;<img class=\"icon\" src=\"/img/%s\" />&nbsp;&nbsp;<a href=\"%s\">%s</a></td><td>%s</td><td><time datetime=\"%s\">%s</time></td></tr>", count, count, icon, href, name, length, time, time);
                builder.append(row);
                ++count;
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
