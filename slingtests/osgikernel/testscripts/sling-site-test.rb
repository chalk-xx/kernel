#!/usr/bin/env ruby

require 'sling-interface.rb'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingSites

class TC_MySiteTest < Test::Unit::TestCase

  def setup
    @s = Sling.new()
    @um = UserManager.new(@s)
    @sm = SiteManager.new(@s)
    @test_site = "var/sites/some_test_site"
  end

  def teardown
    @s.switch_user(User.admin_user)
    @s.delete_node(@test_site)
  end

  def do_site_create
    res = @sm.create_site(@test_site)
    assert_equal(201, res.code.to_i, "Expected site to be created")
    props = @s.get_node_props(@test_site)
    assert_equal("sakai/site", props["sling:resourceType"], "Expected resource type to be set")
  end

  def test_create_site
    do_site_create()
  end

  def test_read_default_site
    do_site_create()
    @s.debug = true
    res = @s.execute_get_with_follow(@s.url_for(@test_site))
    puts res.body
    assert_equal(200, res.code.to_i, "Expected site to be able to see site")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_MySiteTest)

