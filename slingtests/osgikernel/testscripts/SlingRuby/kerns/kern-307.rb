#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

class TC_Kern307Test < SlingTest

  def test_node_edit
    m = Time.now.to_i.to_s
    @s.log = true
    node = create_node("some/test/path#{m}", {})
    writers = create_group("g-test-writers-#{m}")
    readers = create_group("g-test-readers-#{m}")
    @s.set_node_acl_entries(node, writers, { "jcr:removeNode" => "granted",
                                             "jcr:modifyProperties" => "granted",
                                             "jcr:removeChildNodes" => "granted",
                                             "jcr:write" => "granted", 
                                             "jcr:addChildNodes" => "granted" })
#    @s.set_node_acl_entries(node, readers, { "jcr:read" => "granted" })
#    everyone = SlingUsers::Group.new("everyone")
#    @s.set_node_acl_entries(node, everyone, { "jcr:read" => "granted" })
    puts @s.get_node_acl_json(node)
    user = create_user("testwriter")
    writers.add_member(@s, user.name, "user")
    @s.switch_user(user)
    @s.debug = true
    child = create_node("#{node}/child", {})
    @s.debug = false
    assert_not_nil(child, "Expected node to be created")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern307Test)

