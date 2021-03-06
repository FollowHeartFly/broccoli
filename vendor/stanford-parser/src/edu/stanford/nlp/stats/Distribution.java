package edu.stanford.nlp.stats;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import edu.stanford.nlp.util.MutableDouble;

/**
 * Immutable class for representing normalized, smoothed discrete distributions
 * from {@link Counters}. Smoothed counters reserve probability mass for unseen
 * items, so queries for the probability of unseen items will return a small
 * positive amount.  {@link #totalCount} should always return 1.
 * <p/>
 * Counter passed in constructors is copied.
 *
 * @author Galen Andrew (galand@cs.stanford.edu)
 */
public class Distribution<E> implements Serializable, Sampler<E> {

  static final long serialVersionUID = 6707148234288637809L;

  private int numberOfKeys;
  private double reservedMass;
  protected Counter<E> counter;
  private static final int NUM_ENTRIES_IN_STRING = 20;

  private static boolean verbose = false;

  public Counter<E> getCounter() {
    return counter;
  }


  /**
   * Exactly the same as sampleFrom(), needed for the Sampler interface.
   */
  public E drawSample() {
    return sampleFrom();
  }

  
  public String toString(NumberFormat nf) {
    return counter.toString(nf);
  }

  public double getReservedMass() {
    return reservedMass;
  }

  public int getNumberOfKeys() {
    return numberOfKeys;
  }

  //--- cdm added Jan 2004 to help old code compile

  public Set<E> keySet() {
    return counter.keySet();
  }

  public boolean containsKey(E key) {
    return counter.containsKey(key);
  }

  /**
   * Returns the current count for the given key, which is 0 if it hasn't
   * been
   * seen before. This is a convenient version of <code>get</code> that casts
   * and extracts the primitive value.
   */
  public double getCount(E key) {
    return counter.getCount(key);
  }

  //---- end cdm added

  //--- JM added for Distributions

  /**
   * Assuming that c has a total count < 1, returns a new Distribution using the counts in c as probabilities.
   * If c has a total count > 1, returns a normalized distribution with no remaining mass.
   */
  public static <E> Distribution<E> getDistributionFromPartiallySpecifiedCounter(Counter<E> c, int numKeys){
    Distribution<E> d;
    double total = c.totalDoubleCount();
    if (total >= 1.0){
      d = getDistribution(c);
      d.numberOfKeys = numKeys;
    } else {
      d = new Distribution<E>();
      d.numberOfKeys = numKeys;  
      d.counter = c;
      d.reservedMass = 1.0 - total;
    }
    return d;
  }
  //--- end JM added

  
  /**
   * @param s a Set of keys.
   * @return
   */
  public static <E> Distribution<E> getUniformDistribution(Set<E> s) {
    Distribution<E> norm = new Distribution<E>();
    norm.counter = new Counter<E>();
    norm.numberOfKeys = s.size();
    norm.reservedMass = 0;
    double total = (double) s.size();
    double count = 1.0 / total;
    for (E key : s) {
      norm.counter.setCount(key, count);
    }
    return norm;
  }

  /**
   * @param s a Set of keys.
   * @return
   */
  public static <E> Distribution<E> getPerturbedUniformDistribution(Set<E> s, Random r) {
    Distribution<E> norm = new Distribution<E>();
    norm.counter = new Counter<E>();
    norm.numberOfKeys = s.size();
    norm.reservedMass = 0;
    double total = (double) s.size();
    double prob = 1.0 / total;
    double stdev = prob / 1000.0;
    for (E key : s) {
      norm.counter.setCount(key, prob + (r.nextGaussian() * stdev));
    }
    return norm;
  }

  public static <E> Distribution<E> getPerturbedDistribution(GenericCounter<E> wordCounter, Random r) {
    Distribution<E> norm = new Distribution<E>();
    norm.counter = new Counter<E>();
    norm.numberOfKeys = wordCounter.size();
    norm.reservedMass = 0;
    double totalCount = wordCounter.totalDoubleCount();
    double stdev = 1.0 / (double) norm.numberOfKeys / 1000.0; // tiny relative to average value
    for (E key : wordCounter.keySet()) {
      double prob = wordCounter.getCount(key) / totalCount;
      double perturbedProb = prob + (r.nextGaussian() * stdev);
      if (perturbedProb < 0.0) {
        perturbedProb = 0.0;
      }
      norm.counter.setCount(key, perturbedProb);
    }
    return norm;
  }

  /**
   * Creates a Distribution from the given counter, ie makes an internal
   * copy of the counter and divides all counts by the total count.
   *
   * @param counter
   * @return a new Distribution
   */
  public static <E> Distribution<E> getDistribution(GenericCounter<E> counter) {
    return getDistributionWithReservedMass(counter, 0.0);
  }

  public static <E> Distribution<E> getDistributionWithReservedMass(GenericCounter<E> counter, double reservedMass) {
    Distribution<E> norm = new Distribution<E>();
    norm.counter = new Counter<E>();
    norm.numberOfKeys = counter.size();
    norm.reservedMass = reservedMass;
    double total = counter.totalDoubleCount() * (1 + reservedMass);
    if (total == 0.0) {
      total = 1.0;
    }
    for (E key : counter.keySet()) {
      double count = counter.getCount(key) / total;
      //      if (Double.isNaN(count) || count < 0.0 || count> 1.0 ) throw new RuntimeException("count=" + counter.getCount(key) + " total=" + total);
      norm.counter.setCount(key, count);
    }
    return norm;
  }

  /**
   * Creates a Distribution from the given counter, ie makes an internal
   * copy of the counter and divides all counts by the total count.
   *
   * @param counter
   * @return a new Distribution
   */
  public static <E> Distribution<E> getDistributionFromLogValues(GenericCounter<E> counter) {
    Counter<E> c = new Counter<E>();
    // go through once to get the max
    // shift all by max so as to minimize the possibility of underflow
    double max = counter.doubleMax();
    for (E key : counter.keySet()) {
      double count = Math.exp(counter.getCount(key) - max);
      c.setCount(key, count);
    }
    return getDistribution(c);
  }

  public static <E> Distribution<E> absolutelyDiscountedDistribution(GenericCounter<E> counter, int numberOfKeys, double discount) {
    Distribution<E> norm = new Distribution<E>();
    norm.counter = new Counter<E>();
    double total = counter.totalDoubleCount();
    double reservedMass = 0.0;
    for (E key : counter.keySet()) {
      double count = counter.getCount(key);
      if (count > discount) {
        double newCount = (count - discount) / total;
        norm.counter.setCount(key, newCount); // a positive count left over
        //        System.out.println("seen: " + newCount);
        reservedMass += discount;
      } else { // count <= discount
        reservedMass += count;
        // if the count <= discount, don't put key in counter, and we treat it as unseen!!
      }
    }
    norm.numberOfKeys = numberOfKeys;
    norm.reservedMass = reservedMass / total;
    if (verbose) {
      System.err.println("unseenKeys=" + (norm.numberOfKeys - norm.counter.size()) + " seenKeys=" + norm.counter.size() + " reservedMass=" + norm.reservedMass);
      double zeroCountProb = (norm.reservedMass / (numberOfKeys - norm.counter.size()));
      System.err.println("0 count prob: " + zeroCountProb);
      if (discount >= 1.0) {
        System.err.println("1 count prob: " + zeroCountProb);
      } else {
        System.err.println("1 count prob: " + (1.0 - discount) / total);
      }
      if (discount >= 2.0) {
        System.err.println("2 count prob: " + zeroCountProb);
      } else {
        System.err.println("2 count prob: " + (2.0 - discount) / total);
      }
      if (discount >= 3.0) {
        System.err.println("3 count prob: " + zeroCountProb);
      } else {
        System.err.println("3 count prob: " + (3.0 - discount) / total);
      }
    }
    //    System.out.println("UNSEEN: " + reservedMass / total / (numberOfKeys - counter.size()));
    return norm;
  }

  /**
   * Creates an Laplace smoothed Distribution from the given counter, ie adds one count
   * to every item, including unseen ones, and divides by the total count.
   *
   * @param counter
   * @param numberOfKeys
   * @return a new add-1 smoothed Distribution
   */
  public static <E> Distribution<E> laplaceSmoothedDistribution(GenericCounter<E> counter, int numberOfKeys) {
    return laplaceSmoothedDistribution(counter, numberOfKeys, 1.0);
  }

  /**
   * Creates a smoothed Distribution using Lidstone's law, ie adds lambda (typically
   * between 0 and 1) to every item, including unseen ones, and divides by the total count.
   *
   * @param counter
   * @param numberOfKeys
   * @param lambda
   * @return a new Lidstone smoothed Distribution
   */
  public static <E> Distribution<E> laplaceSmoothedDistribution(GenericCounter<E> counter, int numberOfKeys, double lambda) {
    Distribution<E> norm = new Distribution<E>();
    norm.counter = new Counter<E>();
    double total = counter.totalDoubleCount();
    double newTotal = total + (lambda * (double) numberOfKeys);
    double reservedMass = ((double) numberOfKeys - counter.size()) * lambda / newTotal;
    if (verbose) {
      System.err.println(((double) numberOfKeys - counter.size()) + " * " + lambda + " / (" + total + " + ( " + lambda + " * " + (double) numberOfKeys + ") )");
    }
    norm.numberOfKeys = numberOfKeys;
    norm.reservedMass = reservedMass;
    if (verbose) {
      System.err.println("reserved mass=" + reservedMass);
    }
    for (E key : counter.keySet()) {
      double count = counter.getCount(key);
      norm.counter.setCount(key, (count + lambda) / newTotal);
    }
    if (verbose) {
      System.err.println("unseenKeys=" + (norm.numberOfKeys - norm.counter.size()) + " seenKeys=" + norm.counter.size() + " reservedMass=" + norm.reservedMass);
      System.err.println("0 count prob: " + lambda / newTotal);
      System.err.println("1 count prob: " + (1.0 + lambda) / newTotal);
      System.err.println("2 count prob: " + (2.0 + lambda) / newTotal);
      System.err.println("3 count prob: " + (3.0 + lambda) / newTotal);
    }
    return norm;
  }

  /**
   * Creates a smoothed Distribution with Laplace smoothing, but assumes an explicit
   * count of "UNKNOWN" items.  Thus anything not in the original counter will have
   * probability zero.
   *
   * @param counter the counter to normalize
   * @param lambda  the value to add to each count
   * @param UNK     the UNKNOWN symbol
   * @return a new Laplace-smoothed distribution
   */
  public static <E> Distribution<E> laplaceWithExplicitUnknown(GenericCounter<E> counter, double lambda, E UNK) {
    Distribution<E> norm = new Distribution<E>();
    norm.counter = new Counter<E>();
    double total = counter.totalDoubleCount() + (lambda * (counter.size() - 1));
    norm.numberOfKeys = counter.size();
    norm.reservedMass = 0.0;
    for (E key : counter.keySet()) {
      if (key.equals(UNK)) {
        norm.counter.setCount(key, counter.getCount(key) / total);
      } else {
        norm.counter.setCount(key, (counter.getCount(key) + lambda) / total);
      }
    }
    return norm;
  }

  /**
   * Creates a Good-Turing smoothed Distribution from the given counter.
   *
   * @param counter
   * @param numberOfKeys
   * @return a new Good-Turing smoothed Distribution.
   */
  public static <E> Distribution<E> goodTuringSmoothedCounter(GenericCounter<E> counter, int numberOfKeys) {
    // gather count-counts
    int[] countCounts = getCountCounts(counter);

    // if count-counts are unreliable, we shouldn't be using G-T
    // revert to laplace
    for (int i = 1; i <= 10; i++) {
      if (countCounts[i] < 3) {
        return laplaceSmoothedDistribution(counter, numberOfKeys, 0.5);
      }
    }

    double observedMass = counter.totalDoubleCount();
    double reservedMass = countCounts[1] / observedMass;

    // calculate and cache adjusted frequencies
    // also adjusting total mass of observed items
    double[] adjustedFreq = new double[10];
    for (int freq = 1; freq < 10; freq++) {
      adjustedFreq[freq] = (double) (freq + 1) * (double) countCounts[freq + 1] / (double) countCounts[freq];
      observedMass -= ((double) freq - adjustedFreq[freq]) * countCounts[freq];
    }

    double normFactor = (1.0 - reservedMass) / observedMass;

    Distribution<E> norm = new Distribution<E>();
    norm.counter = new Counter<E>();

    // fill in the new Distribution, renormalizing as we go
    for (E key : counter.keySet()) {
      int origFreq = (int) Math.round(counter.getCount(key));
      if (origFreq < 10) {
        norm.counter.setCount(key, adjustedFreq[origFreq] * normFactor);
      } else {
        norm.counter.setCount(key, (double) origFreq * normFactor);
      }
    }

    norm.numberOfKeys = numberOfKeys;
    norm.reservedMass = reservedMass;
    return norm;
  }

  /**
   * Creates a Good-Turing smoothed Distribution from the given counter without
   * creating any reserved mass-- instead, the special object UNK in the counter
   * is assumed to be the count of "UNSEEN" items.  Probability of objects not in
   * original counter will be zero.
   *
   * @param counter the counter
   * @param UNK     the unknown symbol
   * @return a good-turing smoothed distribution
   */
  public static <E> Distribution<E> goodTuringWithExplicitUnknown(GenericCounter<E> counter, E UNK) {
    // gather count-counts
    int[] countCounts = getCountCounts(counter);

    // if count-counts are unreliable, we shouldn't be using G-T
    // revert to laplace
    for (int i = 1; i <= 10; i++) {
      if (countCounts[i] < 3) {
        return laplaceWithExplicitUnknown(counter, 0.5, UNK);
      }
    }

    double observedMass = counter.totalDoubleCount();

    // calculate and cache adjusted frequencies
    // also adjusting total mass of observed items
    double[] adjustedFreq = new double[10];
    for (int freq = 1; freq < 10; freq++) {
      adjustedFreq[freq] = (double) (freq + 1) * (double) countCounts[freq + 1] / (double) countCounts[freq];
      observedMass -= ((double) freq - adjustedFreq[freq]) * countCounts[freq];
    }

    Distribution<E> norm = new Distribution<E>();
    norm.counter = new Counter<E>();

    // fill in the new Distribution, renormalizing as we go
    for (E key : counter.keySet()) {
      int origFreq = (int) Math.round(counter.getCount(key));
      if (origFreq < 10) {
        norm.counter.setCount(key, adjustedFreq[origFreq] / observedMass);
      } else {
        norm.counter.setCount(key, (double) origFreq / observedMass);
      }
    }

    norm.numberOfKeys = counter.size();
    norm.reservedMass = 0.0;
    return norm;

  }

  private static <E> int[] getCountCounts(GenericCounter<E> counter) {
    int[] countCounts = new int[11];
    for (int i = 0; i <= 10; i++) {
      countCounts[i] = 0;
    }
    for (E key : counter.keySet()) {
      int count = (int) Math.round(counter.getCount(key));
      if (count <= 10) {
        countCounts[count]++;
      }
    }
    return countCounts;
  }

  /**
   * Returns a Distribution that uses prior as a Dirichlet prior
   * weighted by weight.  Essentially adds "pseudo-counts" for each Object
   * in prior equal to that Object's mass in prior times weight,
   * then normalizes.
   * <p/>
   * WARNING: If unseen item is encountered in c, total may not be 1.
   * NOTE: This will not work if prior is a DynamicDistribution
   * to fix this, you could add a CounterView to Distribution and use that
   * in the linearCombination call below
   *
   * @param c
   * @param prior
   * @param weight multiplier of prior to get "pseudo-count"
   * @return new Distribution
   */
  public static <E> Distribution<E> distributionWithDirichletPrior(GenericCounter<E> c, Distribution<E> prior, double weight) {
    Distribution<E> norm = new Distribution<E>();
    double totalWeight = c.totalDoubleCount() + weight;
    if (prior instanceof DynamicDistribution) {
      throw new UnsupportedOperationException("Cannot make normalized counter with Dynamic prior.");
    }
    norm.counter = Counters.linearCombination(c, 1 / totalWeight, prior.counter, weight / totalWeight);
    norm.numberOfKeys = prior.numberOfKeys;
    norm.reservedMass = prior.reservedMass * weight / totalWeight;
    //System.out.println("totalCount: " + norm.totalCount());
    return norm;
  }

  /**
   * Like normalizedCounterWithDirichletPrior except probabilities are
   * computed dynamically from the counter and prior instead of all at once up front.
   * The main advantage of this is if you are making many distributions from relatively
   * sparse counters using the same relatively dense prior, the prior is only represented
   * once, for major memory savings.
   *
   * @param c
   * @param prior
   * @param weight multiplier of prior to get "pseudo-count"
   * @return new Distribution
   */
  public static <E> Distribution<E> dynamicCounterWithDirichletPrior(GenericCounter<E> c, Distribution<E> prior, double weight) {
    double totalWeight = c.totalDoubleCount() + weight;
    Distribution<E> norm = new DynamicDistribution<E>(prior, weight / totalWeight);
    norm.counter = new Counter<E>();
    // this might be done more efficiently with entrySet but there isn't a way to get
    // the entrySet from a Counter now.  In most cases c will be small(-ish) anyway
    for (E key : c.keySet()) {
      double count = c.getCount(key) / totalWeight;
      prior.addToKeySet(key);
      norm.counter.setCount(key, count);
    }
    norm.numberOfKeys = prior.numberOfKeys;
    return norm;
  }

  private static class DynamicDistribution<E> extends Distribution<E> {
    private Distribution<E> prior;
    private double priorMultiplier;

    public DynamicDistribution(Distribution<E> prior, double priorMultiplier) {
      super();
      this.prior = prior;
      this.priorMultiplier = priorMultiplier;
    }

    public double probabilityOf(E o) {
      return this.counter.getCount(o) + prior.probabilityOf(o) * priorMultiplier;
    }

    public double totalCount() {
      return this.counter.totalCount() + prior.totalCount() * priorMultiplier;
    }

    public Set<E> keySet() {
      return prior.keySet();
    }

    public void addToKeySet(E o) {
      prior.addToKeySet(o);
    }

    public boolean containsKey(E key) {
      return prior.containsKey(key);
    }

    public Object argMax() {
      return Counters.linearCombination(this.counter, 1.0, prior.counter, priorMultiplier).argmax();
    }
    
    public E sampleFrom() {
      double d = Math.random();
      Set<E> s = prior.keySet();
      for (E o : s) {
        d -= probabilityOf(o);
        if (d < 0) {
          return o;
        }
      }
      System.err.println("ERROR: Distribution sums to less than 1");
      System.err.println("Sampled " + d + "      sum is " + totalCount());
      throw new RuntimeException("");
    }
  };

  /**
   * Maps a counter representing the linear weights of a multiclass
   * logistic regression model to the probabilities of each class.
   */
  public static <E> Distribution<E> distributionFromLogisticCounter(GenericCounter<E> cntr) {
    double expSum = 0.0;
    int numKeys = 0;
    for (E key : cntr.keySet()) {
      expSum += Math.exp(cntr.getCount(key));
      numKeys++;
    }
    Distribution<E> probs = new Distribution<E>();
    probs.counter = new Counter<E>();
    probs.reservedMass = 0.0;
    probs.numberOfKeys = numKeys;
    for (E key : cntr.keySet()) {
      probs.counter.setCount(key, Math.exp(cntr.getCount(key)) / expSum);
    }
    return probs;
  }

  /**
   * Returns an object sampled from the distribution.
   * There may be a faster way to do this if you need to...
   *
   * @return a sampled object
   */
  public E sampleFrom() {
    double d = Math.random();
    for (Iterator<Map.Entry<E,MutableDouble>> entryIter = counter.entrySet().iterator(); entryIter.hasNext();) {
      Map.Entry<E,MutableDouble> entry = entryIter.next();
      d -= entry.getValue().doubleValue();
      if (d < 0) {
        return entry.getKey();
      }
    }
    throw new RuntimeException("ERROR: Distribution sums to more than 1");
  }

  /**
   * Returns the normalized count of the given object.
   *
   * @param key
   * @return the normalized count of the object
   */
  public double probabilityOf(E key) {
    if (counter.containsKey(key)) {
      return counter.getCount(key);
    } else {
      int remainingKeys = numberOfKeys - counter.size();
      if (remainingKeys <= 0) {
        return 0.0;
      } else {
        return (reservedMass / remainingKeys);
      }
    }
  }

  public E argmax() {
    return counter.argmax();
  }

  public double totalCount() {
    return counter.totalCount() + reservedMass;
  }

  /**
   * Insures that object is in keyset (with possibly zero value)
   *
   * @param o object to put in keyset
   */
  public void addToKeySet(E o) {
    if (!counter.containsKey(o)) {
      counter.setCount(o, 0);
    }
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Distribution)) {
      return false;
    }
    return equals((Distribution) o);
  }

  public boolean equals(Distribution distribution) {
    if (numberOfKeys != distribution.numberOfKeys) {
      return false;
    }
    if (reservedMass != distribution.reservedMass) {
      return false;
    }
    if (!counter.equals(distribution.counter)) {
      return false;
    }

    return true;
  }

  public int hashCode() {
    int result = numberOfKeys;
    long temp = Double.doubleToLongBits(reservedMass);
    result = 29 * result + (int) (temp ^ (temp >>> 32));
    result = 29 * result + counter.hashCode();
    return result;
  }

  // no public constructor; use static methods instead
  private Distribution() {
  }

  public String toString() {
    NumberFormat nf = new DecimalFormat("0.0##E0");
    List<E> keyList = new ArrayList<E>(keySet());
    Collections.sort(keyList, new Comparator<E>() {
      public int compare(E o1, E o2) {
        if (probabilityOf(o1) < probabilityOf(o2)) {
          return 1;
        } else {
          return -1;
        }
      }
    });
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    for (int i = 0; i < NUM_ENTRIES_IN_STRING; i++) {
      if (keyList.size() <= i) {
        break;
      }
      E o = keyList.get(i);
      double prob = probabilityOf(o);
      sb.append(o + ":" + nf.format(prob) + " ");
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * For internal testing purposes only.
   */
  public static void main(String[] args) {
    Counter<String> c = new Counter<String>();

    final double p = 1000;

    String UNK = "!*UNKNOWN*!";
    Set<String> s = new HashSet<String>();
    s.add(UNK);

    // fill counter with roughly Zipfian distribution
    for (int rank = 1; rank < 2000; rank++) {
      String i = String.valueOf(rank);
      c.setCount(i, Math.round(p / (double) rank));
      s.add(i);
    }

    for (int rank = 2000; rank <= 4000; rank++) {
      String i = String.valueOf(rank);
      s.add(i);
    }

    Distribution<String> n = getDistribution(c);
    Distribution<String> prior = getUniformDistribution(s);
    Distribution<String> dir1 = distributionWithDirichletPrior(c, prior, 4000);
    Distribution<String> dir2 = dynamicCounterWithDirichletPrior(c, prior, 4000);
    Distribution<String> add1;
    Distribution<String> gt;
    if (false) {
      add1 = laplaceSmoothedDistribution(c, 4000);
      gt = goodTuringSmoothedCounter(c, 4000);
    } else {
      c.setCount(UNK, 45);
      add1 = laplaceWithExplicitUnknown(c, 0.5, UNK);
      gt = goodTuringWithExplicitUnknown(c, UNK);
    }


    System.out.println("Freq    Norm                  Add1                  Dir1                  Dir2                  G-T");
    for (int i = 1; i < 5; i++) {
      System.out.print(Math.round(p / (double) i));
      System.out.print("   ");
      String in = String.valueOf(i);
      System.out.print(n.probabilityOf(String.valueOf(in)));
      System.out.print("  ");
      System.out.print(add1.probabilityOf(in));
      System.out.print("  ");
      System.out.print(dir1.probabilityOf(in));
      System.out.print("  ");
      System.out.print(dir2.probabilityOf(in));
      System.out.print("  ");
      System.out.print(gt.probabilityOf(in));
      System.out.println("  ");
    }
    System.out.println();
    System.out.println("--------------------------------------------");
    System.out.println();
    System.out.print(0);
    System.out.print("   ");
    System.out.print(n.probabilityOf(UNK));
    System.out.print("  ");
    System.out.print(add1.probabilityOf(UNK));
    System.out.print("  ");
    System.out.print(dir1.probabilityOf(UNK));
    System.out.print("  ");
    System.out.print(dir2.probabilityOf(UNK));
    System.out.print("  ");
    System.out.print(gt.probabilityOf(UNK));
    System.out.println();
    System.out.println("--------------------------------------------");
    System.out.println();
    System.out.print(1);
    System.out.print("   ");
    String last = String.valueOf(1500);
    System.out.print(n.probabilityOf(last));
    System.out.print("  ");
    System.out.print(add1.probabilityOf(last));
    System.out.print("  ");
    System.out.print(dir1.probabilityOf(last));
    System.out.print("  ");
    System.out.print(dir2.probabilityOf(last));
    System.out.print("  ");
    System.out.print(gt.probabilityOf(last));
    System.out.println();

    System.out.println("Totals:");

    System.out.print(n.totalCount());
    System.out.print("  ");
    System.out.print(add1.totalCount());
    System.out.print("  ");
    System.out.print(dir1.totalCount());
    System.out.print("  ");
    System.out.print(dir2.totalCount());
    System.out.print("  ");
    System.out.print(gt.totalCount());
    System.out.println();

  }

}
