require File.dirname(__FILE__) + '/../test_helper'
require 'easy_answer'

class EasyAnswerTest < Test::Unit::TestCase
  def setup
  end
  
  def test_should_answer_yes
    @question = "Is Socrates a man?"
    @text = "Socrates is a man."
    assert_not_nil EasyAnswer.answer(@question, @text)
    
    pp EasyAnswer.answer(@question, @text)
  end
  
  def test_should_answer_no
    @question = "Is Socrates a bachelor?"
    @text = "Socrates is a man."
    assert_not_nil EasyAnswer.answer(@question, @text)
    
    pp EasyAnswer.answer(@question, @text)
  end

end