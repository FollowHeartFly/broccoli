require 'singleton'
require 'parse_tree_matcher'

=begin rdoc
  Generates questions for statements of the form "On October 19, 1998 something happened."
=end
class DateMatcher < ParseTreeMatcher
  include Singleton
  
  def initialize
    @matcher_tree = ParseNode.new({
      :label => "ROOT",
      :children => [
        {
          :label => "S",
          :children => 
          [
            {
              :label => "PP",
              :children => 
              [
                {
                  :label => "IN"
                },
                {
                  :label => "NP", 
                }
              ]
            },
            {
              :label => ","
            },
            {
              :label => "NP"
            },
            {
              :label => "VP"
            }
          ]
        }
      ]      
    })
  end

  def matches?(parse_tree)
    if parse_tree.contains_tree?(@matcher_tree)
      parse_tree.children.first.children.first.to_sentence =~ /\d\d\d\d/
    else
      false
    end
  end
  
=begin rdoc
  Turns the provided parse tree into a question.
=end
  def to_question(parse_tree)
    tree = parse_tree.extract_tree(@matcher_tree)
    tree.children.first.children.first.to_sentence =~ /(\d\d\d\d)/
    date = $1
    tree.children.first.children.slice!(0, 2)
    
    sentence = tree.to_sentence.gsub(/\./, "") + " in what year?"
    words = sentence.split
    words.first.capitalize!
    [words.join(" "), date]
  end
end