#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

class TC_Kern325Test < SlingTest


  def test_site_template_versioning
    m = Time.now.to_i.to_s
    template_path = "/dummy_template#{m}"
    template_site = @sm.create_site("templates", "A Template", "#{template_path}")
    @s.execute_post(@s.url_for(template_path), "sakai:is-site-template" => "true")
    template = create_node("#{template_site.path}/a1#{m}", "fish" => "cat")
    site = @sm.create_site("sites", "Site test", "/testsite#{m}", "#{template_path}")
    versions = @s.versions(site.path)
    assert(versions.size > 1, "Expected node '#{site.path}' to get some versions back")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern325Test)

