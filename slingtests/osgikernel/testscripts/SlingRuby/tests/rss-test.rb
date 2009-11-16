#!/usr/bin/env ruby


require 'sling/sling'
require 'sling/test'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers

class TC_RSSTest < SlingTest

  def test_valid_rss_file
    # Do a GET request to a valid RSS file.
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://newsrss.bbc.co.uk/rss/newsonline_uk_edition/front_page/rss.xml"})
    assert_equal(200, res.code.to_i)
  end

  def test_regular_file
    # Do a GET request to a valid RSS file.
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://www.google.com"})
    assert_equal(403, res.code.to_i)
  end

  def test_invalid_xml_file
    # Do a GET request to a valid RSS file.
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://www.w3schools.com/xml/note.xml"})
    assert_equal(403, res.code.to_i)
  end


  def huge_file
    # Do a GET request to a valid RSS file.
    res = @s.execute_get(@s.url_for("var/proxy/rss.json"), {"rss" => "http://mirror.ox.ac.uk/sites/releases.ubuntu.com/releases/karmic/ubuntu-9.10-desktop-i386.iso"})
    assert_equal(403, res.code.to_i)
  end



end

Test::Unit::UI::Console::TestRunner.run(TC_RSSTest)