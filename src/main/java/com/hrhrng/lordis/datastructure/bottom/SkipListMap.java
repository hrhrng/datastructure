package com.hrhrng.lordis.datastructure.bottom;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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

    private transient Entry<K,V> tail;

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
        buildFromSorted(m);
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
        for (SkipListMap.Entry<K,V> e = getFirstEntry(); e != null; e = successor(e))
            if (valEquals(value, e.value))
                return true;
        return false;
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

    @Override
    public V putIfAbsent(K key, V value) {
        return addEntry(key, value, false);
    }

    private V addEntry(K key, V value, boolean replaceOld) {
        if (head == null) {
            head = new Entry<>(null, null);
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
        if (entry.nextList.get(0) == null) {
            tail = entry;
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

        if(entry == tail) tail = entry.preEntry;

        for (int i = 0; i < entry.level; i++) {
            predecessors.get(i).nextList.set(i, entry.nextList.get(i));
        }
    }

    /**
     * Gets first valid node, unlinking deleted nodes if encountered.
     * @return first node or null if empty
     */
    final Entry<K,V> findFirst() {
        if (head.nextList.get(0) != null) {
            return head.nextList.get(0);
        }
        return null;
    }

    final Entry<K,V> findLast() {
        if (tail != null && tail != head) {
            return tail;
        }
        return null;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public K firstKey() {
        Entry<K,V> n = findFirst();
        if (n == null)
            throw new NoSuchElementException();
        return n.key;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public K lastKey() {
        Entry<K,V> n = findLast();
        if (n == null)
            throw new NoSuchElementException();
        return n.key;
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

    public void putAll(Map<? extends K, ? extends V> map) {
        int mapSize = map.size();
        if (size == 0 && mapSize !=0 && map instanceof SortedMap) {
            if(Objects.equals(comparator, ((SortedMap<?,?>)map).comparator())) {
                buildFromSorted((SortedMap<K,? extends V>)map);
                return;
            }
        }
        super.putAll(map);
    }

    /**
     * Streamlined bulk insertion to initialize from elements of
     * given sorted map.  Call only from constructor or clone
     * method.
     */
    private void buildFromSorted(SortedMap<K, ? extends V> map) {

        Iterator<? extends Map.Entry<? extends K, ? extends V>> it =
                map.entrySet().iterator();

        head = new Entry<>(null, null);

        Entry<K, V> t = head;

        List<Entry<K, V>> ps = Arrays.asList(new Entry[SKIPLIST_MAXLEVEL]);

        ps.forEach(kvEntry -> {
            kvEntry = t;
        });

        while (it.hasNext()) {
            int level = getRamdomLevel();
            Map.Entry<? extends K, ? extends V> e = it.next();
            Entry<K, V> entry = new Entry<>(e.getKey(), e.getValue());
            for (int i = 0; i < level; i++) {
                ps.get(i).nextList.set(i, entry);
                ps.set(i, entry);
            }
        }
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
    final Entry<K,V> getCeilingEntry(K key) {
        if(head == null)
            return null;

        Entry<K,V> t = head;
        int cmp;

        Comparator<? super K> cptr = comparator;
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
                    return t.nextList.get(i);
                }
            }
            ps.addFirst(t);
        }
        return ps.get(0).nextList.get(0);
    }



    /**
     * Gets the entry corresponding to the specified key; if no such entry
     * exists, returns the entry for the greatest key less than the specified
     * key; if no such entry exists, returns {@code null}.
     */
    final SkipListMap.Entry<K,V> getFloorEntry(K key) {
        if(head == null)
            return null;

        Entry<K,V> t = head;
        int cmp;

        Comparator<? super K> cptr = comparator;
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
                    return t.nextList.get(i);
                }
            }
            ps.addFirst(t);
        }
        return ps.get(0);
    }

    /**
     * Gets the entry for the least key greater than the specified
     * key; if no such entry exists, returns the entry for the least
     * key greater than the specified key; if no such entry exists
     * returns {@code null}.
     */
    final Entry<K,V> getHigherEntry(K key) {
        if(head == null)
            return null;

        Entry<K,V> t = head;
        int cmp;

        Comparator<? super K> cptr = comparator;
        LinkedList<Entry<K,V>> ps = new LinkedList<>();

        for (int i = maxLevel - 1; i >= 0; i--) {
            while(t.nextList.get(i) != null) {
                cmp = cpr(cptr, key, t.nextList.get(i).key);
                cmp = cpr(cptr, key, t.nextList.get(i).key);
                if (cmp < 0) {
                    break;
                }
                else if (cmp > 0){
                    t = t.nextList.get(i);
                }
                else {
                    return t.nextList.get(i).nextList.get(i);
                }
            }
            ps.addFirst(t);
        }
        return ps.get(0).nextList.get(0);
    }

    /**
     * Returns the entry for the greatest key less than the specified key; if
     * no such entry exists (i.e., the least key in the Tree is greater than
     * the specified key), returns {@code null}.
     */
    final Entry<K,V> getLowerEntry(K key) {
        if(head == null)
            return null;

        Entry<K,V> t = head;
        int cmp;

        Comparator<? super K> cptr = comparator;
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
                    return t;
                }
            }
            ps.addFirst(t);
        }
        return ps.get(0).nextList.get(0);
    }


    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        head = null;
        tail = null;
    }

    /**
     * Returns a shallow copy of this {@code SkipListMap} instance. (The keys and
     * values themselves are not cloned.)
     *
     * @return a shallow copy of this map
     */
    public Object clone() {
        SkipListMap<K, V> clone;
        try {
            clone = (SkipListMap<K, V>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }

        // Initialize clone with our mappings
        clone.buildFromSorted(this);

        return clone;
    }

    /* ------ NavigableMap API methods ------ */
    /* ------ NavigableMap API methods is Here ------ */

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public Map.Entry<K,V> lowerEntry(K key) {
        return exportEntry(getLowerEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public K lowerKey(K key) {
        return keyOrNull(getLowerEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public Map.Entry<K,V> floorEntry(K key) {
        return exportEntry(getFloorEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public K floorKey(K key) {
        return keyOrNull(getFloorEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public Map.Entry<K,V> ceilingEntry(K key) {
        return exportEntry(getCeilingEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public K ceilingKey(K key) {
        return keyOrNull(getCeilingEntry(key));
    }


    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public Map.Entry<K,V> higherEntry(K key) {
        return exportEntry(getHigherEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public K higherKey(K key) {
        return keyOrNull(getHigherEntry(key));
    }


    /**
     * @since 1.6
     */
    public Map.Entry<K,V> firstEntry() {
        return exportEntry(getFirstEntry());
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> lastEntry() {
        return exportEntry(getLastEntry());
    }


    public Map.Entry<K,V> pollFirstEntry() {
        SkipListMap.Entry<K,V> p = getFirstEntry();
        Map.Entry<K,V> result = exportEntry(p);
        if (p != null)
            removeEntry(p);
        return result;
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> pollLastEntry() {
        SkipListMap.Entry<K,V> p = getLastEntry();
        Map.Entry<K,V> result = exportEntry(p);
        if (p != null)
            removeEntry(p);
        return result;
    }

    /* ---------------- View is Here-------------- */

    /**
     * Fields initialized to contain an instance of the entry set view
     * the first time this view is requested.  Views are stateless, so
     * there's no reason to create more than one.
     */
    // these Object is default in AbstractMap
    private transient Set<K>        keySet;
    private transient Collection<V> values;


    private transient EntrySet entrySet;
    private transient KeySet<K> navigableKeySet;
    private transient NavigableMap<K,V> descendingMap;

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     *
     * <p>The set's iterator returns the keys in ascending order.
     * The set's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#SORTED}
     * and {@link Spliterator#ORDERED} with an encounter order that is ascending
     * key order.  The spliterator's comparator (see
     * {@link java.util.Spliterator#getComparator()}) is {@code null} if
     * the tree map's comparator (see {@link #comparator()}) is {@code null}.
     * Otherwise, the spliterator's comparator is the same as or imposes the
     * same total ordering as the tree map's comparator.
     *
     * <p>The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own {@code remove} operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.  It does not support the {@code add} or {@code addAll}
     * operations.
     */
    public Set<K> keySet() {
        return navigableKeySet();
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return null;
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return null;
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     *
     * <p>The collection's iterator returns the values in ascending order
     * of the corresponding keys. The collection's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#ORDERED}
     * with an encounter order that is ascending order of the corresponding
     * keys.
     *
     * <p>The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own {@code remove} operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll} and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     *
     * <p>The set's iterator returns the entries in ascending key order. The
     * set's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#SORTED} and
     * {@link Spliterator#ORDERED} with an encounter order that is ascending key
     * order.
     *
     * <p>The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own {@code remove} operation, or through the
     * {@code setValue} operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Set.remove}, {@code removeAll}, {@code retainAll} and
     * {@code clear} operations.  It does not support the
     * {@code add} or {@code addAll} operations.
     */
    public Set<Map.Entry<K,V>> entrySet() {
        EntrySet es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet());
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        NavigableMap<K, V> km = descendingMap;
        return (km != null) ? km :
                (descendingMap = new DescendingSubMap<>(this,
                        true, null, true,
                        true, null, true));
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

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} or {@code toKey} is
     *         null and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public SortedMap<K,V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }


    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code toKey} is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public SortedMap<K,V> headMap(K toKey) {
        return headMap(toKey, false);
    }


    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public SortedMap<K,V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Entry<K,V> p = getEntry(key);
        if (p!=null && Objects.equals(oldValue, p.value)) {
            p.value = newValue;
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        Entry<K,V> p = getEntry(key);
        if (p!=null) {
            V oldValue = p.value;
            p.value = value;
            return oldValue;
        }
        return null;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            action.accept(e.key, e.value);
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);

        for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            e.value = function.apply(e.key, e.value);
        }
    }

    /* ------ View class support ------ */

    class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return new ValueIterator(getFirstEntry());
        }

        public int size() {
            return SkipListMap.this.size();
        }

        public boolean contains(Object o) {
            return SkipListMap.this.containsValue(o);
        }

        public boolean remove(Object o) {
            for (Entry<K,V> e = getFirstEntry(); e != null; e = successor(e)) {
                if (valEquals(e.getValue(), o)) {
                    removeEntry(e);
                    return true;
                }
            }
            return false;
        }

        public void clear() {
            SkipListMap.this.clear();
        }

        //TODO 加入 Spliterator
//        public Spliterator<V> spliterator() {
//            return new ValueSpliterator<>(this, null, null, 0, -1, 0);
//        }


    }

    class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator(getFirstEntry());
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
            Object value = entry.getValue();
            Entry<K,V> p = getEntry(entry.getKey());
            return p != null && valEquals(p.getValue(), value);
        }

        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
            Object value = entry.getValue();
            Entry<K,V> p = getEntry(entry.getKey());
            if (p != null && valEquals(p.getValue(), value)) {
                removeEntry(p);
                return true;
            }
            return false;
        }

        public int size() {
            return SkipListMap.this.size();
        }

        public void clear() {
            SkipListMap.this.clear();
        }
        // TODO spliterator
//        public Spliterator<Map.Entry<K,V>> spliterator() {
//            return new EntrySpliterator<>(this, null, null, 0, -1, 0);
//        }
    }

    /*
     * Unlike Values and EntrySet, the KeySet class is static,
     * delegating to a NavigableMap to allow use by SubMaps, which
     * outweighs the ugliness of needing type-tests for the following
     * Iterator methods that are defined appropriately in main versus
     * submap classes.
     */

    Iterator<K> keyIterator() {
        return new KeyIterator(getFirstEntry());
    }

    Iterator<K> descendingKeyIterator() {
        return new DescendingKeyIterator(getLastEntry());
    }

    // TODO 为什么要静态
    static final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final NavigableMap<E, ?> m;
        KeySet(NavigableMap<E,?> map) { m = map; }

        public Iterator<E> iterator() {
            if (m instanceof SkipListMap)
                return ((SkipListMap<E,?>)m).keyIterator();
            else
                return ((NavigableSubMap<E,?>)m).keyIterator();
        }

        public Iterator<E> descendingIterator() {
            if (m instanceof SkipListMap)
                return ((SkipListMap<E,?>)m).descendingKeyIterator();
            // 这里只会提供给JDK内部使用
            else
                return ((NavigableSubMap<E,?>)m).descendingKeyIterator();
        }

        public int size() { return m.size(); }
        public boolean isEmpty() { return m.isEmpty(); }
        public boolean contains(Object o) { return m.containsKey(o); }
        public void clear() { m.clear(); }
        public E lower(E e) { return m.lowerKey(e); }
        public E floor(E e) { return m.floorKey(e); }
        public E ceiling(E e) { return m.ceilingKey(e); }
        public E higher(E e) { return m.higherKey(e); }
        public E first() { return m.firstKey(); }
        public E last() { return m.lastKey(); }
        public Comparator<? super E> comparator() { return m.comparator(); }
        public E pollFirst() {
            Map.Entry<E,?> e = m.pollFirstEntry();
            return (e == null) ? null : e.getKey();
        }
        public E pollLast() {
            Map.Entry<E,?> e = m.pollLastEntry();
            return (e == null) ? null : e.getKey();
        }
        public boolean remove(Object o) {
            int oldSize = size();
            m.remove(o);
            return size() != oldSize;
        }
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                      E toElement,   boolean toInclusive) {
            return new KeySet<>(m.subMap(fromElement, fromInclusive,
                    toElement,   toInclusive));
        }
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new KeySet<>(m.headMap(toElement, inclusive));
        }
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new KeySet<>(m.tailMap(fromElement, inclusive));
        }
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }
        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }
        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
        public NavigableSet<E> descendingSet() {
            return new KeySet<>(m.descendingMap());
        }
        // TODO 完成SP
//        public Spliterator<E> spliterator() {
//            return keySpliteratorFor(m);
//        }
    }



    /**
     * Base class for SkipListMap Iterators 迭代器 is Here
     */
    abstract class PrivateEntryIterator<T> implements Iterator<T> {
        Entry<K,V> next;
        Entry<K,V> lastReturned;


        PrivateEntryIterator(Entry<K,V> first) {
            lastReturned = null;
            next = first;
        }

        public final boolean hasNext() {
            return next != null;
        }


        final Entry<K,V> nextEntry() {
            Entry<K,V> e = next;
            if (e == null)
                throw new NoSuchElementException();
            next = successor(e);
            lastReturned = e;
            return e;
        }

        final Entry<K,V> prevEntry() {
            Entry<K,V> e = next;
            if (e == null)
                throw new NoSuchElementException();
            next = predecessor(e);
            lastReturned = e;
            return e;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            // deleted entries are replaced by their successors
            removeEntry(lastReturned);
            lastReturned = null;
        }
    }

    final class EntryIterator extends PrivateEntryIterator<Map.Entry<K,V>> {
        EntryIterator(Entry<K,V> first) {
            super(first);
        }
        public Map.Entry<K,V> next() {
            return nextEntry();
        }
    }

    final class ValueIterator extends PrivateEntryIterator<V> {
        ValueIterator(Entry<K,V> first) {
            super(first);
        }
        public V next() {
            return nextEntry().value;
        }
    }

    final class KeyIterator extends PrivateEntryIterator<K> {
        KeyIterator(Entry<K,V> first) {
            super(first);
        }
        public K next() {
            return nextEntry().key;
        }
    }

    // TODO 感觉这里有坑，和head和tail有关
    final class DescendingKeyIterator extends PrivateEntryIterator<K> {
        DescendingKeyIterator(Entry<K,V> first) {
            super(first);
        }
        public K next() {
            return prevEntry().key;
        }
        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            removeEntry(lastReturned);
            lastReturned = null;
        }
    }

    /* ------ Little utilities ------ */

    /**
     * Compares using comparator or natural ordering if null.
     * Called only by methods that have performed required type checks.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static int cpr(Comparator c, Object x, Object y) {
        return (c != null) ? c.compare(x, y) : ((Comparable)x).compareTo(y);
    }

    /**
     * Test two values for equality.  Differs from o1.equals(o2) only in
     * that it copes with {@code null} o1 properly.
     */
    static final boolean valEquals(Object o1, Object o2) {
        return (o1==null ? o2==null : o1.equals(o2));
    }

    /**
     * Return SimpleImmutableEntry for entry, or null if null
     */
    static <K,V> Map.Entry<K,V> exportEntry(Entry<K,V> e) {
        return (e == null) ? null :
                new AbstractMap.SimpleImmutableEntry<>(e);
    }

    /**
     * Return key for entry, or null if null
     */
    static <K,V> K keyOrNull(Entry<K,V> e) {
        return (e == null) ? null : e.key;
    }

    /**
     * Returns the key corresponding to the specified Entry.
     * @throws NoSuchElementException if the Entry is null
     */
    static <K> K key(Entry<K,?> e) {
        if (e==null)
            throw new NoSuchElementException();
        return e.key;
    }


    /*------ SubMaps is Here --------*/

    /**
     * @serial include
     */
    abstract static class NavigableSubMap<K,V> extends AbstractMap<K,V>
            implements NavigableMap<K,V>, java.io.Serializable {
        @java.io.Serial
        private static final long serialVersionUID = -2102997345730753016L;
        /**
         * The backing map.
         */
        final SkipListMap<K,V> m;

        /**
         * Endpoints are represented as triples (fromStart, lo,
         * loInclusive) and (toEnd, hi, hiInclusive). If fromStart is
         * true, then the low (absolute) bound is the start of the
         * backing map, and the other values are ignored. Otherwise,
         * if loInclusive is true, lo is the inclusive bound, else lo
         * is the exclusive bound. Similarly for the upper bound.
         */
        @SuppressWarnings("serial") // Conditionally serializable
        final K lo;
        @SuppressWarnings("serial") // Conditionally serializable
        final K hi;
        final boolean fromStart, toEnd;
        final boolean loInclusive, hiInclusive;

        NavigableSubMap(SkipListMap<K,V> m,
                        boolean fromStart, K lo, boolean loInclusive,
                        boolean toEnd,     K hi, boolean hiInclusive) {
            if (!fromStart && !toEnd) {
                if (cpr(m.comparator,lo, hi) > 0)
                    throw new IllegalArgumentException("fromKey > toKey");
            } else {
                if (!fromStart) // type check
                    cpr(m.comparator,lo, lo);
                if (!toEnd)
                    cpr(m.comparator,hi, hi);
            }

            this.m = m;
            this.fromStart = fromStart;
            this.lo = lo;
            this.loInclusive = loInclusive;
            this.toEnd = toEnd;
            this.hi = hi;
            this.hiInclusive = hiInclusive;
        }

        // internal utilities

        final boolean tooLow(Object key) {
            if (!fromStart) {
                int c = cpr(m.comparator,key, lo);
                if (c < 0 || (c == 0 && !loInclusive))
                    return true;
            }
            return false;
        }

        final boolean tooHigh(Object key) {
            if (!toEnd) {
                int c = cpr(m.comparator,key, hi);
                if (c > 0 || (c == 0 && !hiInclusive))
                    return true;
            }
            return false;
        }

        final boolean inRange(Object key) {
            return !tooLow(key) && !tooHigh(key);
        }

        final boolean inClosedRange(Object key) {
            return (fromStart || cpr(m.comparator,key, lo) >= 0)
                    && (toEnd || cpr(m.comparator,hi, key) >= 0);
        }

        final boolean inRange(Object key, boolean inclusive) {
            return inclusive ? inRange(key) : inClosedRange(key);
        }

        /*
         * Absolute versions of relation operations.
         * Subclasses map to these using like-named "sub"
         * versions that invert senses for descending maps
         */

        final SkipListMap.Entry<K,V> absLowest() {
            SkipListMap.Entry<K,V> e =
                    (fromStart ?  m.getFirstEntry() :
                            (loInclusive ? m.getCeilingEntry(lo) :
                                    m.getHigherEntry(lo)));
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        final SkipListMap.Entry<K,V> absHighest() {
            SkipListMap.Entry<K,V> e =
                    (toEnd ?  m.getLastEntry() :
                            (hiInclusive ?  m.getFloorEntry(hi) :
                                    m.getLowerEntry(hi)));
            return (e == null || tooLow(e.key)) ? null : e;
        }

        final SkipListMap.Entry<K,V> absCeiling(K key) {
            if (tooLow(key))
                return absLowest();
            SkipListMap.Entry<K,V> e = m.getCeilingEntry(key);
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        final SkipListMap.Entry<K,V> absHigher(K key) {
            if (tooLow(key))
                return absLowest();
            SkipListMap.Entry<K,V> e = m.getHigherEntry(key);
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        final SkipListMap.Entry<K,V> absFloor(K key) {
            if (tooHigh(key))
                return absHighest();
            SkipListMap.Entry<K,V> e = m.getFloorEntry(key);
            return (e == null || tooLow(e.key)) ? null : e;
        }

        final SkipListMap.Entry<K,V> absLower(K key) {
            if (tooHigh(key))
                return absHighest();
            SkipListMap.Entry<K,V> e = m.getLowerEntry(key);
            return (e == null || tooLow(e.key)) ? null : e;
        }

        /** Returns the absolute high fence for ascending traversal */
        final SkipListMap.Entry<K,V> absHighFence() {
            return (toEnd ? null : (hiInclusive ?
                    m.getHigherEntry(hi) :
                    m.getCeilingEntry(hi)));
        }

        /** Return the absolute low fence for descending traversal  */
        final SkipListMap.Entry<K,V> absLowFence() {
            return (fromStart ? null : (loInclusive ?
                    m.getLowerEntry(lo) :
                    m.getFloorEntry(lo)));
        }

        // Abstract methods defined in ascending vs descending classes
        // These relay to the appropriate absolute versions

        abstract SkipListMap.Entry<K,V> subLowest();
        abstract SkipListMap.Entry<K,V> subHighest();
        abstract SkipListMap.Entry<K,V> subCeiling(K key);
        abstract SkipListMap.Entry<K,V> subHigher(K key);
        abstract SkipListMap.Entry<K,V> subFloor(K key);
        abstract SkipListMap.Entry<K,V> subLower(K key);

        /** Returns ascending iterator from the perspective of this submap */
        abstract Iterator<K> keyIterator();

        abstract Spliterator<K> keySpliterator();

        /** Returns descending iterator from the perspective of this submap */
        abstract Iterator<K> descendingKeyIterator();

        // public methods

        public boolean isEmpty() {
            return (fromStart && toEnd) ? m.isEmpty() : entrySet().isEmpty();
        }

        public int size() {
            return (fromStart && toEnd) ? m.size() : entrySet().size();
        }

        public final boolean containsKey(Object key) {
            return inRange(key) && m.containsKey(key);
        }

        public final V put(K key, V value) {
            if (!inRange(key))
                throw new IllegalArgumentException("key out of range");
            return m.put(key, value);
        }

        public final V get(Object key) {
            return !inRange(key) ? null :  m.get(key);
        }

        public final V remove(Object key) {
            return !inRange(key) ? null : m.remove(key);
        }

        public final Map.Entry<K,V> ceilingEntry(K key) {
            return exportEntry(subCeiling(key));
        }

        public final K ceilingKey(K key) {
            return keyOrNull(subCeiling(key));
        }

        public final Map.Entry<K,V> higherEntry(K key) {
            return exportEntry(subHigher(key));
        }

        public final K higherKey(K key) {
            return keyOrNull(subHigher(key));
        }

        public final Map.Entry<K,V> floorEntry(K key) {
            return exportEntry(subFloor(key));
        }

        public final K floorKey(K key) {
            return keyOrNull(subFloor(key));
        }

        public final Map.Entry<K,V> lowerEntry(K key) {
            return exportEntry(subLower(key));
        }

        public final K lowerKey(K key) {
            return keyOrNull(subLower(key));
        }

        public final K firstKey() {
            return key(subLowest());
        }

        public final K lastKey() {
            return key(subHighest());
        }

        public final Map.Entry<K,V> firstEntry() {
            return exportEntry(subLowest());
        }

        public final Map.Entry<K,V> lastEntry() {
            return exportEntry(subHighest());
        }

        public final Map.Entry<K,V> pollFirstEntry() {
            SkipListMap.Entry<K,V> e = subLowest();
            Map.Entry<K,V> result = exportEntry(e);
            if (e != null)
                m.removeEntry(e);
            return result;
        }

        public final Map.Entry<K,V> pollLastEntry() {
            SkipListMap.Entry<K,V> e = subHighest();
            Map.Entry<K,V> result = exportEntry(e);
            if (e != null)
                m.removeEntry(e);
            return result;
        }

        // Views
        transient NavigableMap<K,V> descendingMapView;
        transient SkipListMap.NavigableSubMap.EntrySetView entrySetView;
        transient SkipListMap.KeySet<K> navigableKeySetView;

        public final NavigableSet<K> navigableKeySet() {
            SkipListMap.KeySet<K> nksv = navigableKeySetView;
            return (nksv != null) ? nksv :
                    (navigableKeySetView = new SkipListMap.KeySet<>(this));
        }

        public final Set<K> keySet() {
            return navigableKeySet();
        }

        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        public final SortedMap<K,V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public final SortedMap<K,V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        public final SortedMap<K,V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        // View classes

        abstract class EntrySetView extends AbstractSet<Map.Entry<K,V>> {
            private transient int size = -1, sizeModCount;

            public int size() {
                if (fromStart && toEnd)
                    return m.size();
                if (size == -1 ) {
                    size = 0;
                    Iterator<?> i = iterator();
                    while (i.hasNext()) {
                        size++;
                        i.next();
                    }
                }
                return size;
            }

            public boolean isEmpty() {
                SkipListMap.Entry<K,V> n = absLowest();
                return n == null || tooHigh(n.key);
            }

            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
                Object key = entry.getKey();
                if (!inRange(key))
                    return false;
                SkipListMap.Entry<?,?> node = m.getEntry(key);
                return node != null &&
                        valEquals(node.getValue(), entry.getValue());
            }

            public boolean remove(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
                Object key = entry.getKey();
                if (!inRange(key))
                    return false;
                SkipListMap.Entry<K,V> node = m.getEntry(key);
                if (node!=null && valEquals(node.getValue(),
                        entry.getValue())) {
                    m.removeEntry(node);
                    return true;
                }
                return false;
            }
        }

        /**
         * Iterators for SubMaps
         */
        abstract class SubMapIterator<T> implements Iterator<T> {
            SkipListMap.Entry<K,V> lastReturned;
            SkipListMap.Entry<K,V> next;
            final Object fenceKey;

            SubMapIterator(SkipListMap.Entry<K,V> first,
                           SkipListMap.Entry<K,V> fence) {
                lastReturned = null;
                next = first;
                fenceKey = fence == null ? null : fence.key;
            }

            public final boolean hasNext() {
                return next != null && next.key != fenceKey;
            }

            final SkipListMap.Entry<K,V> nextEntry() {
                SkipListMap.Entry<K,V> e = next;
                if (e == null || e.key == fenceKey)
                    throw new NoSuchElementException();
                next = successor(e);
                lastReturned = e;
                return e;
            }

            final SkipListMap.Entry<K,V> prevEntry() {
                SkipListMap.Entry<K,V> e = next;
                if (e == null || e.key == fenceKey)
                    throw new NoSuchElementException();
                next = predecessor(e);
                lastReturned = e;
                return e;
            }

            final void removeAscending() {
                if (lastReturned == null)
                    throw new IllegalStateException();
                // deleted entries are replaced by their successors
                m.removeEntry(lastReturned);
                lastReturned = null;
            }

            final void removeDescending() {
                if (lastReturned == null)
                    throw new IllegalStateException();
                m.removeEntry(lastReturned);
                lastReturned = null;
            }

        }

        final class SubMapEntryIterator extends SubMapIterator<Map.Entry<K,V>> {
            SubMapEntryIterator(SkipListMap.Entry<K,V> first,
                                SkipListMap.Entry<K,V> fence) {
                super(first, fence);
            }
            public Map.Entry<K,V> next() {
                return nextEntry();
            }
            public void remove() {
                removeAscending();
            }
        }

        final class DescendingSubMapEntryIterator extends SubMapIterator<Map.Entry<K,V>> {
            DescendingSubMapEntryIterator(SkipListMap.Entry<K,V> last,
                                          SkipListMap.Entry<K,V> fence) {
                super(last, fence);
            }

            public Map.Entry<K,V> next() {
                return prevEntry();
            }
            public void remove() {
                removeDescending();
            }
        }

        // Implement minimal Spliterator as KeySpliterator backup
        final class SubMapKeyIterator extends SubMapIterator<K>
                implements Spliterator<K> {
            SubMapKeyIterator(SkipListMap.Entry<K,V> first,
                              SkipListMap.Entry<K,V> fence) {
                super(first, fence);
            }
            public K next() {
                return nextEntry().key;
            }
            public void remove() {
                removeAscending();
            }
            public Spliterator<K> trySplit() {
                return null;
            }
            public void forEachRemaining(Consumer<? super K> action) {
                while (hasNext())
                    action.accept(next());
            }
            public boolean tryAdvance(Consumer<? super K> action) {
                if (hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }
            public long estimateSize() {
                return Long.MAX_VALUE;
            }
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED |
                        Spliterator.SORTED;
            }
            public final Comparator<? super K>  getComparator() {
                return SkipListMap.NavigableSubMap.this.comparator();
            }
        }

        final class DescendingSubMapKeyIterator extends SubMapIterator<K>
                implements Spliterator<K> {
            DescendingSubMapKeyIterator(SkipListMap.Entry<K,V> last,
                                        SkipListMap.Entry<K,V> fence) {
                super(last, fence);
            }
            public K next() {
                return prevEntry().key;
            }
            public void remove() {
                removeDescending();
            }
            public Spliterator<K> trySplit() {
                return null;
            }
            public void forEachRemaining(Consumer<? super K> action) {
                while (hasNext())
                    action.accept(next());
            }
            public boolean tryAdvance(Consumer<? super K> action) {
                if (hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }
            public long estimateSize() {
                return Long.MAX_VALUE;
            }
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED;
            }
        }
    }

    /**
     * @serial include
     */
    static final class AscendingSubMap<K,V> extends NavigableSubMap<K,V> {
        @java.io.Serial
        private static final long serialVersionUID = 912986545866124060L;

        AscendingSubMap(SkipListMap<K,V> m,
                        boolean fromStart, K lo, boolean loInclusive,
                        boolean toEnd,     K hi, boolean hiInclusive) {
            super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        public Comparator<? super K> comparator() {
            return m.comparator();
        }

        public NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                        K toKey,   boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive))
                throw new IllegalArgumentException("fromKey out of range");
            if (!inRange(toKey, toInclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new AscendingSubMap<>(m,
                    false, fromKey, fromInclusive,
                    false, toKey,   toInclusive);
        }

        public NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
            if (!inRange(toKey, inclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new AscendingSubMap<>(m,
                    fromStart, lo,    loInclusive,
                    false,     toKey, inclusive);
        }

        public NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
            if (!inRange(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new AscendingSubMap<>(m,
                    false, fromKey, inclusive,
                    toEnd, hi,      hiInclusive);
        }

        public NavigableMap<K,V> descendingMap() {
            NavigableMap<K,V> mv = descendingMapView;
            return (mv != null) ? mv :
                    (descendingMapView =
                            new DescendingSubMap<>(m,
                                    fromStart, lo, loInclusive,
                                    toEnd,     hi, hiInclusive));
        }

        Iterator<K> keyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        Spliterator<K> keySpliterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        Iterator<K> descendingKeyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        final class AscendingEntrySetView extends EntrySetView {
            public Iterator<Map.Entry<K,V>> iterator() {
                return new SubMapEntryIterator(absLowest(), absHighFence());
            }
        }

        public Set<Map.Entry<K,V>> entrySet() {
            EntrySetView es = entrySetView;
            return (es != null) ? es : (entrySetView = new AscendingEntrySetView());
        }

        SkipListMap.Entry<K,V> subLowest()       { return absLowest(); }
        SkipListMap.Entry<K,V> subHighest()      { return absHighest(); }
        SkipListMap.Entry<K,V> subCeiling(K key) { return absCeiling(key); }
        SkipListMap.Entry<K,V> subHigher(K key)  { return absHigher(key); }
        SkipListMap.Entry<K,V> subFloor(K key)   { return absFloor(key); }
        SkipListMap.Entry<K,V> subLower(K key)   { return absLower(key); }
    }

    /**
     * @serial include
     */
    static final class DescendingSubMap<K,V>  extends NavigableSubMap<K,V> {
        @java.io.Serial
        private static final long serialVersionUID = 912986545866120460L;
        DescendingSubMap(SkipListMap<K,V> m,
                         boolean fromStart, K lo, boolean loInclusive,
                         boolean toEnd,     K hi, boolean hiInclusive) {
            super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        @SuppressWarnings("serial") // Conditionally serializable
        private final Comparator<? super K> reverseComparator =
                Collections.reverseOrder(m.comparator);

        public Comparator<? super K> comparator() {
            return reverseComparator;
        }

        public NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                        K toKey,   boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive))
                throw new IllegalArgumentException("fromKey out of range");
            if (!inRange(toKey, toInclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new DescendingSubMap<>(m,
                    false, toKey,   toInclusive,
                    false, fromKey, fromInclusive);
        }

        public NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
            if (!inRange(toKey, inclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new DescendingSubMap<>(m,
                    false, toKey, inclusive,
                    toEnd, hi,    hiInclusive);
        }

        public NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
            if (!inRange(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new DescendingSubMap<>(m,
                    fromStart, lo, loInclusive,
                    false, fromKey, inclusive);
        }

        public NavigableMap<K,V> descendingMap() {
            NavigableMap<K,V> mv = descendingMapView;
            return (mv != null) ? mv :
                    (descendingMapView =
                            new AscendingSubMap<>(m,
                                    fromStart, lo, loInclusive,
                                    toEnd,     hi, hiInclusive));
        }

        Iterator<K> keyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        Spliterator<K> keySpliterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        Iterator<K> descendingKeyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        final class DescendingEntrySetView extends EntrySetView {
            public Iterator<Map.Entry<K,V>> iterator() {
                return new DescendingSubMapEntryIterator(absHighest(), absLowFence());
            }
        }

        public Set<Map.Entry<K,V>> entrySet() {
            EntrySetView es = entrySetView;
            return (es != null) ? es : (entrySetView = new DescendingSubMap.DescendingEntrySetView());
        }

        SkipListMap.Entry<K,V> subLowest()       { return absHighest(); }
        SkipListMap.Entry<K,V> subHighest()      { return absLowest(); }
        SkipListMap.Entry<K,V> subCeiling(K key) { return absFloor(key); }
        SkipListMap.Entry<K,V> subHigher(K key)  { return absLower(key); }
        SkipListMap.Entry<K,V> subFloor(K key)   { return absCeiling(key); }
        SkipListMap.Entry<K,V> subLower(K key)   { return absHigher(key); }
    }


    /**
     * This class exists solely for the sake of serialization
     * compatibility with previous releases of TreeMap that did not
     * support NavigableMap.  It translates an old-version SubMap into
     * a new-version AscendingSubMap. This class is never otherwise
     * used.
     *
     * @serial include
     */
    private class SubMap extends AbstractMap<K,V>
            implements SortedMap<K,V>, java.io.Serializable {
        @java.io.Serial
        private static final long serialVersionUID = -6520786458950516097L;
        private boolean fromStart = false, toEnd = false;
        @SuppressWarnings("serial") // Conditionally serializable
        private K fromKey;
        @SuppressWarnings("serial") // Conditionally serializable
        private K toKey;
        @java.io.Serial
        private Object readResolve() {
            return new AscendingSubMap<>(SkipListMap.this,
                    fromStart, fromKey, true,
                    toEnd, toKey, false);
        }
        public Set<Map.Entry<K,V>> entrySet() { throw new InternalError(); }
        public K lastKey() { throw new InternalError(); }
        public K firstKey() { throw new InternalError(); }
        public SortedMap<K,V> subMap(K fromKey, K toKey) { throw new InternalError(); }
        public SortedMap<K,V> headMap(K toKey) { throw new InternalError(); }
        public SortedMap<K,V> tailMap(K fromKey) { throw new InternalError(); }
        public Comparator<? super K> comparator() { throw new InternalError(); }
    }




    /* ------ SortedMap API methods ------ */

    @Override
    public Comparator<? super K> comparator() {
        return comparator;
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
     * Returns the first Entry in the SkipListMap (according to the SkipListMap's
     * key-sort function).  Returns null if the SkipListMap is empty.
     */
    private Entry<K,V> getFirstEntry() {
        if (size == 0) return null;
        return head.nextList.get(0);
    }
    /**
     * Returns the last Entry in the SkipListMap (according to the SkipListMap's
     * key-sort function).  Returns null if the SkipListMap is empty.
     */
    private Entry<K,V> getLastEntry() {
        if (size == 0) return null;
        return tail;
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
