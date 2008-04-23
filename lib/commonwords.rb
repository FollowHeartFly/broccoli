
class CommonWords
  
  DETERMINERS = ['a', 'an', 'the', 'this', 'that', 'these', 'those']
  AUXILIARIES = ['be', 'am', 'is', 'are', 'were', 'was', 'been',
                 'do', 'does', 'did']
  WHWORDS = ['what', 'which', 'how', 'when', 'where', 'why', 'who']
  MODALS = ['will', 'can', 'may', 'might', 'would', 'should']
  PREPOSITIONS = ['of', 'in', 'for', 'with', 'at', 'from', 'to', 'as',
    'within', 'between', 'there', 'on', 'by']
  OTHERS = ['and', 'or', 'it', 'many', 'much']

  PUNCTUATION = ['?', '!', ',', '.', "'"]

  COMMONWORDS = [DETERMINERS, AUXILIARIES, WHWORDS, MODALS, PREPOSITIONS,
    OTHERS]

  def self.strip_common_words(s)
    COMMONWORDS.each do |category|
      category.each do |word|
        s.gsub!(/\b#{word}\b/i, '')
      end
    end
    
    return s
  end
  
  def self.strip_trailing_punctuation(s)
    PUNCTUATION.each do |symb|
      s.chop! if s[-1] == symb[0]
    end
    
    s
  end
  
end
