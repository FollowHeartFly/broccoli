require File.dirname(__FILE__) + '/../test_helper'

class AnswerTest < Test::Unit::TestCase
  def test_should_answer_questions
    assert_not_nil answer(question(:animals))
  end
end