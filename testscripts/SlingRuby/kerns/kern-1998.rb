#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby 'require' search path
require './ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/users'
require 'test/unit.rb'
require 'sling/file'
include SlingUsers
include SlingFile

class TC_Kern1998Test < Test::Unit::TestCase
  include SlingTest

  def test_get_random_content_all_priority
    @fm = FileManager.new(@s)

    # create test users
    u1 = @um.create_test_user(1)
    @s.switch_user(u1)

    # create a new tag to work with
    m = Time.now.to_f.to_s.gsub('.', '')
    tagname = "test#{m}"
    res = @s.execute_post(@s.url_for("/tags/#{tagname}"), {'_charset_' => 'utf8', 'sakai:tag-name' => tagname, 'sling:resourceType' => 'sakai/tag'})
    assert_equal('201', res.code, 'Should be able to create a new tag.')

    # add some content and tag each thing added
    4.times do |i|
      m = Time.now.to_f.to_s.gsub('.', '')
      res = @fm.upload_pooled_file("random-#{m}.txt", 'Plain content', 'text/plain')
      assert_equal('201', res.code, 'Expected to be able to create pooled content')
      uploadresult = JSON.parse(res.body)
      poolid = uploadresult["random-#{m}.txt"]['poolId']

      res = @s.execute_post(@s.url_for("/p/#{poolid}"), {'_charset_' => 'utf8', ':operation' => 'tag', 'key' => "/tags/#{tagname}"})
      assert_equal('200', res.code, 'Should be able to tag content.')
    end

    # add some content but don't tag it to create the negative case
    2.times do |i|
      m = Time.now.to_f.to_s.gsub('.', '')
      res = @fm.upload_pooled_file("random-#{m}.txt", 'Plain content', 'text/plain')
      assert_equal('201', res.code, 'Expected to be able to create pooled content')
    end

    wait_for_indexer()

    # get some random content
    res = @s.execute_get(@s.url_for('/var/search/public/random-content.json'))
    assert_equal('200', res.code, 'Feed should always return positively.')
    output = JSON.parse(res.body)
    results = output['results']
    assert_equal(4, results.length)
    results.each do |result|
      assert_equal(true, (!result['sakai:tags'].nil? and result['sakai:tags'].length >= 1) || (!result['sakai:taguuid'].nil? and result['sakai:taguuid'].length >= 1) || !result['description'].nil? || result['hasPreview'] == 'true')
    end
  end

  def test_get_random_content_some_priority
    @fm = FileManager.new(@s)

    # create test users
    u1 = @um.create_test_user(1)
    @s.switch_user(u1)

    # create a new tag to work with
    m = Time.now.to_f.to_s.gsub('.', '')
    tagname = "test#{m}"
    res = @s.execute_post(@s.url_for("/tags/#{tagname}"), {'_charset_' => 'utf8', 'sakai:tag-name' => tagname, 'sling:resourceType' => 'sakai/tag'})
    assert_equal('201', res.code, 'Should be able to create a new tag.')

    # add some content and tag each thing added
    2.times do |i|
      m = Time.now.to_f.to_s.gsub('.', '')
      res = @fm.upload_pooled_file("random-#{m}.txt", 'Plain content', 'text/plain')
      assert_equal('201', res.code, 'Expected to be able to create pooled content')
      uploadresult = JSON.parse(res.body)
      poolid = uploadresult["random-#{m}.txt"]['poolId']

      res = @s.execute_post(@s.url_for("/p/#{poolid}"), {'_charset_' => 'utf8', ':operation' => 'tag', 'key' => "/tags/#{tagname}"})
      assert_equal('200', res.code, 'Should be able to tag content.')
    end

    # add some content but don't tag it to create the negative case
    4.times do |i|
      m = Time.now.to_f.to_s.gsub('.', '')
      res = @fm.upload_pooled_file("random-#{m}.txt", 'Plain content', 'text/plain')
      assert_equal('201', res.code, 'Expected to be able to create pooled content')
    end

    wait_for_indexer()

    # get some random content
    res = @s.execute_get(@s.url_for('/var/search/public/random-content.json'))
    assert_equal('200', res.code, 'Feed should always return positively.')
    output = JSON.parse(res.body)
    results = output['results']
    assert_equal(4, results.length)

    priority = 0
    standard = 0
    results.each do |result|
      if (!result['sakai:tags'].nil? && result['sakai:tags'].length >= 1) || (!result['sakai:taguuid'].nil? && result['sakai:taguuid'].length >= 1) || !result['description'].nil? || result['hasPreview'] == 'true'
        priority += 1
      else
        standard += 1
      end
    end

    assert_equal(2, priority, 'Should have found both priority items')
    assert_equal(2, standard, 'Should have some non-priority items')
  end
end
