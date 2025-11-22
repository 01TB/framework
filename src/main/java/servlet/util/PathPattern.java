package servlet.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class PathPattern {
    private final Pattern regex;
    private final String[] paramNames;
    private final String httpMethod;

    public PathPattern(String path, String httpMethod) {
        String regexStr = path;
        ArrayList<String> names = new ArrayList<String>();

        // Transformer {id} → (?<id>[^/]+)
        Matcher matcher = Pattern.compile("\\{([^}]+)\\}").matcher(path);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            names.add(paramName);
            matcher.appendReplacement(sb, "(?<" + paramName + ">[^/]+)");
        }
        matcher.appendTail(sb);
        regexStr = sb.toString();

        // Ajouter ^ et $ + gérer le cas où ça finit par /
        if (!regexStr.startsWith("^")) regexStr = "^" + regexStr;
        if (!regexStr.endsWith("$")) regexStr += "$";

        this.regex = Pattern.compile(regexStr.replace("/", "\\/"));
        this.paramNames = names.toArray(new String[0]);
        this.httpMethod = httpMethod.toUpperCase();
    }

    public boolean matches(String uri, String method) {
        return regex.matcher(uri).matches() && this.httpMethod.toLowerCase().compareTo(method.toLowerCase()) == 0;
    }

    public Map<String, String> extractParameters(String uri) {
        Map<String, String> params = new HashMap<>();
        Matcher matcher = regex.matcher(uri);
        if (matcher.matches()) {
            for (String name : paramNames) {
                String value = matcher.group(name);
                if (value != null) {
                    params.put(name, value);
                }
            }
        }
        return params;
    }

    public Pattern getRegex() {
        return regex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathPattern)) return false;
        PathPattern otherPathPattern = (PathPattern) o;
        return getRegex().pattern().equals(otherPathPattern.getRegex().pattern()) &&
               this.httpMethod.equals(otherPathPattern.httpMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRegex().pattern(), httpMethod);
    }
}