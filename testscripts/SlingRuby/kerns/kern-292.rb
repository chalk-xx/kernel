#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
include SlingSearch

class TC_Kern292Test < Test::Unit::TestCase
  include SlingTest

  def test_add_member_acyclic
    m = Time.now.to_f.to_s.gsub('.', '')
    g1 = create_group("g-testgroup1-#{m}")
    g2 = create_group("g-testgroup2-#{m}")
    @log.info("adding group #{g1.name} to #{g2.name} ")
    res = g2.add_member(@s, g1.name, "group")
    assert_equal("200", res.code, "Expected first add to succeed")
    assert(g2.has_member(@s, g1.name), "Expected member name in group")
    @log.info("adding group #{g2.name} to #{g1.name} ")
    res = g1.add_member(@s, g2.name, "group")
    assert_equal("200", res.code, "Expected second add to be Ok")
    members = g1.members(@s)
   # this test would be 1 with the JR user manager, but the sparse user manager does not maintain RI, perposfully
    assert_equal(0, members.size, "Expected group to have no extra members #{members} ")
  end

  def test_add_members_acyclic
    m = Time.now.to_f.to_s.gsub('.', '')
    g1 = create_group("g-testgroup3-#{m}")
    g2 = create_group("g-testgroup4-#{m}")
    @log.info("Adding #{g1.name} to #{g2.name} ")
    res = g2.add_member(@s, g1.name, "group")
    assert_equal("200", res.code, "Expected first add to succeed")
    assert(g2.has_member(@s, g1.name), "Expected member name in group")
    users = [ "bob", "sam", "jim" ].collect do |u|
      create_user("#{u}-#{m}")
    end
    res = g1.add_members(@s, users.map { |u| u.name } << g2.name)
    assert_equal("200", res.code, "Expected second add to be Ok")
    members = g1.members(@s)
    assert_equal(3, members.size, "Expected group to only those members that it should have: bob, sam, and jim")
  end

  def test_remove_ignores_nonmembers
    m = Time.now.to_f.to_s.gsub('.', '')
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
    assert_equal("200", res.code, "Expected remove to be Ok")
    members = g1.members(@s)
    assert_equal(1, members.size, "Expected group to remove only pav and steve")
  end

end


