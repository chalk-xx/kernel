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
    @m = Time.now.to_i.to_s
    @siteid = "var/stuff/testsite"+@m
	@siteurl = @s.url_for(@siteid)
	puts("Site id is #{@siteid} ")
	@s.debug = true
    res = create_site(@siteurl)
    assert_not_nil(res, "Expected site to be created")
    props = @s.get_node_props(@siteid)
    assert_equal("sakai/site", props["sling:resourceType"], "Expected resource type to be set")
  end

  def test_create_site
    do_site_create()
  end

  def xtest_read_default_site
    do_site_create()
    res = @s.execute_get_with_follow(@siteurl+".html")
    puts res.body
    assert_equal(200, res.code.to_i, "Expected site to be able to see site")
  end

  def xtest_add_group_to_site
   site_group = create_group("g-mysitegroup")
   test_site = create_site("somesite")
   test_site.add_group(site_group.name)
   groups = SlingSites::Site.get_groups("somesite", @s)
   assert_equal(1, groups.size, "Expected 1 group")
   assert_equal("g-mysitegroup", groups[0], "Expected group to be added")
  end

  def xtest_join_unjoinable_site
    site_group = create_group("g-mysitegroup")
    site_user = create_user("mysiteuser")
    test_site = create_site("someothersite")
    test_site.add_group(site_group.name)
    @s.switch_user(site_user)
    test_site.join(site_group.name)
    members = test_site.get_members
    assert_not_nil(members, "Expected to get member list")
    assert_equal(0, members.size, "Expected no site members")
  end

  def do_join(site, group, user)
    site_group = create_group(group)
    site_group.set_joinable(@s, "yes")
    site_user = create_user(user)
    test_site = create_site(site)
    test_site.add_group(site_group.name)
    test_site.set_joinable("yes")
    @s.switch_user(site_user)
    test_site.join(site_group.name)
    members = test_site.get_members
    assert_not_nil(members, "Expected to get member list")
    assert_equal(1, members.size, "Expected site members")
    assert_equal(site_user.name, members[0]["rep:userId"], "Expected user to match")
    @s.switch_user(SlingUsers::User.admin_user)
    return test_site
  end

  def xtest_join
    return do_join("someothersite", "g-mysitegroup", "mysiteuser")    
  end

  def xtest_join_and_search
    do_join("anothersite", "g-mysitegroup", "mysiteuser")
    res = @s.update_node_props("anothersite", "fish" => "dog")
    assert_equal(200, res.code.to_i, "Expected site property to be updated")
    result = @search.search_for_site("dog")
    assert_not_nil(result, "Expected results back")
    assert(result["results"].size >= 1, "Expected at least one site")
    created_site = result["results"].select { |r| r["path"] == "/anothersite" }
    assert_equal(1, created_site.size, "Expected to find site with matching path")
    assert_equal(1, created_site[0]["member-count"].to_i, "Expected single member")
  end

  def xtest_multi_group_join
    site = do_join("anothersite", "g-mysitegroup", "mysiteuser")
    group2 = create_group("g-sitegroup2")
    group2.add_member(@s, "mysiteuser", "user")
    site.add_group(group2.name)
    members = site.get_members
    assert_equal(1, members.size, "Expected a single member")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_MySiteTest)

