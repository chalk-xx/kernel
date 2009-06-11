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
    create_node("some/test/location", { "a" => "anunusualstring", "b" => "bar" })
    result = @sm.search_for("anunusualstring")
    assert_not_nil(result, "Expected result back")
    paths = result["results"]
    assert_equal(1, paths.size, "Expected one matching node")
    assert_equal("node /some/test/location", paths[0], "Expected path to match")
  end

  def test_user_search
    create_user("unusualuser")
    result = @sm.search_for_user("unusualuser")
    assert_not_nil(result, "Expected result back")
    users = result["results"]
    assert_equal(1, users.size, "Expected one matching user")
    assert_not_nil(users[0].match(/.*\/unusualuser\/.*/), "Expected user to match")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_MySearchTest)

