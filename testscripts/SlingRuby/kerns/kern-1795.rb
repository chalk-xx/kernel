#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/users'
require 'sling/contacts'
require 'test/unit.rb'
include SlingUsers
include SlingFile
include SlingContacts

class TC_Kern1795Test < Test::Unit::TestCase
  include SlingTest

  def setup
    super
    @fm = FileManager.new(@s)
    @um = UserManager.new(@s)
    @cm = ContactManager.new(@s)
    m = Time.now.to_i.to_s
    @test_user1 = create_user "test_user1_#{m}", "Test", "User1"
    @test_user2 = create_user "test_user2_#{m}", "Test", "User2"  
    @test_group = create_group "g-test_group_#{m}", "Test Group"
  end
  
  def test_add_user_to_and_remove_user_from_group
    @s.switch_user(@test_user1)
    res = @s.execute_get(@s.url_for("/system/me.json"))
    @log.info("/system/me response #{res.inspect}")
    assert_equal("200", res.code, "Me servlet should return successfully")
    me = JSON.parse(res.body)
    counts = me['profile']['counts']
    @log.info("test_add_user_to_and_remove_user_from_group initial user counts are: #{counts.inspect}")   
    assert_equal(0, counts['contentCount'])
    assert_equal(0, counts['membershipsCount'])
    assert_equal(0, counts['contactsCount'])
    
    @s.switch_user(User.admin_user)
    @test_group.add_member @s, @test_user1.name, 'user'
    wait_for_indexer    
    @s.switch_user(@test_user1)
    res = @s.execute_get(@s.url_for("/system/me.json"))
    @log.info("after group.add_member /system/me response #{res.inspect}")
    assert_equal("200", res.code, "Me servlet should return successfully")
    me = JSON.parse(res.body)
    counts = me['profile']['counts']
    @log.info("after group.add_member user counts are: #{counts.inspect}")
    assert_equal(1, counts['membershipsCount'])
    
    @s.switch_user(User.admin_user)
    @test_group.remove_member @s, @test_user1.name, 'user'
    wait_for_indexer    
    @s.switch_user(@test_user1)
    res = @s.execute_get(@s.url_for("/system/me.json"))
    @log.info("after group.remove_member /system/me response #{res.inspect}")
    assert_equal("200", res.code, "Me servlet should return successfully")
    
    me = JSON.parse(res.body)
    counts = me['profile']['counts']
    @log.info("after group.remove_member user counts are: #{counts.inspect}")
    assert_equal(0, counts['membershipsCount'])
  end

  # see KERN-1003.rb
  def test_and_and_delete_content_counts
    @s.switch_user(@test_user1)
    res = @s.execute_get(@s.url_for("/system/me.json"))
    assert_equal("200", res.code, "Me servlet should return successfully")
    me = JSON.parse(res.body)
    counts = me['profile']['counts']
    @log.info("test_and_and_delete_content_counts() user counts are: #{counts.inspect}")   
    assert_equal(0, counts['contentCount'])
    assert_equal(0, counts['membershipsCount'])
    assert_equal(0, counts['contactsCount'])
    
    # test uploading a file
    res = @fm.upload_pooled_file('random.txt', 'This is some random content that should be stored in the pooled content area.', 'text/plain')
    wait_for_indexer
    assert_equal("201", res.code, "should be able to upload content")
    file = JSON.parse(res.body)
    id = file['random.txt']['poolId']
    url = @fm.url_for_pooled_file(id)
    res = @s.execute_get(@s.url_for("/system/me.json"))
    assert_equal("200", res.code, "Me servlet should return successfully")
    me = JSON.parse(res.body)
    counts = me['profile']['counts']
    @log.info("after fm.upload_pooled_file user counts are: #{counts.inspect}")
    assert_equal(1, counts['contentCount'], 'contentCount should be 1 after 1 upload')
    
    # test deleting the file    
    res = @s.execute_post(url, {":operation" => "delete"})
    assert_equal(200, res.code.to_i, "Expected to be able to delete the file.")
    wait_for_indexer #this was a solr delete
    res = @s.execute_get(@s.url_for("/system/me.json"))
    @log.info("/system/me response #{res.inspect}")
    assert_equal("200", res.code, "Me servlet should return successfully")
    
    me = JSON.parse(res.body)
    counts = me['profile']['counts']
    @log.info("user counts are: #{counts.inspect}")
    assert_equal(0, counts['contentCount'], 'contentCount should be 1 after 1 upload')
  end
  
  def test_add_contact_for_user    
    @s.switch_user(@test_user1)
    res = @s.execute_get(@s.url_for("/system/me.json"))
    @log.info("test_add_contact_for_user() /system/me response #{res.inspect}")
    assert_equal("200", res.code, "Me servlet should return successfully")
    me = JSON.parse(res.body)
    counts = me['profile']['counts']
    @log.info("test_add_contact_for_user initial user counts are: #{counts.inspect}")   
    assert_equal(0, counts['contentCount'])
    assert_equal(0, counts['membershipsCount'])
    assert_equal(0, counts['contactsCount'])
    
    create_connection(@test_user1, @test_user2) 
    
    @s.switch_user(@test_user1)
    res = @s.execute_get(@s.url_for("/system/me.json"))
    @log.info("/system/me response #{res.inspect}")
    assert_equal("200", res.code, "Me servlet should return successfully")
    me = JSON.parse(res.body)
    counts = me['profile']['counts']
    @log.info("user counts are: #{counts.inspect}")
    assert_equal(1, counts['contactsCount'], 'contentCount should be 1 after 1 invitation')
    
    #remove the contact
    res = @cm.remove_contact(@test_user2.name)
    @log.info("@cm.remove_contact() #{res.inspect}")    
    wait_for_indexer
    res = @s.execute_get(@s.url_for("/system/me.json"))
    @log.info("/system/me response #{res.inspect}")
    assert_equal("200", res.code, "Me servlet should return successfully")
    me = JSON.parse(res.body)
    counts = me['profile']['counts']
    @log.info("user counts are: #{counts.inspect}")
    assert_equal(0, counts['contactsCount'], 'contentCount should be 0 after 1 removal')
  end
    
  def create_connection(baseUser, otherUser) 
    @s.switch_user(baseUser)
    @cm.invite_contact(otherUser.name, "follower")
    @s.switch_user(otherUser)
    @cm.accept_contact(baseUser.name)
  end
    
  def teardown
    @s.switch_user(User.admin_user)
    super
  end
end
