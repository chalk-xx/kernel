#!/usr/bin/env ruby

require 'sling-interface.rb'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface

class TC_MyTest < Test::Unit::TestCase

  def setup
    @s = Sling.new()
    @test_node = "some_test_node"
    @s.delete_node(@test_node)
  end

  def teardown
    @s.switch_user(User.admin_user)
    @s.delete_node(@test_node)
  end

  def test_ownership_privs

    # Set up user and group
    user = @s.create_user(10)
    assert_not_nil(user, "Expected user to be created")
    owner = @s.create_group("owner")
    if (owner == nil)
      # assume already exists
      owner = Group.new("owner")
    end
    #@s.debug = true
    @s.update_properties(owner, { "dynamic" => "true" })
    assert_not_nil(owner, "Expected owner group to be created")

    # Create admin-owned parent node
    @s.create_node(@test_node, { "foo" => "bar", "baz" => "jim" })
    props = @s.get_node_props(@test_node)
    puts props
    assert_equal("bar", props["foo"])
    @s.clear_acl(@test_node)
    acl = @s.get_node_acl(@test_node)
    assert(acl.size == 0, "Expected ACL to be cleared")
    @s.set_node_acl_entries(@test_node, user, { "jcr:write" => "granted",
                                                "jcr:addChildNodes" => "granted",
                                                "jcr:readAccessControl" => "granted",
                                                "jcr:modifyProperties" => "granted" })
    puts @s.get_node_acl_json(@test_node)

    # Switch to unprivileged user, create child node owned by user
    @s.switch_user(user)
    child_node = "#{@test_node}/bar"
    @s.create_node(child_node, { "bob" => "cat" })

    # Switch to admin, add "modifyAccessControl" priv to owner on new child node
    @s.switch_user(User.admin_user)
    @s.set_node_acl_entries(child_node, owner, "jcr:modifyAccessControl" => "granted")

    # Switch back to unprivileged user and exercise the owner grant
    @s.switch_user(user)
    puts @s.get_node_props_json(child_node)
    puts @s.get_node_acl_json(child_node)
    res = @s.set_node_acl_entries(child_node, user, { "jcr:addChildNodes" => "denied" })
    assert_equal(200, res.code.to_i, "Expected to be able to modify ACL")
    puts @s.get_node_acl_json(@test_node)

    # Switch to a different unprivileged user and assert owner grant is not in effect
    user2 = @s.create_user(11)
    @s.switch_user(user2)
    res = @s.set_node_acl_entries(child_node, user2, { "jcr:addChildNodes" => "granted" })
    assert_equal(500, res.code.to_i, "Expected not to be able to modify ACL")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_MyTest)

