import fileserver.Template;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateTests {
    @Test
    public void templatingTest() {
        final Template template = new Template("%hello%");

        template.set("hello", "goodbye");

        Assert.assertEquals("goodbye", template.toString());
        System.out.println(template.toString());
    }

    @Test
    public void templatingWithFunctionsTest() {
        final Template template = new Template("%time%");
        // This is my birthday in case you want to buy me a gift - AR
        final LocalDateTime expected = LocalDateTime.of(1991, 11, 27, 12, 0, 0);

        template.set("time", () -> expected);
        Assert.assertEquals(expected.toString(), template.toString());
        System.out.println(template.toString());
    }

    @Test
    public void regexTest() {
        final Pattern tester = Template.TEMPLATE_PATTERN;
        final String testerText = """
                <!DOCTYPE html>
                <html lang="en">
                    <head>
                        <meta charset="utf-8" />
                        <meta name="viewport" content="width=device-width, initial-scale=1" />
                        <link rel="stylesheet" href="/main.css" />
                        <link rel="stylesheet" href="%theme_path%" />
                        <title>%title%</title>
                    </head>
                    <body>
                        <header>
                            %header%
                        </header>
                        <main>
                            %body%
                        </main>
                        <footer>
                            %footer%
                        </footer>
                    </body>
                </html>""";
        final Matcher matcher = tester.matcher(testerText);
        final String[] expected = { "%theme_path%", "%title%", "%header%", "%body%", "%footer%" };
        final String[] actual = new String[expected.length];

        for (int i = 0; matcher.find(); ++i)
        {
            actual[i] = matcher.group();
            System.out.println(actual[i]);
        }

        Assert.assertArrayEquals(expected, actual);
    }
}
