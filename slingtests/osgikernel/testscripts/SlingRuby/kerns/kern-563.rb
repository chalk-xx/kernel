#!/usr/bin/env ruby

require 'sling/sling'
require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingAuthz


class TC_Kern563Test < SlingTest
  
  def test_default_locale
    m = Time.now.to_i.to_s
    userid = "testuser-#{m}"
    user = create_user(userid)
    @s.switch_user(user)
    params = {"locale" => "_"}
    
    @s.execute_post(@s.url_for("system/userManager/user/#{userid}.update.html"), params)
    
    res = @s.execute_get(@s.url_for("/system/me"))
    assert_equal(res.code.to_i, 200)
    # Default is US
    json = JSON.parse(res.body)
    assert_equal(json["user"]["locale"]["country"], "US")
  end
  
  
  def test_malformed_locale
    m = Time.now.to_i.to_s
    userid = "testuser-#{m}"
    user = create_user(userid)
    @s.switch_user(user)
    params = {"locale" => "nl_BE"}
    
    @s.execute_post(@s.url_for("system/userManager/user/#{userid}.update.html"), params)
    
    resp = @s.execute_get(@s.url_for("/system/me"))
    assert_equal(resp.code.to_i, 200)
    json = JSON.parse(resp.body)
    # Default is US, should be BE
    assert_equal(json["user"]["locale"]["country"], "BE")
  end
  
end

Test::Unit::UI::Console::TestRunner.run(TC_Kern563Test)
