#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby 'require' search path
require './ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/users'
require 'test/unit.rb'
require 'sling/file'
include SlingUsers
include SlingFile

class TC_Kern2078Test < Test::Unit::TestCase
  include SlingTest

  def test_get_redirected_content
    @s.log.level = Logger::INFO
    @fm = FileManager.new(@s)
    m = Time.now.to_f.to_s.gsub('.', '')

    u1 = create_user("testuser-#{m}", "The", "Author")
    @s.switch_user(u1)
    
    # Create a path visible only to logged-in users.
    u1spot = @s.url_for(u1.home_path_for(@s)) + "/public"
    u1realdata = "#{u1spot}/real-#{m}"
    res = @s.execute_post(u1realdata, {
      "prop" => "val"
    })
    assert_equal('201', res.code, 'Expected to be able to create test data')
    res = @s.execute_post("#{u1realdata}.modifyAce.html", {
      'principalId' => 'anonymous',
      'privilege@jcr:read' => 'denied'
    })
    assert_equal('200', res.code, 'Expected to change ACL')
    res = @s.execute_post("#{u1realdata}.modifyAce.html", {
      'principalId' => 'everyone',
      'privilege@jcr:read' => 'granted'
    })
    assert_equal('200', res.code, 'Expected to change ACL')
    
    # Create a redirect.
    u1redirect = "#{u1spot}/redirect-#{m}"
    res = @s.execute_post(u1redirect, {
      "sling:resourceType" => "sling:redirect",
      "sling:target" => u1realdata
    })
    assert_equal('201', res.code, 'Expected to be able to create redirect pointer')
    
    # Test the redirect.
    res = @s.execute_get(u1redirect)
    assert_equal('200', res.code, 'Expected to read redirect path')
    json = JSON.parse(res.body)
    assert_equal("val", json["prop"], "Expected to receive data through redirect. Body=#{res.body}")
  end
end
