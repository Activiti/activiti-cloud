package org.activiti.cloud.services.events.message;

public class MapBuilder<K, V> extends java.util.HashMap<K, V> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public MapBuilder<K, V> with(K key, V value) {
        put(key, value);
        return this;
    }

    public static <K, V> MapBuilder<K, V> map(K key, V value) {
        return new MapBuilder<K, V>().with(key, value);
    }

    public static <K, V> MapBuilder<K, V> emptyMap() {
        return new MapBuilder<K, V>();
    }
    
}