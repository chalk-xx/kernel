#!/usr/bin/env ruby

module SlingMessage

  class MessageManager

    def initialize(sling)
      @sling = sling
    end

    def create(name, type, box = "drafts")
      return @sling.execute_post(@sling.url_for("_user/message.create.html"), "sakai:type" => type, "sakai:to" => name, "sakai:sendstate" => "pending", "sakai:messagebox" => box )
    end
 
    def send(messageId)
      return @sling.execute_post(@sling.url_for("_user/message/#{messageId}.html"), "sakai:messagebox" => "outbox" )
    end

    def list_all_noopts()
      return @sling.execute_get(@sling.url_for("_user/message/all.json"))
    end

    def list_all(sortOn = "jcr:created", sortOrder = "descending" )
      return @sling.execute_get(@sling.url_for("_user/message/all.json?sortOn="+sortOn+"&sortOrder="+sortOrder))
    end

    def list_inbox(sortOn = "jcr:created", sortOrder = "descending" )
      return @sling.execute_get(@sling.url_for("_user/message/box.json?box=inbox&sortOn="+sortOn+"&sortOrder="+sortOrder))
    end

    def list_outbox(sortOn = "jcr:created", sortOrder = "descending" )
      return @sling.execute_get(@sling.url_for("_user/message/box.json?box=outbox&sortOn="+sortOn+"&sortOrder="+sortOrder))
    end
	
    
  end

end
