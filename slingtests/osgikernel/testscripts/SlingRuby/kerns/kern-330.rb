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
    path = user1.private_path_for()
    res = @s.execute_post(@s.url_for("#{path}/test/b"), {"foo" => "bar"})
    
    # Batch post to private store
    str = [{
          "url" => "#{path}/test/a",
          "method" => "POST",
          "parameters" => {
              "title" => "alfa",
              "foo" => "bar",
              "unit" => 10,
              "unit@TypeHint" => "Long"
      }
    },
    {
          "url" => "#{path}/test/c",
          "method" => "POST",
          "parameters" => {
              "title" => "charlie",
              "unit" => 30,
              "unit@TypeHint" => "Long"
      }
    },
    {
          "url" => "#{path}/test/c",
          "method" => "POST",
          "parameters" => {
              "title" => "charlieRedux",
              "unit" => 40,
              "unit@TypeHint" => "Long"
      }
    },
    {
          "url" => "#{path}/test/a.json",
          "method" => "GET",
          "parameters" => {}
    },
    {
          "url" => "#{path}/test/c.json",
          "method" => "GET",
          "parameters" => {}
    }
    ]
    
    parameters = {
      "requests" => JSON.generate(str)
    }
    
    res = @s.execute_post(@s.url_for("system/batch"), parameters)
    
    jsonRes = JSON.parse(res.body)
    
    assert_equal(jsonRes[0]["url"], "#{path}/test/a")
    assert_equal(jsonRes[0]["status"], 201, "Expexted to get a created statuscode.")
    assert_equal(jsonRes[1]["url"], "#{path}/test/c")
    assert_equal(jsonRes[1]["status"], 201, "Expexted to get a created statuscode.")
    assert_equal(jsonRes[2]["url"], "#{path}/test/c")
    assert_equal(jsonRes[2]["status"], 200, "Expected to be get a modified statuscode.")
    assert_equal(jsonRes[3]["url"], "#{path}/test/a.json")
    assert_equal(jsonRes[3]["status"], 200, "Expexted to get a proper statuscode.")
    aBody = JSON.parse(jsonRes[3]["body"])
    assert_equal(aBody["unit"], 10);
  end
  
  def test_accessdenied
    
    m = Time.now.to_i.to_s
    user2 = create_user("user2-"+m)
    
    homefolder = user2.home_folder_for()
    
    @s.switch_user(user2)
    str = [{
          "url" => "#{homefolder}/public/admin/foo/bar",
          "method" => "POST",
          "data" => {
              "title" => "alfa",
              "foo" => "bar",
              "unit" => 10,
              "unit@TypeHint" => "Long"
      }
    },
    {
          "url" => "#{homefolder}/private/foo/bar",
          "method" => "POST",
          "data" => {
              "title" => "beta",
              "unit" => 20,
              "unit@TypeHint" => "Long"
      }
    }
    ]
    
    parameters = {
      "requests" => JSON.generate(str)
    }
    
    
    res = @s.execute_post(@s.url_for("system/batch"), parameters)
    
    jsonRes = JSON.parse(res.body)
    
    assert_equal(jsonRes[0]["url"], "#{homefolder}/public/admin/foo/bar")
    assert_equal(jsonRes[0]["status"], 500, "Expexted access denied.")
    assert_equal(jsonRes[1]["url"], "#{homefolder}/private/foo/bar")
    assert_equal(jsonRes[1]["status"], 201, "Expected to get a created statuscode.")
    
  end
  
  def test_values
    m = Time.now.to_i.to_s
    user3 = create_user("user3-"+m)
    
    homefolder = user3.home_folder_for()
    @s.switch_user(user3)
    str = [
    {
          "url" => "#{homefolder}/private/foo/bar/a",
          "method" => "POST",
          "parameters" => {
              "title" => "alfa",
              "unit" => 20,
              "unit@TypeHint" => "Long"
      }
    },{
          "url" => "#{homefolder}/private/foo/bar/b",
          "method" => "POST",
          "parameters" => {
              "title" => "beta",
              "sakai:tags" => ["a", "b"],
      }
    }
    ]
    
    parameters = {
      "requests" => JSON.generate(str)
    }
    
    
    res = @s.execute_post(@s.url_for("system/batch"), parameters)
    
    # Check batch post response
    jsonRes = JSON.parse(res.body)
    assert_equal(jsonRes[0]["url"], "#{homefolder}/private/foo/bar/a")
    assert_equal(jsonRes[0]["status"], 201, "Expexted to get a created statuscode.")
    assert_equal(jsonRes[1]["url"], "#{homefolder}/private/foo/bar/b")
    assert_equal(jsonRes[1]["status"], 201, "Expected to get a created statuscode.")
    
    # Check individual nodes
    res = @s.execute_get(@s.url_for("#{homefolder}/private/foo/bar/a.json"))
    result = JSON.parse(res.body);
    assert_equal(result["title"], "alfa", "Expected string value 'alfa'")
    assert_equal(result["unit"], 20, "Expected proper integer value '20'")
    res = @s.execute_get(@s.url_for("#{homefolder}/private/foo/bar/b.json"))
    result = JSON.parse(res.body);
    assert_equal(result["title"], "beta", "Expected string value 'beta'")
    assert_equal(result["sakai:tags"].length, 2, "Expected multivalued property")
    
  end
  
  def test_doGet404
    #
    # This test checks some sling syntax.
    # If one of the requests is a GET to a none existing resource,
    # Sling sends a 404 on the initial requests, instead of the wrapped one.
    #
    
    m = Time.now.to_i.to_s
    user3 = create_user("user3-"+m)
    homefolder = user3.home_folder_for()
    
    @s.switch_user(user3)
    str = [
    {
          "url" => "#{homefolder}/private/random/bla",
          "method" => "POST",
          "parameters" => {"foo" => "bar"}
    },
    {
          "url" => "/this/is/a/none/existing/node",
          "method" => "GET",
          "parameters" => {}
    }
    ]
    parameters = {
      "requests" => JSON.generate(str)
    }
    res = @s.execute_post(@s.url_for("system/batch"), parameters)
    assert_equal(200, res.code.to_i, "Batch servlet should always return a 200 (with good parameters)")
    result = JSON.parse(res.body)
    assert_equal(201, result[0]["status"], "Expected a created status code.")
    assert_equal(404, result[1]["status"], "Expected a not found status code.")
  end
  
end

Test::Unit::UI::Console::TestRunner.run(TC_Kern330Test)
