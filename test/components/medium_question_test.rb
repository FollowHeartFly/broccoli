require File.dirname(__FILE__) + '/../test_helper'
require 'medium_question'

class MediumQuestionTest < Test::Unit::TestCase
  def setup
  end
  
  def test_should_generate_questions_from_article
    questions = MediumQuestion.ask(article(:presidents))
    pp questions
    
    assert_not_nil questions
  end
end