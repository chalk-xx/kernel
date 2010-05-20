#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

class TC_MySearchTest < SlingTest

  def setup
    super
    @sm = SearchManager.new(@s)
  end

  def test_node_search
    m = Time.now.to_i.to_s
	nodelocation = "some/test/location#{m}"
    create_node(nodelocation, { "a" => "anunusualstring", "b" => "bar" })
    result = @sm.search_for("anunusualstring")
    assert_not_nil(result, "Expected result back")
    nodes = result["results"]
    assert_equal(1, nodes.size, "Expected one matching node")
    assert_equal("bar", nodes[0]["b"], "Expected data to be loaded")
  end

  def test_user_search
    m = Time.now.to_i.to_s
	username = "unusualuser#{m}"
    create_user(username)
    result = @sm.search_for_user(username)
    assert_not_nil(result, "Expected result back")
    users = result["results"]
    assert_equal(1, users.size, "Expected one matching user")
    assert_equal(username, users[0]["rep:userId"], "Expected user to match")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_MySearchTest)

