#!/usr/bin/env ruby

require 'sling/sling'
require 'sling/test'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingSites

class TC_MySiteTest < SlingTest

  def do_site_create
    res = create_site("var/stuff/testsite")
    assert_not_nil(res, "Expected site to be created")
    props = @s.get_node_props("var/stuff/testsite")
    assert_equal("sakai/site", props["sling:resourceType"], "Expected resource type to be set")
  end

  def test_create_site
    do_site_create()
  end

  def test_read_default_site
    do_site_create()
    res = @s.execute_get_with_follow("#{@s.url_for("var/stuff/testsite")}.html")
    puts res.body
    assert_equal(200, res.code.to_i, "Expected site to be able to see site")
  end

  def test_add_group_to_site
   site_group = create_group("mysitegroup")
   test_site = create_site("somesite")
   test_site.add_group(site_group.name)
   groups = SlingSites::Site.get_groups("somesite", @s)
   assert_equal(1, groups.size, "Expected 1 group")
   assert_equal("mysitegroup", groups[0], "Expected group to be added")
  end

  def test_join_unjoinable_site
    site_group = create_group("mysitegroup")
    site_user = create_user("mysiteuser")
    test_site = create_site("someothersite")
    test_site.add_group(site_group.name)
    @s.switch_user(site_user)
    test_site.join(site_group.name)
    members = test_site.get_members
    assert_not_nil(members, "Expected to get member list")
    assert_equal(0, members.size, "Expected no site members")
  end

  def test_join
    site_group = create_group("mysitegroup")
    site_group.set_joinable(@s, "yes")
    site_user = create_user("mysiteuser")
    test_site = create_site("someothersite")
    test_site.add_group(site_group.name)
    test_site.set_joinable("yes")
    @s.switch_user(site_user)
    test_site.join(site_group.name)
    members = test_site.get_members
    assert_not_nil(members, "Expected to get member list")
    assert_equal(1, members.size, "Expected site members")
    assert_equal(site_user.name, members[0]["rep:userId"], "Expected user to match")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_MySiteTest)

