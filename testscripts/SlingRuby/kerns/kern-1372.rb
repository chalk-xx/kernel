#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require './ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
include SlingUsers
include SlingFile

class TC_Kern1372Test < Test::Unit::TestCase
  include SlingTest

  def add_node_to_directory(node, directorystring)
    directorytag = "directory/#{directorystring}"
    directorytagnode = "/tags/#{directorytag}"
    res = @fm.createTag(directorytag, directorytagnode)
    assert(200 == res.code.to_i || 201 == res.code.to_i, "@fm.createTag(#{directorytag}, #{directorytagnode})")
    res = @fm.tag(node, directorytagnode)
    assert_equal("200", res.code, "@fm.tag(#{node}, #{directorytagnode})")
    for subpath in directorystring.split('/')
      tagnode = "/tags/#{subpath}"
      res = @fm.createTag(subpath, tagnode)
      assert(200 == res.code.to_i || 201 == res.code.to_i, "@fm.createTag(#{subpath}, #{tagnode})")
      res = @fm.tag(node, tagnode)
      assert_equal("200", res.code, "@fm.tag(#{node}, #{tagnode})")
    end
  end

  def add_pooled(filename)
      res = @fm.upload_pooled_file(filename, "Plain content", "text/plain")
      assert_equal("201", res.code, "Expected to be able to create pooled content")
      json = JSON.parse(res.body)
      contentid = json[filename]['poolId']
      res = @s.execute_post(@s.url_for("/p/#{contentid}"), {
        "sakai:permissions" => "everyone"
      })
      assert_equal("200", res.code, "sakai:permissions => everyone")
      res = @s.execute_post(@s.url_for("/p/#{contentid}.modifyAce.html"), {
        "principalId" => "everyone",
        "privilege@jcr:read" => "granted"
      })
      assert_equal("200", res.code, "principalId => everyone; privilege@jcr:read => granted")
      contentid
  end

  def test_get_related_content
    @log.level = Logger::INFO
    @fm = FileManager.new(@s)
    m = Time.now.to_f.to_s.gsub('.', '')
    user = create_user("user-#{m}")
	other = create_user("other-#{m}")

    @s.switch_user(user)
    fileid = add_pooled("test#{m}.txt")
    @log.debug("fileid=#{fileid}")
    fileurl = @s.url_for("/p/#{fileid}")
    onetag = "one#{m}"
    res = @fm.createTag(onetag, "/tags/#{onetag}")
    assert_equal("201", res.code, "create tag one")
    twotag = "two#{m}"
    res = @fm.createTag(twotag, "/tags/#{twotag}")
    assert_equal("201", res.code, "create tag two")
    res = @s.execute_post(fileurl, {
      ":operation" => "tag",
      "key" => "/tags/#{onetag}"
    })
    assert_equal("200", res.code, "tag file with tag one")
    res = @s.execute_post(fileurl, {
      ":operation" => "tag",
      "key" => "/tags/#{twotag}"
    })
    assert_equal("200", res.code, "tag file with tag two")

    # create two files as other user; do not tag yet
    @s.switch_user(other)
    otherfileids = (0..2).collect {|i|
      add_pooled("test#{m}#{i}.txt")
    }
    @log.debug(otherfileids)

    @s.switch_user(user)
    wait_for_indexer()
    res = @s.execute_get("#{fileurl}.related.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(0, relateds.size)
    res = @s.execute_get("#{fileurl}.relatedpublic.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(0, relateds.size)

    @log.debug("tag first, other file with tag one; now should be related")
    @s.switch_user(other)
    res = @s.execute_post(@s.url_for("/p/#{otherfileids[0]}"), {
      ":operation" => "tag",
      "key" => "/tags/#{onetag}"
    })
    assert_equal("200", res.code, "tag first, other file with tag one")

    @log.debug("now should have one related content")
    @s.switch_user(user)
    wait_for_indexer()
    res = @s.execute_get("#{fileurl}.related.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(1, relateds.size)
    assert_equal(otherfileids[0], relateds[0]["jcr:name"])
    res = @s.execute_get("#{fileurl}.relatedpublic.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(0, relateds.size)

    @s.switch_user(other)
    res = @s.execute_post(@s.url_for("/p/#{otherfileids[1]}"), {
      "sakai:permissions" => "public"
    })
    res = @s.execute_post(@s.url_for("/p/#{otherfileids[1]}.modifyAce.html"), {
      "principalId" => "anonymous",
      "privilege@jcr:read" => "granted"
    })
    res = @s.execute_post(@s.url_for("/p/#{otherfileids[1]}"), {
      ":operation" => "tag",
      "key" => "/tags/#{onetag}"
    })
    res = @s.execute_post(@s.url_for("/p/#{otherfileids[1]}"), {
      ":operation" => "tag",
      "key" => "/tags/#{twotag}"
    })

    @s.switch_user(user)
    wait_for_indexer()
    res = @s.execute_get("#{fileurl}.related.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(2, relateds.size)
    assert_equal(otherfileids[1], relateds[0]["jcr:name"])
    assert_equal(otherfileids[0], relateds[1]["jcr:name"])
    res = @s.execute_get("#{fileurl}.relatedpublic.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(1, relateds.size)
    assert_equal(otherfileids[1], relateds[0]["jcr:name"])
  end

  def test_related_by_directory
    @log.level = Logger::DEBUG
    @fm = FileManager.new(@s)
    m = Time.now.to_f.to_s.gsub('.', '')
    user = create_user("user-#{m}")
	other = create_user("other-#{m}")
	directorypath = "CollegeAnnex-#{m}/Psychology-#{m}"
	halfmatch = "CollegeAnnex-#{m}/Biology-#{m}"

    @s.switch_user(user)
    fileid = add_pooled("test#{m}.txt")
    filepath = "/p/#{fileid}"
    add_node_to_directory(filepath, directorypath)

    @s.switch_user(other)
    otherfileids = (0..2).collect {|i|
      add_pooled("test#{m}#{i}.txt")
    }

    @s.switch_user(user)
    fileurl = @s.url_for(filepath)
    res = @s.execute_get("#{fileurl}.related.tidy.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(0, relateds.size)
    res = @s.execute_get("#{fileurl}.relatedpublic.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(0, relateds.size)

    @s.switch_user(other)
    add_node_to_directory("/p/#{otherfileids[1]}", directorypath)
    add_node_to_directory("/p/#{otherfileids[0]}", halfmatch)

    @s.switch_user(user)
    wait_for_indexer()
    res = @s.execute_get("#{fileurl}.related.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(2, relateds.size)
    assert_equal(otherfileids[1], relateds[0]["jcr:name"])
    assert_equal(otherfileids[0], relateds[1]["jcr:name"])
  end


  def test_limit_related_content
    @fm = FileManager.new(@s)
    m = Time.now.to_f.to_s.gsub('.', '')
    user = create_user("user-#{m}")
	other = create_user("other-#{m}")

    @s.switch_user(user)
    fileid = add_pooled("test#{m}.txt")
    fileurl = @s.url_for("/p/#{fileid}")
    onetag = "one#{m}"
    res = @fm.createTag(onetag, "/tags/#{onetag}")
    res = @s.execute_post(fileurl, {
      ":operation" => "tag",
      "key" => "/tags/#{onetag}"
    })

    @s.switch_user(other)
    otherfileids = (0..20).collect {|i|
      otherfileid = add_pooled("test#{m}#{i}.txt")
      res = @s.execute_post(@s.url_for("/p/#{otherfileid}"), {
        ":operation" => "tag",
        "key" => "/tags/#{onetag}"
      })
      otherfileid
    }

    @s.switch_user(user)
    wait_for_indexer()
    res = @s.execute_get("#{fileurl}.related.json")
    assert_equal("200", res.code, "Related feed not working")
    relateds = JSON.parse(res.body)
    assert_equal(10, relateds.size, "Should have limit of 10 related files in feed")
  end

end
