#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'sling/contacts'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch
include SlingUsers
include SlingContacts


class TC_Kern759Test < SlingTest
  
  
  def test_private_group
    # Create a couple of user who are connected
    m = Time.now.to_i.to_s
	
    manager = create_user("user-manager-#{m}")
    viewer = create_user("user-viewer-#{m}")
    other = create_user("user-other-#{m}")
    admin = User.new("admin","admin")

	# Create a group 
    contactsgroup = create_group("g-test-group-#{m}")
	
	@s.switch_user(other)
	
    res = @s.execute_get(@s.url_for(Group.url_for(contactsgroup.name) + ".json"))
	assert_equal("200",res.code)
	
	@s.switch_user(admin)
	
	contactsgroup.update_properties(@s, { "rep:group-managers" => [ manager.name ], "rep:group-viewers" => [ viewer.name ] } )
    res = @s.execute_get(@s.url_for(Group.url_for(contactsgroup.name) + ".json"))
	assert_equal("200",res.code)
	puts(res.body)
	
	
	@s.switch_user(other)
	
    res = @s.execute_get(@s.url_for(Group.url_for(contactsgroup.name) + ".json"))
	assert_equal("404",res.code, res.body)
	res = contactsgroup.update_properties(@s, { "testing" => "Should Fail to Update" } )
	assert_equal("500",res.code, res.body)
	
	@s.switch_user(viewer)
	@s.debug = true
    res = @s.execute_get(@s.url_for(Group.url_for(contactsgroup.name) + ".json"))
	@s.debug = false
	assert_equal("200",res.code, res.body)
	res = contactsgroup.update_properties(@s, { "testing" => "Should Fail to Update" } ) 
	assert_equal("500",res.code, res.body)
	
	@s.switch_user(manager)
    res = @s.execute_get(@s.url_for(Group.url_for(contactsgroup.name) + ".json"))
	assert_equal("200",res.code, res.body)
	res = contactsgroup.update_properties(@s, { "testing" => "Should Be Ok" } ) 
	assert_equal("200",res.code, res.body)
	
	    
  end
  
end

Test::Unit::UI::Console::TestRunner.run(TC_Kern759Test)
