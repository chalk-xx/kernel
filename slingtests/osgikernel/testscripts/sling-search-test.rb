#!/usr/bin/env ruby

require 'sling/sling'
require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingSearch

class TC_MySearchTest < SlingTest

  def setup
    super
    @sm = SlingSearch::SearchManager.new(@s)
  end

  def test_node_search
    create_node("some/test/location", { "a" => "anunusualstring", "b" => "bar" })
    result = @sm.search_for("anunusualstring")
    paths = result["results"]
    assert_equal(1, paths.size, "Expected one matching node")
    assert_equal("node /some/test/location", paths[0], "Expected path to match")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_MySearchTest)

