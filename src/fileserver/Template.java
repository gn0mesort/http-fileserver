package fileserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template {
    public static final long CACHE_DURATION_MILLIS = 60_000; // 1 minute
    public static final Pattern TEMPLATE_PATTERN = Pattern.compile("%([^%]+)%");

    private final Map<String, Object> mappings;
    private String cache;
    private long time;
    private boolean dirty;
    private final String template;

    public static Template from(final Path path) throws IOException {
        return new Template(Files.readString(path));
    }

    public Template(final String template) {
        this.mappings = new HashMap<>();
        this.cache = null;
        this.time = 0;
        this.dirty = true;
        this.template = template;
    }

    public void set(final String variable, final Object obj) {
        this.mappings.put(variable, obj);
    }

    public <Type> void set(final String variable, final Supplier<Type> fn) {
        this.mappings.put(variable, fn);
        this.dirty = true;
    }

    public void unset(final String variable) {
        this.mappings.remove(variable);
        this.dirty = true;
    }

    public void processTemplate() {
        this.cache = this.template;
        final Matcher matcher = TEMPLATE_PATTERN.matcher(this.cache);
        if (matcher.find())
        {
            this.cache = matcher.replaceAll((match) -> {
                final String name = match.group().substring(1, match.group().length() - 1);
                final Object obj = this.mappings.get(name);
                if (obj instanceof Supplier)
                {
                    final Supplier fn = (Supplier) obj;
                    return fn.get().toString();
                }
                if (obj != null)
                {
                    return obj.toString();
                }
                return "";
            });
        }
        this.time = System.currentTimeMillis();
        this.dirty = false;
    }

    @Override
    public String toString() {
        if (this.cache == null || this.dirty || (System.currentTimeMillis() - this.time) >= CACHE_DURATION_MILLIS)
        {
            processTemplate();
        }
        return this.cache;
    }
}
