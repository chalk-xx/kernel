#!/usr/bin/env ruby

require 'sling/sling.rb'
require 'test/unit.rb'
include SlingInterface
include SlingUsers

class TC_MyTest < Test::Unit::TestCase

  def setup
    @s = Sling.new()
    @um = UserManager.new(@s)
    #@s.debug = true
    m = Time.now.to_i.to_s
    @test_node = "some_test_node"+m
    @s.delete_node(@test_node)
  end

  def teardown
    @s.switch_user(User.admin_user)
    @s.delete_node(@test_node)
  end

  def test_ownership_privs

    # Set up user and group
    puts("Creating test user ")
    user = @um.create_test_user(10)
    assert_not_nil(user, "Expected user to be created")
    # assume already exists
    owner = Group.new("owner")
    #@s.debug = true
    puts("Updating dynamic properties for owner just in case ")
    @s.update_properties(owner, { "dynamic" => "true" })
    assert_not_nil(owner, "Expected owner group to be created")
    puts("Owner group created fully")

    # Create admin-owned parent node
    puts("Creating admin owned test node #{@test_node} ")
    @s.create_node(@test_node, { "jcr:mixinTypes" => "mix:created", "foo" => "bar", "baz" => "jim" })
    props = @s.get_node_props(@test_node)
    puts("Got properties of the node as: "+@s.get_node_props_json(@test_node))
    assert_equal("bar", props["foo"])
    puts(" Clear the acl ")
    @s.clear_acl(@test_node)
    acl = @s.get_node_acl(@test_node)
    assert(acl.size == 0, "Expected ACL to be cleared")


    @s.set_node_acl_entries(@test_node, user, { "jcr:write" => "granted",
                                                "jcr:addChildNodes" => "granted",
                                                "jcr:readAccessControl" => "granted",
                                                "jcr:modifyProperties" => "granted" ,
												"jcr:nodeTypeManagement" => "granted" })
    puts("ACL For test Node is #{@test_node} "+ @s.get_node_acl_json(@test_node))

    # Switch to unprivileged user, create child node owned by user
    @s.switch_user(user)
    child_node = "#{@test_node}/bar"
    @s.create_node(child_node, {  "jcr:mixinTypes" => "mix:created", "bob" => "cat" })
    puts("Got properties of the child node as: "+@s.get_node_props_json(child_node))
    puts("ACL For test Child Node is  #{child_node}"+ @s.get_node_acl_json(child_node))


    # Switch to admin, add "modifyAccessControl" priv to owner on new child node
    @s.switch_user(User.admin_user)
    @s.set_node_acl_entries(child_node, owner, "jcr:modifyAccessControl" => "granted")

    # Switch back to unprivileged user and exercise the owner grant
    @s.switch_user(user)
    puts("As Unprivileged User #{user} properties are ")
    @s.get_node_props_json(child_node)
    puts("As Unprivileged User ACLs are ")
    @s.get_node_acl_json(child_node)
    puts("Modifying ACL as unpivileged User #{user} ")
    res = @s.set_node_acl_entries(child_node, user, { "jcr:addChildNodes" => "denied" })
    assert_equal(200, res.code.to_i, "Expected to be able to modify ACL")
    puts("As Unprivileged User ACLs are set as #{user}  ")
    puts @s.get_node_acl_json(@test_node)

    # Switch to a different unprivileged user and assert owner grant is not in effect
    user2 = @um.create_test_user(11)
    @s.switch_user(user2)
    puts("As non owner checking  ")
    res = @s.set_node_acl_entries(child_node, user2, { "jcr:addChildNodes" => "granted" })
    assert_equal(500, res.code.to_i, "Expected not to be able to modify ACL")
  end

end


