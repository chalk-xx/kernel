#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

class TC_UserManagerTest < SlingTest

  def test_create_user
    u = create_user("testuser")
    details = @um.get_user_props(u.name)
    assert_equal("testuser", details["rep:principalName"], "Expected username to match")
  end

  def test_create_group
    g = create_group("testgroup")
    details = @um.get_group_props(g.name)
    assert_equal("testgroup", details["properties"]["rep:principalName"], "Expected groupname to match")
  end

  def test_group_deletion
    g = @um.create_group("testgroup")
    details = @um.get_group_props(g.name)
    assert_equal("testgroup", details["properties"]["rep:principalName"], "Expected groupname to match")
    profile = details["profile"]
    props = @s.get_node_props(profile[1..-1])
    assert_not_nil(props, "Expected group profile")
    @um.delete_group(g.name)
    res = @s.execute_get(@s.url_for(profile[1..-1] + ".json"))
    assert_equal("404", res.code, "Expected no group profile")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_UserManagerTest)

