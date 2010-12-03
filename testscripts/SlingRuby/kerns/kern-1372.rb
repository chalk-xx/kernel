#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
include SlingUsers
include SlingFile

class TC_Kern1372Test < Test::Unit::TestCase
  include SlingTest

  def add_pooled(filename)
      res = @fm.upload_pooled_file(filename, "Plain content", "text/plain")
      assert_equal("201", res.code, "Expected to be able to create pooled content")
      json = JSON.parse(res.body)
      json[filename]
  end

  def test_get_related_content
    @fm = FileManager.new(@s)
    m = Time.now.to_f.to_s.gsub('.', '')
    user = create_user("user-#{m}")
	other = create_user("other-#{m}")

    @s.switch_user(user)
    fileid = add_pooled("test#{m}.txt")
    fileurl = @s.url_for("/p/#{fileid}")
    onetag = "one#{m}"
    res = @fm.createTag(onetag, "/tags/#{onetag}")
    twotag = "two#{m}"
    res = @fm.createTag(twotag, "/tags/#{twotag}")
    res = @s.execute_post(fileurl, {
      ":operation" => "tag",
      "key" => "/tags/#{onetag}"
    })
    res = @s.execute_post(fileurl, {
      ":operation" => "tag",
      "key" => "/tags/#{twotag}"
    })

    @s.switch_user(other)
    fileids = (0..2).collect {|i|
      res = @fm.upload_pooled_file("test#{m}#{i}.txt", "Plain content", "text/plain")
      assert_equal("201", res.code, "Expected to be able to create pooled content")
      json = JSON.parse(res.body)
      fileid = json["test#{m}#{i}.txt"]
      res = @s.execute_post(@s.url_for("/p/#{fileid}.modifyAce.html"), {
        "principalId" => "everyone",
        "privilege@jcr:read" => "granted"
      })
      fileid
    }

    @s.switch_user(user)
    res = @s.execute_get("#{fileurl}.related.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(0, relateds.size)
    res = @s.execute_get("#{fileurl}.relatedpublic.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(0, relateds.size)

    @s.switch_user(other)
    res = @s.execute_post(@s.url_for("/p/#{fileids[0]}"), {
      "sakai:tags" => onetag,
      "sakai:permissions" => "everyone"
    })
    res = @s.execute_post(@s.url_for("/p/#{fileids[0]}"), {
      ":operation" => "tag",
      "key" => "/tags/#{onetag}"
    })

    @s.switch_user(user)
    res = @s.execute_get("#{fileurl}.related.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(1, relateds.size)
    assert_equal(fileids[0], relateds[0]["jcr:name"])
    res = @s.execute_get("#{fileurl}.relatedpublic.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(0, relateds.size)

    @s.switch_user(other)
    res = @s.execute_post(@s.url_for("/p/#{fileids[1]}"), {
      "sakai:tags" => [onetag, twotag],
      "sakai:permissions" => "public"
    })
    res = @s.execute_post(@s.url_for("/p/#{fileids[1]}"), {
      ":operation" => "tag",
      "key" => "/tags/#{onetag}"
    })
    res = @s.execute_post(@s.url_for("/p/#{fileids[1]}"), {
      ":operation" => "tag",
      "key" => "/tags/#{twotag}"
    })

    @s.switch_user(user)
    res = @s.execute_get("#{fileurl}.related.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(2, relateds.size)
    assert_equal(fileids[1], relateds[0]["jcr:name"])
    assert_equal(fileids[0], relateds[1]["jcr:name"])
    res = @s.execute_get("#{fileurl}.relatedpublic.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(1, relateds.size)
    assert_equal(fileids[1], relateds[0]["jcr:name"])

    # TODO Test directory locations.
    # TODO Test maximum of 10 returns.

  end

end
