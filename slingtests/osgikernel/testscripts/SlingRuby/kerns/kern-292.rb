#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

class TC_Kern292Test < SlingTest

  def test_mutual_group_addition
    m = Time.now.to_i.to_s
    g1 = create_group("g-testgroup1-#{m}")
    g2 = create_group("g-testgroup2-#{m}")
    res = g2.add_member(@s, g1.name, "group")
    assert_equal("200", res.code, "Expected first add to succeed")
    members = g2.members(@s)
    assert_equal(g1.name, members[0], "Expected member name to match")
    res = g1.add_member(@s, g2.name, "group")
    assert_equal("500", res.code, "Expected second add to fail")
    members = g1.members(@s)
    assert_equal(0, members.size, "Expected group to have no members")
  end

  def test_addition_is_transactional
    m = Time.now.to_i.to_s
    g1 = create_group("g-testgroup3-#{m}")
    g2 = create_group("g-testgroup4-#{m}")
    res = g2.add_member(@s, g1.name, "group")
    assert_equal("200", res.code, "Expected first add to succeed")
    members = g2.members(@s)
    assert_equal(g1.name, members[0], "Expected member name to match")
    users = [ "bob", "sam", "jim" ].collect do |u|
      create_user("#{u}-#{m}")
    end
    res = g1.add_members(@s, users.map { |u| u.name } << g2.name)
    assert_equal("500", res.code, "Expected second add to fail")
    members = g1.members(@s)
    assert_equal(0, members.size, "Expected group to have no members")
  end

  def test_deletion_is_transactional
    m = Time.now.to_i.to_s
    g1 = create_group("g-testgroup5-#{m}")
    users = [ "pav", "simon", "steve" ].collect do |u|
      create_user("#{u}-#{m}")
    end
    other = create_user("dave-#{m}")
    res = g1.add_members(@s, users.map { |u| u.name })
    assert_equal("200", res.code, "Expected add to succeed")
    members = g1.members(@s)
    assert_equal(3, members.size, "Expected group to have three members")
    res = g1.remove_members(@s, [ users[0].name, other.name, users[2].name ])
    assert_equal("500", res.code, "Expected remove to fail")
    members = g1.members(@s)
    assert_equal(3, members.size, "Expected group to have three members")
    res = g1.remove_members(@s, [ users[0].name, users[2].name ])
    members = g1.members(@s)
    assert_equal(1, members.size, "Expected group to have one member")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern292Test)

