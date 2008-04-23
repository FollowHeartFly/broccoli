require 'rubygems'
require 'hpricot'
require 'open-uri'
require 'wikipedia_sanitizer'

module PresidentScraper
  include WikipediaSanitizer
  
  ANIMALS = ['Alligator', 'Alpaca', 'American Bison', 'Angelfish', 'Animal', 'Ant', 'Anteater', 'Antelope', 'Ape', 'Armadillo', 'Baboon', 'Bacon', 'Badger', 'Bat', 'Bear', 'Beaver', 'Bee', 'Beef', 'Beetle', 'Bird', 'Bison', 'Bittern', 'Black panther', 'Bluebird', 'Bos', 'Bullock', 'Bunting', 'Butterfly', 'Buzzard', 'Calf', 'Camel', 'Capon', 'Cassowary', 'Castration', 'Cat', 'Cattle', 'Chamois', 'Cheetah', 'Chicken', 'Chimpanzee', 'Clam', 'Cobra', 'Cock', 'Cockroach', 'Cormorant', 'Cougar', 'Coyote', 'Crab', 'Crocodile', 'Crow', 'Deer', 'Dog', 'Dolphin', 'Donkey', 'Dove', 'Duck', 'Dust Mite', 'Eagle', 'Elephant', 'Elk', 'Emu', 'Escargot', 'Falcon', 'Female', 'Fennec', 'Ferret', 'Filly', 'Finch', 'Fish', 'Flamingo', 'Flea', 'Flying fox', 'Fox', 'Frog', 'Gazelle', 'Gelding', 'Gerbil', 'Gib', 'Gibbon', 'Gilt', 'Giraffe', 'Goat', 'Goldfinch', 'Goose', 'Gorilla', 'Guinea pig', 'Gull', 'Ham', 'Hamster', 'Hare', 'Hawk', 'Hedgehog', 'Heifer', 'Heron', 'Hippopotamus', 'Hornet', 'Horse', 'Hummingbird', 'Hyena', 'Insect', 'Jackal', 'Jaguar', 'Jay', 'Jellyfish', 'Kangaroo', 'Kitten', 'Ladybug', 'Lark', 'Lemur', 'Leopard', 'Lion', 'Lizard', 'Llama', 'Lobster', 'Louse', 'Magpie', 'Mallard', 'Manatee', 'Meerkat', 'Mink', 'Minnows', 'Mongoose', 'Monkey', 'Moose', 'Mosquito', 'Mouse', 'Mule', 'Nightingale', 'Opossum', 'Ostrich', 'Otter', 'Owl', 'Ox', 'Oyster', 'Panda', 'Parrot', 'Partridge', 'Peafowl', 'Pelican', 'Penguin', 'Pheasant', 'Pig', 'Pigeon', 'Pinniped', 'Platypus', 'Plover', 'Polar bear', 'Polecat', 'Pony', 'Porcupine', 'Porpoise', 'Poultry', 'Profanity', 'Pythonidae', 'Quail', 'Rabbit', 'Raccoon', 'Rallidae', 'Rat', 'Raven', 'Reindeer', 'Reptile', 'Rhinoceros', 'Rooster', 'Salamander', 'Salmon', 'Scorpion', 'Sea horse', 'Sea lion', 'Sea urchin', 'Shark', 'Sheep', 'Skunk', 'Skylark', 'Slug', 'Smelt', 'Snail', 'Snake', 'Sparrow', 'Species', 'Spider', 'Squirrel', 'Steers', 'Stork', 'Swallow', 'Swan', 'Swift', 'Tapir', 'Tiger', 'Toad', 'Turdidae', 'Turtle', 'Veal', 'Venison', 'Weasel', 'Wether', 'Whale', 'Wildfowl', 'Wolf', 'Wombat', 'Worm', 'Wren', 'Yak', 'Zebra']

  class << self
    def run
      ANIMALS.each_with_index do |animal, i|
        filename = "%03d_%s.txt" % [i + 1, animal.gsub(/\W/, "_").squeeze("_")]
        File.open(File.expand_path(File.dirname(__FILE__) + '/../tuning/animals/' + filename), 'w') do |f|
          f.write(scrape(animal))
        end
        puts "- #{filename}"
        sleep 5 # Diminish server load
      end
    end
  
    def scrape(animal)
      url = "http://en.wikipedia.org/wiki/#{animal.gsub(/\s/, '_')}"
      doc = Hpricot(open(url))
      return WikipediaSanitizer.sanitize!(doc)
    end
  end
end

PresidentScraper.run