#!/usr/bin/env ruby

require 'sling/test'
require 'sling/sling'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingSearch

class TC_Kern308Test < SlingTest



  def test_308
    @m = Time.now.to_i.to_s
	u = create_user("ian"+@m)
	g1t = create_group("g-group1-"+@m)
	ga = Group.new("everyone")
	ga.add_member(@s,u.name,"user")
	@s.switch_user(u)
	g = create_group("g-group"+@m)
	@s.debug = false
	assert_not_nil(g,"Failed to create group node ")
  end


end

Test::Unit::UI::Console::TestRunner.run(TC_Kern308Test)

