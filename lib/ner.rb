require 'tempfile'

=begin rdoc
  +NamedEntityRecognizer+ provides named entity recognition using the Stanford NER package.  
  The class defines constants +PERSON+, +LOCATION+, and +ORGANIZATION+ to represent tags.
=end
class NamedEntityRecognizer
  attr_reader :entities
  
  PERSON = "PERSON"
  LOCATION = "LOCATION"
  ORGANIZATION = "ORGANIZATION"
  OTHER = "O"
  DEFAULT_NER_PATH = File.expand_path(File.dirname(__FILE__) + '/../vendor/stanford-ner/ner.sh')
  
=begin rdoc
  Initalize with an optional path to an external NER tool.
=end
  def initialize(ner_path = nil)
    ner_path ||= DEFAULT_NER_PATH
    @ner_directory = File.dirname(ner_path)
    @ner_file = File.basename(ner_path)
    @entities = {}
  end
  
=begin rdoc
  Perform named entity recognition on the provided text.  Returns a hash mapping
  strings to named entity tags.
=end
  def recognize(text)
    tempfile = Tempfile.new("ner_input")
    tempfile.write(text)
    tempfile.close()

    Dir.chdir(@ner_directory)
    result = `./#{@ner_file} #{tempfile.path} 2>/dev/null`
    
    last_tag = OTHER
    named_entity = ""
    @entities = {}
    result.split(" ").each do |token|
      (word, tag) = token.match(/(\S+)\/(\w+)/).captures

      if tag == OTHER and last_tag != OTHER
        @entities[named_entity] = last_tag
        named_entity = word
      elsif tag != OTHER
        if tag == last_tag
          named_entity << (" " + word)        
        else
          named_entity = word
        end
      end
            
      last_tag = tag
    end
    
    if last_tag != OTHER
      @entities[named_entity] = last_tag
    end
    
    return @entities
  end

=begin rdoc
  Returns an array of entities tagged as +PERSON+
=end  
  def people
    @entities.keys.delete_if{|k| @entities[k] != PERSON}
  end

=begin rdoc
  Returns an array of entities tagged as +LOCATION+
=end    
  def locations
    @entities.keys.delete_if{|k| @entities[k] != PERSON}
  end

=begin rdoc
  Returns an array of entities tagged as +ORGANIZATION+
=end    
  def organizations
    @entities.keys.delete_if{|k| @entities[k] != PERSON}
  end
  
=begin rdoc
  Returns an array of entities tagged as +OTHER+
=end    
  def others
    @entities.keys.delete_if{|k| @entities[k] != OTHER}
  end

#:nodoc  
  def to_s
    return @entities
  end
end