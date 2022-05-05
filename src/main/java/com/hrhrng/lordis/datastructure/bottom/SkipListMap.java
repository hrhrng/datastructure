package com.hrhrng.lordis.datastructure.bottom;


import java.util.*;

/**
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author hrhrng
 */

//TODO 封装成Zset，当用户的K相同时，也能插入，将score封装成一个带泛型的类，
// 类中加入时间戳，Comparator中比较score和时间戳

public class SkipListMap<K, V> extends AbstractMap<K,V>
        implements NavigableMap<K,V>, Cloneable, java.io.Serializable
{

    private static final Integer SKIPLIST_MAXLEVEL = 32;
    private static final Double SKIPLIST_P = 0.25;

    /**
     * The comparator used to maintain order in this skip list-map, or
     * null if it uses the natural ordering of its keys.
     *
     * @serial
     */
    private final Comparator<? super K> comparator;



    private transient Entry<K,V> head;

    /**
     * The number of entries in the list
     */
    private transient int size = 0;

    /**
     * The max level of all entry in the list
     */
    private transient int maxLevel = 0;


    /**
     * Constructs a new, empty skip-list map, using the natural ordering of its
     * keys.  All keys inserted into the map must implement the {@link
     * Comparable} interface.  Furthermore, all such keys must be
     * <em>mutually comparable</em>: {@code k1.compareTo(k2)} must not throw
     * a {@code ClassCastException} for any keys {@code k1} and
     * {@code k2} in the map.  If the user attempts to put a key into the
     * map that violates this constraint (for example, the user attempts to
     * put a string key into a map whose keys are integers), the
     * {@code put(Object key, Object value)} call will throw a
     * {@code ClassCastException}.
     */
    public SkipListMap() {
        comparator = null;
    }

    /**
     * Constructs a new, empty skip-list map, ordered according to the given
     * comparator.  All keys inserted into the map must be <em>mutually
     * comparable</em> by the given comparator: {@code comparator.compare(k1,
     * k2)} must not throw a {@code ClassCastException} for any keys
     * {@code k1} and {@code k2} in the map.  If the user attempts to put
     * a key into the map that violates this constraint, the {@code put(Object
     * key, Object value)} call will throw a
     * {@code ClassCastException}.
     *
     * @param comparator the comparator that will be used to order this map.
     *        If {@code null}, the {@linkplain Comparable natural
     *        ordering} of the keys will be used.
     */
    public SkipListMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    /**
     * Constructs a new skip-list map containing the same mappings as the given
     * map, ordered according to the <em>natural ordering</em> of its keys.
     * All keys inserted into the new map must implement the {@link
     * Comparable} interface.  Furthermore, all such keys must be
     * <em>mutually comparable</em>: {@code k1.compareTo(k2)} must not throw
     * a {@code ClassCastException} for any keys {@code k1} and
     * {@code k2} in the map.  This method runs in n*log(n) time.
     *
     * @param  m the map whose mappings are to be placed in this map
     * @throws ClassCastException if the keys in m are not {@link Comparable},
     *         or are not mutually comparable
     * @throws NullPointerException if the specified map is null
     */
    public SkipListMap(Map<? extends K, ? extends V> m) {
        comparator = null;
        putAll(m);
    }



    /**
     * Constructs a new skip-list map containing the same mappings and
     * using the same ordering as the specified sorted map.  This
     * method runs in linear time.
     *
     * @param  m the sorted map whose mappings are to be placed in this map,
     *         and whose comparator is to be used to sort this map
     * @throws NullPointerException if the specified map is null
     */
    public SkipListMap(SortedMap<K, ? extends V> m) {
        comparator = m.comparator();
//        try {
//            buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
//        } catch (java.io.IOException | ClassNotFoundException cannotHappen) {
//        }
    }


    // Query Operations

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() { return size; }

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the
     *         specified key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     */
    public boolean containsKey(Object key) { return getEntry(key) != null; }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value.  More formally, returns {@code true} if and only if
     * this map contains at least one mapping to a value {@code v} such
     * that {@code (value==null ? v==null : value.equals(v))}.  This
     * operation will probably require time linear in the map size for
     * most implementations.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if a mapping to {@code value} exists;
     *         {@code false} otherwise
     * @since 1.2
     */
    public boolean containsValue(Object value) {
//        for (SkipListMap.Entry<K,V> e = getFirstEntry(); e != null; e = successor(e))
//            if (valEquals(value, e.value))
//                return true;
        return false;
    }

    /**
     * Compares using comparator or natural ordering if null.
     * Called only by methods that have performed required type checks.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static int cpr(Comparator c, Object x, Object y) {
        return (c != null) ? c.compare(x, y) : ((Comparable)x).compareTo(y);
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code key} compares
     * equal to {@code k} according to the map's ordering, then this
     * method returns {@code v}; otherwise it returns {@code null}.
     * (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <em>necessarily</em>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     */
    public V get(Object key) {
        Entry<K,V> p = getEntry(key);
        return (p==null ? null : p.value);
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     */
    public V put(K key, V value) {
        return addEntry(key, value, true);
    }

    private V addEntry(K key, V value, boolean replaceOld) {
        if (head == null) {
            head = new Entry(null, null);
        }
        Entry<K,V> t = head;
        int cmp;
        // split comparator and comparable paths
        Comparator<? super K> cptr = comparator;
        //
        LinkedList<Entry<K,V>> ps = new LinkedList<>();
        for (int i = maxLevel - 1; i >= 0; i--) {
            while(t.nextList.get(i) != null) {
                cmp = cpr(cptr, key, t.nextList.get(i).key);
                if (cmp < 0) {
                    break;
                }
                else if (cmp > 0){
                    t = t.nextList.get(i);
                }
                else {
                    V oldValue = t.nextList.get(i).value;
                    if (replaceOld || oldValue == null) {
                        t.nextList.get(i).value = value;
                    }
                    return oldValue;
                }
            }
            ps.addFirst(t);
        }
        size++;
        linkNode(new Entry<>(key, value), ps);
        return null;
    }


    private void linkNode(Entry<K,V> entry, List<Entry<K,V>> predecessors) {
        Integer level = getRamdomLevel();
        maxLevel = Math.max(maxLevel, level);
        entry.level = level;
        if (predecessors.size()>0)
            entry.preEntry = predecessors.get(0);
        for (int i = 0; i < level; i++) {
            if(i < predecessors.size()) {
                entry.nextList.set(i, predecessors.get(i).nextList.get(i));
                predecessors.get(i).nextList.set(i, entry);
            }
            else {
                head.nextList.set(i, entry);
            }
        }
    }

    /**
     * use to generate random level
     */
    static Random random = new Random();

    /**
     * Returns a random level for the new skiplist node we are going to create.
     * The return value of this function is between 1 and SKIPLIST_MAXLEVEL
     * (both inclusive), with a powerlaw-alike distribution where higher
     * levels are less likely to be returned.
     */

    private  Integer getRamdomLevel() {
        int level = 1;
        while ((random.nextInt()&0xFFFF) < (SKIPLIST_P * 0xFFFF))
            level += 1;
        return (level<SKIPLIST_MAXLEVEL) ? level : SKIPLIST_MAXLEVEL;
    }

    public V remove(Object key) {
        Entry<K,V> p = getEntry(key);
        if (p == null)
            return null;

        V oldValue = p.value;
        removeEntry(p);
        return oldValue;
    }

    private void removeEntry(Entry<K,V> p) {
        --size;
        Entry<K,V> t = head;
        int cmp;
        // split comparator and comparable paths
        Comparator<? super K> cptr = comparator;
        //
        LinkedList<Entry<K,V>> ps = new LinkedList<>();
        for (int i = maxLevel - 1; i >= 0; i--) {
            while(t.nextList.get(i) != null) {
                cmp = cpr(cptr, p.key, t.nextList.get(i).key);
                if (cmp <= 0) {
                    break;
                }
                else {
                    t = t.nextList.get(i);
                }
            }
            ps.addFirst(t);
        }
        unLinkNode(p, ps);
    }

    private void unLinkNode(Entry<K,V> entry, List<Entry<K,V>> predecessors) {
        for (int i = 0; i < entry.level; i++) {
            predecessors.get(i).nextList.set(i, entry.nextList.get(i));
        }
    }

    static final class Entry<K,V> implements Map.Entry<K,V> {
        K key;
        V value;
        Integer level;
        Entry<K, V> preEntry;
        List<Entry<K, V>> nextList;
        List<Integer> span;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
            nextList = Arrays.asList(new Entry[SKIPLIST_MAXLEVEL]);
        }

        /**
         * Returns the key.
         *
         * @return the key
         */
        public K getKey() {
            return key;
        }

        /**
         * Returns the value associated with the key.
         *
         * @return the value associated with the key
         */
        public V getValue() {
            return value;
        }

        /**
         * Replaces the value currently associated with the key with the given
         * value.
         *
         * @return the value associated with the key before this method was
         *         called
         */
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;

            return valEquals(key,e.getKey()) && valEquals(value,e.getValue());
        }

        public int hashCode() {
            int keyHash = (key==null ? 0 : key.hashCode());
            int valueHash = (value==null ? 0 : value.hashCode());
            return keyHash ^ valueHash;
        }

        public java.lang.String toString() {
            return key + "=" + value;
        }
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public K firstKey() {
        return head.nextList.get(0).key;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public K lastKey() {
        return null;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings replace any mappings that this map had for any
     * of the keys currently in the specified map.
     *
     * @param  map mappings to be stored in this map
     * @throws ClassCastException if the class of a key or value in
     *         the specified map prevents it from being stored in this map
     * @throws NullPointerException if the specified map is null or
     *         the specified map contains a null key and this map does not
     *         permit null keys
     */
    // TODO improvement by skiplist's features
    public void putAll(Map<? extends K, ? extends V> map) {
        super.putAll(map);
    }

    /**
     * Returns this map's entry for the given key, or {@code null} if the map
     * does not contain an entry for the key.
     *
     * @return this map's entry for the given key, or {@code null} if the map
     *         does not contain an entry for the key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     */
    final SkipListMap.Entry<K,V> getEntry(Object key) {

        if(head == null)
            return null;

        Entry<K,V> t = head;
        int cmp;

        Comparator<? super K> cptr = comparator;
        for (int i = maxLevel - 1; i >= 0; i--) {
            while(t.nextList.get(i) != null) {
                cmp = cpr(cptr, key, t.nextList.get(i).key);
                if (cmp < 0) {
                    break;
                }
                else if (cmp > 0){
                    t = t.nextList.get(i);
                }
                else {
                    return t.nextList.get(i);
                }
            }
        }
        return null;
    }


    /**
     * Gets the entry corresponding to the specified key; if no such entry
     * exists, returns the entry for the least key greater than the specified
     * key; if no such entry exists (i.e., the greatest key in the Tree is less
     * than the specified key), returns {@code null}.
     */
    final SkipListMap.Entry<K,V> getCeilingEntry(K key) {
        return null;
    }

    /**
     * Gets the entry corresponding to the specified key; if no such entry
     * exists, returns the entry for the greatest key less than the specified
     * key; if no such entry exists, returns {@code null}.
     */
    final SkipListMap.Entry<K,V> getFloorEntry(K key) {
        return null;
    }

    /**
     * Gets the entry for the least key greater than the specified
     * key; if no such entry exists, returns the entry for the least
     * key greater than the specified key; if no such entry exists
     * returns {@code null}.
     */
    final Entry<K,V> getHigherEntry(K key) {
        return null;
    }



    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return null;
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        return null;
    }

    @Override
    public K lowerKey(K key) {
        return null;
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        return null;
    }

    @Override
    public K floorKey(K key) {
        return null;
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        return null;
    }

    @Override
    public K ceilingKey(K key) {
        return null;
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        return null;
    }

    @Override
    public K higherKey(K key) {
        return null;
    }

    @Override
    public Entry<K, V> firstEntry() {
        return null;
    }

    @Override
    public Entry<K, V> lastEntry() {
        return null;
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        return null;
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        return null;
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        return null;
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return null;
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return null;
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return null;
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return null;
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return null;
    }

    @Override
    public Comparator<? super K> comparator() {
        return null;
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return null;
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
        return null;
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        return null;
    }

    static final boolean valEquals(Object o1, Object o2) {
        return (o1==null ? o2==null : o1.equals(o2));
    }
    /**
     * Returns the successor of the specified Entry, or null if no such.
     */
    static <K,V> SkipListMap.Entry<K,V> successor(SkipListMap.Entry<K,V> t) {
        return t.nextList.get(0);
    }

    /**
     * Returns the predecessor of the specified Entry, or null if no such.
     */
    static <K,V> SkipListMap.Entry<K,V> predecessor(SkipListMap.Entry<K,V> t) {
        return  t.preEntry;
    }
}
