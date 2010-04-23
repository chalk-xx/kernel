#!/usr/bin/env ruby

require 'sling/test'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch
include SlingUsers

class TC_Kern740Test < SlingTest
  
  #
  # Test changing the admin password using the form auth mechanism.
  #
  
  def teardown 
    # Reset the password
    @s.trustedauth = false
    admin2 = User.new("admin","2admin2")
	@s.switch_user(admin2)
	puts("401 is Ok")
	@s.execute_get(@s.url_for("/var/cluster/user.json?performing_teardown"))
	puts("401 is Ok")
	admin2.change_password(@s,"admin")
	super
  end
  
  def test_change_password_basicAuth
    m = Time.now.to_i.to_s
    @s.trustedauth = false
	@s.execute_get(@s.url_for("/var/cluster/user.json?Starting_Basic_AuthTest"))
	puts("Changing Admin Password with Basic Auth")
	runChangePassword()
	puts("Done Changing Admin Password with Basic Auth")
	@s.execute_get(@s.url_for("/var/cluster/user.json?Done_Basic_AuthTest"))
  end
  
  def test_change_password_trustedAuth
    m = Time.now.to_i.to_s
    @s.trustedauth = true
	@s.execute_get(@s.url_for("/var/cluster/user.json?Starting_Trusted_AuthTest"))
	puts("Changing Admin Password with Trusted Auth")
	runChangePassword()
	puts("Done Changing Admin Password with Trusted Auth")
	@s.execute_get(@s.url_for("/var/cluster/user.json?Done_Trusted_AuthTest"))
  end

  def runChangePassword
    admin = User.new("admin","admin")
	@s.switch_user(admin)
    res = admin.update_properties(@s,"testproperty" => "newvalue")
	assert_equal("200",res.code,res.body)

	res = admin.change_password(@s,"2admin2")
	
	assert_equal("200",res.code,res.body)

    admin2 = User.new("admin","2admin2")
	@s.switch_user(admin2)
    res = admin2.update_properties(@s,"testproperty" => "newvalue1")
	assert_equal("200",res.code,res.body)
	res = admin2.change_password(@s,"admin")
	assert_equal("200",res.code,res.body)

	@s.switch_user(admin)
    res = admin.update_properties(@s,"testproperty" => "newvalue2")
	assert_equal("200",res.code,res.body)
  end
  
end

Test::Unit::UI::Console::TestRunner.run(TC_Kern740Test)
