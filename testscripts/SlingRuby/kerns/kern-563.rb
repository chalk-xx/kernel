#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require './ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
include SlingInterface
include SlingUsers
include SlingAuthz


class TC_Kern563Test < Test::Unit::TestCase
  include SlingTest
  
  def test_default_locale
    m = Time.now.to_i.to_s
    userid = "testuser-#{m}"
    user = create_user(userid)
    @s.switch_user(user)
    # find the default country locale for this user
    response = @s.execute_get(@s.url_for("/system/me"))
    assert_equal(200, response.code.to_i)
    json = JSON.parse(response.body)
    default_country_locale = json["user"]["locale"]["country"]
    
    # un-set the default country local for this user (eg go from en_US to _ )
    params = {"locale" => "_"}
    
    @s.execute_post(@s.url_for("system/userManager/user/#{userid}.update.html"), params)
    
    # confirm default country locale still exists for this user
    res = @s.execute_get(@s.url_for("/system/me"))
    assert_equal(200, res.code.to_i)
    json = JSON.parse(res.body)
    assert_equal(default_country_locale, json["user"]["locale"]["country"])
  end
  
  
  def test_malformed_locale
    m = Time.now.to_i.to_s
    userid = "testuser-#{m}"
    user = create_user(userid)
    @s.switch_user(user)
    params = {"locale" => "nl_BE"}
    
    @s.execute_post(@s.url_for("system/userManager/user/#{userid}.update.html"), params)
    
    resp = @s.execute_get(@s.url_for("/system/me"))
    assert_equal(200, resp.code.to_i)
    json = JSON.parse(resp.body)
    # Default is US, should be BE
    assert_equal("BE", json["user"]["locale"]["country"])
  end
  
end

