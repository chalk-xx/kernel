#!/usr/bin/env ruby

require 'sling/sling'
require 'sling/test'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingSites

class TC_PrivateNodeCreateTest < SlingTest

  def test_create__private_node
    puts("test_create_private_node---------------------------------------------------START")
    m = Time.now.to_i.to_s
    testpath = "/_user/private/testnode"
    user1 = "user1-"+m
    u1 = create_user(user1)
    user2 = "user2-"+m
    u2 = create_user(user2)
    @s.switch_user(u1)
    create_node(testpath, "a" => "user1", "b" => "bar")

    @s.switch_user(u2)
    create_node(testpath, "a" => "user2", "b" => "bar")

    @s.switch_user(u1)
    props = @s.get_node_props(testpath)
    assert_equal("user1", props["a"], "Expected property to be set")
    assert_equal("bar", props["b"], "Expected property to be set")
    @s.switch_user(u2)
    props = @s.get_node_props(testpath)
    assert_equal("user2", props["a"], "Expected property to be set")
    assert_equal("bar", props["b"], "Expected property to be set")


    puts("test_create_node---------------------------------------------------END")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_PrivateNodeCreateTest)

