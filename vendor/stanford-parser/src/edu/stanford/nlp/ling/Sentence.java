package edu.stanford.nlp.ling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Sentence holds a single sentence, and
 * mediates between word numbers and words.
 * A sentence may contain a list of <code>Word</code>, or of a subtype,
 * such as <code>TaggedWord</code>.  A Sentence is just a slightly
 * glorified <code>ArrayList</code>.
 *
 * @author Dan Klein
 * @author Christopher Manning (generified)
 * @version 2007
 */
public class Sentence<T extends HasWord> extends ArrayList<T> {

  /**
   * Constructs an empty sentence.
   */
  public Sentence() {
    super();
  }


  /**
   * Constructs a sentence from the input Collection.
   *
   * @param w A Collection (interpreted as ordered) to make the sentence
   *          out of.
   */
  public Sentence(Collection<T> w) {
    super(w);
  }


  /**
   * Create a Sentence as a list of <code>TaggedWord</code> from two
   * lists of <code>String</code>, one for the words, and the second for
   * the tags.
   *
   * @param lex  a list whose items are of type <code>String</code> and
   *             are the words
   * @param tags a list whose items are of type <code>String</code> and
   *             are the tags
   * @return The Sentence
   */
  public static Sentence<TaggedWord> toSentence(List<String> lex, List<String> tags) {
    Sentence<TaggedWord> sent = new Sentence<TaggedWord>();
    int ls = lex.size();
    int ts = tags.size();
    if (ls != ts) {
      throw new IllegalArgumentException("Sentence.toSentence: lengths differ");
    }
    for (int i = 0; i < ls; i++) {
      sent.add(new TaggedWord(lex.get(i), tags.get(i)));
    }
    return sent;
  }

  /**
   * Create a Sentence as a list of <code>Word</code> objects from
   * an array of String objects.
   *
   * @param words  The words to make it from
   * @return The Sentence
   */
  public static Sentence<Word> toSentence(String... words) {
    Sentence<Word> sent = new Sentence<Word>();
    for (String str : words) {
      sent.add(new Word(str));
    }
    return sent;
  }

  /**
   * Set the Sentence to this Collection of words.
   *
   * @param wordList A collection of words (interpreted as ordered)
   */
  public void setWords(Collection<T> wordList) {
    clear();
    addAll(wordList);
  }


  /**
   * Return the Word at the given index.  Does type casting.
   * TODO: should be deleted in generic world.  Just use get().
   *
   * @param index The index to use
   * @return The word at this index
   */
  public T getHasWord(int index) {
    return get(index);
  }


  /**
   * A convenience method since we normally say sentences have a length.
   * Same as <code>size()</code>.
   *
   * @return the length of the sentence
   */
  public int length() {
    return size();
  }


  /**
   * Returns the sentence as a string with a space between words.
   * It strictly prints out the <code>value()</code> of each item -
   * this will give the expected answer for a shortform representation
   * of the "sentence" over a range of cases.  It is equivalent to
   * calling <code>toString(true)</code>
   *
   * @return The sentence
   */
  public String toString() {
    return toString(true);
  }


  /**
   * Returns the sentence as a string with a space between words.
   * Designed to work robustly, even if the elements stored in the
   * 'Sentence' are not of type Label.
   *
   * @param justValue If <code>true</code> and the elements are of type
   *                  <code>Label</code>, return just the
   *                  <code>value()</code> of the <code>Label</code> of each word;
   *                  otherwise,
   *                  call the <code>toString()</code> method on each item.
   * @return The sentence in String form
   */
  public String toString(final boolean justValue) {
    StringBuilder s = new StringBuilder();
    for (Iterator wordIterator = iterator(); wordIterator.hasNext();) {
      Object o = wordIterator.next();
      if (justValue && o instanceof Label) {
        s.append(((Label) o).value());
      } else {
        s.append(o.toString());
      }
      if (wordIterator.hasNext()) {
        s.append(" ");
      }
    }
    return s.toString();
  }

}
