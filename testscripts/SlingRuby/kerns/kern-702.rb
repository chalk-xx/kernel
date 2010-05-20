#!/usr/bin/env ruby

require 'set'
require 'sling/test'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

class TC_Kern702Test < SlingTest

  def test_create_site_without_template
    m = Time.now.to_f.to_s.gsub('.', '_')
    siteid = "testsite#{m}"
    sitename = "Test Site #{m}"
    sitecreator = create_user("testuser#{m}")
    @s.switch_user(sitecreator)
    res = @s.execute_post(@s.url_for("/sites.createsite.json"),
      ":sitepath" => "/#{siteid}",
      "name" => sitename,
      "description" => sitename,
      "id" => siteid)
    assert_equal("200", res.code, "Expected to create site: #{res.body}")
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    assert_equal("200", res.code, "Expected to get site: #{res.body}")
    puts res.body
    props = JSON.parse(res.body)
    # assert_equal("/var/templates/site/systemtemplate", props["sakai:site-template"])
    assert_equal(sitename, props["name"])
    assert_equal(sitename, props["description"])
    newname = "New Name for Test Site #{m}"
    res = @s.execute_post(@s.url_for("/sites/#{siteid}"), "name" => newname)
    assert_equal("200", res.code, "Should be able to change site " + res.body)
    res = @s.execute_get(@s.url_for("/sites/#{siteid}.json"))
    puts res.body
    props = JSON.parse(res.body)
    assert_equal(newname, props["name"])
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern702Test)
