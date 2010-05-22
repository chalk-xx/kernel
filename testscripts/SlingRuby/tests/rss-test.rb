#!/usr/bin/env ruby


require 'sling/sling'
require 'sling/test'
require 'test/unit.rb'
include SlingInterface
include SlingUsers

class TC_RSSTest < Test::Unit::TestCase
  include SlingTest

  def test_valid_rss_file
    # Do a GET request to a valid RSS file.
    puts("Getting BBCNews")
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://newsrss.bbc.co.uk/rss/newsonline_uk_edition/front_page/rss.xml"})
    puts("Done Getting BBCNews")
    assert_equal(200, res.code.to_i, "This is a valid XML file, this should return 200.")
  end

  def test_regular_file
    # Do a GET request to a non XML file.
    puts("Getting Google.com")
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://www.google.com"})
    puts("Done Getting Google.com")
    assert_equal(403, res.code.to_i, "This is not an XML file. Service should return 403.")
  end

  def test_invalid_xml_file
    # Do a GET request to a valid XML file but it is not an RSS file.
    puts("Getting W3Schools.com")
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://www.w3schools.com/xml/note.xml"})
    puts("Done Getting W3Schools.com")
    assert_equal(403, res.code.to_i, "This is a plain XML (non-RSS) file. Service should return 403.")
  end


  def test_big_file
    # Do a GET request to a huge file.
    puts("Getting Huge file")
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://ftp.belnet.be/packages/apache/sling/org.apache.sling.launchpad.app-5-incubator-bin.tar.gz"})
    puts("Done Getting Huge file")
    assert_equal(403, res.code.to_i, "This file is way to big. Service should return 403")
  end



end

