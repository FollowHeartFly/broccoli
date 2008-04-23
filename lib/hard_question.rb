require 'article'

=begin rdoc
  +HardQuestion+
  
  Generates hard-difficulty questions from a body of text. 
  Uses Bayesian Classifier to generate questions based on what
  kind of article is being analyzed
=end  
module HardQuestion

=begin rdoc
  Returns an array of hard-difficulty questions. Generates up to _n_ if specified.
=end  
  def self.ask(text, n = nil)
    @article = Article.new(text)
    
    questions = []
    #pp @article
    
    case @article.classification
    when "Animals"
      questions << "What is the diet of the %s?"
      questions << "Where does the %s live?"
    when "Countries"
      questions << "What are the official languages of %s?"
      questions << "What langauges are spoken in %s?"
      questions << "What is the currency of %s?"
      questions << "Who is the leader of %s?"
    when "Presidents"
      questions << "When was %s born?"
      questions << "When was %s sworn into office?"
      questions << "Which political party did %s belong to?"
    end
    
    questions.collect!{|q| q % @article.subject}
    questions.slice!(0..n - 1) if n
    return questions
  end
end