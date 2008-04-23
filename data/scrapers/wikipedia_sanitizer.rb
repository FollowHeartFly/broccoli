module WikipediaSanitizer
  class << self
    def sanitize!(doc)
      ["table", "script", "ul", "ol", "dl", "#contentSub" "#siteSub", "div#siteSub", "div#jump-to-nav", "div.dablink",
        "div.catlinks", "div.printfooter", ".noprint", ".infobox", ".editsection"].each do |e| 
        doc.search(e).remove
      end
      content = (doc/"div#bodyContent").first.inner_text
      paragraphs = content.split(/\n/).collect{|p| p.strip}.delete_if{|p| p.length < 30}.join("\n\n")
      return paragraphs.gsub(/\[\d+\]/,"").gsub("From Wikipedia, the free encyclopedia", "").strip
    end
  end
end