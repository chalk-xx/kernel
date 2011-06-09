#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require './ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/file'
require 'sling/message'
require 'test/unit.rb'
include SlingInterface
include SlingUsers
include SlingMessage
include SlingFile

class TC_MyFileTest_891 < Test::Unit::TestCase
  include SlingTest

  def setup
    super
    @ff = FileManager.new(@s)
  end

  def test_upload_file
    m = Time.now.to_i.to_s
    @log.info("Creating user simon"+m)
    simon = create_user("simon"+m)

    @s.switch_user(simon)
    publicSimon = simon.public_path_for(@s)

    # Upload a couple of files to the user his public space.
    res = @s.execute_file_post(@s.url_for("/system/pool/createfile"), "alfa", "alfa", "This is some random content: alfaalfa.", "text/plain")
    assert_equal(201, res.code.to_i(), "Expected to be able to upload a file.")
	uploadresult = JSON.parse(res.body)
	alphaID = uploadresult['alfa']
	assert_not_nil(alphaID)

    res = @s.execute_file_post(@s.url_for("/system/pool/createfile"), "beta", "beta", "This is some random content: betabeta.", "text/plain")
    # This will return modified..
    assert_equal(201, res.code.to_i(), "Expected to be able to upload a file.")
	uploadresult = JSON.parse(res.body)
	betaID = uploadresult['beta']
    assert_not_nil(alphaID)

    # Create a tag.
    res = @ff.createTag("foobar", "#{publicSimon}/tags/footag")
    assert_equal(201, res.code.to_i(), "Expected to be able to create a tag.")
    # Get tag info
    res = @s.execute_get(@s.url_for("#{publicSimon}/tags/footag.json"))
    tag = JSON.parse(res.body)
    assert_not_nil(tag, "No response when creating a tag.")

    # Tag the alfa file.
    res = @ff.tag("/p/#{alphaID}", "#{publicSimon}/tags/footag")
    assert_equal(200, res.code.to_i(), "Expected to be able to tag an uploaded file.")

    # Tag a file with a non-existing tag.
    res = @ff.tag("/p/#{betaID}", "foobar")
    assert_equal(404, res.code.to_i(), "Tagging something with a non existing tag should return 404.")

    #Try uploading as anonymous
    @log.info("Check that Anon is denied ")
    @s.switch_user(SlingUsers::User.anonymous)
    res = @s.execute_file_post(@s.url_for("/system/pool/createfile"), "anon", "anon", "This is some random content: anonanon.", "text/plain")
    if ( res.code == "200" )
      assert_equal("-1",res.code,"Expected not be be able to upload a file as anon user "+res.body)
    end

    res = @ff.myfiles("*")
    myfiles = JSON.parse(res.body)
    assert_equal(0, myfiles["total"].to_i(), "Expected 0 files for anon.")

  end

  def teardown
    @created_users.each do |user|
      #@s.debug = true
      @s.switch_user(user)
      #@s.debug = false
    end

    super
  end

end

