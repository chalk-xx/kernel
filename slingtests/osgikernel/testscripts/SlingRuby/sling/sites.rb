#!/usr/bin/env ruby

module SlingSites

  class Site

    def initialize(sling, path)
      @sling = sling
      @path = path
    end

    def add_group(groupname)
      auths = Site.get_groups(@path, @sling)
      auths << groupname
      @sling.execute_post(@sling.url_for(@path), "sakai:authorizables" => auths)
    end

    def set_joinable(joinable)
      return @sling.execute_post(@sling.url_for(@path), "sakai:joinable" => joinable)
    end
  
    def join(groupname)
      return @sling.execute_post("#{@sling.url_for(@path)}.join.html", "targetGroup" => groupname)
    end

    def self.get_groups(path, sling)
      props = sling.get_node_props(path)
      groups = props["sakai:authorizables"]
      if (groups == nil)
        return []
      else
        case groups
        when String
          return [groups]
        end
        return groups
      end
    end
  
    def get_members
      return @sling.get_node_props("#{@path}.members")
    end

  end

  class SiteManager

    def initialize(sling)
      @sling = sling
    end

    def create_site(path)
      res = @sling.create_node(path, { "sling:resourceType" => "sakai/site" })
      if (res.code != "201")
        puts "Unable to create site: #{res.code}"
        return nil
      end
      return Site.new(@sling, path)
    end

    def delete_site(path)
      return @sling.delete_node(path)
    end

  end

end
