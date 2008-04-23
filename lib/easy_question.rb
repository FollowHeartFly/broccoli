require 'parser'
require 'article_preprocessor'
require 'factoid_matcher'
require 'date_matcher'

=begin rdoc
  +EasyQuestion+
  
  Generates easy difficulty questions from an article using parsing.
=end
module EasyQuestion

  ALLOWED_WORD_COUNTS = (4..15)

  def self.ask(text, n = 10)
    @parser = Parser.instance
    @parser.silence_errors = true
    
    @matchers = [FactoidMatcher.instance, DateMatcher.instance]
    
    questions = []
    
    candidates = ArticlePreprocessor.sentences(text)
    title = candidates[0].gsub(/_/, ' ')
    title.downcase!
    candidates.each do |candidate|
      wordcount = candidate.split(/\s+/).length
      next if not ALLOWED_WORD_COUNTS.include?(wordcount)
      
      parse_tree = @parser.parse(candidate + '.')
      $logger.debug "Considering \"#{parse_tree.to_sentence}\""
      @matchers.each do |matcher|
        if matcher.matches?(parse_tree)
          q, a = matcher.to_question(parse_tree)
          a.downcase!
          
          # Throw out the question if one of the article title and the answer
          # is a substring of the other.
          if not title.include?(a) and not a.include?(title)
            questions << q
          end
        end
      end
      break if questions.length >= n
    end
    
    questions
  end
  
  
end