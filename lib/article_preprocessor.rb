=begin rdoc
  Simple library for preprocessing Wikipedia articles.
=end
module ArticlePreprocessor
    
=begin rdoc
  Perform text sanitization including:
  * Removing parentheses and their content
  * Removing abbreviations that end with a period
  * Removing the related articles section
  * Removing bulleted lists
  * Removing image tags
  * Removing disambiguation links
=end
  def self.sanitize(text)
    # Parenthetical asides and references make things nasty.  Goodbye.
    text.gsub!(/\([^\)]*\)/, "")

    # Truncate the article when encounter the related articles list
    text.gsub!(/Related Wikipedia Articles.*/m, "")
    
    # Delete bulleted lists (not sure if this is a good idea, but it gets rid of the citations list)
    # text.gsub!(/\*[^\n]*\n/, "")
    
    # Remove images
    text.gsub!(/Image:.*$/, "")
    
    # Remove disambiguation links
    text.gsub!(/^:.*$/, "")
    
    text
  end
  
  def self.sentences(text)
    # Get rid of periods that don't seem to end a sentence. Then split text
    # into sentences on each period, question mark or exclamation point
    # followed by whitespace.
    #
    # Some articles (such as the kangaroo article) have a lot of sentences like
    # "Australia's coat of arms URL accessed January 6, 2007." denoting an image or link (I think)
    # Get rid of those.
    
    text.gsub!(/(\s[A-Za-z]{1,2})\.(\s)/, '\1\2')
    text.gsub!(/(\.|\?|!)\s+/, "\n")
    text.split(/\n/).reject { |sentence| sentence =~ /URL accessed/ }        
  end
  
  
end