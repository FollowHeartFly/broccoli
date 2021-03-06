package edu.stanford.nlp.stats;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.math.ArrayMath;
import edu.stanford.nlp.math.SloppyMath;
import edu.stanford.nlp.util.MapFactory;
import edu.stanford.nlp.util.MutableDouble;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;

/**
 * A class representing a mapping between pairs of typed objects and double values.
 * 
 * @author Teg Grenager
 */
public class TwoDimensionalCounter<K1, K2> implements Serializable {
  
  public static final long serialVersionUID = 1L;

  // the outermost Map
  private Map<K1, Counter<K2>> map;

  // the total of all counts
  private double total;

  // the MapFactory used to make new maps to counters
  private MapFactory<K1,Counter<K2>> outerMF;
  
  // the MapFactory used to make new maps in the inner counter
  private MapFactory<K2,MutableDouble> innerMF;

  public boolean equals(Object o) {
    if (!(o instanceof TwoDimensionalCounter)) {
      return false;
    }
    return ((TwoDimensionalCounter)o).map.equals(map);
  }

  public int hashCode() {
    return map.hashCode() + 17;
  }
  
  /**
   * @param o
   * @return the inner Counter associated with key o
   */
  public Counter<K2> getCounter(K1 o) {
    Counter<K2> c = map.get(o);
    if (c == null) {
      c = new Counter<K2>(innerMF);
      map.put(o, c);
    }
    return c;
  }
  
  public Set<Map.Entry<K1,Counter<K2>>> entrySet(){
    return map.entrySet();
  }

  /**
   * @return total number of entries (key pairs)
   */
  public int size() {
    int result = 0;
    for (K1 o : firstKeySet()) {
      Counter<K2> c = map.get(o);
      result += c.size();
    }
    return result;
  }

  public boolean containsKey(K1 o1, K2 o2) {
    if (!map.containsKey(o1)) return false;
    Counter<K2> c = map.get(o1);
    if (!c.containsKey(o2)) return false;
    return true;
  }

  /**
   * @param o1
   * @param o2
   */
  public void incrementCount(K1 o1, K2 o2) {
    incrementCount(o1, o2, 1.0);
  }

  /**
   * @param o1
   * @param o2
   * @param count
   */
  public void incrementCount(K1 o1, K2 o2, double count) {
    Counter<K2> c = getCounter(o1);
    c.incrementCount(o2, count);
    total += count;
  }

  /**
   * @param o1
   * @param o2
   */
  public void decrementCount(K1 o1, K2 o2) {
    incrementCount(o1, o2, -1.0);
  }

  /**
   * @param o1
   * @param o2
   * @param count
   */
  public void decrementCount(K1 o1, K2 o2, double count) {
    incrementCount(o1, o2, -count);
  }

  /**
   * @param o1
   * @param o2
   * @param count
   */
  public void setCount(K1 o1, K2 o2, double count) {
    Counter<K2> c = getCounter(o1);
    double oldCount = getCount(o1, o2);
    total -= oldCount;
    c.setCount(o2, count);
    total += count;
  }

  public double remove(K1 o1, K2 o2) {
    Counter<K2> c = getCounter(o1);
    double oldCount = getCount(o1, o2);
    total -= oldCount;
    c.remove(o2);
    if (c.size()==0) {
      map.remove(o1);
    }
    return oldCount;
  }

  /**
   * @param o1
   * @param o2
   * @return
   */
  public double getCount(K1 o1, K2 o2) {
    Counter c = getCounter(o1);
    return c.getCount(o2);
  }

  /**
   * Takes linear time.
   * 
   * @return
   */
  public double totalCount() {
    return total;
  }

  /**
   * @return
   */
  public double totalCount(K1 k1) {
    Counter c = getCounter(k1);
    return c.totalCount();
  }

  public Set<K1> firstKeySet() {
    return map.keySet();
  }

  public Counter<K2> setCounter(K1 o, Counter<K2> c) {
    Counter<K2> old = getCounter(o);
    total -= old.totalCount();
    map.put(o, c);
    total += c.totalCount();
    return old;
  }

  /**
   * Produces a new ConditionalCounter.
   * 
   * @param cc
   * @return a new ConditionalCounter, where order of indices is reversed
   */
  public static <K1,K2> TwoDimensionalCounter<K2,K1> reverseIndexOrder(TwoDimensionalCounter<K1,K2> cc) {
    TwoDimensionalCounter<K2,K1> result = new TwoDimensionalCounter<K2,K1>(
        (MapFactory)cc.outerMF, (MapFactory)cc.innerMF);
    
    for (K1 key1 : cc.firstKeySet()) {
      Counter<K2> c = cc.getCounter(key1);
      for (K2 key2 : c.keySet()) {
        double count = c.getCount(key2);
        result.setCount(key2, key1, count);
      }
    }
    return result;
  }

  /**
   * A simple String representation of this TwoDimensionalCounter, which has 
   * the String representation of each key pair
   * on a separate line, followed by the count for that pair.
   * The items are tab separated, so the result is a tab-separated value (TSV)
   * file.  Iff none of the keys contain spaces, it will also be possible to
   * treat this as whitespace separated fields.
   */
  public String toString() {
    StringBuilder buff = new StringBuilder();
    for (K1 key1 : map.keySet()) {
      Counter<K2> c = getCounter(key1);
      for (K2 key2 : c.keySet()) {
        double score = c.getCount(key2);
        buff.append(key1 + "\t" + key2 + "\t" + score + "\n");
      }
    }
    return buff.toString();
  }

  public String toMatrixString(int cellSize) {
    List<K1> firstKeys = new ArrayList<K1>(firstKeySet());
    List<K2> secondKeys = new ArrayList<K2>(secondKeySet());
    Collections.sort((List<? extends Comparable>)firstKeys);
    Collections.sort((List<? extends Comparable>)secondKeys);
    double[][] counts = toMatrix(firstKeys, secondKeys);
    return ArrayMath.toString(counts, cellSize, firstKeys.toArray(), secondKeys.toArray(), new DecimalFormat(), true);
  }

  /**
   * Given an ordering of the first (row) and second (column) keys, will produce a double matrix.
   * 
   * @param firstKeys
   * @param secondKeys
   * @return
   */
  public double[][] toMatrix(List<K1> firstKeys, List<K2> secondKeys) {
    double[][] counts = new double[firstKeys.size()][secondKeys.size()];
    for (int i = 0; i < firstKeys.size(); i++) {
      for (int j = 0; j < secondKeys.size(); j++) {
        counts[i][j] = getCount(firstKeys.get(i), secondKeys.get(j));
      }
    }
    return counts;
  }

  public String toCSVString(NumberFormat nf) {
    List<K1> firstKeys = new ArrayList<K1>(firstKeySet());
    List<K2> secondKeys = new ArrayList<K2>(secondKeySet());
    Collections.sort((List<? extends Comparable>)firstKeys);
    Collections.sort((List<? extends Comparable>)secondKeys);
    StringBuilder b = new StringBuilder();
    String[] headerRow = new String[secondKeys.size() + 1];
    headerRow[0] = "";
    for (int j = 0; j < secondKeys.size(); j++) {
      headerRow[j + 1] = secondKeys.get(j).toString();
    }
    b.append(StringUtils.toCSVString(headerRow)).append("\n");
    for (int i = 0; i < firstKeys.size(); i++) {
      String[] row = new String[secondKeys.size() + 1];
      K1 rowLabel = (K1) firstKeys.get(i);
      row[0] = rowLabel.toString();
      for (int j = 0; j < secondKeys.size(); j++) {
        K2 colLabel = (K2) secondKeys.get(j);
        row[j + 1] = nf.format(getCount(rowLabel, colLabel));
      }
      b.append(StringUtils.toCSVString(row)).append("\n");
    }
    return b.toString();
  }

  public Set<K2> secondKeySet() {
    Set<K2> result = new HashSet<K2>();
    for (K1 k1 : firstKeySet()) {
      for (K2 k2 : getCounter(k1).keySet()) {
        result.add(k2);
      }
    }
    return result;
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public Counter<Pair<K1, K2>> flatten() {
    Counter<Pair<K1, K2>> result = new Counter<Pair<K1, K2>>();
    for (K1 key1 : firstKeySet()) {
      Counter<K2> inner = getCounter(key1);
      for (K2 key2 : inner.keySet()) {
        result.setCount(new Pair<K1, K2>(key1, key2), inner.getCount(key2));
      }
    }
    return result;
  }

  public void addAll(TwoDimensionalCounter<K1, K2> c) {
    for (K1 key : c.firstKeySet()) {
      Counter<K2> inner = c.getCounter(key);
      Counter<K2> myInner = getCounter(key);
      myInner.addAll(inner);
      total += inner.totalCount();
    }
  }

  public void addAll(K1 key, Counter<K2> c) {
    Counter<K2> myInner = getCounter(key);
    myInner.addAll(c);
    total += c.totalCount();
  }

  public void subtractAll(K1 key, Counter<K2> c) {
    Counter<K2> myInner = getCounter(key);
    myInner.subtractAll(c);
    total -= c.totalCount();
  }

  
  
  public void subtractAll(TwoDimensionalCounter<K1, K2> c, boolean removeKeys) {
    for (K1 key : c.firstKeySet()) {
      Counter<K2> inner = c.getCounter(key);
      Counter<K2> myInner = getCounter(key);
      myInner.subtractAll(inner, removeKeys);
      total -= inner.totalCount();
    }
  }

  public void removeZeroCounts() {
    Set<K1> firstKeySet = new HashSet<K1>(firstKeySet());
    for (K1 k1 : firstKeySet) {
      Counter<K2> c = getCounter(k1);
      c.removeZeroCounts();
      if (c.size()==0) map.remove(k1); // it's empty, get rid of it!
    }
  }

  public void remove(K1 key) {
    Counter<K2> counter = map.get(key);
    if (counter != null) { total -= counter.totalCount(); }
    map.remove(key);
  }

  public void clean() {
    for (K1 key1 : new HashSet<K1>(map.keySet())) {
      Counter<K2> c = map.get(key1);
      for (K2 key2 : new HashSet<K2>(c.keySet())) {
        if (SloppyMath.isCloseTo(0.0, c.getCount(key2))) {
          c.remove(key2);
        }
      }
      if (c.keySet().isEmpty()) {
        map.remove(key1);
      }
    }
  }
  
  public MapFactory<K1,Counter<K2>> getOuterMapFactory() {
    return outerMF;
  }
  
  public MapFactory<K2,MutableDouble> getInnerMapFactory() {
    return innerMF;
  }

  public TwoDimensionalCounter() {
    this(MapFactory.HASH_MAP_FACTORY, MapFactory.HASH_MAP_FACTORY);
  }

  public TwoDimensionalCounter(MapFactory<K1,Counter<K2>> outerFactory, MapFactory<K2,MutableDouble> innerFactory) {
    innerMF = innerFactory;
    outerMF = outerFactory;
    map = outerFactory.newMap();
    total = 0.0;
  }

  public static void main(String[] args) {
    TwoDimensionalCounter<String,String> cc = new TwoDimensionalCounter<String,String>();
    cc.setCount("a", "c", 1.0);
    cc.setCount("b", "c", 1.0);
    cc.setCount("a", "d", 1.0);
    cc.setCount("a", "d", -1.0);
    cc.setCount("b", "d", 1.0);
    System.out.println(cc);
    cc.incrementCount("b", "d", 1.0);
    System.out.println(cc);
    TwoDimensionalCounter<String,String> cc2 = TwoDimensionalCounter.reverseIndexOrder(cc);
    System.out.println(cc2);
  }

}
