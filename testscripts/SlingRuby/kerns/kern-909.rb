#!/usr/bin/env ruby

require 'sling/test'
require 'test/unit.rb'
include SlingSearch

class TC_Kern909Test < Test::Unit::TestCase
  include SlingTest
  
  #
  # Batch post test
  #
  
  def test_normal
    m = Time.now.to_i.to_s
    user1 = create_user("user1-"+m)
    
    @s.switch_user(user1)
    path = user1.private_path_for(@s)
    res = @s.execute_post(@s.url_for("#{path}/test/b"), {"foo" => "bar"})
    res = @s.execute_get(@s.url_for("#{path}/test/b.json"))
    @log.info(res.body)
    
    # Batch post to private store
    str = [{
          "url" => "#{path}/test/b",
          "method" => "POST",
          "parameters" => {
              "title" => "alfa",
              "foo" => "bar",
              "unit" => 10,
              "unit@TypeHint" => "Long"
      }
    },
    {
          "url" => "#{path}/test/b.modifyAce.html",
          "method" => "POST",
          "parameters" => {
              "principalId" => "#{user1.name}",
              "privilege@jcr:read" => "granted"
      }
    }
    ]
    
    parameters = {
      "requests" => JSON.generate(str)
    }
    
    res = @s.execute_post(@s.url_for("system/batch"), parameters)
    
    jsonRes = JSON.parse(res.body)
    
    assert_equal(jsonRes[0]["url"], "#{path}/test/b")
    assert_equal(jsonRes[0]["status"], 200, "Expexted to get a created statuscode.")
    assert_equal(jsonRes[1]["url"], "#{path}/test/b.modifyAce.html")
    assert_equal(jsonRes[1]["status"], 200, "Expexted to get a created statuscode. #{jsonRes[1]["body"]} ")
  end
  
  
end

