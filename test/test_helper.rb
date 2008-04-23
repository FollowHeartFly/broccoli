$: << File.join(File.dirname(__FILE__), '../lib')
$:.concat(Dir.glob(File.join(File.dirname(__FILE__), '../vendor/*/lib')))

require 'test/unit'
require 'pp'
require 'logger'

logdir = File.join(File.dirname(__FILE__), '../log')
Dir.mkdir(logdir) unless File.directory?(logdir)
$logger = Logger.new(File.join(File.dirname(__FILE__), '../log/' +
  Time.now.strftime("ask_%m%d%Y%H%M") + ".log"))
  
$VERBOSE = false

class Test::Unit::TestCase
  
  def ask(article, n = 1)
    IO.popen(['./bin/ask', article, n].join(' ')).readlines
  end
  
  def answer(questions, article = nil)
    article ||= questions.gsub(/q(\d+)\.txt/, 'a\1.txt')
    IO.popen(['./bin/answer', article, questions].join(' ')).readlines
  end
  
  def article(category, file = nil)
    path    = File.join("../", "../", "data", "training", category.to_s)
    entries = Dir.entries(path).select{|f| f =~ /a\d+\.txt/}
    if entries.empty?
      raise "No articles for category #{category}" 
    else
      return File.open(File.join(path, file || entries.first)).read
    end
  end
  
  def question(category, file = nil)
    path    = File.join("../", "../", "data", "training", category.to_s)
    entries = Dir.entries(path).select{|f| f =~ /q\d+\.txt/}
    if entries.empty?
      raise "No questions for category #{category}" 
    else
      return File.open(File.join(path, entries.first)).read
    end
  end
end