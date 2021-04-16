package fileserver;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * A Runnable handler for HTTP requests.
 *
 * This is intended to be executed by a threadpool.
 *
 * @author Alexander Rothman #714145 <alexanderpaul.rothman@calbaptist.edu>
 * @since April 16, 2021
 */
public class RequestHandler implements Runnable {
    /**
     * The maximum size, in bytes, of received HTTP requests.
     */
    public final int MAX_REQUEST_SIZE = 8192; // 8KiB

    private enum SupportedHttpMethod {
        Unknown,
        Head,
        Get
    }

    private final Map<Integer, Template> errors;
    private final Map<Path, Template> directories;
    private final Socket client;
    private final Path root;
    private final Path metaDirectory;
    private final Path defaultTemplate;
    private final boolean showHidden;

    private static SupportedHttpMethod toMethod(final String str) {
        if (str.equalsIgnoreCase("HEAD"))
        {
            return SupportedHttpMethod.Head;
        }
        if (str.equalsIgnoreCase("GET"))
        {
            return SupportedHttpMethod.Get;
        }
        return SupportedHttpMethod.Unknown;
    }

    private static Path root(final Path root, final Path child) {
        return Path.of(root.toString(), child.toString()).toAbsolutePath();
    }

    /**
     * Constructs a new RequestHandler with the given page caches, Configuration, and client Socket.
     *
     * @param errors A error page cache. This should be thread-safe or unique to this handler.
     * @param directories A directory page cache. This should be thread-safe or unique to this handler.
     * @param config The Server Configuration.
     * @param client The client Socket to respond to.
     */
    public RequestHandler(final Map<Integer, Template> errors, final Map<Path, Template> directories,
                          final Configuration config, final Socket client) {
        this.errors = errors;
        this.directories = directories;
        this.root = config.getRoot();
        this.metaDirectory = Path.of(config.getRoot().toString(), config.getMetaRoot().toString()).toAbsolutePath();
        this.defaultTemplate = Path.of(this.metaDirectory.toString(), "templates/default.template.html");
        this.showHidden = config.shouldShowHidden();
        this.client = client;
    }

    private String getFooter() {
        final String template = "<p>Generated by Java HTTP Fileserver v%d.%d.%d</p>";
        return String.format(template, Constants.VERSION_MAJOR, Constants.VERSION_MINOR, Constants.VERSION_PATCH);
    }

    private Map<String, String> prepareHeaders() {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Date", LocalDateTime.now(ZoneId.of("UTC")).toString());
        final String template = "Java HTTP Fileserver v%d.%d.%d";
        headers.put("Server", String.format(template, Constants.VERSION_MAJOR, Constants.VERSION_MINOR,
                Constants.VERSION_PATCH));
        headers.put("Connection", "Close");
        return headers;
    }

    private void printHttpHeader(final int status, final String message, final PrintStream output) {
        output.printf("HTTP/1.1 %d %s\r\n", status, message);
    }

    private void printHeaders(final Map<String, String> headers, final PrintStream output) {
        for (final Map.Entry<String, String> entry : headers.entrySet())
        {
            output.printf("%s: %s\r\n", entry.getKey(), entry.getValue());
        }
    }

    private void pipeContent(final InputStream content, final PrintStream output) throws IOException {
        output.print("\r\n");
        final byte[] buffer = new byte[MAX_REQUEST_SIZE];
        int read;
        while ((read = content.read(buffer)) >= 1)
        {
            output.write(buffer, 0, read);
        }
    }

    private void respondError(final SupportedHttpMethod method, int status, final String message,
                              final PrintStream output) throws IOException {
        System.out.printf("%d %s%n", status, message);
        final Map<String, String> headers = prepareHeaders();
        Template template = this.errors.get(status);
        if (template == null)
        {
            template = Template.from(this.defaultTemplate);
            this.errors.put(status, template);
            template.set("meta", this.root.relativize(this.metaDirectory));
            template.set("title", message);
            template.set("header", String.format("<h1>Error %d</h1>", status));
            template.set("body", String.format("<p>%s</p>", message));
            template.set("footer", String.format("%s", getFooter()));
        }
        headers.put("Content-Type", "text/html");
        final byte[] contentBytes = template.toString().getBytes(StandardCharsets.UTF_8);
        final long size = contentBytes.length;
        final InputStream content = new ByteArrayInputStream(contentBytes);
        headers.put("Content-Length", Long.toString(size));
        printHttpHeader(status, message, output);
        printHeaders(headers, output);
        if (method == SupportedHttpMethod.Get)
        {
            pipeContent(content, output);
        }
        output.flush();
    }

    private void respondBadReq(final SupportedHttpMethod method, final PrintStream output) throws IOException {
        respondError(method, 400, "Bad Request", output);
    }

    private void respondNotFound(final SupportedHttpMethod method, final PrintStream output) throws IOException {
        respondError(method, 404, "File Not Found", output);
    }

    private void respondOK(final SupportedHttpMethod method, final Path desired,
                           final PrintStream output) throws IOException {
        System.out.printf("200 OK%n");
        final Map<String, String> headers = prepareHeaders();
        final InputStream content;
        final long size;
        if (Files.isDirectory(desired))
        {
            Template template = this.directories.get(desired);
            if (template == null)
            {
                template = Template.from(this.defaultTemplate);
                this.directories.put(desired, template);
                template.set("meta", this.root.relativize(this.metaDirectory));
                template.set("title", () -> String.format("Index of /%s", this.root.relativize(desired)));
                template.set("header", () -> String.format("<h1>Index of /%s</h1>", this.root.relativize(desired)));
                template.set("body", new DirectorySupplier(this.root, this.metaDirectory, this.showHidden, desired));
                template.set("footer", String.format("%s", getFooter()));
            }
            headers.put("Content-Type", "text/html");
            final byte[] contentBytes = template.toString().getBytes(StandardCharsets.UTF_8);
            content = new ByteArrayInputStream(contentBytes);
            size = contentBytes.length;
        }
        else
        {
            headers.put("Content-Type", Files.probeContentType(desired));
            final File contentFile = desired.toFile();
            content = new FileInputStream(contentFile);
            size = contentFile.length();
        }
        headers.put("Content-Length", Long.toString(size));
        printHttpHeader(200, "OK", output);
        printHeaders(headers, output);
        if (method == SupportedHttpMethod.Get)
        {
            pipeContent(content, output);
        }
        output.flush();
    }

    private void respondInternalServerError(final SupportedHttpMethod method, final PrintStream output) throws IOException {
        respondError(method, 500, "Internal Server Error", output);
    }

    /**
     * Handles incoming HTTP GET or HEAD requests and responds accordingly.
     */
    @Override
    public void run() {
        try
        {
            final DataInputStream input = new DataInputStream(this.client.getInputStream());
            final PrintStream output = new PrintStream(this.client.getOutputStream());
            try
            {
                final byte[] requestBuffer = new byte[MAX_REQUEST_SIZE];
                if (input.read(requestBuffer) < 1)
                {
                    respondBadReq(SupportedHttpMethod.Get, output);
                }
                final String[] request = (new String(requestBuffer, StandardCharsets.UTF_8)).split("\r\n");
                final StringTokenizer tokens = new StringTokenizer(request[0]);
                final SupportedHttpMethod method = toMethod(tokens.nextToken());
                final Path desired = root(this.root, Path.of(tokens.nextToken()));
                System.out.printf("%s:%s REQ -> %s %s ", this.client.getInetAddress().getCanonicalHostName(),
                        this.client.getPort(), method, desired);
                if (Files.exists(desired))
                {
                    respondOK(method, desired, output);
                }
                else
                {
                    respondNotFound(method, output);
                }
            }
            catch (final Exception err)
            {
                respondInternalServerError(SupportedHttpMethod.Get, output);
                err.printStackTrace();
            }
        }
        catch (final IOException err)
        {
            System.err.printf("I/O Error%n");
            err.printStackTrace();
        }
    }
}