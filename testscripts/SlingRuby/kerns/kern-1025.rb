#!/usr/bin/env ruby

require 'sling/test'
include SlingUsers

class TC_Kern1025Test < Test::Unit::TestCase
  include SlingTest

  def test_me_servlet_from_group_manager
    m = Time.now.to_f.to_s.gsub('.', '')
    manager = create_user("user-manager-#{m}")
    group = Group.new("g-test-#{m}")
    @s.switch_user(User.admin_user())
    res = @s.execute_post(@s.url_for("#{$GROUP_URI}"), {
      ":name" => group.name,
      ":sakai:manager" => manager.name,
      "_charset_" => "UTF-8"
    })
    assert_equal("200", res.code, "Should have created group as admin")
    @s.switch_user(manager)
    details = group.details(@s)
    managersgroupname = details["properties"]["sakai:managers-group"]
    assert_not_nil(managersgroupname, "Managers group property should be set")
    assert(details["properties"]["rep:group-managers"].include?(managersgroupname), "Group managers should include its own managers group")
    managersgroup = Group.new(managersgroupname)
    details = managersgroup.details(@s)
    assert_equal(group.name, details["properties"]["sakai:managed-group"], "Managers group should point to its managed group")
    assert_equal(managersgroupname, details["properties"]["rep:group-managers"], "Managers group should manage itself")
    members = details["members"]
    assert(members.include?(manager.name), "Should have added user to managers group")

    # Real test starts here.
    res = @s.execute_get(@s.url_for("/system/me.json"))
    puts "Manager me = " + res.code, res.body
    assert_equal("200", res.code, "Me servlet should return successfully")
  end

end
