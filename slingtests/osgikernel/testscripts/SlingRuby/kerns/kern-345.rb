#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'sling/contacts'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingContacts

class TC_Kern345Test < SlingTest

  def test_asymmetric_relationships
    m = Time.now.to_i.to_s
    u1 = create_user("testuser#{m}")
    u2 = create_user("otheruser#{m}")
    cm = ContactManager.new(@s)
    @s.switch_user(u1)
    cm.invite_contact(u2.name, [], ["follower"], ["leader"])
    check_contact_relationships(cm, "follower")
    @s.switch_user(u2)
    check_contact_relationships(cm, "leader")
  end

  def test_shared_and_asymmetric_relationships
    m = Time.now.to_i.to_s
    u1 = create_user("testuser#{m}")
    u2 = create_user("otheruser#{m}")
    cm = ContactManager.new(@s)
    @s.switch_user(u1)
    cm.invite_contact(u2.name, ["friend", "colleague"], ["follower"], ["leader"])
    check_contact_relationships(cm, "friend", "colleague", "follower")
    @s.switch_user(u2)
    check_contact_relationships(cm, "friend", "colleague", "leader")
  end

  def test_removed_and_revised_relationships
    m = Time.now.to_i.to_s
    u1 = create_user("testuser#{m}")
    u2 = create_user("otheruser#{m}")
    cm = ContactManager.new(@s)
    @s.switch_user(u1)
    cm.invite_contact(u2.name, [], ["teacher"], ["student"])
    @s.switch_user(u2)
    puts "About to accept invitation"
    cm.accept_contact(u1.name)
    puts "About to remove contact"
    cm.remove_contact(u1.name)
    puts "Afterwards..."
    contacts = @s.get_node_props("/_user/contacts/all")
    assert_equal(0, cm.get_all()["results"].length, "Should have removed all contacts")
    @s.switch_user(u1)
    cm.invite_contact(u2.name, ["colleague"])
    @s.switch_user(u2)
    cm.accept_contact(u1.name)
    check_contact_relationships(cm, "colleague")
  end

  def check_contact_relationships(cm, *relationships)
    firstContact = cm.get_all()["results"][0]["details"]
    types = firstContact["sakai:types"]
    assert_not_nil(types, "Expected relationships to be stored")
    assert_equal(relationships.length, types.length, "Should have #{relationships.length} relationships")
    relationships.each {
      |relationship| assert(types.include?(relationship), "Relationships should include '#{relationship}'")
    }
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern345Test)
