require 'singleton'
require 'yaml'
require 'classifier'

#:nodoc
module Classifier
  # Monkey-patch Bayesian Classifier to support cacheing
  class Bayes
    def dump(filepath)
      File.open(filepath, 'w'){|yf| YAML.dump({:categories  => @categories, :total_words => @total_words}, yf)}
    end
  
    def load(filepath)
      hash = File.open(filepath, 'r'){|yf| YAML.load(yf)}
      @categories = hash[:categories]
      @total_words = hash[:total_words]
    end
  
    def categories=(hash)
      @categories = hash
    end
  
    def total_words=(n)
      @total_words = n
    end
  end
end

class ArticleClassifier
  include Singleton
  
  attr_reader :classifier
  
  DEFAULT_CORPUS_PATH = File.expand_path(File.dirname(__FILE__) + '/../data/tuning')
  DEFAULT_CACHE_PATH  = File.expand_path(File.dirname(__FILE__) + '/../data/classifier.yml')
  
  ANIMAL    = 'animals'
  COUNTRY   = 'countries'
  PRESIDENT = 'presidents'
  
  CLASSIFICATIONS = [ANIMAL, COUNTRY, PRESIDENT]
  
  def initialize(reload = false)
    @classifier = Classifier::Bayes.new(*CLASSIFICATIONS)
    if File.exists?(DEFAULT_CACHE_PATH) && ! reload
      @classifier.load(DEFAULT_CACHE_PATH)
    else
      reload!
    end
  end
  
  def reload!
    CLASSIFICATIONS.each do |classification|
      Dir[DEFAULT_CORPUS_PATH + "/**/" + classification + "/*.txt"].each do |file|
        @classifier.send("train_" + classification, File.open(file).read)
      end
    end
    @classifier.dump(DEFAULT_CACHE_PATH)
  end
  
  def method_missing(name, *args)
    @classifier.send(name, *args)
  end
end