package com.designer.admin.common.util;

/**
 * 精简 Ant 风格路径匹配（支持 {@code *} 与 {@code **}）。
 */
public final class AntPathMatcher {

    private AntPathMatcher() {
    }

    public static boolean match(String pattern, String path) {
        if (pattern == null || path == null) {
            return false;
        }
        String p = normalize(pattern);
        String t = normalize(path);
        return doMatch(p, t);
    }

    private static String normalize(String s) {
        String x = s.trim();
        if (x.isEmpty()) {
            return "/";
        }
        if (!x.startsWith("/")) {
            x = "/" + x;
        }
        while (x.contains("//")) {
            x = x.replace("//", "/");
        }
        if (x.length() > 1 && x.endsWith("/")) {
            x = x.substring(0, x.length() - 1);
        }
        return x;
    }

    private static boolean doMatch(String pattern, String path) {
        String[] pats = split(pattern);
        String[] paths = split(path);
        int pi = 0;
        int ti = 0;
        while (pi < pats.length && ti < paths.length) {
            String pat = pats[pi];
            if ("**".equals(pat)) {
                if (pi == pats.length - 1) {
                    return true;
                }
                pi++;
                String next = pats[pi];
                while (ti < paths.length) {
                    if (matchSegment(next, paths[ti]) && doMatch(joinFrom(pats, pi), joinFrom(paths, ti))) {
                        return true;
                    }
                    ti++;
                }
                return false;
            }
            if (!matchSegment(pat, paths[ti])) {
                return false;
            }
            pi++;
            ti++;
        }
        while (pi < pats.length && "**".equals(pats[pi])) {
            pi++;
        }
        return pi == pats.length && ti == paths.length;
    }

    private static boolean matchSegment(String pattern, String segment) {
        if ("*".equals(pattern) || "**".equals(pattern)) {
            return true;
        }
        if (!pattern.contains("*") && !pattern.contains("?")) {
            return pattern.equals(segment);
        }
        int i = 0;
        int j = 0;
        int star = -1;
        int match = 0;
        while (j < segment.length()) {
            if (i < pattern.length() && (pattern.charAt(i) == '?' || pattern.charAt(i) == segment.charAt(j))) {
                i++;
                j++;
            } else if (i < pattern.length() && pattern.charAt(i) == '*') {
                star = i++;
                match = j;
            } else if (star >= 0) {
                i = star + 1;
                j = ++match;
            } else {
                return false;
            }
        }
        while (i < pattern.length() && pattern.charAt(i) == '*') {
            i++;
        }
        return i == pattern.length();
    }

    private static String[] split(String path) {
        String p = path.startsWith("/") ? path.substring(1) : path;
        if (p.isEmpty()) {
            return new String[0];
        }
        return p.split("/");
    }

    private static String joinFrom(String[] arr, int from) {
        if (from >= arr.length) {
            return "/";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < arr.length; i++) {
            sb.append('/').append(arr[i]);
        }
        return sb.toString();
    }
}
