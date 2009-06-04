#!/usr/bin/env ruby

module SlingSites

  class SiteManager

    def initialize(sling)
      @sling = sling
    end

    def create_site(path)
      return @sling.create_node(path, { "sling:resourceType" => "sakai/site" })      
    end

  end

end
