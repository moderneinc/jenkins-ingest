package io.moderne.jenkins.ingest;

import java.util.Comparator;

record Key(String origin, String path, String branch) implements Comparable<Key> {
    @Override
    public int compareTo(Key o) {
        return Comparator.comparing(Key::origin)
                .thenComparing(Key::path)
                .thenComparing(Key::branch)
                .compare(this, o);
    }
}
