#!/usr/bin/env ruby

require 'sling/sling'
require 'sling/test'
require 'sling/contacts'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingSites
include SlingContacts

class TC_MyContactTest < SlingTest

  def setup
    super
    @cm = ContactManager.new(@s)
  end

  def test_connect_users
    m = Time.now.to_i.to_s
    a = create_user("aaron"+m)
    n = create_user("nico"+m)
    i = create_user("ian"+m)
    @s.switch_user(a)
    res = @cm.add_contact("nico"+m, [ "coworker", "friend" ])
    assert_equal("200", res.code, "Expected to be able to request contact addition")
    @s.debug = true
    contacts = @cm.get_contacts()
    @s.debug = false
    assert_not_nil(contacts, "Expected to get contacts back")
    assert_equal(contacts["results"].size, 1, "Expected single request back")
    contact = contacts["results"][0]
    assert_equal("nico"+m, contact["target"], "Expected nico to be my friend")
    assert_equal("requested", contact["details"]["sakai:state"], "Expected state to be 'requested'")
  end

  def teardown
    @created_users.each do |user|
      @s.debug = true
      @s.switch_user(user)
      contacts = @cm.get_contacts()
      contacts["results"].each do |result|
        assert_not_nil(result["target"], "Expected contacts to have names")
        res = @cm.remove_contact(result["target"])
        assert_equal("200", res.code, "Expected removal to succeed")
      end
      @s.debug = false
    end
    super
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_MyContactTest)

