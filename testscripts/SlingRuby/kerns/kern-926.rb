#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require './ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/users'
require 'test/unit.rb'
include SlingUsers
include SlingFile

class TC_Kern926Test < Test::Unit::TestCase
  include SlingTest

  #
  # These are test cases for KERN-925, KERN-926, KERN-927 and KERN-928
  # All of these are around viewers and managers for a file.
  #


  def setup
    super
    @fm = FileManager.new(@s)
    @um = UserManager.new(@s)
  end

  def test_manager_users
    m = Time.now.to_f.to_s.gsub('.', '')

    # Create some users
    creator = create_user("creator-#{m}")
    manager = create_user("manager-#{m}")
    viewer = create_user("viewer-#{m}")

    # Upload a file
    @s.switch_user(creator)
    res = @fm.upload_pooled_file('random.txt', 'This is some random content that should be stored in the pooled content area.', 'text/plain')
    file = JSON.parse(res.body)
    id = file['random.txt']['poolId']
    url = @fm.url_for_pooled_file(id)
    res = @fm.get_members(id)
    @log.info("Before Member Changes #{res.body} ")

    # Make sure that the other users cannot access it yet.
    @s.switch_user(manager)
    res = @s.execute_get(url)
    assert_equal(404, res.code.to_i, "Only the creator should be able to view the file at this point.")
    @s.switch_user(viewer)
    res = @s.execute_get(url)
    assert_equal(404, res.code.to_i, "Only the creator should be able to view the file at this point.")


    # Make somebody a viewer
    @s.switch_user(creator)
    res = @fm.manage_members(id, viewer.name, nil, nil, nil)
    assert_equal(200, res.code.to_i, "Expected to be able to manipulate the member lists as a creator.")
    @log.info("Added #{viewer.name} as a viewer ")

    res = @fm.get_members(id)
    @log.info("Added Viewer #{res.body} ")

    # Check if the viewer can see the file
    @s.switch_user(viewer)
    res = @s.execute_get(url)
    assert_equal(200, res.code.to_i, "The viewer should be able to view the file at this point.")


    # Add a manager
    @s.switch_user(creator)
    @fm.manage_members(id, nil, viewer.name, manager.name, nil)
    assert_equal(200, res.code.to_i, "Expected to be able to manipulate the member lists as a creator.")
    @log.info("Added #{manager.name} as a manager removed viewer #{viewer.name} ")

    res = @fm.get_members(id)
    @log.info("Added Manageri removed viewer #{res.body} ")

    # The viewer shouldn't have access anymore
    @s.switch_user(viewer)
    res = @s.execute_get(url)
    assert_equal(404, res.code.to_i, "The viewer should NOT be able to view the file when the manager has removed him. #{res.code} [#{res.body}]")

    # Get a member list.
    @s.switch_user(manager)
    res = @fm.get_members(id)
    assert_equal(200, res.code.to_i, "Expected to be able to get the members list.")
    members = JSON.parse(res.body)
    assert_equal(0, members["viewers"].length, "There should be no more viewers for this file.")
  end



  def test_search_me
    m = Time.now.to_f.to_s.gsub('.', '')

    # Create some users
    owner = create_user("creator2-#{m}")
    viewer = create_user("manager2-#{m}")
    groupuser = create_user("groupuser2-#{m}")

    @s.switch_user(owner)
    content = Time.now.to_f
    name = "random-#{content}.txt"
    res = @fm.upload_pooled_file(name, "Add the time to make it sort of random #{Time.now.to_f}.", 'text/plain')
    json = JSON.parse(res.body)
    id = json[name]['poolId']
    url = @fm.url_for_pooled_file(id)

    # Search the files that I manage .. should be 1
    # wait_for_indexer()
    sleep(5)
    res = @fm.search_my_managed("*")
    files = JSON.parse(res.body)
    assert_equal(1, files["total"], "Expected 1 file.")
    assert_equal(name, files["results"][0]["sakai:pooled-content-file-name"])

    # Grant the other user viewer rights.
    res = @fm.manage_members(id, viewer.name, nil, nil, nil)
    assert_equal(200, res.code.to_i, "Expected to be able to set member rights.")

    @s.switch_user(viewer)
    # Search the files that I can view .. should be 1
    # wait_for_indexer()
    sleep(5)
    res = @fm.search_my_viewed("*")
    files = JSON.parse(res.body)
    assert_equal(1, files["total"], "Expected 1 file.")
    assert_equal(name, files["results"][0]["sakai:pooled-content-file-name"])

    # The group user shouldn't be able to see anything (yet.)
    @s.switch_user(groupuser)
    res = @fm.search_my_viewed("*")
    files = JSON.parse(res.body)
    assert_equal(0, files["total"], "Expected 0 files.")


    @s.switch_user(owner)
    # Create a group, add the group user and give the group viewing rights.
    # This should then popup in that user's list of files he/she can view.
    group = @um.create_group("g-testgroup-#{m}")
    assert_not_nil(group, "Expected to be able to create a group.")
    group.add_members(@s, [groupuser.name])
    res = @fm.manage_members(id, group.name, nil, nil, nil)
    assert_equal("200",res.code)
    res = @s.execute_get("#{url}.tidy.10.json")
    @log.info("Got File at #{url} as #{res.body}")

    props  = @um.get_group_props(group.name)
    members = props["members"]
    assert_not_nil(members)
    assert_equal(members.include?(groupuser.name),true)

    @log.info("Got Group Props for #{group.name} as #{props["members"]} which contains #{groupuser.name}")



    # The group user should now see 1 file.
    @s.switch_user(groupuser)
    # is does groupuser have group as one of its principals, it should do
    props  = @um.get_user_props(groupuser.name)
    principals = props["principals"]
    assert_not_nil(principals)
    assert_equal(principals.include?(group.name),true)
    @log.info("Got User principals for #{groupuser.name} as #{principals} which contains #{group.name}")

    # wait_for_indexer()
    sleep(5)
    res = @fm.search_my_viewed("*")
    files = JSON.parse(res.body)
    assert_equal(1, files["total"], "Expected 1 file. #{res.body}")
  end

end
