#!/usr/bin/env ruby

require 'sling/test'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

class TC_Kern330Test < SlingTest
  
  #
  # Batch post test
  #
  
  def test_normal
    m = Time.now.to_i.to_s
    user1 = create_user("user1-"+m)
    
    @s.switch_user(user1)
    
    res = @s.execute_post(@s.url_for("_user/private/test/b"), {"foo" => "bar"})
    
    # Batch post to private store
    str = [{
          "url" => "/_user/private/test/a",
          "data" => {
              "title" => "alfa",
              "foo" => "bar",
              "unit" => 10
      }
    },
    {
          "url" => "/_user/private/test/b",
          "data" => {
              "title" => "beta",
              "unit" => 20
      }
    },
    {
          "url" => "/_user/private/test/c",
          "data" => {
              "title" => "charlie",
              "unit" => 30
      }
    }
    ]
    
    parameters = {
      "p" => JSON.generate(str)
    }
    
    res = @s.execute_post(@s.url_for("system/batch/post"), parameters)
    
    jsonRes = JSON.parse(res.body)
    
    assert_equal(jsonRes[0]["url"], "/_user/private/test/a")
    assert_equal(jsonRes[0]["status"], 201, "Expexted to get a created statuscode.")
    assert_equal(jsonRes[1]["url"], "/_user/private/test/b")
    assert_equal(jsonRes[1]["status"], 200, "Expected to be get a modified statuscode.")
    assert_equal(jsonRes[2]["url"], "/_user/private/test/c")
    assert_equal(jsonRes[2]["status"], 201, "Expexted to get a created statuscode.")
  end
  
  def test_accessdenied
    
    m = Time.now.to_i.to_s
    user2 = create_user("user2-"+m)
    
    @s.switch_user(user2)
    str = [{
          "url" => "/foo/bar",
          "data" => {
              "title" => "alfa",
              "foo" => "bar",
              "unit" => 10
      }
    },
    {
          "url" => "/_user/private/foo/bar",
          "data" => {
              "title" => "beta",
              "unit" => 20
      }
    }
    ]
    
    parameters = {
      "p" => JSON.generate(str)
    }
    
    
    res = @s.execute_post(@s.url_for("system/batch/post"), parameters)
    
    jsonRes = JSON.parse(res.body)
    
    assert_equal(jsonRes[0]["url"], "/foo/bar")
    assert_equal(jsonRes[0]["status"], 500, "Expexted access denied.")
    assert_equal(jsonRes[1]["url"], "/_user/private/foo/bar")
    assert_equal(jsonRes[1]["status"], 201, "Expected to get a created statuscode.")
    
  end
  
  def test_values
    m = Time.now.to_i.to_s
    user3 = create_user("user3-"+m)
    
    @s.switch_user(user3)
    str = [
    {
          "url" => "/_user/private/foo/bar/a",
          "data" => {
              "title" => "alfa",
              "unit" => 20,
              "unit@TypeHint" => "Long"
      }
    },{
          "url" => "/_user/private/foo/bar/b",
          "data" => {
              "title" => "beta",
              "sakai:tags" => ["a", "b"],
      }
    }
    ]
    
    parameters = {
      "p" => JSON.generate(str)
    }
    
    
    res = @s.execute_post(@s.url_for("system/batch/post"), parameters)
    
    # Check batch post response
    jsonRes = JSON.parse(res.body)
    assert_equal(jsonRes[0]["url"], "/_user/private/foo/bar/a")
    assert_equal(jsonRes[0]["status"], 201, "Expexted to get a created statuscode.")
    assert_equal(jsonRes[1]["url"], "/_user/private/foo/bar/b")
    assert_equal(jsonRes[1]["status"], 201, "Expected to get a created statuscode.")
    
    # Check individual nodes
    res = @s.execute_get(@s.url_for("_user/private/foo/bar/a.json"))
    result = JSON.parse(res.body);
    assert_equal(result["title"], "alfa", "Expected string value 'alfa'")
    assert_equal(result["unit"], 20, "Expected proper integer value '20'")
    res = @s.execute_get(@s.url_for("_user/private/foo/bar/b.json"))
    result = JSON.parse(res.body);
    assert_equal(result["title"], "beta", "Expected string value 'beta'")
    assert_equal(result["sakai:tags"].length, 2, "Expected multivalued property")
    
  end
  
end

Test::Unit::UI::Console::TestRunner.run(TC_Kern330Test)
