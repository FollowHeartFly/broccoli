// StanfordLexicalizedParser -- a probabilistic lexicalized NL CFG parser
// Copyright (c) 2002, 2003, 2004, 2005 The Board of Trustees of
// The Leland Stanford Junior University. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// For more information, bug reports, fixes, contact:
//    Christopher Manning
//    Dept of Computer Science, Gates 4A
//    Stanford CA 94305-9040
//    USA
//    parser-support@lists.stanford.edu
//    http://nlp.stanford.edu/downloads/lex-parser.shtml

package edu.stanford.nlp.parser.ui;

import edu.stanford.nlp.io.ui.OpenPageDialog;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.parser.lexparser.ChineseLexiconAndWordSegmenter;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.Processor;
import edu.stanford.nlp.process.StripTagsProcessor;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.WordSegmentingTokenizer;
import edu.stanford.nlp.swing.FontDetector;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.international.pennchinese.ChineseTreebankLanguagePack;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;


/**
 * Provides a simple GUI Panel for Parsing.  Allows a user to load a parser
 * created using lexparser.LexicalizedParser, load a text data file or type
 * in text, parse sentences within the input text, and view the resultant
 * parse tree.
 *
 * @author Huy Nguyen (htnguyen@cs.stanford.edu)
 */
public class ParserPanel extends JPanel {

  // constants for language specification
  public static final int UNTOKENIZED_ENGLISH = 0;
  public static final int TOKENIZED_CHINESE = 1;
  public static final int UNTOKENIZED_CHINESE = 2;

  private static TreebankLanguagePack tlp;
  private String encoding = "UTF-8";
  private boolean segmentWords = false;

  // one second in milliseconds
  private static final int ONE_SECOND = 1000;
  // parser takes approximately a minute to load
  private static final int PARSER_LOAD_TIME = 60;
  // parser takes 5-60 seconds to parse a sentence
  private static final int PARSE_TIME = 30;

  // constants for finding nearest sentence boundary
  private static final int SEEK_FORWARD = 1;
  private static final int SEEK_BACK = -1;

  private final JFileChooser jfc;
  private OpenPageDialog pageDialog;

  // for highlighting
  private SimpleAttributeSet normalStyle, highlightStyle;
  private int startIndex, endIndex;

  private TreeJPanel treePanel;
  private LexicalizedParser parser;

  // worker threads to handle long operations
  private LoadParserThread lpThread;
  private ParseThread parseThread;

  // to monitor progress of long operations
  private javax.swing.Timer timer;
  //private ProgressMonitor progressMonitor;
  private int count; // progress count
  // use glass pane to block input to components other than progressMonitor
  private Component glassPane;

  /** Whether to scroll one sentence forward after parsing. */
  private boolean scrollWhenDone;

  /**
   * Creates new form ParserPanel
   */
  public ParserPanel() {
    initComponents();

    // create dialogs for file selection
    jfc = new JFileChooser();
    pageDialog = new OpenPageDialog(new Frame(), true);
    pageDialog.setFileChooser(jfc);

    setLanguage(UNTOKENIZED_ENGLISH);

    // create a timer
    timer = new javax.swing.Timer(ONE_SECOND, new TimerListener());

    // for (un)highlighting text
    highlightStyle = new SimpleAttributeSet();
    normalStyle = new SimpleAttributeSet();
    StyleConstants.ColorConstants.setBackground(highlightStyle, Color.yellow);
    StyleConstants.ColorConstants.setBackground(normalStyle, textPane.getBackground());
  }

  /**
   * Scrolls back one sentence in the text
   */
  public void scrollBack() {
    highlightSentence(startIndex - 1);
    // scroll to highlight location
    textPane.setCaretPosition(startIndex);
  }

  /**
   * Scrolls forward one sentence in the text
   */
  public void scrollForward() {
    highlightSentence(endIndex + 1);
    // scroll to highlight location
    textPane.setCaretPosition(startIndex);
  }

  /**
   * Highlights specified text region by changing the character attributes
   */
  private void highlightText(int start, int end, SimpleAttributeSet style) {
    if (start < end) {
      textPane.getStyledDocument().setCharacterAttributes(start, end - start + 1, style, false);
    }
  }

  /**
   * Finds the sentence delimited by the closest sentence delimiter preceding
   * start and closest period following start.
   */
  private void highlightSentence(int start) {
    highlightSentence(start, -1);
  }

  /**
   * Finds the sentence delimited by the closest sentence delimiter preceding
   * start and closest period following end.  If end is less than start
   * (or -1), sets right boundary as closest period following start.
   * Actually starts search for preceding sentence delimiter at (start-1)
   */
  private void highlightSentence(int start, int end) {
    // clears highlight.  paints over entire document because the document may have changed
    highlightText(0, textPane.getText().length(), normalStyle);

    // if start<1 set startIndex to 0, otherwise set to index following closest preceding period
    startIndex = (start < 1) ? 0 : nearestDelimiter(textPane.getText(), start - 1, SEEK_BACK) + 1;

    // if end<startIndex, set endIndex to closest period following startIndex
    // else, set it to closest period following end
    endIndex = nearestDelimiter(textPane.getText(), (end < startIndex) ? startIndex : end, SEEK_FORWARD);
    if (endIndex == -1) {
      endIndex = textPane.getText().length() - 1;
    }

    highlightText(startIndex, endIndex, highlightStyle);

    // enable/disable scroll buttons as necessary
    backButton.setEnabled(startIndex != 0);
    forwardButton.setEnabled(endIndex != textPane.getText().length() - 1);
    parseNextButton.setEnabled(forwardButton.isEnabled() && parser != null);
  }

  /**
   * Finds the nearest delimiter starting from index start. If <tt>seekDir</tt>
   * is SEEK_FORWARD, finds the nearest delimiter after start.  Else, if it is
   * SEEK_BACK, finds the nearest delimiter before start.
   */
  private int nearestDelimiter(String text, int start, int seekDir) {
    int curIndex = start;
    int textLeng = text.length();
    String[] puncWords = tlp.sentenceFinalPunctuationWords();
    while (curIndex >= 0 && curIndex < textLeng) {
      for (int i = 0; i < puncWords.length; i++) {
        if (puncWords[i].equals(Character.toString(text.charAt(curIndex)))) {
          return curIndex;
        }
      }
      curIndex += seekDir;
    }
    return -1;
  }

  /**
   * Highlights the sentence that is currently being selected by user
   * (via mouse highlight)
   */
  private void highlightSelectedSentence() {
    highlightSentence(textPane.getSelectionStart(), textPane.getSelectionEnd());
  }

  /**
   * Highlights the sentence that is currently being edited
   */
  private void highlightEditedSentence() {
    highlightSentence(textPane.getCaretPosition());

  }

  /**
   * Sets the status text at the bottom of the ParserPanel.
   */
  public void setStatus(String status) {
    statusLabel.setText(status);
  }

  /**
   * Sets the language used by the ParserPanel to tokenize, parse, and
   * display sentences.
   *
   * @param language One of several predefined language codes. e.g.
   *                 <tt>UNTOKENIZED_ENGLISH</tt>, <tt>TOKENIZED_CHINESE</tt>, etc.
   */
  public void setLanguage(int language) {
    switch (language) {
      case UNTOKENIZED_ENGLISH:
        tlp = new PennTreebankLanguagePack();
        encoding = tlp.getEncoding();
        textPane.setFont(new Font("Sans Serif", Font.PLAIN, 14));
        treePanel.setFont(new Font("Sans Serif", Font.PLAIN, 14));
        break;
      case UNTOKENIZED_CHINESE:
        segmentWords = true;
        tlp = new ChineseTreebankLanguagePack();
        encoding = "UTF-8"; // we support that not GB18030 currently....
        setChineseFont();
        break;
      case TOKENIZED_CHINESE:
        segmentWords = false;
        tlp = new ChineseTreebankLanguagePack();
        encoding = "UTF-8"; // we support that not GB18030 currently....
        setChineseFont();
        break;
    }
  }

  private void setChineseFont() {
    java.util.List fonts = FontDetector.supportedFonts(FontDetector.CHINESE);
    if (fonts.size() > 0) {
      Font font = new Font(((Font) fonts.get(0)).getName(), Font.PLAIN, 14);
      textPane.setFont(font);
      treePanel.setFont(font);
      System.err.println("Selected font " + font);
    } else if (FontDetector.hasFont("Watanabe Mincho")) {
      textPane.setFont(new Font("Watanabe Mincho", Font.PLAIN, 14));
      treePanel.setFont(new Font("Watanabe Mincho", Font.PLAIN, 14));
    }
  }


  /**
   * Tokenizes the highlighted text (using a tokenizer appropriate for the
   * selected language, and initiates the ParseThread to parse the tokenized
   * text.
   */
  public void parse() {
    if (textPane.getText().length() == 0) {
      return;
    }

    // use endIndex+1 because substring subtracts 1
    String text = textPane.getText().substring(startIndex, endIndex + 1).trim();

    if (parser != null && text.length() > 0) {
      if (segmentWords) {
        ChineseLexiconAndWordSegmenter lex = (ChineseLexiconAndWordSegmenter) parser.getLexicon();
        ChineseTreebankLanguagePack.setTokenizerFactory(WordSegmentingTokenizer.factory(lex));
      }
      Tokenizer<? extends HasWord> toke = tlp.getTokenizerFactory().getTokenizer(new CharArrayReader(text.toCharArray()));
      List<? extends HasWord> wordList = toke.tokenize();
      parseThread = new ParseThread(wordList);
      parseThread.start();
      startProgressMonitor("Parsing", PARSE_TIME);
    }
  }

  /**
   * Opens dialog to load a text data file
   */
  public void loadFile() {
    // centers dialog in panel
    pageDialog.setLocation(getLocationOnScreen().x + (getWidth() - pageDialog.getWidth()) / 2, getLocationOnScreen().y + (getHeight() - pageDialog.getHeight()) / 2);
    pageDialog.setVisible(true);

    if (pageDialog.getStatus() == OpenPageDialog.APPROVE_OPTION) {
      loadFile(pageDialog.getPage());
    }
  }

  /**
   * Loads a text or html file from a file path or URL.  Treats anything
   * beginning with <tt>http:\\</tt>,<tt>.htm</tt>, or <tt>.html</tt> as an
   * html file, and strips all tags from the document
   */
  public void loadFile(String filename) {
    if (filename == null) {
      return;
    }

    File file = new File(filename);

    String urlOrFile = filename;
    // if file can't be found locally, try prepending http:// and looking on web
    if (!file.exists() && filename.indexOf("://") == -1) {
      urlOrFile = "http://" + filename;
    }
    // else prepend file:// to handle local html file urls
    else if (filename.indexOf("://") == -1) {
      urlOrFile = "file://" + filename;
    }

    // load the document
    Document doc;
    try {
      if (urlOrFile.startsWith("http://") || urlOrFile.endsWith(".htm") || urlOrFile.endsWith(".html")) {
        // strip tags from html documents
        Document docPre = new BasicDocument().init(new URL(urlOrFile));
        Processor noTags = new StripTagsProcessor();
        doc = noTags.processDocument(docPre);
      } else {
        doc = new BasicDocument(tlp.getTokenizerFactory()).init(new InputStreamReader(new FileInputStream(filename), encoding));
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Could not load file " + filename + "\n" + e, null, JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
      setStatus("Error loading document");
      return;
    }

    // load the document into the text pane
    StringBuilder docStr = new StringBuilder();
    for (Iterator it = doc.iterator(); it.hasNext(); ) {
      if (docStr.length() > 0) {
        docStr.append(' ');
      }
      docStr.append(it.next().toString());
    }
    textPane.setText(docStr.toString());
    dataFileLabel.setText(urlOrFile);

    highlightSentence(0);
    forwardButton.setEnabled(endIndex != textPane.getText().length() - 1);
    // scroll to top of document
    textPane.setCaretPosition(0);

    setStatus("Done");
  }

  /**
   * Opens dialog to load a serialized parser
   */
  public void loadParser() {
    jfc.setDialogTitle("Load parser");
    int status = jfc.showOpenDialog(this);
    if (status == JFileChooser.APPROVE_OPTION) {
      loadParser(jfc.getSelectedFile().getPath());
    }
  }

  /**
   * Loads a serialized parser specified by given path
   */
  public void loadParser(String filename) {
    if (filename == null) {
      return;
    }

    // check if file exists before we start the worker thread and progress monitor
    File file = new File(filename);
    if (file.exists()) {
      lpThread = new LoadParserThread(filename);
      lpThread.start();
      startProgressMonitor("Loading Parser", PARSER_LOAD_TIME);
    } else {
      JOptionPane.showMessageDialog(this, "Could not find file " + filename, null, JOptionPane.ERROR_MESSAGE);
      setStatus("Error loading parser");
    }
  }

  /**
   * Initializes the progress bar with the status text, and the expected
   * number of seconds the process will take, and starts the timer.
   */
  private void startProgressMonitor(String text, int maxCount) {
    if (glassPane == null) {
      if (getRootPane() != null) {
        glassPane = getRootPane().getGlassPane();
        glassPane.addMouseListener(new MouseAdapter() {
          public void mouseClicked(MouseEvent evt) {
            Toolkit.getDefaultToolkit().beep();
          }
        });
      }
    }
    if (glassPane != null) {
      glassPane.setVisible(true); // block input to components
    }

    statusLabel.setText(text);
    progressBar.setMaximum(maxCount);
    progressBar.setValue(0);
    count = 0;
    timer.start();
    progressBar.setVisible(true);
  }

  /**
   * At the end of a task, shut down the progress monitor
   */
  private void stopProgressMonitor() {
    timer.stop();
    /*if(progressMonitor!=null) {
        progressMonitor.setProgress(progressMonitor.getMaximum());
        progressMonitor.close();
    }*/
    progressBar.setVisible(false);
    if (glassPane != null) {
      glassPane.setVisible(false); // restore input to components
    }
    lpThread = null;
    parseThread = null;
  }

  /**
   * Worker thread for loading the parser.  Loading a parser usually takes ~2 min
   */
  private class LoadParserThread extends Thread {
    String filename;

    LoadParserThread(String filename) {
      this.filename = filename;
    }

    public void run() {
      try {
        parser = new edu.stanford.nlp.parser.lexparser.LexicalizedParser(filename);
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(ParserPanel.this, "Error loading parser: " + filename, null, JOptionPane.ERROR_MESSAGE);
        setStatus("Error loading parser");
        parser = null;
      } catch (OutOfMemoryError e) {
        JOptionPane.showMessageDialog(ParserPanel.this, "Could not load parser. Out of memory.", null, JOptionPane.ERROR_MESSAGE);
        setStatus("Error loading parser");
        parser = null;
      }

      stopProgressMonitor();
      if (parser != null) {
        setStatus("Loaded parser.");
        parserFileLabel.setText("Parser: " + filename);
        parseButton.setEnabled(true);
        parseNextButton.setEnabled(true);
      }
    }
  }

  /**
   * Worker thread for parsing.  Parsing a sentence usually takes ~5-60 sec
   */
  private class ParseThread extends Thread {

    List<? extends HasWord> sentence;

    public ParseThread(List<? extends HasWord> sentence) {
      this.sentence = sentence;
    }

    public void run() {
      boolean successful;
      try {
        successful = parser.parse(sentence);
      } catch (Exception e) {
        stopProgressMonitor();
        JOptionPane.showMessageDialog(ParserPanel.this, "Could not parse selected sentence\n(sentence probably too long)", null, JOptionPane.ERROR_MESSAGE);
        setStatus("Error parsing");
        return;
      }

      stopProgressMonitor();
      setStatus("Done");
      if (successful) {
        // display the best parse
        Tree tree = parser.getBestParse();
        //tree.pennPrint();
        treePanel.setTree(tree);
        clearButton.setEnabled(true);
      } else {
        JOptionPane.showMessageDialog(ParserPanel.this, "Could not parse selected sentence", null, JOptionPane.ERROR_MESSAGE);
        setStatus("Error parsing");
        treePanel.setTree(null);
        clearButton.setEnabled(false);
      }
      if (scrollWhenDone) {
        scrollForward();
      }
    }
  }

  /**
   * Simulates a timer to update the progress monitor
   */
  private class TimerListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      //progressMonitor.setProgress(Math.min(count++,progressMonitor.getMaximum()-1));
      progressBar.setValue(Math.min(count++, progressBar.getMaximum() - 1));
    }
  }

  /**
   * This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  private void initComponents()//GEN-BEGIN:initComponents
  {
    splitPane = new javax.swing.JSplitPane();
    topPanel = new javax.swing.JPanel();
    buttonsAndFilePanel = new javax.swing.JPanel();
    loadButtonPanel = new javax.swing.JPanel();
    loadFileButton = new javax.swing.JButton();
    loadParserButton = new javax.swing.JButton();
    buttonPanel = new javax.swing.JPanel();
    backButton = new javax.swing.JButton();
    if (getClass().getResource("/edu/stanford/nlp/parser/ui/leftarrow.gif") != null) {
      backButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/stanford/nlp/parser/ui/leftarrow.gif")));
    } else {
      backButton.setText("< Prev");
    }
    forwardButton = new javax.swing.JButton();
    if (getClass().getResource("/edu/stanford/nlp/parser/ui/rightarrow.gif") != null) {
      forwardButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/stanford/nlp/parser/ui/rightarrow.gif")));
    } else {
      forwardButton.setText("Next >");
    }
    parseButton = new javax.swing.JButton();
    parseNextButton = new javax.swing.JButton();
    clearButton = new javax.swing.JButton();
    dataFilePanel = new javax.swing.JPanel();
    dataFileLabel = new javax.swing.JLabel();
    textScrollPane = new javax.swing.JScrollPane();
    textPane = new javax.swing.JTextPane();
    treeContainer = new javax.swing.JPanel();
    parserFilePanel = new javax.swing.JPanel();
    parserFileLabel = new javax.swing.JLabel();
    statusPanel = new javax.swing.JPanel();
    statusLabel = new javax.swing.JLabel();
    progressBar = new javax.swing.JProgressBar();
    progressBar.setVisible(false);

    setLayout(new java.awt.BorderLayout());

    splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
    topPanel.setLayout(new java.awt.BorderLayout());

    buttonsAndFilePanel.setLayout(new javax.swing.BoxLayout(buttonsAndFilePanel, javax.swing.BoxLayout.Y_AXIS));

    loadButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

    loadFileButton.setText("Load File");
    loadFileButton.setToolTipText("Load a data file.");
    loadFileButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        loadFileButtonActionPerformed(evt);
      }
    });

    loadButtonPanel.add(loadFileButton);

    loadParserButton.setText("Load Parser");
    loadParserButton.setToolTipText("Load a serialized parser.");
    loadParserButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        loadParserButtonActionPerformed(evt);
      }
    });

    loadButtonPanel.add(loadParserButton);

    buttonsAndFilePanel.add(loadButtonPanel);

    buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

    backButton.setToolTipText("Scroll backward one sentence.");
    backButton.setEnabled(false);
    backButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        backButtonActionPerformed(evt);
      }
    });

    buttonPanel.add(backButton);

    forwardButton.setToolTipText("Scroll forward one sentence.");
    forwardButton.setEnabled(false);
    forwardButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        forwardButtonActionPerformed(evt);
      }
    });

    buttonPanel.add(forwardButton);

    parseButton.setText("Parse");
    parseButton.setToolTipText("Parse selected sentence.");
    parseButton.setEnabled(false);
    parseButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        parseButtonActionPerformed(evt);
      }
    });

    buttonPanel.add(parseButton);

    parseNextButton.setText("Parse >");
    parseNextButton.setToolTipText("Parse selected sentence and then scrolls forward one sentence.");
    parseNextButton.setEnabled(false);
    parseNextButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        parseNextButtonActionPerformed(evt);
      }
    });

    buttonPanel.add(parseNextButton);

    clearButton.setText("Clear");
    clearButton.setToolTipText("Clears parse tree.");
    clearButton.setEnabled(false);
    clearButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        clearButtonActionPerformed(evt);
      }
    });

    buttonPanel.add(clearButton);

    buttonsAndFilePanel.add(buttonPanel);

    dataFilePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

    dataFilePanel.add(dataFileLabel);

    buttonsAndFilePanel.add(dataFilePanel);

    topPanel.add(buttonsAndFilePanel, java.awt.BorderLayout.NORTH);

    textPane.setPreferredSize(new java.awt.Dimension(250, 250));
    textPane.addFocusListener(new java.awt.event.FocusAdapter() {
      public void focusLost(java.awt.event.FocusEvent evt) {
        textPaneFocusLost(evt);
      }
    });

    textPane.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        textPaneMouseClicked(evt);
      }
    });

    textPane.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
      public void mouseDragged(java.awt.event.MouseEvent evt) {
        textPaneMouseDragged(evt);
      }
    });

    textScrollPane.setViewportView(textPane);

    topPanel.add(textScrollPane, java.awt.BorderLayout.CENTER);

    splitPane.setLeftComponent(topPanel);

    treeContainer.setLayout(new java.awt.BorderLayout());

    treeContainer.setBackground(new java.awt.Color(255, 255, 255));
    treeContainer.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.RAISED));
    treeContainer.setForeground(new java.awt.Color(0, 0, 0));
    treeContainer.setPreferredSize(new java.awt.Dimension(200, 200));
    treePanel = new TreeJPanel();
    treeContainer.add("Center", treePanel);
    treePanel.setBackground(Color.white);
    parserFilePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

    parserFilePanel.setBackground(new java.awt.Color(255, 255, 255));
    parserFileLabel.setText("Parser: None");
    parserFilePanel.add(parserFileLabel);

    treeContainer.add(parserFilePanel, java.awt.BorderLayout.NORTH);

    splitPane.setRightComponent(treeContainer);

    add(splitPane, java.awt.BorderLayout.CENTER);

    statusPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

    statusLabel.setText("Ready");
    statusPanel.add(statusLabel);

    progressBar.setName("");
    statusPanel.add(progressBar);

    add(statusPanel, java.awt.BorderLayout.SOUTH);

    //Roger -- test to see if I can get a bit of a fix with new font

  }//GEN-END:initComponents

  private void textPaneFocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_textPaneFocusLost
  {//GEN-HEADEREND:event_textPaneFocusLost
    // highlights the sentence containing the current location of the cursor
    // note that the cursor is set to the beginning of the sentence when scrolling
    highlightEditedSentence();
  }//GEN-LAST:event_textPaneFocusLost

  private void parseNextButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_parseNextButtonActionPerformed
  {//GEN-HEADEREND:event_parseNextButtonActionPerformed
    parse();
    scrollWhenDone = true;
  }//GEN-LAST:event_parseNextButtonActionPerformed

  private void clearButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearButtonActionPerformed
  {//GEN-HEADEREND:event_clearButtonActionPerformed
    treePanel.setTree(null);
    clearButton.setEnabled(false);
  }//GEN-LAST:event_clearButtonActionPerformed

  private void textPaneMouseDragged(java.awt.event.MouseEvent evt)//GEN-FIRST:event_textPaneMouseDragged
  {//GEN-HEADEREND:event_textPaneMouseDragged
    highlightSelectedSentence();
  }//GEN-LAST:event_textPaneMouseDragged

  private void textPaneMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_textPaneMouseClicked
  {//GEN-HEADEREND:event_textPaneMouseClicked
    highlightSelectedSentence();
  }//GEN-LAST:event_textPaneMouseClicked

  private void parseButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_parseButtonActionPerformed
  {//GEN-HEADEREND:event_parseButtonActionPerformed
    parse();
    scrollWhenDone = false;
  }//GEN-LAST:event_parseButtonActionPerformed

  private void loadParserButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_loadParserButtonActionPerformed
  {//GEN-HEADEREND:event_loadParserButtonActionPerformed
    loadParser();
  }//GEN-LAST:event_loadParserButtonActionPerformed

  private void loadFileButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_loadFileButtonActionPerformed
  {//GEN-HEADEREND:event_loadFileButtonActionPerformed
    loadFile();
  }//GEN-LAST:event_loadFileButtonActionPerformed

  private void backButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_backButtonActionPerformed
  {//GEN-HEADEREND:event_backButtonActionPerformed
    scrollBack();
  }//GEN-LAST:event_backButtonActionPerformed

  private void forwardButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_forwardButtonActionPerformed
  {//GEN-HEADEREND:event_forwardButtonActionPerformed
    scrollForward();
  }//GEN-LAST:event_forwardButtonActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel dataFileLabel;
  private javax.swing.JPanel treeContainer;
  private javax.swing.JPanel topPanel;
  private javax.swing.JScrollPane textScrollPane;
  private javax.swing.JButton backButton;
  private javax.swing.JLabel statusLabel;
  private javax.swing.JButton loadFileButton;
  private javax.swing.JPanel loadButtonPanel;
  private javax.swing.JPanel buttonsAndFilePanel;
  private javax.swing.JButton parseButton;
  private javax.swing.JButton parseNextButton;
  private javax.swing.JButton forwardButton;
  private javax.swing.JLabel parserFileLabel;
  private javax.swing.JButton clearButton;
  private javax.swing.JSplitPane splitPane;
  private javax.swing.JPanel statusPanel;
  private javax.swing.JPanel dataFilePanel;
  private javax.swing.JPanel buttonPanel;
  private javax.swing.JTextPane textPane;
  private javax.swing.JProgressBar progressBar;
  private javax.swing.JPanel parserFilePanel;
  private javax.swing.JButton loadParserButton;
  // End of variables declaration//GEN-END:variables
}
