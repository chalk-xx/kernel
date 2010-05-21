#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
include SlingSearch

class TC_Kern292Test < Test::Unit::TestCase
  include SlingTest

  def test_mutual_group_addition
    m = Time.now.to_i.to_s
    g1 = create_group("g-testgroup1-#{m}")
    g2 = create_group("g-testgroup2-#{m}")
	puts("adding group #{g1.name} to #{g2.name} ")
    res = g2.add_member(@s, g1.name, "group")
    assert_equal("200", res.code, "Expected first add to succeed")
    members = g2.members(@s)
    assert_equal(g1.name, members[0], "Expected member name to match")
	@s.debug=true
	puts("adding group #{g2.name} to #{g1.name} ")
    res = g1.add_member(@s, g2.name, "group")
	@s.debug=false
    ## assert_equal("500", res.code, "Expected second add to fail"), in JR2 this does not fail, but it does not add.
    assert_equal("200", res.code, "Expected second add to be Ok")
    members = g1.members(@s)
    assert_equal(0, members.size, "Expected group to have no members")
  end

  def test_addition_is_transactional
    m = Time.now.to_i.to_s
    g1 = create_group("g-testgroup3-#{m}")
    g2 = create_group("g-testgroup4-#{m}")
	puts("Adding #{g1.name} to #{g2.name} ")
    res = g2.add_member(@s, g1.name, "group")
    assert_equal("200", res.code, "Expected first add to succeed")
    members = g2.members(@s)
    assert_equal(g1.name, members[0], "Expected member name to match")
    users = [ "bob", "sam", "jim" ].collect do |u|
      create_user("#{u}-#{m}")
    end
    res = g1.add_members(@s, users.map { |u| u.name } << g2.name)
    ##assert_equal("500", res.code, "Expected second add to fail"), in JR2 it only removes those users that are present and does not fail
    assert_equal("200", res.code, "Expected second add to be Ok")
	members = g1.members(@s)
    assert_equal(3, members.size, "Expected group to only those members that it should have, bob, sam and jim")
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
    ##assert_equal("500", res.code, "Expected remove to fail") in JR2 it only removes those users that it should be able to, and does not fail
    assert_equal("200", res.code, "Expected remove to be Ok")
	members = g1.members(@s)
    assert_equal(1, members.size, "Expected group to remove only pav and simon")
  end

end


