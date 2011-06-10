#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require './ruby-lib-dir.rb'

require 'sling/test'
include SlingUsers

class TC_Kern929Test < Test::Unit::TestCase
  include SlingTest

  def test_import_content_from_json_to_profile
    # create a new user
    m = Time.now.to_f.to_s.gsub('.', '')
    userid = "testuser-#{m}"
    password = "testuser"
    @s.switch_user(User.admin_user())
    res = @s.execute_post(@s.url_for("#{$USER_URI}"), {
      ":name" => userid,
      "pwd" => password,
      "pwdConfirm" => password,
      "_charset_" => "UTF-8"
    })
    assert_equal("200", res.code, "Should have created user as admin")
    testuser = User.new(userid)
    public = testuser.public_path_for(@s)

    # create authprofile "aboutme" json string
    hobbies = "#{m}-Card Draw Poker"
    json_hobbies = "\"hobbies\": {\"value\":\"#{hobbies}\"}"

    aboutme = "#{m}-I like to play Poker"
    json_aboutme = "\"aboutme\": {\"value\":\"#{aboutme}\"}"

    academicinterests = "#{m}-reading"
    json_academicinterests = "\"academicinterests\": {\"value\":\"#{academicinterests}\"}"

    personalinterests = "#{m}-writing"
    json_personalinterests = "\"personalinterests\": {\"value\":\"#{personalinterests}\"}"

    profilecontent = "{\"aboutme\": {\"elements\": { #{json_hobbies}, #{json_aboutme}, #{json_academicinterests}, #{json_personalinterests} }}}"

    # post aboutme json to authprofile
    res = @s.execute_post(@s.url_for("#{public}/authprofile"), {
          ":content" => profilecontent,
          ":contentType" => "json",
          ":operation" => "import",
          ":removeTree" => "true",
          ":replace" => "true",
          ":replaceProperties" => "true",
          "_charset_" => "UTF-8"
        })
    assert_equal("200", res.code, "Should have updated the user profile data")

    # get the user's authprofile
    res = @s.execute_get(@s.url_for("#{public}/authprofile.json"))
    assert_equal("200", res.code, "Should have read the user profile data")
    json = JSON.parse(res.body)

    # check the users aboutme profile data is correct.
    assert_equal(json["aboutme"]["elements"]["hobbies"]["value"], hobbies, "Profile property not set")
    assert_equal(json["aboutme"]["elements"]["aboutme"]["value"], aboutme, "Profile property not set")
    assert_equal(json["aboutme"]["elements"]["academicinterests"]["value"], academicinterests, "Profile property not set")
    assert_equal(json["aboutme"]["elements"]["personalinterests"]["value"], personalinterests, "Profile property not set")

  end

end
