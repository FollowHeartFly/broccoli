package edu.stanford.nlp.trees;

import edu.stanford.nlp.ling.Label;

/**
 * Normalizes trees in the way used in Manning and Carpenter 1997.
 * NB: This implementation is still incomplete!
 * The normalizations performed are: (i) terminals are interned, (ii)
 * nonterminals are stripped of alternants, functional tags and
 * cross-reference codes, and then interned, (iii) empty
 * elements (ones with nonterminal label "-NONE-") are deleted from the
 * tree, (iv) the null label at the root node is replaced with the label
 * "ROOT". <br>
 * 17 Apr 2001: This was fixed to work with different kinds of labels,
 * by making proper use of the Label interface, after it was moved into
 * the trees module.
 * <p/>
 * The normalizations of the original (Prolog) BobChrisNormalize were:
 * 1. Remap the root node to be called 'ROOT'
 * 2. Truncate all nonterminal labels before characters introducing
 * annotations according to TreebankLanguagePack
 * (traditionally, -, =, | or # (last for BLLIP))
 * 3. Remap the representation of certain leaf symbols (brackets etc.)
 * 4. Map to lowercase all leaf nodes
 * 5. Delete empty/trace nodes (ones marked '-NONE-')
 * 6. Recursively delete any nodes that do not dominate any words
 * 7. Delete A over A nodes where the top A dominates nothing else
 * 8. Remove backquotes from lexical items
 * (the Treebank inserts them to escape slashes (/) and stars (*)
 * 4 is deliberately omitted, and a few things are purely aesthetic.
 * <p/>
 * 14 June 2002: It now deletes unary A over A if both nodes labels are equal
 * (7), and (6) was always part of the Tree.prune() functionality...
 * 30 June 2005: Also splice out an EDITED node, just in case you're parsing
 * the Brown corpus.
 *
 * @author Christopher Manning
 */
public class BobChrisTreeNormalizer extends TreeNormalizer {

  protected final TreebankLanguagePack tlp;


  public BobChrisTreeNormalizer() {
    this(new PennTreebankLanguagePack());
  }

  public BobChrisTreeNormalizer(TreebankLanguagePack tlp) {
    this.tlp = tlp;
  }


  /**
   * Normalizes a leaf contents.
   * This implementation interns the leaf.
   */
  public String normalizeTerminal(String leaf) {
    // We could unquote * and / with backslash \ in front of them
    return leaf.intern();
  }


  /**
   * Normalizes a nonterminal contents.
   * This implementation strips functional tags, etc. and interns the
   * nonterminal.
   */
  public String normalizeNonterminal(String category) {
    return cleanUpLabel(category).intern();
  }


  /**
   * Normalize a whole tree -- one can assume that this is the
   * root.  This implementation deletes empty elements (ones with
   * nonterminal tag label '-NONE-') from the tree, and splices out
   * unary A over A nodes.  It does work for a null tree.
   */
  public Tree normalizeWholeTree(Tree tree, TreeFactory tf) {
    return tree.prune(new edu.stanford.nlp.util.Filter() {
      // Delete nodes that only cover an empty
      public boolean accept(Object obj) {
        Tree t = (Tree) obj;
        Tree[] kids = t.children();
        Label l = t.label();
        if ((l != null) && l.value() != null && (l.value().equals("-NONE-")) && !t.isLeaf() && kids.length == 1 && kids[0].isLeaf()) {
          // Delete empty/trace nodes (ones marked '-NONE-')
          return false;
        }
        return true;
      }
    }, tf).spliceOut(new edu.stanford.nlp.util.Filter<Tree>() {
      // Delete nodes that are now A over A nodes (perhaps due to
      // empty removal or are EDITED nodes
      public boolean accept(Tree t) {
        if (t.isLeaf() || t.isPreTerminal()) {
          return true;
        }
        // The special switchboard non-terminals clause
        if ("EDITED".equals(t.label().value()) || "CODE".equals(t.label().value())) {
          return false;
        }
        if (t.numChildren() != 1) {
          return true;
        }
        if (t.label() != null && t.label().value() != null && t.label().value().equals(t.children()[0].label().value())) {
          return false;
        }
        return true;
      }
    }, tf);
  }

  /**
   * Remove things like hyphened functional tags and equals from the
   * end of a node label.  This version always just returns the phrase
   * structure category, or "ROOT" if the label was <code>null</code>.
   *
   * @param label The label from the treebank
   * @return The cleaned up label (phrase structure category)
   */
  protected String cleanUpLabel(String label) {
    if (label == null) {
      label = "ROOT";
      // String constants are always interned
    } else {
      label = tlp.basicCategory(label);
    }
    return label;
  }

}
