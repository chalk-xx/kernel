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
	puts(res.body)
    assert_equal("200", res.code, "Expected to create a message ")
	message = JSON.parse(res.body)
	puts(message)
	assert_not_nil( message['id'],"Expected to be given a location ")
	messageid = message['id']
	messagelocation = "http://localhost:8080"+a.message_path_for(@s,messageid)+".json"	
	puts("==========getting location" + messagelocation)
	res = @s.execute_get(messagelocation)
	assert_equal("200",res.code,"Expected to get Inbox Ok")
	puts("Message from Aaron's outbox ")
	puts(res.body)
	message = JSON.parse(res.body)
	
	
	assert_not_nil(message,"No Response to a get on the message")
	assert_equal("drafts",message['sakai:messagebox'],"Message Box Incorrect")
	assert_equal("pending",message['sakai:sendstate'],"Message State Incorrect")
	assert_equal("aaron"+m,message['sakai:from'],"Message From Incorrect")
	assert_equal("nico"+m,message['sakai:to'],"Message To Incorrect")
	assert_equal("true",message['sakai:read'],"Message Sould be marked read")
	assert_equal("sakai/message",message['sling:resourceType'],"Resource Type not correct")


	puts("Sending Message ")
	res = @mm.send(messageid)
	assert_equal("200", res.code, "Dispatched ok")


	puts("==========getting location" + messagelocation)
	res = @s.execute_get(messagelocation)
	assert_equal("200",res.code,"Expected to get Inbox Ok")
	puts("Message from Aaron's outbox after sending ")
	puts(res.body)
	message = JSON.parse(res.body)
	
	assert_not_nil(message,"No Response to a get on the message");
	assert_equal("outbox",message['sakai:messagebox'],"Message Box Incorrect")
	assert_equal("notified",message['sakai:sendstate'],"Message State Incorrect")
	assert_equal("aaron"+m,message['sakai:from'],"Message From Incorrect")
	assert_equal("nico"+m,message['sakai:to'],"Message To Incorrect")
	assert_equal("true",message['sakai:read'],"Message Sould be marked read")
	assert_equal("sakai/message",message['sling:resourceType'],"Resource Type not correct");
	res = @mm.list_outbox()
	assert_equal("200", res.code, "Expected to be able to list the outbox")

    @s.switch_user(n)	
	messagelocation = "http://localhost:8080"+n.message_path_for(@s,messageid)+".json"	

        
	puts("==========getting location" + messagelocation)
	res = @s.execute_get(messagelocation)
	assert_equal("200",res.code,"Expected to get Inbox Ok")
	puts("Message from Nicos inbox after sending ")
	puts(res.body)
	message = JSON.parse(res.body)
	
	assert_not_nil(message,"No Response to a get on the message");
	assert_equal("inbox",message['sakai:messagebox'],"Message Box Incorrect")
	assert_equal("notified",message['sakai:sendstate'],"Message State Incorrect")
	assert_equal("aaron"+m,message['sakai:from'],"Message From Incorrect")
	assert_equal("nico"+m,message['sakai:to'],"Message To Incorrect")
	assert_equal(false,message['sakai:read'],"Message Sould be marked read")
	assert_equal("sakai/message",message['sling:resourceType'],"Resource Type not correct")

	res = @mm.list_inbox()
	assert_equal("200", res.code, "Expected to be able to list the outbox")

	puts("List Of the Inbox for user nico, should have 1 entry")
	puts(res.body)
	box = JSON.parse(res.body)
	assert_equal(1,box["total"],"Should have given 1 entry in the inbox for nico");

	res = @mm.list_all()
	assert_equal("200", res.code, "Expected to be able to list the outbox")
	puts("List Of all for user nico, should have 1 entry")
	puts(res.body)
	box = JSON.parse(res.body)
	assert_equal(1,box["total"],"Should have given 1 entry in all boxes for nico");

	res = @mm.list_outbox()
	assert_equal("200", res.code, "Expected to be able to list the outbox")
	puts("List Of outbox for user nico, should have 0 entrys")
	puts(res.body)
	box = JSON.parse(res.body)
	assert_equal(0,box["total"],"Should have given 0 entry in the outbox for nico");

    @s.switch_user(a)	
	messagelocation = "http://localhost:8080"+a.message_path_for(@s,messageid)+".json"	
	res = @mm.list_inbox()
	assert_equal("200", res.code, "Expected to be able to list the outbox")
	puts("List Of the Inbox for user aaron 0 entries")
	puts(res.body)
	box = JSON.parse(res.body)
	assert_equal(0,box["total"],"Should have given 0 entry in all boxes for aarono");


	res = @mm.list_all()
	assert_equal("200", res.code, "Expected to be able to list the outbox")
	puts("List Of all for user aaron, should have 1 entry")
	puts(res.body)
	box = JSON.parse(res.body)
	assert_equal(1,box["total"],"Should have given 1 entry in all boxes for aaron");

	res = @mm.list_all_noopts()
	assert_equal("200", res.code, "Expected to be able to list the outbox")
	puts("List Of all for user aaron, should have 1 entry")
	puts(res.body)
	box = JSON.parse(res.body)
	assert_equal(1,box["total"],"Should have given 1 entry in all boxes for aaron");

	res = @mm.list_outbox()
	assert_equal("200", res.code, "Expected to be able to list the outbox")
	puts("List Of outbox for user aaron, should have 0 entrys")
	puts(res.body)
	box = JSON.parse(res.body)
	assert_equal(1,box["total"],"Should have given 1 entry in all boxes for aaron");
	
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

