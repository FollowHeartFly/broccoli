require File.dirname(__FILE__) + '/../test_helper'
require 'parser'
require 'factoid_matcher'

class FactoidMatcherTest < Test::Unit::TestCase
  def setup
    @p = Parser.instance
    @matcher = FactoidMatcher.instance
  end
  
  def test_should_match
    ptree = @p.parse("Carnegie Mellon University is in Pittsburgh, PA.")
    assert(@matcher.matches?(ptree))
    assert_equal("What is in Pittsburgh, PA?", @matcher.to_question(ptree))
  end
end


