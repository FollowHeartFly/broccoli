require 'singleton'
require 'parse_tree_matcher'

=begin rdoc
  Generates questions for simple factoid statements
=end
class FactoidMatcher < ParseTreeMatcher
  include Singleton
  
  def initialize
    @matcher_tree = ParseNode.new({
      :label => "ROOT",
      :children => 
      [
        :label => "S",
        :children => 
        [
          {
            :label => "NP"
          },
          {
            :label => "VP"
          }
        ]
      ]
    })
  end

def matches?(parse_tree)
  if parse_tree.contains_tree?(@matcher_tree)
    parse_tree.children.first.children.first.to_sentence =~ /[A-Z]/
  else
    false
  end
end
  
=begin rdoc
  Turns the provided parse tree into a question.
=end
  def to_question(parse_tree)
    np, vp = parse_tree.extract_tree(@matcher_tree).children.first.children
    if np.children.first.label == "PRP" and np.children.first.to_sentence != "It"
      question_word = "Who" 
    elsif np.children.first.label == "PRP$" and np.children.length == 1
      question_word = "Whose"
    else
      question_word = "What"
    end
    q = question_word + ' ' + vp.to_sentence.gsub(/\.$/, "") + '?'
    [q, np.to_sentence]
  end
end