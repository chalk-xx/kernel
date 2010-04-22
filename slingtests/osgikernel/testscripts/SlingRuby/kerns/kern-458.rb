#!/usr/bin/env ruby

require 'set'
require 'sling/test'
require 'sling/message'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch
include SlingMessage

class TC_Kern458Test < SlingTest
  

  
  def test_getdev
    # We create a test site.
    m = Time.now.to_i.to_s
    res = @s.execute_get(@s.url_for("/dev/"))
    assert_equal("text/html",res.content_type())
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern458Test)
