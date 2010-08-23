#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
include SlingSearch
include SlingUsers

class TC_UserManagerTest < Test::Unit::TestCase
  include SlingTest

  def test_create_user
    m = Time.now.to_i.to_s
    u = create_user("testuser"+m)
    details = @um.get_user_props(u.name)
    assert_equal("testuser"+m, details["rep:principalName"], "Expected username to match")
  end

  def test_create_group
    m = Time.now.to_i.to_s
    g = create_group("g-testgroup"+m)
    assert_not_nil(g,"Failed to create a group")
    assert_not_nil(g.name,"Failed to create a group, no name")
    details = @um.get_group_props(g.name)
    assert_equal("g-testgroup"+m, details["properties"]["rep:principalName"], "Expected groupname to match")
  end

  def test_group_deletion
    m = Time.now.to_i.to_s
    g = @um.create_group("g-testgroup"+m)
    assert_not_nil(g,"Failed to create a group")
    assert_not_nil(g.name,"Failed to create a group, no name")
    details = @um.get_group_props(g.name)
    assert_equal("g-testgroup"+m, details["properties"]["rep:principalName"], "Expected groupname to match")
    @um.delete_group(g.name)
    res = @s.execute_get(@s.url_for(Group.url_for(g.name + ".json")))
    assert_equal("404", res.code, "Expected no group node")
  end
  
  def test_create_email_username
    m = Time.now.to_i.to_s
    u = create_user("testuser@gmail.com"+m)
	details = @um.get_user_props(u.name)
    assert_equal("testuser@gmail.com"+m, details["rep:principalName"], "Expected username to match")
  end

end


