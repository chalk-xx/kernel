#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

class TC_Kern294Test < SlingTest

  def test_move
    m = Time.now.to_i.to_s
    n1 = create_node("test/d1#{m}")
    @s.debug = true
    res = @s.execute_post(@s.url_for(n1), { ":operation" => "move",
                                                 ":dest" => "d2#{m}" }) 
    @s.debug = false
    assert_equal("201", res.code, "Expected to be able to move node")
  end

  def test_move_at_root
    m = Time.now.to_i.to_s
    n1 = create_node("d1#{m}")
    @s.debug = true
    res = @s.execute_post(@s.url_for(n1), { ":operation" => "move",
                                                 ":dest" => "d2#{m}" }) 
    @s.debug = false
    assert_equal("201", res.code, "Expected to be able to move node")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern294Test)

