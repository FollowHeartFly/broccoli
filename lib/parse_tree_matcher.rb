=begin rdoc
  Superclass for parse tree matchers.  Should not be instantiated.
=end
class ParseTreeMatcher
=begin rdoc
  Returns true if this matcher can generate a question from the provided tree.
=end
  def matches?(parse_tree)
    parse_tree.contains_tree?(@matcher_tree)
  end
end