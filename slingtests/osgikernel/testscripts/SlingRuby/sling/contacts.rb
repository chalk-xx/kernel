#!/usr/bin/env ruby

module SlingContacts

  class ContactManager

    def initialize(sling)
      @sling = sling
    end

    def add_contact(name, types)
      return @sling.execute_post(@sling.url_for("_user/contacts.request.html"), "type" => types, "contact" => name)
    end
 
    def remove_contact(name)
      return @sling.execute_post(@sling.url_for("_user/contacts/#{name}.remove.html"), {})
    end

    def get_contacts()
      return @sling.get_node_props("_user/user_contacts")
    end

  end

end
