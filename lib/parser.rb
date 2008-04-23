require 'tempfile'
require 'pp'
require 'singleton'

=begin rdoc
  +Parser+ provides phrase structure parser via the Stanford Parser.
  
  Author: Zach Paine
=end
class Parser
  include Singleton
  
  attr_writer :silence_errors

  DEFAULT_PARSER_PATH = File.expand_path(File.dirname(__FILE__) + '/../vendor/stanford-parser/lexparser.csh')
  ROOT_SYMBOL = "ROOT"
  
  
=begin rdoc
  Initialize with an optional path to a parser.
=end
  def initialize(parser_path = nil)
    parser_path ||= DEFAULT_PARSER_PATH
    @parser_directory = File.dirname(parser_path)
    @parser_cmd = File.basename(parser_path)
    Dir.chdir(@parser_directory)
    @parser = IO.popen("./#{@parser_cmd} -stream 2>/dev/null", 'r+')
    @silence_errors = false
  end
    
=begin rdoc
  Parse a string.  Returns a +ParseNode+.  Raises +ParseError+ if the sentence cannot be parsed.
=end
  def parse(text)
    @parser.write(text.chomp + "\n")
    lines = []
    while @parser.gets
      lines << $_
      break if $_.length == 1
    end
    
    # Hack: can't be bothered to modify build_trees to know there's only one
    return build_trees(lines.join)[0]
  end
  
private

  # These parsing methods are based on the Assignment 8 handout.

  def build_trees(text)
    trees = []
    buf = ""

    lines = text.split(/\n/)

    lines.each_with_index do |line, i|      
      if line =~ /^Sentence skipped|SENTENCE_SKIPPED/
        next if @silence_errors
        raise ParseError, "Unable to parse text #{line}"
      end

      if line =~ /^\(#{ROOT_SYMBOL}/ 
        if !buf.empty?
          buf.gsub!(/\n/, " ")
          tree, rest = recursive_descent(buf)
          trees << tree
        end
        buf = line
      else
        buf << line
      end
    end

    # Ugh.  Fencepost.
    if !buf.empty?
      buf.gsub!(/\n/, " ")
      tree, rest = recursive_descent(buf)
      trees << tree
    end    
        
    trees
  end
  
  def recursive_descent(line)
    label, z = /^\(([^\(\)\ ]*)\s+(.*)$/.match(line).captures
    
    root = ParseNode.new(label)
    
    while z =~ /^\(/
      child, z = recursive_descent(z)
      root << child
    end

    if z =~ /^\)\s*(.*)$/ # eat the right paren
      z = $1
    else # preterminal
      term, z = /^([^\)]+)\)\s*(.*)$/.match(z).captures
      root << ParseNode.new(term)
    end
        
    return root, z
  end
  
end

=begin rdoc
  +ParseNode+ represents a node in a parse tree.
=end
class ParseNode
  include Enumerable

  attr_reader :children
  attr_reader :label

=begin rdoc
  call-seq:
    Array.new(label, children = [])
    Array.new(hash)

  In the first form, +label+ is a string and +children+ is an optional initial list of child nodes.

  The second form allows for an entire node to be constructed at once.  The hash should have a key +:label+, which is the label of the root node.  
  Optionall, it may have a key called +:children+ which is an array of similarly formatted hashes.
=end
  def initialize(*args)
    if args.size == 2
      @label = args[0]
      @children = args[1]
    elsif args.size == 1
      if args[0].class == Hash
        h = args[0]
        @label = h[:label]
        @children = []
        if h.has_key?(:children)
          h[:children].each do |child_hash|
            @children << ParseNode.new(child_hash)
          end
        end
      else
        @label = args[0]
        @children = []
      end      
    else
      raise "Invalid arguments to ParseNode initializer."
    end
  end

=begin rdoc
  call-seq:
    node << child_node  -> node
    
  Append---Pushes the child node on to the end of this node's child array.
=end  
  def <<(node)
    @children << node    
  end

=begin rdoc
  Recover the sentence (fragment) rooted at this node.
=end
  def to_sentence
    if has_children?
      @children.map { |child| child.to_sentence }.join(" ").gsub(/\s([^\s\w])/, '\1') 
    else
      @label
    end
  end

=begin rdoc
  Returns true if this node is not a leaf.
=end  
  def has_children?
    not @children.empty?
  end

  def each(&block)
    yield self
    @children.each do |child|
      child.each(&block)
    end
  end
  
=begin rdoc
  Returns true if this tree contains +another_tree+ as a subtree.
=end
  def contains_tree?(another_tree)
    self.any? do |node|
      node.contains(another_tree)
    end
  end

=begin rdoc
  Extracts the subtree matching +another_tree+ if it exists, nil otherwise.
=end
  def extract_tree(another_tree)
    self.find { |n| n.contains(another_tree) }
  end

    
  def pretty_print(printer)
    printer.group(0, "(", ")") do
      printer.text(@label.to_s)
      unless @children.empty?
        printer.nest(3) do
          @children.each do |child|
            printer.breakable
            child.pretty_print(printer)
          end
        end
      end
    end
  end
  
  def to_s
    @label.to_s
  end

protected
  def contains(other_tree)
    if @label == other_tree.label
      if other_tree.has_children?
        if other_tree.children.length > @children.length
          false
        else
          result = true
          other_tree.children.each_with_index do |node, i|
            result &&= @children[i].contains(node)
          end
          result
        end
      else
        true
      end
    else
      false
    end
  end    
    
end

class ParseError < RuntimeError
end
