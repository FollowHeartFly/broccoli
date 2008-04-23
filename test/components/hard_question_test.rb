require File.dirname(__FILE__) + '/../test_helper'
require 'hard_question'

class HardQuestionTest < Test::Unit::TestCase
  def setup
  end
  
  def test_should_generate_questions_from_article
    questions = HardQuestion.ask(article(:presidents))
    pp questions
    
    assert_not_nil questions
    
    answers_should_be = ["When was John Adams born?",
     "When was John Adams sworn into office?",
     "Which political party did John Adams belong to?"]
     
    assert_equal(answers_should_be, questions)
    
  end
end