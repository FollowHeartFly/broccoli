require 'commonwords'
require 'stemmer'
require 'article_preprocessor'
require 'inflector'
require 'inflections'

module EasyAnswer

  def self.answer(question, text)
    # Strip common words out of the question and split it into tokens on
    # whitespace. This should leave us with a list of the key words in the
    # question. See commonwords.rb for what these words are.
    question = CommonWords.strip_common_words(question)
    qtokens = question.strip.split(/\s+/)
    qtokens.map! {|tok| CommonWords.strip_trailing_punctuation(tok)}
    qtokens = qtokens.join(' ').stem.split(' ')
    
    sentences = ArticlePreprocessor.sentences(text)
    
    # Prepare a stemmed version of the article and match up the normal
    # sentences and the stemmed ones. We need to search the stemmed ones for
    # key words, but return the normal ones as answers.
    stemmed_text = sentences.join("\n").stem
    stemmed_sentences = stemmed_text.split(/\n/)
    
    # The article title will probably appear in a lot of sentences and in the
    # question, and it's not relevant to finding the answer.
    title = stemmed_sentences[0].split('_')
    title.each {|word| qtokens.delete(word)}
    $logger.debug "KEYWORDS: " + qtokens.join(' ')
    
    # Iterate through the sentences, and count how many of the keywords we
    # extracted from the question appear in each one. We take a sentence with
    # the maximum number of keywords, and return it as the answer.
    bestmatch = 0
    bestindices = []
    stemmed_sentences.each_with_index do |stemmed, i|
      match = 0
      qtokens.each do |token|
        # This isn't the cleanest. If the token has length one, it's most
        # likely "a", "I" or a bare punctuation mark (this can result from the
        # common-word stripping, for example if the question ends with "what
        # was _ part of?") so we're not interested.
        if token.length > 1
          match += 1 if stemmed =~ /\b#{token}\b/i
        end
      end
      if match == bestmatch
        bestindices << i
      end
      if match > bestmatch
        bestindices = [i]
        bestmatch = match
      end
    end
    
    # If the best-matching sentences matched less than half of the keywords
    # from the question, then we determine that this answering strategy didn't
    # work. Whoever calls this code is going to have to use some other
    # strategy.
    #  
    # If we found a good match, return the first-occurring matching sentence.
    # This is actually a good heuristic; sentences towards the beginning of
    # the article are more likely to be introductory and thus be factoids.
    $logger.debug "QUALITY:  #{bestmatch.to_i}/#{qtokens.length.to_i}"
    if bestmatch < (qtokens.length / 2.0)
      return nil
    else
      answer = sentences[bestindices[0]] + '.'
    end
    
    # Some heuristics to deal with pronouns. If the first word is "it", the
    # referent must be in a previous sentence; include the previous sentence
    # in the answer. If it's "he", "she" or "they", substitute in the title of
    # the article, pluralizing it if it's "they". (Of course the latter
    # heuristic doesn't always work perfectly, but it's right quite often.)
    humantitle = sentences[0].gsub('_', ' ')
    if answer =~ /^(he|she) /i
      answer[/^(he|she) /i] = humantitle + ' '
    elsif answer =~ /^they /i
      answer[/^they /i] = Inflector.pluralize(humantitle) + ' '
    elsif answer =~ /^its? /i
      answer = sentences[bestindices[0] - 1] + '. ' + answer
    end
    
    # Make sure it's capitalized properly.
    answer[0] = answer[0].chr.capitalize
    answer
  end
  
end
