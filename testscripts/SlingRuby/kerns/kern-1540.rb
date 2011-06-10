#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require './ruby-lib-dir.rb'

require 'sling/test'
include SlingUsers

class TC_Kern1540Test < Test::Unit::TestCase
  include SlingTest

  def test_calendar_in_jcr
    m = Time.now.to_f.to_s.gsub('.', '')
    calendarpath = "/testinjcr-#{m}/testcalendar";
    @s.switch_user(User.admin_user())
    res = @s.execute_post(@s.url_for("#{calendarpath}"), {
      "sling:resourceType" => "sakai/calendar"
    })
    assert_equal("201", res.code, "Admin should be able to create rooted calendar")
    res = @s.execute_get(@s.url_for("#{calendarpath}.event.ics"))
    assert_equal("200", res.code, "Admin should find rooted calendar")
    @log.info("Empty calendar = '#{res.body}'")
    assert(res.body.start_with?("BEGIN:VCALENDAR"), "Not ICS : ${res.body}")
    assert(!res.body.include?("BEGIN:VEVENT"), "Not empty : ${res.body}")
    res = @s.execute_post(@s.url_for("#{calendarpath}/firstevent"), {
      "sling:resourceType" => "sakai/calendar-event",
      "sakai:vcal-DTSTART" => "20110107T172000Z",
      "sakai:vcal-DTEND" => "20110107T180000Z",
      "sakai:vcal-SUMMARY" => "Big party"
    })
    assert_equal("201", res.code, "Admin should be able to create event")
    res = @s.execute_get(@s.url_for("#{calendarpath}.event.ics"))
    assert_equal("200", res.code, "Admin should find rooted calendar")
    @log.info("Calendar with explicit selector = '#{res.body}'")
    assert(res.body.include?("BEGIN:VEVENT"), "Should have an event : ${res.body}")
    assert(res.body.include?("SUMMARY:Big party"), "Missing summary : ${res.body}")
    res = @s.execute_post(@s.url_for("#{calendarpath}/secondevent"), {
      "sling:resourceType" => "sakai/calendar-vevent",
      "sakai:vcal-DTSTART" => "20110207T172000Z",
      "sakai:vcal-DTEND" => "20110207T180000Z",
      "sakai:vcal-SUMMARY" => "Lesser party"
    })
    assert_equal("201", res.code, "Admin should be able to create event")
    res = @s.execute_get(@s.url_for("#{calendarpath}.ics"))
    assert_equal("200", res.code, "Admin should find rooted calendar")
    @log.info("Calendar without explicit selector = '#{res.body}'")
    assert(res.body.include?("BEGIN:VEVENT"), "Should have an event : ${res.body}")
    assert(res.body.include?("SUMMARY:Lesser party"), "Missing summary : ${res.body}")
    assert(!res.body.include?("SUMMARY:Big party"), "Should only have one summary : ${res.body}")
  end

  def test_calendar_in_user_home
    m = Time.now.to_f.to_s.gsub('.', '')
    user = create_user("user-#{m}")
    home = user.home_path_for(@s)
    public = user.public_path_for(@s)
    @s.switch_user(user)
    calendarpath = "#{public}/testcalendar";
    res = @s.execute_post(@s.url_for("#{calendarpath}"), {
      "sling:resourceType" => "sakai/calendar"
    })
    assert_equal("201", res.code, "User should be able to create calendar")
    res = @s.execute_get(@s.url_for("#{calendarpath}.event.ics"))
    assert_equal("200", res.code, "User should find calendar")
    @log.info("Empty calendar = '#{res.body}'")
    assert(res.body.start_with?("BEGIN:VCALENDAR"), "Not ICS : ${res.body}")
    assert(!res.body.include?("BEGIN:VEVENT"), "Not empty : ${res.body}")
    res = @s.execute_post(@s.url_for("#{calendarpath}/firstevent"), {
      "sling:resourceType" => "sakai/calendar-event",
      "sakai:vcal-DTSTART" => "20110107T172000Z",
      "sakai:vcal-DTEND" => "20110107T180000Z",
      "sakai:vcal-SUMMARY" => "Big party"
    })
    assert_equal("201", res.code, "User should be able to create event")
    res = @s.execute_get(@s.url_for("#{calendarpath}.event.ics"))
    assert_equal("200", res.code, "User should find calendar")
    @log.info("Calendar with explicit selector = '#{res.body}'")
    assert(res.body.include?("BEGIN:VEVENT"), "Should have an event : ${res.body}")
    assert(res.body.include?("SUMMARY:Big party"), "Missing summary : ${res.body}")
    res = @s.execute_post(@s.url_for("#{calendarpath}/secondevent"), {
      "sling:resourceType" => "sakai/calendar-vevent",
      "sakai:vcal-DTSTART" => "20110207T172000Z",
      "sakai:vcal-DTEND" => "20110207T180000Z",
      "sakai:vcal-SUMMARY" => "Lesser party"
    })
    assert_equal("201", res.code, "User should be able to create event")
    res = @s.execute_get(@s.url_for("#{calendarpath}.ics"))
    assert_equal("200", res.code, "User should find calendar")
    @log.info("Calendar without explicit selector = '#{res.body}'")
    assert(res.body.include?("BEGIN:VEVENT"), "Should have an event : ${res.body}")
    assert(res.body.include?("SUMMARY:Lesser party"), "Missing summary : ${res.body}")
    assert(!res.body.include?("SUMMARY:Big party"), "Should only have one summary : ${res.body}")
  end

end
