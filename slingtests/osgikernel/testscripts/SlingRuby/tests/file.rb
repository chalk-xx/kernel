#!/usr/bin/env ruby


require 'sling/sling'
require 'sling/test'
require '../sling/file'
require '../sling/sites'
require '../sling/message'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingSites
include SlingMessage
include SlingFile

class TC_MyFileTest < SlingTest
  
  def setup
    super
    @ff = FileManager.new(@s)
    @ss = SiteManager.new(@s)
  end
  
  def test_upload_file
   
    
    @s.switch_user(SlingUsers::User.admin_user())
    @ff.upload("/sites/ian/_files/file1.txt", "/sites/ian")
    
    
  end
  
  
end

Test::Unit::UI::Console::TestRunner.run(TC_MyFileTest)