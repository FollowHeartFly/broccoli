require 'ner'

=begin rdoc
  +MediumQuestion+
  
  Generates medium-difficulty questions from a body of text. 
  Uses the Stanford Named-Entity Recognizer to naively construct questions 
  based on the what tagging the entity recieved.
=end  
module MediumQuestion

=begin rdoc
  Returns an array of medium-difficulty questions. Generates up to _n_ if specified.
=end  
  def self.ask(text, n = nil)
    @ner = NamedEntityRecognizer.new
    @ner.recognize(text)
    
    questions = []
    
    @ner.entities.each do |entity, tag|
      questions << case tag
                   when NamedEntityRecognizer::PERSON
                     "Who is #{entity}?"
                   when NamedEntityRecognizer::LOCATION
                     "Where is #{entity}?"
                   when NamedEntityRecognizer::ORGANIZATION
                     "What is #{entity}?"
                   when NamedEntityRecognizer::OTHER
                     "What is a #{entity}?"
      end
      
      break if n and questions.length >= n
    end
    
    return questions
  end
end