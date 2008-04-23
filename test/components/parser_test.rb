require File.dirname(__FILE__) + '/../test_helper'
require 'parser'

class ParserTest < Test::Unit::TestCase
  UNPARSEABLE =<<EOF
Ellen Wrenshall Grant
Jesse Root Grant
April 27
1822
Point Pleasant, Ohio
Wilton, New York
Mathew Brady
United States Army
Army of the Tennessee
Military Division of the Mississippi
United States Army
United States Army
Mexican-American War
Battle of Resaca de la Palma
Battle of Palo Alto
Battle of Monterrey
Battle of Veracruz
Battle of Molino del Rey
Battle of Chapultepec
American Civil War
Battle of Fort Donelson
Battle of Shiloh
Battle of Vicksburg
Third Battle of Chattanooga
Overland Campaign
Battle of Petersburg
Appomattox Campaign
President of the United States
United States Military Academy
West Point, New York
EOF


  def setup
    @p = Parser.instance
    
    @search_tree = {
      :label => "S",
      :children => 
      [
        {
          :label => "VP", 
          :children => 
          [
            {
              :label => "VB" 
            },
            {
              :label => "NP" 
            }
          ] 
        }
      ]
    }

    @search_tree = ParseNode.new(@search_tree)
  end
  
  def test_should_parse
    trees = @p.parse("Parse this sentence.")
    
    assert_not_nil(trees)
    
    assert(trees.contains_tree?(@search_tree))
    assert(!trees.contains_tree?(ParseNode.new("JJ")))
    
    assert_equal("Parse this sentence.", trees.to_sentence)
  end
  
  # def test_should_raise_parse_error
  #   assert_raise(ParseError) { @p.parse(UNPARSEABLE) }
  # end
  # 
  # def test_should_ignore_parse_error
  #   @p.silence_errors = true
  #   assert_nothing_raised do
  #     result = @p.parse(UNPARSEABLE)
  #     assert_equal([], result)
  #   end
  # end
end
