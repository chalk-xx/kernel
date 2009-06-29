#!/usr/bin/env ruby

module SlingContacts

  class ContactManager

    def initialize(sling)
      @sling = sling
    end

    def add_contact(name, types)
      return @sling.execute_post(@sling.url_for("_user/contacts/#{name}.invite.html"), "type" => types)
    end
 
    def remove_contact(name)
      return @sling.execute_post(@sling.url_for("_user/contacts/#{name}.remove.html"), {})
    end

    def get_contacts()
      return @sling.get_node_props("_user/contacts/accepted.json")
    end

    def get_pending()
      return @sling.get_node_props("_user/contacts/pending.json")
    end
  end

end
