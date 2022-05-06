package com.hrhrng.lordis.datastructure.bottom;

import java.util.HashMap;

public class SkipListHashMap<K, V> {

    //TODO 动态代理
    private HashMap<K, V> hashMap;

    private SkipListMap<K, V> skipListMap;

    SkipListHashMap(){
        skipListMap = new SkipListMap<>();
        hashMap = new HashMap<>();
    }

    synchronized public V get(K key) {
        return hashMap.get(key);
    }

    synchronized public void put(K key, V value) {
        skipListMap.put(key, value);
        hashMap.put(key, value);
    }

}
