#!/usr/bin/env ruby

module SlingMessage

  class MessageManager

    def initialize(sling)
      @sling = sling
    end

    def create(name, types)
      return @sling.execute_post(@sling.url_for("_user/private/message.create.html"), "sakai:type" => type, "sakai:to" => name)
    end
 
    def accept_contact(name)
      return @sling.execute_post(@sling.url_for("_user/contacts/#{name}.accept.html"), {})
    end

    def reject_contact(name)
      return @sling.execute_post(@sling.url_for("_user/contacts/#{name}.reject.html"), {})
    end

    def ignore_contact(name)
      return @sling.execute_post(@sling.url_for("_user/contacts/#{name}.ignore.html"), {})
    end

    def block_contact(name)
      return @sling.execute_post(@sling.url_for("_user/contacts/#{name}.block.html"), {})
    end

    def remove_contact(name)
      return @sling.execute_post(@sling.url_for("_user/contacts/#{name}.remove.html"), {})
    end


    def get_accepted()
      return @sling.get_node_props("_user/contacts/accepted.json")
    end

    def get_pending()
      return @sling.get_node_props("_user/contacts/pending.json")
    end

    def get_invited()
      return @sling.get_node_props("_user/contacts/invited.json")
    end

    def get_blocked()
      return @sling.get_node_props("_user/contacts/blocked.json")
    end

    def get_ignored()
      return @sling.get_node_props("_user/contacts/ignored.json")
    end


    def get_all()
      return @sling.get_node_props("_user/contacts/all.json")
    end
    
  end

end
