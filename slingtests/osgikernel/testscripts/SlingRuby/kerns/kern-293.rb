#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

class TC_Kern293Test < SlingTest

=begin
  def test_overwrite
    m = Time.now.to_i.to_s
    n1 = create_node("d1#{m}")
    n2 = create_node("d2#{m}")
    res = @s.execute_post(@s.url_for(n1), { ":operation" => "copy",
                                                 ":dest" => n2 }) 
    assert_equal("412", res.code, "Expected not to be allowed to overwrite")
  end
=end

  def test_overwrite_with_replace
    m = Time.now.to_i.to_s
    @s.debug = true
    @s.log = true
    n1 = create_node("a1#{m}")
    n2 = create_node("a2#{m}")
    res = @s.execute_post(@s.url_for(n1), { ":operation" => "copy",
                                                 ":dest" => n2,
                                              ":replace" => "true" }) 
    assert_equal("200", res.code, "Expected to be allowed to overwrite")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern293Test)

