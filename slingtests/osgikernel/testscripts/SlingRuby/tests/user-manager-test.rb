#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch
include SlingUsers

class TC_UserManagerTest < SlingTest

  def test_create_user
    u = create_user("testuser")
    details = @um.get_user_props(u.name)
    assert_equal("testuser", details["rep:principalName"], "Expected username to match")
  end

  def test_create_group
    g = create_group("g-testgroup")
    details = @um.get_group_props(g.name)
    assert_equal("g-testgroup", details["properties"]["rep:principalName"], "Expected groupname to match")
  end

  def test_group_deletion
    g = @um.create_group("g-testgroup")
    details = @um.get_group_props(g.name)
    assert_equal("g-testgroup", details["properties"]["rep:principalName"], "Expected groupname to match")
    profile = details["profile"]
    props = @s.get_node_props(profile)
    assert_not_nil(props, "Expected group profile")
    @um.delete_group(g.name)
    res = @s.execute_get(@s.url_for(Group.url_for(g.name + ".json")))
    assert_equal("404", res.code, "Expected no group node")
  end

  def test_invalid_group_create
    g = @um.create_group("testgroup")
    assert_nil(g, "Expected group not to be created")
  end
  
  def test_invalid_user_create
    u = @um.create_user("g-testuser")
    assert_nil(u, "Expected user not to be created")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_UserManagerTest)

