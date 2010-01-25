#!/usr/bin/env ruby

require 'sling/sling'
require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
require 'time'
include SlingInterface
include SlingUsers
include SlingAuthz


class TC_Kern568Test < SlingTest
  
  def test_malformed_time
    m = Time.now.to_f.to_s.gsub('.', '')
    userid = "testuser-#{m}"
    user = create_user(userid)
    @s.switch_user(user)
    firstres = @s.execute_get(@s.url_for("/_user/message.chatupdate.json"))
    assert_equal(200, firstres.code.to_i)

    params = {"t" => "invalid"}
    res = @s.execute_get(@s.url_for("/_user/message.chatupdate.json"), params)
    assert_equal(200, res.code.to_i)
  end

  def test_correct_pulltime
    m = Time.now.to_f.to_s.gsub('.', '')
    userid = "testuser-#{m}"
    user = create_user(userid)
    @s.switch_user(user)
    firstres = @s.execute_get(@s.url_for("/_user/message.chatupdate.json"))
    assert_equal(200, firstres.code.to_i)

    sleep(1)

    # Note that ruby and the the server JVM  have to be in the same timezone for this to pass.
    # This should not pose a problem because testing is generally against localhost but is worth noting.
    msec = (Time.now.to_f * 1000).to_i
    now = Time.at(msec / 1000.0)
    expected = now.xmlschema(3)

    params = {"t" => msec}
    res = @s.execute_get(@s.url_for("/_user/message.chatupdate.json"), params)
    json = JSON.parse(res.body)
    assert_equal(expected, json["pulltime"])
  end

  # We want to make sure that, no matter what time is specified,
  # If this is the first request on record for the user, we update
  # and that the pulltime is recorded as the system's "now"
  def test_preserve_first_check
    m = Time.now.to_f.to_s.gsub('.', '')
    userid = "testuser-#{m}"
    user = create_user(userid)
    @s.switch_user(user)

    # Time.at accepts an unsigned long, so go as far into the future as we can.
    # This ends up sometime in 2038.
    sec = 2147483647
    time = Time.at(sec)
    schematime = time.xmlschema(3)
    params = {"t" => sec * 1000 }

    firstres = @s.execute_get(@s.url_for("/_user/message.chatupdate.json"), params)
    json = JSON.parse(firstres.body)
    assert_not_equal(schematime, json["pulltime"])
    assert_equal(true, json["update"])

    # On second request for the same time, we should see false
    res = @s.execute_get(@s.url_for("/_user/message.chatupdate.json"), params)
    json = JSON.parse(res.body)
    assert_equal(schematime, json["pulltime"])
    assert_equal(false, json["update"])
  end

  # Make sure that omitting the t parameter results in a matching time/pulltime
  # A better check might be to verify the system time on the server, but this is close
  def test_default_time
    m = Time.now.to_f.to_s.gsub('.', '')
    userid = "testuser-#{m}"
    user = create_user(userid)
    @s.switch_user(user)

    firstres = @s.execute_get(@s.url_for("/_user/message.chatupdate.json"))
    assert_equal(200, firstres.code.to_i)
    json = JSON.parse(firstres.body)

    msec = json["time"].to_i
    t = Time.at(msec / 1000.0)
    expected = t.xmlschema(3)
    assert_equal(expected, json["pulltime"])
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern568Test)
