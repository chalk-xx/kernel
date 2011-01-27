package org.sakaiproject.nakamura.memory;

import org.sakaiproject.nakamura.api.memory.Cache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MapDeligate<K, V> implements Map<K, V> {

  private Cache<V> cache;

  public MapDeligate(Cache<V> cache) {
    this.cache = cache;
  }

  public void clear() {
    cache.clear();
  }

  public boolean containsKey(Object key) {
    return cache.containsKey((java.lang.String) key);
  }

  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException("This map is lookup only.");
  }

  public Set<Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException("This map is lookup only.");
  }

  public V get(Object key) {
    return cache.get((String) key);
  }

  public boolean isEmpty() {
    return false;
  }

  public Set<K> keySet() {
    throw new UnsupportedOperationException("This map is lookup only.");
  }

  public V put(K key, V value) {
    return cache.put((String) key, value);
  }

  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException("This map is singly add only, use an iterator or loop.");
  }

  public int size() {
    throw new UnsupportedOperationException("This map is lookup only.");
  }

  public V remove(Object key) {
    V value = cache.get((String) key);
    cache.remove((String) key);
    return value;
  }

  public Collection<V> values() {
    throw new UnsupportedOperationException("This map is lookup only.");
  }


}
