package com.hrhrng.lordis.datastructure;

import com.hrhrng.lordis.datastructure.bottom.SkipListHashMap;

import java.util.Date;


public class Zset<K, V> {

    SkipListHashMap<KeyObject<K>, V> skipListHashMap;

    static final class KeyObject<K> {
        K key;
        Date date;
        KeyObject(K key){
            this.key = key;
            date = new Date();
        }
    }

}
