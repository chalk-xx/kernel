#!/usr/bin/env ruby

require 'sling/sling'
require 'sling/test'
require 'sling/authz'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingAuthz


class TC_Kern483Test < SlingTest

  def checkGetAccess(path, user, expected)
    @s.switch_user(user)
    res = @s.execute_get(@s.url_for(path+".json"))
    assert_equal(expected, res.code, "Expected #{expected} for GET #{path} from" + user.to_s())  	
  end

  def test_node_access_changes
    m = Time.now.to_i.to_s
    @authz = SlingAuthz::Authz.new(@s)
    creatorid = "testuser_creator#{m}"
    creator = create_user(creatorid)
    collabid = "testuser_collab#{m}"
    collab = create_user(collabid)
    
    # Mimic "sites" ACL.
    nodeparent = "testparent#{m}"
    create_node(nodeparent,"parentproperty" => "parentvalue")
    @authz.grant(nodeparent, "everyone", "jcr:removeChildNodes" => "granted")
    @authz.grant(nodeparent, "anonymous", "jcr:removeChildNodes" => "denied")
    
    # Mimic site creation.
    nodepath = nodeparent + "/node" + m
	create_node(nodepath,"testproperty" => "testvalue")
	
	# Note that "jcr:read" is missing from the pseudo-creator's privilege list.
	@authz.grant(nodepath, creatorid, {"jcr:readAccessControl" => "granted", "jcr:modifyAccessControl" => "granted","jcr:removeChildNodes" => "granted","jcr:write" => "granted","jcr:removeNode" => "granted","jcr:addChildNodes" => "granted","jcr:modifyProperties" => "granted"})
    res = @s.execute_get(@s.url_for(nodepath+".acl.json"))
    puts("path=#{nodepath}, ACL=#{res.body}")

    @s.switch_user(creator)
    collabsid = "g-group-"+m
    collabs = create_group(collabsid)
    collabs.add_member(@s, creatorid, "user")
    collabs.add_member(@s, collabid, "user")
    @authz.grant(nodepath, collabsid, {"jcr:readAccessControl" => "granted","jcr:read" => "granted","jcr:modifyAccessControl" => "granted","jcr:removeChildNodes" => "granted","jcr:write" => "granted","jcr:removeNode" => "granted","jcr:addChildNodes" => "granted","jcr:modifyProperties" => "granted"})
    everyone = SlingUsers::Group.new("everyone")
    @authz.grant(nodepath, "everyone", {"jcr:read" => "denied"})
    @s.switch_user(SlingUsers::User.admin_user())
    res = @s.execute_get(@s.url_for(nodepath+".acl.json"))
    puts("path=#{nodepath}, ACL=#{res.body}")
    
    # How is the pseudo-creator doing now?
    # We'd like to see "200" since that user should still have read access via group
    # membership, but we don't.
    checkGetAccess(nodepath, creator, "404");

    # And the other group member?
    checkGetAccess(nodepath, collab, "200");

    # Now delete the apparently unnecessary creator ACE.
    @s.switch_user(SlingUsers::User.admin_user())
    res = @s.execute_post(@s.url_for(nodepath + ".deleteAce.json"), ":applyTo" => creatorid)
    res = @s.execute_get(@s.url_for(nodepath+".acl.json"))
    puts("path=#{nodepath}, ACL=#{res.body}")
    
    # At this point, the ACLs show the creator and the collab member to be
    # in the same condition. Are they actually treated the same, though?
    checkGetAccess(nodepath, creator, "200");
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern483Test)
