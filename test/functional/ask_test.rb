require File.dirname(__FILE__) + '/../test_helper'

class AskTest < Test::Unit::TestCase
  def test_should_generate_n_questions
    [1, 10, 100].each do |n|
      assert_equal ask(article(:animals), n).length, n
    end
  end
end