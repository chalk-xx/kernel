#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'sling/contacts'
require 'test/unit.rb'
require '../tests/authz-base.rb'
include SlingSearch
include SlingUsers
include SlingContacts


class TC_Kern629Test < Test::Unit::TestCase
  include SlingTest, AuthZBase
  
  # Test Time Based ACLs
  def test_create_timebase_acl_rule
	user = createUser("1")
    node = createNode("1")
    @s.set_node_acl_rule_entries(node, user, {"jcr:read" => "granted"}, {"rule" => "TestingThatRuleWorks"})
  end
  
  def test_activate_timebase_acl_active
	user = createUser("2")
    node = createNode("2")
    @s.set_node_acl_rule_entries(node, user, {"jcr:read" => "granted"}, {"active" => ["20100304/20100404","2010-04-05T01:00:00Z/20100405T020000Z"]})
  end
  
  def test_deactivate_timebase_acl
	user = createUser("3")
    node = createNode("3")
    @s.set_node_acl_rule_entries(node, user, {"jcr:read" => "granted"}, {"inactive" => ["20100304/20100404","20100405T010000Z/20100405T020000Z"]})
  end
  
  def createUser(n) 
    m = Time.now.to_i.to_s
    return create_user("kern-629-user-#{n}-#{m}")
  end
  
  def createNode(n) 
    m = Time.now.to_i.to_s
	path = "/kern-629/testNode-#{n}-#{m}"
    @s.create_node(path , {"foo" => "bar"})
	return path
  end
  
  
end

