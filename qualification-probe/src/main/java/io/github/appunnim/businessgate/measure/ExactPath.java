package io.github.appunnim.businessgate.measure;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/** One caller-selected path. No search, sibling enumeration, or recursive walk. */
public final class ExactPath {
    private ExactPath() {}
    public static List<Integer> parse(String raw) {
        if (raw == null || raw.length() > 48) throw new IllegalArgumentException("INVALID_PATH");
        if (raw.isEmpty()) return List.of();
        String[] parts = raw.split(",", -1);
        if (parts.length > 12) throw new IllegalArgumentException("PATH_TOO_DEEP");
        List<Integer> result = new ArrayList<>();
        for (String part : parts) {
            if (!part.matches("0|[1-9][0-9]?")) throw new IllegalArgumentException("INVALID_INDEX");
            int value = Integer.parseInt(part);
            if (value > 63) throw new IllegalArgumentException("INDEX_TOO_LARGE");
            result.add(value);
        }
        return List.copyOf(result);
    }
    public interface Access<N> {
        boolean belongs(N node);
        int children(N node);
        N child(N node, int index);
        void release(N node);
    }
    public interface Capture<N, R> { R read(List<N> path); }
    public static <N, R> R read(N root, List<Integer> path, Access<N> access, Capture<N, R> capture) {
        IdentityHashMap<N, Boolean> owned = new IdentityHashMap<>();
        List<N> chain = new ArrayList<>();
        if (root != null) owned.put(root, true);
        try {
            if (path.size() > 12) throw new IllegalArgumentException("PATH_TOO_DEEP");
            N current = root;
            for (int depth = 0; depth <= path.size(); depth++) {
                if (current == null || !access.belongs(current)) throw new IllegalStateException("PATH_UNAVAILABLE");
                if (chain.contains(current)) throw new IllegalStateException("CYCLIC_PATH");
                chain.add(current);
                if (depth == path.size()) return capture.read(List.copyOf(chain));
                int count = access.children(current), index = path.get(depth);
                if (count < 1 || count > 64 || index < 0 || index >= count || index > 63)
                    throw new IllegalStateException("PATH_UNAVAILABLE");
                current = access.child(current, index);
                if (current != null) owned.put(current, true);
            }
            throw new IllegalStateException("PATH_UNAVAILABLE");
        } finally {
            for (N node : owned.keySet()) access.release(node);
        }
    }
}
