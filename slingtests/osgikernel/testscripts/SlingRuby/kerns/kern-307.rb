#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

class TC_Kern307Test < SlingTest

=begin
  def test_node_edit
    m = Time.now.to_i.to_s
    node = create_node("some/test/path#{m}", {})
    writers = create_group("g-test-writers-#{m}")
    readers = create_group("g-test-readers-#{m}")
    @s.set_node_acl_entries(node, writers, { "jcr:removeNode" => "granted",
                                             "jcr:modifyProperties" => "granted",
                                             "jcr:removeChildNodes" => "granted",
                                             "jcr:write" => "granted", 
                                             "jcr:addChildNodes" => "granted" })
    @s.set_node_acl_entries(node, readers, { "jcr:read" => "granted" })
    everyone = SlingUsers::Group.new("everyone")
    @s.set_node_acl_entries(node, everyone, { "jcr:read" => "granted" })
    puts @s.get_node_acl_json(node)
    user = create_user("testwriter#{m}")
    writers.add_member(@s, user.name, "user")
    @s.switch_user(user)
    child = create_node("#{node}/child", {})
    assert_not_nil(child, "Expected node to be created")
  end
=end

  def try_content_upload(user)
    m = Time.now.to_i.to_s
    @s.switch_user(SlingUsers::User.admin_user)
    site = create_site("sites", "Test", "/test#{m}")
    @s.update_node_props(site.path, { "name" => "Test",
                                      "description" => "Test",
                                      "id" => "test",
                                      "sakai:site-template" => "/dev/_skins/original/original.html",
                                      "status" => "online" })
    create_file_node("#{site.path}/_pages/welcome", "content", "This is some test content")
    collaborators = create_group("g-collaborators#{m}")
    viewers = create_group("g-viewers#{m}")
    @s.update_node_props(site.path, "sakai:authorizables" => [ collaborators.name, viewers.name ] )
    collaborators.update_properties(@s, "sakai:site" => [ "/" + site.path ])
    viewers.update_properties(@s, "sakai:site" => [ "/" + site.path ])
    collaborators.add_member(@s, user.name, "user")
    @s.set_node_acl_entries(site.path, collaborators, { "jcr:removeNode" => "granted",
                                                   "jcr:modifyProperties" => "granted",
                                                   "jcr:removeChildNodes" => "granted",
                                                   "jcr:modifyAccessControl" => "granted",
                                                   "jcr:write" => "granted",
                                                   "jcr:read" => "granted",
                                                   "jcr:addChildNodes" => "granted" })
    @s.set_node_acl_entries(site.path, viewers, { "jcr:read" => "granted" })
    @s.switch_user(user)
    @s.debug = true
    res = create_file_node("#{site.path}/_pages/welcome", "content", "This is some other test content")
    @s.debug = false
    assert_equal(200, res.code, "Expected update to succeed")
    res = @s.execute_get(@s.url_for("#{site.path}/_pages/welcome/content"))
    assert_equal("This is some other test content", res.body, "Expected content update")
  
    res = create_file_node("#{site.path}/_pages/welcome2", "morecontent", "This is some test content")
    assert_equal(201, res.code, "Expected create to succeed")
    res = @s.execute_get(@s.url_for("#{site.path}/_pages/welcome2/morecontent"))
    assert_equal("This is some test content", res.body, "Expected content update")
  end

  def test_content_upload
    m = Time.now.to_i.to_s
    @s.log = true
    randomuser = create_user("randomuser#{m}")
    puts "FIRST------------------------"
    try_content_upload(randomuser)
    puts "SECOND------------------------"
    try_content_upload(randomuser)
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern307Test)

