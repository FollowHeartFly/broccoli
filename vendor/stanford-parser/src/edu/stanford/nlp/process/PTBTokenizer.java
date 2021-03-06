package edu.stanford.nlp.process;


import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.ling.FeatureLabel;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.util.StringUtils;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Tokenizer implementation that conforms to the Penn Treebank tokenization
 * conventions.
 * This tokenizer is a Java implementation of Professor Chris Manning's Flex
 * tokenizer, pgtt-treebank.l.  It reads raw text and outputs
 * tokens as edu.stanford.nlp.trees.Words in the Penn treebank format. It can
 * optionally return carriage returns as tokens.
 *
 * @author Tim Grow
 * @author Teg Grenager (grenager@stanford.edu)
 * @author Christopher Manning
 * @author Jenny Finkel (integrating in invertible PTB tokenizer)
 */
public class PTBTokenizer<T> extends AbstractTokenizer<T> {

  // whether carriage returns should be returned as tokens
  private boolean tokenizeCRs;
  private boolean invertible;
  private boolean suppressEscaping; // = false;

  // the underlying lexer
  private PTBLexer lexer;
  private LexedTokenFactory<T> tokenFactory;
  // private int position;

  /**
   * Constructs a new PTBTokenizer that treats carriage returns as normal whitespace.
   */
  public static PTBTokenizer<Word> newPTBTokenizer(Reader r) {
    return newPTBTokenizer(r, false);
  }

  /**
   * Constructs a new PTBTokenizer that optionally returns carriage returns
   * as their own token. CRs come back as Words whose text is
   * the value of <code>PTBLexer.cr</code>.
   */
  public static PTBTokenizer<Word> newPTBTokenizer(Reader r, boolean tokenizeCRs) {
    return new PTBTokenizer<Word>(r, tokenizeCRs, new WordTokenFactory());
  }


  /**
   * Constructs a new PTBTokenizer that optionally returns carriage returns
   * as their own token. CRs come back as Words whose text is
   * the value of <code>PTBLexer.cr</code>.
   *
   * @param invertible if set to true, then will produce FeatureLabels which
   * will have fields for the string before and after, and the character offsets
   */
  public static PTBTokenizer<FeatureLabel> newPTBTokenizer(Reader r, boolean tokenizeCRs, boolean invertible) {
    return new PTBTokenizer<FeatureLabel>(r, tokenizeCRs, invertible, new FeatureLabelTokenFactory());
  }


  /**
   * Constructs a new PTBTokenizer that optionally returns carriage returns
   * as their own token, and has a custom LexedTokenFactory.
   * CRs come back as Words whose text is
   * the value of <code>PTBLexer.cr</code>.
   *
   * @param tokenFactory The LexedTokenFactory to use to create
   *  tokens from the text.
   */
  public PTBTokenizer(Reader r, boolean tokenizeCRs,
      LexedTokenFactory<T> tokenFactory)
  {
    this (r, tokenizeCRs, false, tokenFactory);
  }

  private PTBTokenizer(Reader r, boolean tokenizeCRs, boolean invertible,
                       LexedTokenFactory<T> tokenFactory)
  {
    this(r, tokenizeCRs, invertible, false, tokenFactory);
  }

  private PTBTokenizer(Reader r, boolean tokenizeCRs, boolean invertible,
                       boolean suppressEscaping,
                       LexedTokenFactory<T> tokenFactory)
  {
    this.tokenizeCRs = tokenizeCRs;
    this.tokenFactory = tokenFactory;
    this.invertible = invertible;
    this.suppressEscaping = suppressEscaping;
    setSource(r);
  }


  /**
   * Internally fetches the next token.
   *
   * @return the next token in the token stream, or null if none exists.
   */
  protected T getNext() {
    if (lexer == null) {
      return null;
    }
    T token = null;
    try {
      token = (T)lexer.next();
      // get rid of CRs if necessary
      while (!tokenizeCRs && PTBLexer.cr.equals(((HasWord) token).word())) {
        token = (T)lexer.next();
      }
    } catch (Exception e) {
      nextToken = null;
      // do nothing, return null
    }
    return token;
  }

  /**
   * Sets the source of this Tokenizer to be the Reader r.
   */
  public void setSource(Reader r) {
    if (invertible) {
      lexer = new PTBLexer(r, invertible, tokenizeCRs);
    } else {
      lexer = new PTBLexer(r, tokenFactory, tokenizeCRs, suppressEscaping);
    }
    // position = 0;
  }

  /**
   * Returns a presentable version of the given PTB-tokenized text.
   * PTB tokenization splits up punctuation and does various other things
   * that makes simply joining the tokens with spaces look bad. So join
   * the tokens with space and run it through this method to produce nice
   * looking text. It's not perfect, but it works pretty well.
   */
  public static String ptb2Text(String ptbText) {
    StringBuilder sb = new StringBuilder(ptbText.length()); // probably an overestimate
    PTB2TextLexer lexer = new PTB2TextLexer(new StringReader(ptbText));
    try {
      for (String token; (token = lexer.next()) != null; ) {
        sb.append(token);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return (sb.toString());
  }

  /**
   * Returns a presentable version of the given PTB-tokenized words.
   * Pass in a List of Words or Strings, or a Document and this method will
   * join the words with spaces and call {@link #ptb2Text(String) } on the
   * output. This method will check if the elements in the list are subtypes
   * of Word, and if so, it will take the word() values to prevent additional
   * text from creeping in (e.g., POS tags). Otherwise the toString value will
   * be used.
   */
  public static String ptb2Text(List ptbWords) {
    for (int i = 0, sz = ptbWords.size(); i < sz; i++) {
      if (ptbWords.get(i) instanceof Word) {
        ptbWords.set(i, ((Word) ptbWords.get(i)).word());
      }
    }

    return (ptb2Text(StringUtils.join(ptbWords)));
  }

  public static TokenizerFactory<Word> factory() {
    return PTBTokenizerFactory.newPTBTokenizerFactory();
  }

  public static TokenizerFactory<Word> factory(boolean tokenizeCRs) {
    return PTBTokenizerFactory.newPTBTokenizerFactory(tokenizeCRs);
  }


  public static <T>TokenizerFactory<T> factory(boolean tokenizeCRs, LexedTokenFactory<T> factory) {
    return new PTBTokenizerFactory<T>(tokenizeCRs, factory);
  }

  public static TokenizerFactory<FeatureLabel> factory(boolean tokenizeCRs, boolean invertible) {
    return PTBTokenizerFactory.newPTBTokenizerFactory(tokenizeCRs, invertible);
  }

  public static TokenizerFactory<Word> factory(boolean tokenizeCRs, boolean invertible, boolean suppressEscaping) {
    return PTBTokenizerFactory.newPTBTokenizerFactory(tokenizeCRs, invertible, suppressEscaping);
  }


  public static class PTBTokenizerFactory<T> implements TokenizerFactory<T> {

    protected boolean tokenizeCRs;
    protected boolean invertible;
    protected boolean suppressEscaping=false;
    protected LexedTokenFactory<T> factory;

    /**
     * Constructs a new PTBTokenizerFactory that treats carriage returns as
     * normal whitespace.
     */
    public static PTBTokenizerFactory<Word> newPTBTokenizerFactory() {
      return newPTBTokenizerFactory(false);
    }

    /**
     * Constructs a new PTBTokenizer that optionally returns carriage returns
     * as their own token. CRs come back as Words whose text is
     * the value of <code>PTBLexer.cr</code>.
     */
    public static PTBTokenizerFactory<Word> newPTBTokenizerFactory(boolean tokenizeCRs) {
      return new PTBTokenizerFactory<Word>(tokenizeCRs, new WordTokenFactory());
    }

    public PTBTokenizerFactory(boolean tokenizeCRs, LexedTokenFactory<T> factory) {
      this(tokenizeCRs, false, false, factory);
    }

    public static PTBTokenizerFactory<FeatureLabel> newPTBTokenizerFactory(boolean tokenizeCRs, boolean invertible) {
      return new PTBTokenizerFactory<FeatureLabel>(tokenizeCRs, invertible, new FeatureLabelTokenFactory());
    }

    // I'm not sure what will happen
    // if you set both intertible and suppressEscaping to true.
    // -pichuan (Wed Jan 31 23:12:04 2007)
    public static PTBTokenizerFactory<Word> newPTBTokenizerFactory(boolean tokenizeCRs, boolean invertible, boolean suppressEscaping) {
      return new PTBTokenizerFactory<Word>(tokenizeCRs, invertible, suppressEscaping, new WordTokenFactory());
    }

    private PTBTokenizerFactory(boolean tokenizeCRs, boolean invertible, LexedTokenFactory<T> factory) {
      this(tokenizeCRs, invertible, false, factory);
    }

    private PTBTokenizerFactory(boolean tokenizeCRs, boolean invertible, boolean suppressEscaping, LexedTokenFactory<T> factory) {
      this.tokenizeCRs = tokenizeCRs;
      this.invertible = invertible;
      this.suppressEscaping = suppressEscaping;
      this.factory = factory;
    }


    public Iterator<T> getIterator(Reader r) {
      return getTokenizer(r);
    }

    public Tokenizer<T> getTokenizer(Reader r) {
      return new PTBTokenizer<T>(r, tokenizeCRs, invertible, suppressEscaping, factory);
    }

  } // end static class PTBTokenizerFactory

  /**
   * Reads files named as arguments and print their tokens one per line.
   * This is mainly as a testing aid, but it can also be quite useful
   * standalone to turn a corpus into a one-token-per-line file of tokens.
   * This main method assumes that the input file is in utf-8 encoding,
   * unless it is specified.
   * <p/>
   * Usage: <code>java edu.stanford.nlp.process.PTBTokenizer [-charset charset] [-nl] filename+
   * </code>
   * <p/>
   * Options:
   * <ul>
   * <li>
   * -nl means to tokenize newlines; -preserveLines means to do space-separated
   * tokens, except when the original had a line break, not one-token-per-line;
   * <li>
   * -charset specifies a character encoding; -parseInside names an XML-style
   * element to look inside for tokens (regex matching, not an XML parser);
   * <li>
   * -ioFileList means remaining command-line arguments are files that
   * themselves contain lists of pairs of input-output filenames (2 column,
   * whitespace separated).
   * </ul>
   *
   * @param args Command line arguments
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("usage: java edu.stanford.nlp.process.PTBTokenizer [options]* filename+");
      System.err.println("  options: -nl|-preserveLines|-dump|-ioFileList|-charset|-parseInside");
      return;
    }
    int i = 0;
    String charset = "utf-8";
    Pattern parseInsideBegin = null;
    Pattern parseInsideEnd = null;
    boolean tokenizeNL = false;
    boolean preserveLines = false;
    boolean inputOutputFileList = false;
    boolean dump = false;

    while (args[i].charAt(0) == '-') {
      if ("-nl".equals(args[i])) {
        tokenizeNL = true;
      } else if ("-preserveLines".equals(args[i])) {
        preserveLines = true;
        tokenizeNL = true;
      } else if ("-dump".equals(args[i])) {
        dump = true;
      } else if ("-ioFileList".equals(args[i])) {
        inputOutputFileList = true;
      } else if ("-charset".equals(args[i]) && i < args.length - 1) {
        i++;
        charset = args[i];
      } else if ("-parseInside".equals(args[i]) && i < args.length - 1) {
        i++;
        try {
          parseInsideBegin = Pattern.compile("<(?:" + args[i] + ")>");
          parseInsideEnd = Pattern.compile("</(?:" + args[i] + ")>");
        } catch (Exception e) {
          parseInsideBegin = null;
          parseInsideEnd = null;
        }
      } else {
        System.err.println("Unknown option: " + args[i]);
      }
      i++;
    }
    ArrayList<String> inputFileList = new ArrayList<String>();
    ArrayList<String> outputFileList=null;

    if (inputOutputFileList) {
      outputFileList = new ArrayList<String>();
      for (int j = i; j < args.length; j++) {
        BufferedReader r = new BufferedReader(
          new InputStreamReader(new FileInputStream(args[j]), charset));
        for (String inLine; (inLine = r.readLine()) != null; ) {
          String[] fields = inLine.split("\\s+");
          inputFileList.add(fields[0]);
          outputFileList.add(fields[1]);
        }
        r.close();
      }
    } else {
      for (int j = i; j < args.length; j++) inputFileList.add(args[j]);
    }
    for (int j = 0, sz = inputFileList.size(); j < sz; j++) {
      Reader r = new BufferedReader(new InputStreamReader(
        new FileInputStream(inputFileList.get(j)), charset));
      PrintWriter out;
      if (outputFileList == null) {
        out = new PrintWriter(System.out, true);
      } else {
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileList.get(j)), charset)), true);
      }

      PTBTokenizer<FeatureLabel> tokenizer = PTBTokenizer.newPTBTokenizer(r, tokenizeNL, true);
      boolean printing = true;
      if (parseInsideBegin != null) {
        printing = false;
      }
      boolean beginLine = true;
      while (tokenizer.hasNext()) {
        Object obj = tokenizer.next();
        String str = ((HasWord) obj).word();

        if (parseInsideBegin != null && parseInsideBegin.matcher(str).matches()) {
          printing = true;
        } else if (parseInsideEnd != null && parseInsideEnd.matcher(str).matches()) {
          printing = false;
        } else if (printing) {
          if (dump) {
            // after having checked for tags, change str to be exhaustive
            str = obj.toString();
          }
          if (preserveLines) {
            if ("*CR*".equals(str)) {
              beginLine=true;
              out.println("");
            } else {
              if ( ! beginLine) {
                out.print(" ");
              } else {
                beginLine = false;
              }
              out.print(str);
            }
          } else {
            out.println(str);
          }
        }
      }
      r.close();
      if (outputFileList != null) out.close();
    } // end for j going through inputFileList
  } // end main

} // end PTBTokenizer
