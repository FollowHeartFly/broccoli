require 'rubygems'
require 'hpricot'
require 'open-uri'
require 'wikipedia_sanitizer'

module PresidentScraper
  include WikipediaSanitizer

  PRESIDENTS = ['Abraham Lincoln', 'Andrew Jackson', 'Andrew Johnson', 'Benjamin Harrison', 'Bill Clinton', 'Calvin Coolidge', 'Chester A. Arthur', 'Dwight D. Eisenhower', 'Franklin D. Roosevelt', 'Franklin Pierce', 'George H. W. Bush', 'George W. Bush', 'George Washington', 'Gerald Ford', 'Grover Cleveland', 'Harry S. Truman', 'Herbert Hoover', 'James A. Garfield', 'James Buchanan', 'James K. Polk', 'James Madison', 'James Monroe', 'Jimmy Carter', 'John Adams', 'John F. Kennedy', 'John Quincy Adams', 'John Tyler', 'Lyndon B. Johnson', 'Martin Van Buren', 'Millard Fillmore', 'Richard Nixon', 'Ronald Reagan', 'Rutherford B. Hayes', 'Theodore Roosevelt', 'Thomas Jefferson', 'Ulysses S. Grant', 'Warren G. Harding', 'William Henry Harrison', 'William Howard Taft', 'William McKinley', 'Woodrow Wilson', 'Zachary Taylor']
  
  class << self
    def run
      PRESIDENTS.each_with_index do |president, i|
        filename = "%02d_%s.txt" % [i + 1, president.gsub(/\W/, "_").squeeze("_")]
        File.open(File.expand_path(File.dirname(__FILE__) + '/../tuning/presidents/' + filename), 'w') do |f|
          f.write(scrape(president))
        end
        puts "- #{filename}"
        sleep 5 # Diminish server load
      end
    end
  
    def scrape(president)
      url = "http://en.wikipedia.org/wiki/#{president.gsub(/\s/, '_')}"
      doc = Hpricot(open(url))
      return WikipediaSanitizer.sanitize!(doc)
    end
  end
end

PresidentScraper.run