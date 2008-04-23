require File.dirname(__FILE__) + '/../test_helper'
require 'parser'
require 'date_matcher'

# require File.dirname(__FILE__) + '/../../lib/parser'
# require File.dirname(__FILE__) + '/../../lib/date_matcher'

class DateMatcherTest < Test::Unit::TestCase
  def setup
    @p = Parser.instance
    @matcher = DateMatcher.instance
  end
  
  def test_should_match
    ptree = @p.parse("On October 10, 1978, Vice President Spiro Agnew resigned.")
    assert(@matcher.matches?(ptree))
    assert_equal("Vice President Spiro Agnew resigned in what year?", @matcher.to_question(ptree))
    
    ptree = @p.parse("In 1868, Grant was elected president as a Republican.")
    assert(@matcher.matches?(ptree))
    assert_equal("Grant was elected president as a Republican in what year?", @matcher.to_question(ptree))
    
    ptree = @p.parse("In 2008, the people elected Barack Obama president.")
    assert(@matcher.matches?(ptree))
    assert_equal("The people elected Barack Obama president in what year?", @matcher.to_question(ptree))
  end
end


