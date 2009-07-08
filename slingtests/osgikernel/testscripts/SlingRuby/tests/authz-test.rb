#!/usr/bin/env ruby

require 'sling/sling'
require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingAuthz

class TC_MyAuthZTest < SlingTest


  def test_authz
    m = Time.now.to_i.to_s
	@authz = SlingAuthz::Authz.new(@s)
	user1 = "user1-"+m
	user2 = "user2-"+m
	group1 = "g-group1-"+m
	group2 = "g-group2-"+m
	
	path = "test/authztest/node"+m
	create_user(user1)
	create_user(user2)
	create_group(group1)
	create_group(group2)
	puts("Creating Node at #{path}")
	create_node(path,"testproperty" => "testvalue")
	
	@authz.grant(path,user1,"jcr:read" => "granted")
	@authz.grant(path,user1,"jcr:write" => "granted")
    acl = @authz.getacl(path)
	
	
	@authz.grant(path,user2,"jcr:read" => "granted")
	@authz.grant(path,user2,"jcr:write" => "denied")
    acl = @authz.getacl(path)
	
	
	@authz.grant(path,group1,"jcr:read" => "granted")
	@authz.grant(path,group1,"jcr:write" => "granted")
    acl = @authz.getacl(path)
	
	@authz.grant(path,group2,"jcr:read" => "granted")
	@authz.grant(path,group2,"jcr:write" => "denied")
	

    acl = @authz.getacl(path)
	
	# check user1
	assert_not_nil(acl[user1],"Expected for find ACE for #{user1}"+@authz.hashToString(acl))
	ace = acl[user1]
	assert_not_nil(ace["granted"],"Expected ace for #{user1} to have jcr:read granted ace was nil "+@authz.hashToString(acl))
	puts("ACE for user #{user1} was "+@authz.hashToString(ace)+":"+ace["granted"].to_s)
	
	assert_equal(true,ace["granted"].include?("jcr:read"),"Expected ace for #{user1} to have jcr:read granted ace was "+@authz.hashToString(ace))
	assert_equal(true,ace["granted"].include?("jcr:write"),"Expected ace for #{user1} to have jcr:write granted ace was "+@authz.hashToString(ace))
	if ( ace["denied"] != nil ) 
		assert_equal(false,ace["denied"].include?("jcr:read"),"Expected ace for #{user1} to have jcr:read granted ace was "+@authz.hashToString(ace))
		assert_equal(false,ace["denied"].include?("jcr:write"),"Expected ace for #{user1} to have jcr:write granted ace was "+@authz.hashToString(ace))
	end

    # check user2
	assert_not_nil(acl[user2],"Expected for find ACE for #{user2}"+@authz.hashToString(acl))
	ace = acl[user2]
	assert_not_nil(ace["granted"],"Expected ace for #{user2} to have jcr:read granted ace was nil "+@authz.hashToString(acl))
	puts("ACE for user #{user2} was "+@authz.hashToString(ace)+":"+ace["granted"].to_s)
	
	assert_equal(true,ace["granted"].include?("jcr:read"),"Expected ace for #{user2} to have jcr:read granted ace was "+@authz.hashToString(ace))
	assert_equal(false,ace["granted"].include?("jcr:write"),"Expected ace for #{user2} to have jcr:write denied ace was "+@authz.hashToString(ace))
	
	assert_not_nil(ace["denied"],"Expected ace for #{user2} denied to be present, was nil "+@authz.hashToString(acl))
	
    assert_equal(false,ace["denied"].include?("jcr:read"),"Expected ace for #{user1} to have jcr:read granted ace was "+@authz.hashToString(ace))
	assert_equal(true,ace["denied"].include?("jcr:write"),"Expected ace for #{user2} to have jcr:write denied ace was "+@authz.hashToString(ace))

    # check group1
	assert_not_nil(acl[group1],"Expected for find ACE for #{group1}"+@authz.hashToString(acl))
	ace = acl[group1]
	assert_not_nil(ace["granted"],"Expected ace for #{group1} to have jcr:read granted ace was nil "+@authz.hashToString(acl))
	puts("ACE for user #{group1} was "+@authz.hashToString(ace)+":"+ace["granted"].to_s)
	
	assert_equal(true,ace["granted"].include?("jcr:read"),"Expected ace for #{group1} to have jcr:read granted ace was "+@authz.hashToString(ace))
	assert_equal(true,ace["granted"].include?("jcr:write"),"Expected ace for #{group1} to have jcr:write granted ace was "+@authz.hashToString(ace))

    # check group2
	assert_not_nil(acl[group2],"Expected for find ACE for #{group2}"+@authz.hashToString(acl))
	ace = acl[group2]
	assert_not_nil(ace["granted"],"Expected ace for #{group2} to have jcr:read granted ace was nil "+@authz.hashToString(acl))
	puts("ACE for user #{group2} was "+@authz.hashToString(ace)+":"+ace["granted"].to_s)
	
	assert_equal(true,ace["granted"].include?("jcr:read"),"Expected ace for #{group2} to have jcr:read granted ace was "+@authz.hashToString(ace))
	assert_equal(false,ace["granted"].include?("jcr:write"),"Expected ace for #{group2} to have jcr:write denied ace was "+@authz.hashToString(ace))
	
	assert_not_nil(ace["denied"],"Expected ace for #{group2} denied to be present, was nil, indicates that DENIED is silently NOT allowed on groups "+@authz.hashToString(acl))
	
    assert_equal(false,ace["denied"].include?("jcr:read"),"Expected ace for #{group2} to have jcr:read granted ace was "+@authz.hashToString(ace))
	assert_equal(true,ace["denied"].include?("jcr:write"),"Expected ace for #{group2} to have jcr:write denied ace was "+@authz.hashToString(ace))

		
  end


end

Test::Unit::UI::Console::TestRunner.run(TC_MyAuthZTest)

