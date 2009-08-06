#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'sling/contacts'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingContacts

class TC_Kern289Test < SlingTest

  def test_connection_details
    m = Time.now.to_i.to_s
    u1 = create_user("testuser#{m}")
    u2 = create_user("otheruser#{m}")
    cm = ContactManager.new(@s)
    @s.switch_user(u1)
    cm.invite_contact(u2.name, "follower")
    contacts = @s.get_node_props("/_user/contacts/all")
    types = contacts["results"][0]["details"]["sakai:types"]
    assert_not_nil(types, "Expected type to be stored")
    assert_equal(types, "follower", "Expected type to be 'follower'")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern289Test)

