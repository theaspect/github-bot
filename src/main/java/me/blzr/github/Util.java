package me.blzr.github;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * See UtilTest
 */
public class Util {
    public static List<String> tryParse(String text) {
        final Pattern pattern = Pattern.compile("https://github.com/([\\w-]+)");
        final Matcher matcher = pattern.matcher(text);
        TreeSet<String> results = new TreeSet<>();
        while (matcher.find()) {
            results.add(matcher.group(1));
        }
        return new ArrayList<>(results);
    }
}
