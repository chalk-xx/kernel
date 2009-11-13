#!/usr/bin/env ruby


require 'sling/sling'
require 'sling/test'
require '../sling/file'
require '../sling/sites'
require '../sling/message'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingSites
include SlingMessage
include SlingFile

class TC_MyFileTest < SlingTest
  
  def setup
    super
    @ff = FileManager.new(@s)
    @ss = SiteManager.new(@s)
  end
  
  def test_upload_file
    m = Time.now.to_i.to_s
    puts("Creating user simon"+m)
    simon = create_user("simon"+m)
    puts("Creating user ian"+m)
    ian = create_user("ian"+m)
    puts("Creating user oszkar"+m)
    oszkar = create_user("oszkar"+m)
    
    # Create a site for each user.
    @s.switch_user(simon)
    @ss.create_site("/sites", title = "Simons Site", sitepath = "/simon")
    @s.switch_user(ian)
    @ss.create_site("/sites", title = "Ians Site", sitepath = "/ian")
    @s.switch_user(oszkar)
    @ss.create_site("/sites", title = "Oszkar Site", sitepath = "/oszkar")
    
    
    
    # Upload 2 files for user simon.
    @s.switch_user(simon)
    res = @ff.upload("/sites/simon/_files/myFile.txt", "/sites/simon/myFile")
    puts(res.body)
    assert_equal("200", res.code.to_s(), "Expected to upload a file.")
    file = JSON.parse(res.body)
    
    assert_not_nil(file,"No Response when uploading a file.")
    assert_not_nil(file['files'],"No files array in the output.")
    assert_not_nil(file['files'][0]['filename'],"No filename specified")
    assert_not_nil(file['files'][0]['id'],"No id specified")
    assert_not_nil(file['files'][0]['path'],"No path specified")
    
    #Get the content and check if it match up.
    res = @ff.download(file['files'][0]['id'])
    assert_equal("200", res.code.to_s(), "Expected to download the file url was (/_user/files/#{file['files'][0]['id']}). "+res.body)
    assert_equal(res.body, "<html><head><title>KERN 312</title></head><body><p>Should work</p></body></html>", "Content of the file does not match up.")
    
    
    #second file.
    res = @ff.upload("/sites/simon/_files/myFile.txt", "/sites/simon/myFile")
    
    
    #upload 3 files for ian.
    @s.switch_user(ian)
    @ff.upload("/sites/ian/_files/file1.txt", "/sites/ian")
    @ff.upload("/sites/ian/_files/file2.txt", "/sites/ian")
    @ff.upload("/sites/ian/_files/file3.txt", "/sites/ian")
    
    #upload 1 file for oszkar
    @s.switch_user(oszkar)
    @ff.upload("/sites/oszkar/_files/file1.txt", "/sites/oszkar")
    
    
    
    # Check myfiles search results
    @s.switch_user(simon)
    res = @ff.myfiles("*")
    myfiles = JSON.parse(res.body)
    assert_equal("2", myfiles["total"].to_s(), "Expected 2 files for simon.")
    
    
    @s.switch_user(ian)
    res = @ff.myfiles("*")
    myfiles = JSON.parse(res.body)
    assert_equal("3", myfiles["total"].to_s(), "Expected 3 files for ian.")
    
    
    @s.switch_user(oszkar)
    res = @ff.myfiles("*")
    myfiles = JSON.parse(res.body)
    assert_equal("1", myfiles["total"].to_s(), "Expected 1 files for oszar.")
    
    
    
    @s.switch_user(simon)
    #Test the creating of links.
    res = @ff.createlink(file['files'][0]['id'], "/sites/simon/_files/", "/sites/simon")
    assert_equal("200", res.code.to_s(), "Expected to create a link.")
    link = JSON.parse(res.body)
    assert_equal(true, link[0]["succes"], "Failed to create link.")
    
    # Check if link works
    res = @s.execute_get(@s.url_for(link[0]["path"]))
    
    assert_equal(res.body, "<html><head><title>KERN 312</title></head><body><p>Should work</p></body></html>", "Content of the link does not match up.")
    
    
    
    
    
    
  end
  
  def teardown
    @created_users.each do |user|
      @s.debug = true
      @s.switch_user(user)
      @s.debug = false
    end
    
    @s.switch_user(SlingUsers::User.admin_user())
    
    @s.delete_file("http://localhost:8080/sites/simon")
    @s.delete_file("http://localhost:8080/sites/ian")
    @s.delete_file("http://localhost:8080/sites/oszkar")
    
    puts("Deleted /sites/simon, /sites/ian, /sites/oszkar")
    super
  end
  
end

Test::Unit::UI::Console::TestRunner.run(TC_MyFileTest)
