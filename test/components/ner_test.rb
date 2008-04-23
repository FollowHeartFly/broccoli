require File.dirname(__FILE__) + '/../test_helper'
require 'ner'

class NamedEntityRecognizerTest < Test::Unit::TestCase
  def setup
    @ner = NamedEntityRecognizer.new
  end
  
  def test_should_tag_basic_input
    assert_nothing_thrown do
      @ner.recognize("My name is Zach Paine.  I live in Pittsburgh.  I attend Carnegie Mellon University")
    end
    pp @ner
    
    assert_not_nil(@ner.entities)
    assert_instance_of(Hash, @ner.entities)
    
    assert_equal(NamedEntityRecognizer::PERSON, @ner.entities["Zach Paine"])
    assert_equal(NamedEntityRecognizer::LOCATION, @ner.entities["Pittsburgh"])
    assert_equal(NamedEntityRecognizer::ORGANIZATION, @ner.entities["Carnegie Mellon University"])
  end
  
  def test_should_recognize_people
    @ner.recognize("Barack Obama, Thomas Jefferson, and Morgan Freeman appreciate the work of Andy Warhol")
    pp @ner
    
    ["Barack Obama", "Thomas Jefferson", "Morgan Freeman", "Andy Warhol"].each do |person|
      assert @ner.people.include?(person), "#{person} was not recognized as a person"
    end
  end
  
  def test_should_recognize_locations
    @ner.recognize("Paris, capital city of France, and one of the largest metropolitan areas in Europe, 
                    is situated on the River Seine. Incidentally, it's 8977 km from San Francisco, California")
    pp @ner
    
    ["Paris", "France", "Europe", "River Seine", "San Francisco", "California"].each do |location|
      assert @ner.locations.include?(location), "#{location} was not recognized as a location"
    end
  end
  
  def test_should_recognize_organizations
    @ner.recognize("Internet sites like Something Awful have long spoken our against the Church of Scientology,
                    PETA, NAMBLA, Microsoft, and the Westboro Baptist Church")              
    pp @ner
    
    ["Something Awful", "Church of Scientology", "PETA", "NAMBLA", 
     "Microsoft", "Westboro Baptist Church"].each do |organization|
      assert @ner.organizations.include?(organization), "#{organization} was not recognized as a organization"
    end
  end
end