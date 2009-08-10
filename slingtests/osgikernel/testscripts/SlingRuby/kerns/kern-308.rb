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
	n = create_user("nico"+@m)
	g1t = create_group("g-group1-"+@m)
	@s.switch_user(u)
	g = create_group("g-group"+@m)
        puts(g.details(@s))
	assert_not_nil(g,"Failed to create group node ")
        g.add_member(@s, n.name, "user")

        details = g.details(@s)
        members = details["members"]
        assert_not_nil(members, "Expected a list of members")
        puts(members)

        
  end


end

Test::Unit::UI::Console::TestRunner.run(TC_Kern308Test)

