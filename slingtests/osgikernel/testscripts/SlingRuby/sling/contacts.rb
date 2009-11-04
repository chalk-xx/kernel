#!/usr/bin/env ruby

module SlingContacts

  class ContactManager

    def initialize(sling)
      @sling = sling
    end

    def invite_contact(name, sharedRelationships, fromRelationships=[], toRelationships=[])
      case sharedRelationships
        when String
        sharedRelationships = [sharedRelationships]
      end
      return @sling.execute_post(@sling.url_for("_user/contacts/#{name}.invite.html"), "sakai:types" => sharedRelationships,
        "fromRelationships" => fromRelationships, "toRelationships" => toRelationships)
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

    def cancel_invitation(name)
      return @sling.execute_post(@sling.url_for("_user/contacts/#{name}.cancel.html"))
    end


    def get_accepted()
      return @sling.get_node_props("_user/contacts/accepted")
    end

    def get_pending()
      return @sling.get_node_props("_user/contacts/pending")
    end

    def get_invited()
      return @sling.get_node_props("_user/contacts/invited")
    end

    def get_blocked()
      return @sling.get_node_props("_user/contacts/blocked")
    end

    def get_ignored()
      return @sling.get_node_props("_user/contacts/ignored")
    end


    def get_all()
      return @sling.get_node_props("_user/contacts/all")
    end
    
  end

end
