import com.hrhrng.lordis.datastructure.bottom.SkipListMap;
import com.hrhrng.lordis.datastructure.bottom.test;
import org.junit.Test;

import java.util.TreeMap;

public class SkipListMapTest {
    @Test
    public void simpleTest() {
        SkipListMap<Integer, Integer> skipListMap = new SkipListMap();
        for (int i = 0; i < 100; i++) {
            skipListMap.put(i, i);
        }
        for (int i = 0; i < 100; i++) {
            Integer r = skipListMap.get(i);
            System.out.print(r+" ");
        }
        System.out.println();
        for (int i = 0; i < 100; i+=2) {
            skipListMap.remove(i);
        }
        for (int i = 0; i < 100; i++) {
            Integer r = skipListMap.get(i);
            System.out.print(r+" ");
        }
    }


    @Test
    public void t () {
    }
}
