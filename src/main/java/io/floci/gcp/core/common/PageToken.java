package io.floci.gcp.core.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public final class PageToken {

    private PageToken() {}

    public static String encode(int offset) {
        return Base64.getEncoder().encodeToString(
                Integer.toString(offset).getBytes(StandardCharsets.UTF_8));
    }

    public static int decode(String token) {
        if (token == null || token.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(
                    new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return 0;
        }
    }

    public record Page<T>(List<T> items, String nextPageToken) {}

    public static <T> Page<T> paginate(List<T> all, int pageSize, String pageToken) {
        if (pageSize <= 0) {
            return new Page<>(all, null);
        }
        int offset = decode(pageToken);
        if (offset < 0 || offset >= all.size()) {
            return new Page<>(List.of(), null);
        }
        int end = Math.min(offset + pageSize, all.size());
        List<T> page = List.copyOf(all.subList(offset, end));
        String next = end < all.size() ? encode(end) : null;
        return new Page<>(page, next);
    }
}
