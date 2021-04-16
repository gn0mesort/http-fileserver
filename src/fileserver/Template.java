package fileserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class representing templated Strings.
 *
 * This is used for templating HTML output.
 *
 * @author Alexander Rothman #714145 <alexanderpaul.rothman@calbaptist.edu>
 * @since April 16, 2021
 */
public class Template {
    /**
     * The duration, in milliseconds, that a template will cache itself for.
     */
    public static final long CACHE_DURATION_MILLIS = 60_000; // 1 minute

    /**
     * The template replacement pattern.
     * This should match templates of the form %template_variable%.
     */
    public static final Pattern TEMPLATE_PATTERN = Pattern.compile("%([^%]+)%");

    private final Map<String, Object> mappings;
    private String cache;
    private long time;
    private boolean dirty;
    private final String template;

    /**
     * Generates a new Template from a file.
     *
     * @param path The Path containing the template data.
     * @return A new Template containing the data in the given Path.
     * @throws IOException If the input file cannot be read.
     */
    public static Template from(final Path path) throws IOException {
        return new Template(Files.readString(path));
    }

    /**
     * Constructs a Template based on a String.
     *
     * @param template A templatable String.
     */
    public Template(final String template) {
        this.mappings = new HashMap<>();
        this.cache = null;
        this.time = 0;
        this.dirty = true;
        this.template = template;
    }

    /**
     * Set the value of a Template variable.
     *
     * @param variable The name of the variable to set.
     * @param obj The Object to set the variable to.
     */
    public void set(final String variable, final Object obj) {
        this.mappings.put(variable, obj);
    }

    /**
     * A specialized setter for Supplier Objects.
     *
     * @param variable The name of the variable to set.
     * @param fn The Supplier to set the variable to.
     * @param <Type> The return type of the Supplier.
     */
    public <Type> void set(final String variable, final Supplier<Type> fn) {
        this.mappings.put(variable, fn);
        this.dirty = true;
    }

    /**
     * Unset a Template variable.
     *
     * @param variable The name of the variable to remove.
     */
    public void unset(final String variable) {
        this.mappings.remove(variable);
        this.dirty = true;
    }

    private void processTemplate() {
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

    /**
     * Converts a template to its processed String representation.
     *
     * If the template data has changed or if the cache duration has expired the template will be reprocessed. Otherwise
     * the templated page is returned as before.
     *
     * @return The template String after variable replacement.
     */
    @Override
    public String toString() {
        if (this.cache == null || this.dirty || (System.currentTimeMillis() - this.time) >= CACHE_DURATION_MILLIS)
        {
            processTemplate();
        }
        return this.cache;
    }
}
