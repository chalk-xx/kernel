#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require './ruby-lib-dir.rb'

require 'sling/test'
include SlingUsers

class TC_Kern1057Test < Test::Unit::TestCase
  include SlingTest

  def test_update_managers_group_directly
    m = Time.now.to_f.to_s.gsub('.', '')
    manager = create_user("user-manager-#{m}")
    other = create_user("user-other-#{m}")
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
    assert(group.has_member(@s, manager.name), "Should have added user as manager to group")
    res = @s.execute_post(@s.url_for("#{Group.url_for(group.name)}.update.html"), {
      ":member" => other.name
    })
    assert_equal("200", res.code, "Should have added member as manager to group")
    assert(group.has_member(@s, other.name), "Should have found new manager member in group")
  end

end
