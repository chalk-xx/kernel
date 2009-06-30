#!/usr/bin/env ruby


require 'sling/sling'
require 'sling/test'
require 'sling/message'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingSites
include SlingMessage

class TC_MyMessageTest < SlingTest

  def setup
    super
    @mm = MessageManager.new(@s)
  end

  def test_create_message
    m = Time.now.to_i.to_s
    puts("Creating user aaron"+m)
    a = create_user("aaron"+m)
    puts("Creating user nico"+m)
    n = create_user("nico"+m)
    puts("Creating user ian"+m)
    i = create_user("ian"+m)
    @s.switch_user(a)
    puts("Sending a message to Nico")
    res = @mm.create("nico"+m, "internal")
    assert_equal("302", res.code, "Expected to be able to create a message and get a redirect ")
	assert_not_nil( res.header['location'],"Expected to be given a location ")
	puts(res.header['location'])
    @s.debug = true
	res = @s.execute_get(res.header['location']+".json")
	assert_equal("200", res.code, "Expected to be able to view the message")
	
    @s.debug = false
#    puts("Checking that The invitation to Nico is pending")
#    contacts = @cm.get_pending()
#   @s.debug = false
#    assert_not_nil(contacts, "Expected to get contacts back")
#    assert_equal(contacts["results"].size, 1, "Expected single request back")
#    contact = contacts["results"][0]
#    assert_equal("nico"+m, contact["target"], "Expected nico to be my friend")
#    assert_equal("PENDING", contact["details"]["sakai:state"], "Expected state to be 'PENDING'")
#   
#
#    @s.switch_user(n)
#    contacts = @cm.get_invited()
#    assert_not_nil(contacts, "Expected to get an invite back ")
#    assert_equal(contacts["results"].size, 1, "Only expecting a single invite ")
#    contact = contacts["results"][0]
#    assert_equal("aaron"+m,contact["target"], "Expected Aaron to be asking ")
#    assert_equal("INVITED", contact["details"]["sakai:state"], "Expected state to be 'INVITED'") 
#    res = @cm.accept_contact("aaron"+m)
#    assert_equal("200", res.code, "Expecting acceptance of the contact")
#    contacts = @cm.get_accepted()
#    assert_not_nil(contacts, "Expected to get an accepted back ")
#    assert_equal(contacts["results"].size, 1, "Only expecting a single acceptance ")
#    contact = contacts["results"][0]
#    assert_equal("aaron"+m,contact["target"], "Expected Nico to have been accepted ")
#    assert_equal("ACCEPTED", contact["details"]["sakai:state"], "Expected state to be 'ACCEPTED'") 
#
#    @s.switch_user(a)
#    contacts = @cm.get_accepted()
#    assert_not_nil(contacts, "Expected to get an accepted back ")
#    assert_equal(contacts["results"].size, 1, "Only expecting a single acceptance ")
#    contact = contacts["results"][0]
#    assert_equal("nico"+m,contact["target"], "Expected Aaron to have been accepted ")
#    assert_equal("ACCEPTED", contact["details"]["sakai:state"], "Expected state to be 'ACCEPTED'") 

  end

  def teardown
    @created_users.each do |user|
      @s.debug = true
      @s.switch_user(user)
      @s.debug = false
    end
    super
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_MyMessageTest)

