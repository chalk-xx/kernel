#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require './ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/users'
include SlingUsers

class TC_Kern2027Test < Test::Unit::TestCase
  include SlingTest

  def test_user_remove_self_from_group
    # create test users
    u1 = create_test_user('2027-1')
    u2 = create_test_user('2027-2')
    @s.switch_user(u1)

    # have user1 create a group
    m = Time.now.to_f.to_s.gsub('.', '')
    groupid = "testgroup-#{m}"
    group = create_group(groupid)

    # add user2 as a member
    res = group.add_member(@s, u2.name, 'user')
    assert_equal("200", res.code, "Not able to add user2 as member")

    # add user2 as a viewer
    res = group.add_viewer(@s, u2.name)
    assert_equal("200", res.code, "Not able to add user2 as viewer")

    assert_equal(true, group.has_member(@s, u2.name), "Should have #{u2.name} as a member")

    # have user2 leave the group
    @s.switch_user(u2)

    groupurl = @s.url_for(Group.url_for(groupid))
    leaveurl = "#{groupurl}.leave.json"

    res = @s.execute_post(leaveurl)
    assert_equal("200", res.code, "Should be able to leave the group")

    # switch to user1 and check if user2 is a member
    @s.switch_user(u1)
    assert_equal(false, group.has_member(@s, u2.name), "Should not have #{u2.name} as a member")
  end
end
