package edu.stanford.nlp.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Filters contains some simple implementations of the Filter interface.
 *
 * @author Christopher Manning
 * @version 1.0
 */
public class Filters {

  /**
   * Nothing to instantiate
   */
  private Filters() {
  }

  /**
   * The acceptFilter accepts everything.
   */
  public static <T> Filter<T> acceptFilter() {
    return new CategoricalFilter<T>(true);
  }

  /**
   * The rejectFilter accepts nothing.
   */
  public static <T> Filter<T> rejectFilter() {
    return new CategoricalFilter<T>(false);
  }

  private static final class CategoricalFilter<T> implements Filter<T> {

    private final boolean judgment;

    private CategoricalFilter(boolean judgment) {
      this.judgment = judgment;
    }

    /**
     * Checks if the given object passes the filter.
     *
     * @param obj an object to test
     */
    public boolean accept(T obj) {
      return judgment;
    }
  }


  /**
   * The collectionAcceptFilter accepts a certain collection.
   */
  public static <E> Filter<E> collectionAcceptFilter(E[] objs) {
    return new CollectionAcceptFilter<E>(Arrays.asList(objs), true);
  }

  /**
   * The collectionAcceptFilter accepts a certain collection.
   */
  public static <E> Filter<E> collectionAcceptFilter(Collection<E> objs) {
    return new CollectionAcceptFilter<E>(objs, true);
  }

  /**
   * The collectionRejectFilter rejects a certain collection.
   */
  public static <E> Filter collectionRejectFilter(E[] objs) {
    return new CollectionAcceptFilter<E>(Arrays.asList(objs), false);
  }

  /**
   * The collectionRejectFilter rejects a certain collection.
   */
  public static <E> Filter<E> collectionRejectFilter(Collection<E> objs) {
    return new CollectionAcceptFilter<E>(objs, false);
  }

  private static final class CollectionAcceptFilter<E> implements Filter<E>, Serializable {

    private final Collection<E> args;
    private final boolean judgment;

    private CollectionAcceptFilter(Collection<E> c, boolean judgment) {
      this.args = new HashSet<E>(c);
      this.judgment = judgment;
    }

    /**
     * Checks if the given object passes the filter.
     *
     * @param obj an object to test
     */
    public boolean accept(E obj) {
      if (args.contains(obj)) {
        return judgment;
      } else {
        return !judgment;
      }
    }

    private static final long serialVersionUID = -8870550963937943540L;

  } // end class CollectionAcceptFilter

  /**
   * Filter that accepts only when both filters accept (AND).
   */
  public static <E> Filter andFilter(Filter<E> f1, Filter<E> f2) {
    return (new CombinedFilter<E>(f1, f2, true));
  }

  /**
   * Filter that accepts when either filter accepts (OR).
   */
  public static <E> Filter orFilter(Filter<E> f1, Filter<E> f2) {
    return (new CombinedFilter<E>(f1, f2, false));
  }

  /**
   * Conjunction or disjunction of two filters.
   */
  private static class CombinedFilter<E> implements Filter<E> {
    private Filter<E> f1, f2;
    private boolean conjunction; // and vs. or

    public CombinedFilter(Filter<E> f1, Filter<E> f2, boolean conjunction) {
      this.f1 = f1;
      this.f2 = f2;
      this.conjunction = conjunction;
    }

    public boolean accept(E o) {
      if (conjunction) {
        return (f1.accept(o) && f2.accept(o));
      }
      return (f1.accept(o) || f2.accept(o));
    }
  }

  /**
   * Filter that does the opposite of given filter (NOT).
   */
  public static <E> Filter<E> notFilter(Filter<E> filter) {
    return (new NegatedFilter<E>(filter));
  }

  /**
   * Filter that's either negated or normal as specified.
   */
  public static <E> Filter<E> switchedFilter(Filter<E> filter, boolean negated) {
    return (new NegatedFilter<E>(filter, negated));
  }

  /**
   * Negation of a filter.
   */
  private static class NegatedFilter<E> implements Filter<E> {
    private Filter<E> filter;
    private boolean negated;

    public NegatedFilter(Filter<E> filter, boolean negated) {
      this.filter = filter;
      this.negated = negated;
    }

    public NegatedFilter(Filter<E> filter) {
      this(filter, true);
    }

    public boolean accept(E o) {
      return (negated ^ filter.accept(o)); // xor
    }
  }

  /**
   * Applies the given filter to each of the given elems, and returns the
   * list of elems that were accepted. The runtime type of the returned
   * array is the same as the passed in array.
   */
  public static <E> Object[] filter(E[] elems, Filter<E> filter) {
    List<E> filtered = new ArrayList<E>();
    for (E elem: elems) {
      if (filter.accept(elem)) {
        filtered.add(elem);
      }
    }
    return (filtered.toArray((Object[]) Array.newInstance(elems.getClass().getComponentType(), filtered.size())));
  }

  /**
   * Removes all elems in the given Collection that aren't accepted by the given Filter.
   */
  public static <E> void retainAll(Collection<E> elems, Filter<E> filter) {
    for (Iterator<E> iter = elems.iterator(); iter.hasNext();) {
      E elem = iter.next();
      if ( ! filter.accept(elem)) {
        iter.remove();
      }
    }
  }
}
