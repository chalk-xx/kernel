#!/usr/bin/env ruby

module SlingSearch

  $SEARCH = "var/search/"
  $SEARCH_FILES = $SEARCH + "content"

  class SearchManager

    def initialize(sling)
      @sling = sling
    end

    def search_for(string)
       return JSON.parse(@sling.execute_get(@sling.url_for($SEARCH_FILES) + ".json?q=#{string}").body)
    end

    def create_search_template(name, language, template)
      return @sling.create_node("#{$SEARCH}#{name}", "sakai:query-language" => language, "sakai:query-template" => template, "sling:resourceType" => "sakai/search") 
    end
  end

end
