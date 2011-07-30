#!/usr/bin/env ruby

require './ruby-lib-dir.rb'

require 'sling/test'
require 'sling/users'
include SlingUsers

class TC_Kern2113Test < Test::Unit::TestCase
  include SlingTest

  def test_paging_of_messages
    # create test users
    recipient = create_test_user(1)
    sender = create_test_user(2)
    @s.switch_user(sender)
    
    # Test fewer than the maximum-page-size of results. That is 100 by default.
    (1..21).each do |i|
      res = send_message(sender.name, recipient.name, i)
      assert_equal('200', res.code, "Should be able to send message: #{res}\n#{res.body}")
    end
    # Wait for messages to be delivered.
    sleep(2)
    @s.switch_user(recipient)
    props = {
    "box" => "inbox",
    "category" => "message",
    "items" => "10",
    "page" => "0",
    "sortOn" => "_created",
    "sortOrder" => "desc"
    }
    # Get first page.
    res = @s.execute_get(@s.url_for("var/message/boxcategory-all.tidy.json"), props)
    assert_equal('200', res.code, "Should be able to fetch messages: #{res}\n#{res.body}")
    json = JSON.parse(res.body)
    assert_equal(21, json["total"])
    assert_equal(10, json["results"].length)
    # Get second page.
    props["page"] = "1"
    res = @s.execute_get(@s.url_for("var/message/boxcategory-all.tidy.json"), props)
    assert_equal('200', res.code, "Should be able to fetch messages: #{res}\n#{res.body}")
    json = JSON.parse(res.body)
    assert_equal(21, json["total"])
    assert_equal(10, json["results"].length)
    # Get last page.
    props["page"] = "2"
    res = @s.execute_get(@s.url_for("var/message/boxcategory-all.tidy.json"), props)
    assert_equal('200', res.code, "Should be able to fetch messages: #{res}\n#{res.body}")
    json = JSON.parse(res.body)
    assert_equal(21, json["total"])
    assert_equal(1, json["results"].length)
      
    # Now test more than the maximum-page-size of results.
    @s.switch_user(sender)
    (22..101).each do |i|
      res = send_message(sender.name, recipient.name, i)
      assert_equal('200', res.code, "Should be able to send message: #{res}\n#{res.body}")
    end
    sleep(2)
    @s.switch_user(recipient)
    props["items"] = "50"
    props["page"] = "0"
    res = @s.execute_get(@s.url_for("var/message/boxcategory-all.tidy.json"), props)
    assert_equal('200', res.code, "Should be able to fetch messages: #{res}\n#{res.body}")
    json = JSON.parse(res.body)
    # This is a lie, but it's the lie we expect.
    assert_equal(100, json["total"])
    assert_equal(50, json["results"].length)
    props["page"] = "1"
    res = @s.execute_get(@s.url_for("var/message/boxcategory-all.tidy.json"), props)
    assert_equal('200', res.code, "Should be able to fetch messages: #{res}\n#{res.body}")
    json = JSON.parse(res.body)
    # The truth can now be told.
    assert_equal(101, json["total"])
    assert_equal(50, json["results"].length)
  end
  
  def send_message(from, to, count)
    @s.execute_post(@s.url_for("~#{from}/message.create.html"), {
      "sakai:type" => "internal",
      "sakai:sendstate" => "pending",
      "sakai:messagebox" => "outbox",
      "sakai:to" => "internal:#{to}",
      "sakai:from" => "#{from}",
      "sakai:subject" => "Test message #{count}",
      "sakai:body" => "Test body",
      "_charset_" => "utf-8",
      "sakai:category" => "message"
    })
  end

end