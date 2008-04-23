require 'article_classifier'
require 'article_preprocessor'

class Article
  include ArticlePreprocessor
  
  attr_reader :title, :body, :classification
  
  def initialize(text)
    first_sentence = ArticlePreprocessor.sentences(text).first
    if first_sentence.length < 30
      @title = first_sentence.split(/[\W_]/).collect{|t| t.capitalize}.join(" ")
    else
      @title = "Untitled"
    end
    @body = ArticlePreprocessor.sanitize(text)
    @classification = ArticleClassifier.instance.classify(text)
  end
  
  alias_method :subject, :title
end