package edu.stanford.nlp.trees.international.negra;

import java.util.HashMap;

import edu.stanford.nlp.trees.AbstractCollinsHeadFinder;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;


/**
 * HeadFinder for the Negra Treebank.  Adapted from
 * CollinsHeadFinder.
 *
 * @author Roger Levy
 */
public class NegraHeadFinder extends AbstractCollinsHeadFinder {

  /** Vends a "semantic" NegraHeadFinder---one that disprefers modal/auxiliary verbs as the heads of S or VP.
   * 
   * @return a NegraHeadFinder that uses a "semantic" head-finding rule for the S category. 
   */
  public static HeadFinder negraSemanticHeadFinder() {
    NegraHeadFinder result = new NegraHeadFinder();
    result.nonTerminalInfo.put("S", new String[][]{{result.right,  "VVFIN",  "VVIMP"}, {"right", "VP","CVP"}, { "right", "VMFIN", "VAFIN", "VAIMP"}, {"right", "S","CS"}}); 
    result.nonTerminalInfo.put("VP", new String[][]{{"right","VVINF","VVIZU","VVPP"}, {result.right, "VZ", "VAINF", "VMINF", "VMPP", "VAPP", "PP"}}); 
    result.nonTerminalInfo.put("VZ", new String[][]{{result.right,"VVINF","VAINF","VMINF","VVFIN","VVIZU"}}); // note that VZ < VVIZU is very rare, maybe shouldn't even exist.
    return result;
  }
  
  private boolean coordSwitch = false;

  public NegraHeadFinder() {
    this(new NegraPennLanguagePack());
  }

  String left;
  String right;
  
  public NegraHeadFinder(TreebankLanguagePack tlp) {
    super(tlp);

    nonTerminalInfo = new HashMap();

    left = (coordSwitch ? "right" : "left");
    right = (coordSwitch ? "left" : "right");

    /* BEGIN ROGER TODO */
    //
    //    // some special rule for S
    //    if(motherCat.equals("S") && kids[0].label().value().equals("PRELS"))
    //return kids[0];
    //
    nonTerminalInfo.put("S", new String[][]{{left, "PRELS"}});
    /* END ROGER TODO */

    // these are first-cut rules

    // there are non-unary nodes I put in
    nonTerminalInfo.put("NUR", new String[][]{{left, "S"}});

    // root -- yuk
    nonTerminalInfo.put("ROOT", new String[][]{{left, "S", "CS", "VP", "CVP", "NP", "XY", "CNP", "AVP", "CAVP"}});

    // Major syntactic categories -- in order appearing in negra.export
    nonTerminalInfo.put("NP", new String[][]{{right, "NN", "NE", "MPN", "NP", "CNP", "PN", "CAR"}}); // Basic heads are NN/NE/NP; CNP is coordination; CAR is cardinal
    nonTerminalInfo.put("AP", new String[][]{{right, "ADJD", "ADJA", "CAP", "AA", "ADV"}}); // there is one ADJP unary rewrite to AD but otherwise all have JJ or ADJP
    nonTerminalInfo.put("PP", new String[][]{{left, "KOKOM", "APPR", "PROAV"}});
    //nonTerminalInfo.put("S", new String[][] {{right, "S","CS","NP"}}); //Most of the time, S has its head explicitly marked.  CS is coordinated sentence.  I don't fully understand the rest of "non-headed" german sentences to say much.
    nonTerminalInfo.put("S", new String[][]{{right, "VMFIN", "VVFIN", "VAFIN", "VVIMP", "VAIMP" }, {"right", "VP","CVP"}, {"right", "S","CS"}}); // let finite verbs (including imperatives) be head always.
    nonTerminalInfo.put("VP", new String[][]{{right, "VZ", "VAINF", "VMINF", "VVINF", "VVIZU", "VVPP", "VMPP", "VAPP", "PP"}}); // VP usually has explicit head marking; there's lots of garbage here to sort out, though.
    nonTerminalInfo.put("VZ", new String[][]{{left, "PRTZU", "APPR","PTKZU"}}); // we could also try using the verb (on the right) instead of ZU as the head, maybe this would make more sense...
    nonTerminalInfo.put("CO", new String[][]{{left}}); // this is an unlike coordination
    nonTerminalInfo.put("AVP", new String[][]{{right, "ADV", "AVP", "ADJD", "PROAV", "PP"}});
    nonTerminalInfo.put("AA", new String[][]{{right, "ADJD", "ADJA"}}); // superlative adjective phrase with "am"; I'm using the adjective not the "am" marker
    nonTerminalInfo.put("CNP", new String[][]{{right, "NN", "NE", "MPN", "NP", "CNP", "PN", "CAR"}});
    nonTerminalInfo.put("CAP", new String[][]{{right, "ADJD", "ADJA", "CAP", "AA", "ADV"}});
    nonTerminalInfo.put("CPP", new String[][]{{right, "APPR", "PROAV", "PP", "CPP"}});
    nonTerminalInfo.put("CS", new String[][]{{right, "S", "CS"}});
    nonTerminalInfo.put("CVP", new String[][]{{right, "VP", "CVP"}}); // covers all examples
    nonTerminalInfo.put("CVZ", new String[][]{{right, "VZ"}}); // covers all examples
    nonTerminalInfo.put("CAVP", new String[][]{{right, "ADV", "AVP", "ADJD", "PWAV", "APPR", "PTKVZ"}});
    nonTerminalInfo.put("MPN", new String[][]{{right, "NE", "FM", "CARD"}}); //presumably left/right doesn't matter
    nonTerminalInfo.put("NM", new String[][]{{right, "CARD", "NN"}}); // covers all examples
    nonTerminalInfo.put("CAC", new String[][]{{right, "APPR", "AVP"}}); //covers all examples
    nonTerminalInfo.put("CH", new String[][]{{right}});
    nonTerminalInfo.put("MTA", new String[][]{{right, "ADJA", "ADJD", "NN"}});
    nonTerminalInfo.put("CCP", new String[][]{{right, "AVP"}});
    nonTerminalInfo.put("DL", new String[][]{{left}}); // don't understand this one yet
    nonTerminalInfo.put("ISU", new String[][]{{right}}); // idioms, I think
    nonTerminalInfo.put("QL", new String[][]{{right}}); // these are all complicated numerical expressions I think

    nonTerminalInfo.put("--", new String[][]{{right, "PP"}}); // a garbage conjoined phrase appearing once

    // some POS tags apparently sit where phrases are supposed to be
    nonTerminalInfo.put("CD", new String[][]{{right, "CD"}});
    nonTerminalInfo.put("NN", new String[][]{{right, "NN"}});
    nonTerminalInfo.put("NR", new String[][]{{right, "NR"}});
  }

  /* Some Negra local trees have an explicitly marked head.  Use it if
  * possible. */
  protected Tree findMarkedHead(Tree[] kids) {
    for (int i = 0, n = kids.length; i < n; i++) {
      if (kids[i].label() instanceof NegraLabel && ((NegraLabel) kids[i].label()).getEdge() != null && ((NegraLabel) kids[i].label()).getEdge().equals("HD")) {
        //System.err.println("found manually-labeled head");
        return kids[i];
      }
    }
    return null;
  }
}
