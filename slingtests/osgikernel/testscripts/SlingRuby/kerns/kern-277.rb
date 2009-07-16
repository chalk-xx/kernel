#!/usr/bin/env ruby

require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingAuthz

class TC_Kern277Test < SlingTest

  def test_group_deny
    @authz = SlingAuthz::Authz.new(@s)
    m = Time.now.to_i.to_s
    group = "g-group-" + m
    path = "test/authztest/node" + m
    create_group(group)
    create_node(path, "testproperty" => "testvalue")
    @authz.grant(path, group, "jcr:write" => "denied")
    acl = @authz.getacl(path)
    puts "Got ace: #{acl.to_s}"
    ace = acl[group]
    assert_not_nil(ace["denied"], "Expected ACE for #{group} deny")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern277Test)

