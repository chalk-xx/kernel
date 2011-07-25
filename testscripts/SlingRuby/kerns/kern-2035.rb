#!/usr/bin/env ruby

require './ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/users'
include SlingUsers

class TC_Kern2035Test < Test::Unit::TestCase
  include SlingTest
  
  # tests posting "basic" profile to the profile root element
  # this test posts to ~user/public/authprofile.profile.json then checks the same for
  # the information that was posted
  def test_post_basic_to_authprofile
    # create test users
    u = create_test_user(2035)
    @s.switch_user(u)
    
    profile_root = "~#{u.name}/public/authprofile"
    
    # update using the profile update servlet
    data = {}
    data[':operation'] = 'import'
    data[':contentType'] = 'json'
    data[':content'] = '{"basic": {"elements": {"preferredName": {"value": "great tester"}}}}'
    
    res = @s.execute_post(@s.url_for("#{profile_root}.profile.json"), data)
    assert_equal('200', res.code, "Should be able to post successfully to the user profile: #{res}\n#{res.body}")
    
    # check that the new details are on the profile feed
    res = @s.execute_get(@s.url_for("#{profile_root}.profile.json"))
    assert_equal('200', res.code, "Should be able to retrieve profile: #{res}\n#{res.body}")
    profile = JSON.parse(res.body)
    assert_equal('great tester', profile['basic']['elements']['preferredName']['value'])
    
    # check that the new 'basic' details are not stored in the profile content
    res = @s.execute_get(@s.url_for("#{profile_root}.3.json"))
    assert_equal('200', res.code, 'Should be able to retrieve profile data')
    profile = JSON.parse(res.body)
    assert_nil(profile['basic']['elements']['preferredName'])
  end
  
  # tests posting "basic" profile to a "basic" subnode of the profile root element
  # this test posts to ~user/public/authprofile/basic.profile.json then checks
  # ~user/public/authprofile.profile.json and ~user/public/authprofile/basic.json
  # for the information that was posted.
  def test_post_basic_to_subnode
    # create test users
    u = create_test_user(2035)
    @s.switch_user(u)
    
    profile_root = "~#{u.name}/public/authprofile"
    
    # update using the profile update servlet
    data = {}
    data[':operation'] = 'import'
    data[':contentType'] = 'json'
    data[':content'] = '{"elements": {"preferredName": {"value": "great tester"}}}'
    data[':removeTree'] = true
    
    res = @s.execute_post(@s.url_for("#{profile_root}/basic.profile.json"), data)
    assert_equal('200', res.code, "Should be able to post successfully to the user profile: #{res}\n#{res.body}")
    
    # check that the new details are on the profile feed
    res = @s.execute_get(@s.url_for("#{profile_root}.profile.json"))
    assert_equal('200', res.code, "Should be able to retrieve profile: #{res}\n#{res.body}")
    profile = JSON.parse(res.body)
    assert_equal('great tester', profile['basic']['elements']['preferredName']['value'])
    
    # check that the new 'basic' details are not stored in the profile content
    res = @s.execute_get(@s.url_for("#{profile_root}/basic.2.json"))
    assert_equal('200', res.code, 'Should be able to retrieve profile data')
    profile = JSON.parse(res.body)
    assert_nil(profile['elements']['preferredName'])
  end
  
  # tests posting "basic" profile to a "basic" subnode not on the profile root element
  # since this "basic" section is not on the root, it should not be copied to the authorizable
  def test_add_basic_not_at_root
    # create test users
    u = create_test_user(2035)
    @s.switch_user(u)
    
    profile_root = "~#{u.name}/public/authprofile"
    
    # update using the profile update servlet
    data = {}
    data[':operation'] = 'import'
    data[':contentType'] = 'json'
    data[':content'] = '{"elements": {"preferredName": {"value": "great tester"}}}'
    data[':removeTree'] = true
    
    res = @s.execute_post(@s.url_for("#{profile_root}/random/basic.profile.json"), data)
    assert_equal('200', res.code, "Should be able to post successfully to the user profile: #{res}\n#{res.body}")
    
    # check that the new details are not in the root "basic" section
    res = @s.execute_get(@s.url_for("#{profile_root}.profile.json"))
    assert_equal('200', res.code, "Should be able to retrieve profile: #{res}\n#{res.body}")
    profile = JSON.parse(res.body)
    assert_nil(profile['basic']['elements']['preferredName'])
    
    # check that the new 'basic' details are stored in the subnode location
    res = @s.execute_get(@s.url_for("#{profile_root}/random/basic.2.json"))
    assert_equal('200', res.code, 'Should be able to retrieve profile data')
    profile = JSON.parse(res.body)
    assert_equal('great tester', profile['elements']['preferredName']['value'])
    
    # check that the new 'basic' details are available in the profile feed
    res = @s.execute_get(@s.url_for("#{profile_root}.profile.json"))
    assert_equal('200', res.code, 'Should be able to retrieve profile data')
    profile = JSON.parse(res.body)
    assert_equal('great tester', profile['random']['basic']['elements']['preferredName']['value'])
  end
  
  # tests posting "aboutme" profile to the profile root element
  # this test posts to ~user/public/authprofile.profile.json then checks
  # ~user/public/authprofile.profile.json and ~user/public/authprofile/aboutme.json
  # for the information that was posted.
  def test_post_aboutme_to_authprofile
    # create test users
    u = create_test_user(2035)
    @s.switch_user(u)
    
    profile_root = "~#{u.name}/public/authprofile"
    
    # update using the profile update servlet
    data = {}
    data[':operation'] = 'import'
    data[':contentType'] = 'json'
    data[':content'] = '{"aboutme": {"elements":{"aboutme":{"value":"test1"},"academicinterests":{"value":"test2"},"personalinterests":{"value":"test3"},"hobbies":{"value":"test4"}}}}'
    
    res = @s.execute_post(@s.url_for("#{profile_root}.profile.json"), data)
    assert_equal('200', res.code, "Should be able to post successfully to the user profile: #{res}\n#{res.body}")
    
    # check that the new details are on the profile feed
    res = @s.execute_get(@s.url_for("#{profile_root}.profile.json"))
    assert_equal('200', res.code, "Should be able to retrieve profile: #{res}\n#{res.body}")
    profile = JSON.parse(res.body)
    assert_equal('test1', profile['aboutme']['elements']['aboutme']['value'])
    assert_equal('test2', profile['aboutme']['elements']['academicinterests']['value'])
    assert_equal('test3', profile['aboutme']['elements']['personalinterests']['value'])
    assert_equal('test4', profile['aboutme']['elements']['hobbies']['value'])
    
    # check that the new 'basic' details are not stored in the profile content
    res = @s.execute_get(@s.url_for("#{profile_root}.3.json"))
    assert_equal('200', res.code, 'Should be able to retrieve profile data')
    profile = JSON.parse(res.body)
    assert_equal('test1', profile['aboutme']['elements']['aboutme']['value'])
    assert_equal('test2', profile['aboutme']['elements']['academicinterests']['value'])
    assert_equal('test3', profile['aboutme']['elements']['personalinterests']['value'])
    assert_equal('test4', profile['aboutme']['elements']['hobbies']['value'])
  end
  
  # tests posting "aboutme" profile to a "aboutme" subnode of the profile root element
  # this test posts to ~user/public/authprofile/aboutme.profile.json then checks
  # ~user/public/authprofile.profile.json and ~user/public/authprofile/aboutme.json
  # for the information that was posted.
  def test_post_aboutme_to_subnode
    # create test users
    u = create_test_user(2035)
    @s.switch_user(u)
    
    profile_root = "~#{u.name}/public/authprofile"
    
    # update using the profile update servlet
    data = {}
    data[':operation'] = 'import'
    data[':contentType'] = 'json'
    data[':content'] = '{"elements":{"aboutme":{"value":"test1"},"academicinterests":{"value":"test2"},"personalinterests":{"value":"test3"},"hobbies":{"value":"test4"}}}'
    data[':removeTree'] = true
    
    res = @s.execute_post(@s.url_for("#{profile_root}/aboutme.profile.json"), data)
    assert_equal('200', res.code, "Should be able to post successfully to the user profile: #{res}\n#{res.body}")
    
    # check that the new details are on the profile feed
    res = @s.execute_get(@s.url_for("#{profile_root}.profile.json"))
    assert_equal('200', res.code, "Should be able to retrieve profile: #{res}\n#{res.body}")
    profile = JSON.parse(res.body)
    assert_equal('test1', profile['aboutme']['elements']['aboutme']['value'])
    assert_equal('test2', profile['aboutme']['elements']['academicinterests']['value'])
    assert_equal('test3', profile['aboutme']['elements']['personalinterests']['value'])
    assert_equal('test4', profile['aboutme']['elements']['hobbies']['value'])
    
    # check that the new 'basic' details are not stored in the profile content
    res = @s.execute_get(@s.url_for("#{profile_root}/aboutme.2.json"))
    assert_equal('200', res.code, 'Should be able to retrieve profile data')
    profile = JSON.parse(res.body)
    assert_equal('test1', profile['elements']['aboutme']['value'])
    assert_equal('test2', profile['elements']['academicinterests']['value'])
    assert_equal('test3', profile['elements']['personalinterests']['value'])
    assert_equal('test4', profile['elements']['hobbies']['value'])
  end
  
  # tests adding,updating,replacing a list of publications
  # this test posts to ~user/public/authprofile/publications.profile.json then checks
  # ~user/public/authprofile.profile.json and ~user/public/authprofile/publications.json
  # for the information that was posted. the test then replaces an entry in the list
  # and replaces the entire list
  def test_update_publications
    # create test users
    u = create_test_user(2035)
    @s.switch_user(u)
    
    profile_root = "~#{u.name}/public/authprofile"
    
    #
    # add publications using the profile update servlet to the publication subnode
    #
    data = {}
    data[':operation'] = 'import'
    data[':contentType'] = 'json'
    data[':removeTree'] = true
    elements = []
    (1..3).each do |i|
      elements << "'#{i}':{'id':{'display':false,'value':'#{i}'},'maintitle':{'label':'__MSG__PROFILE_PUBLICATIONS_MAIN_TITLE__','required':true,'display':true,'example':'__MSG__PROFILE_PUBLICATIONS_MAIN_TITLE_EXAMPLE__','value':'test#{i}'},'mainauthor':{'label':'__MSG__PROFILE_PUBLICATIONS_MAIN_AUTHOR__','required':true,'display':true,'value':'test#{i}'},'coauthor':{'label':'__MSG__PROFILE_PUBLICATIONS_CO_AUTHOR__','required':false,'display':true,'example':'__MSG__PROFILE_PUBLICATIONS_CO_AUTHOR_EXAMPLE__','value':''},'publisher':{'label':'__MSG__PROFILE_PUBLICATIONS_PUBLISHER__','required':true,'display':true,'value':'test#{i}'},'placeofpublication':{'label':'__MSG__PROFILE_PUBLICATIONS_PLACE_OF_PUBLICATION__','required':true,'display':true,'value':'test#{i}'},'volumetitle':{'label':'__MSG__PROFILE_PUBLICATIONS_VOLUME_TITLE__','required':false,'display':true,'value':''},'volumeinformation':{'label':'__MSG__PROFILE_PUBLICATIONS_VOLUME_INFORMATION__','required':false,'display':true,'example':'__MSG__PROFILE_PUBLICATIONS_VOLUME_INFORMATION_EXAMPLE__','value':''},'year':{'label':'__MSG__PROFILE_PUBLICATIONS_YEAR__','required':true,'display':true,'value':'test#{i}'},'number':{'label':'__MSG__PROFILE_PUBLICATIONS_NUMBER__','required':false,'display':true,'value':''},'series title':{'label':'__MSG__PROFILE_PUBLICATIONS_SERIES_TITLE__','required':false,'display':true,'value':''},'url':{'label':'__MSG__PROFILE_PUBLICATIONS_URL__','required':false,'display':true,'validation':'appendhttp url','value':''},'order':0}"
    end
    data[':content'] = "{'elements':{#{elements.join(',')}}}"
    
    res = @s.execute_post(@s.url_for("#{profile_root}/publications.profile.json"), data)
    assert_equal('200', res.code, "Should be able to post successfully to the user profile: #{res}\n#{res.body}")
    
    # check that the new details are on the profile feed
    res = @s.execute_get(@s.url_for("#{profile_root}.profile.json"))
    assert_equal('200', res.code, "Should be able to retrieve profile: #{res}\n#{res.body}")
    profile = JSON.parse(res.body)
    # number of entries + 1 for the _path element
    assert_equal(4, profile['publications']['elements'].length)
    assert_not_nil(profile['publications']['elements']['1'])
    assert_not_nil(profile['publications']['elements']['2'])
    assert_not_nil(profile['publications']['elements']['3'])
    
    #
    # replace an entry in the list
    #
    i = 4
    elements[1] = "'#{i}':{'id':{'display':false,'value':'#{i}'},'maintitle':{'label':'__MSG__PROFILE_PUBLICATIONS_MAIN_TITLE__','required':true,'display':true,'example':'__MSG__PROFILE_PUBLICATIONS_MAIN_TITLE_EXAMPLE__','value':'test#{i}'},'mainauthor':{'label':'__MSG__PROFILE_PUBLICATIONS_MAIN_AUTHOR__','required':true,'display':true,'value':'test#{i}'},'coauthor':{'label':'__MSG__PROFILE_PUBLICATIONS_CO_AUTHOR__','required':false,'display':true,'example':'__MSG__PROFILE_PUBLICATIONS_CO_AUTHOR_EXAMPLE__','value':''},'publisher':{'label':'__MSG__PROFILE_PUBLICATIONS_PUBLISHER__','required':true,'display':true,'value':'test#{i}'},'placeofpublication':{'label':'__MSG__PROFILE_PUBLICATIONS_PLACE_OF_PUBLICATION__','required':true,'display':true,'value':'test#{i}'},'volumetitle':{'label':'__MSG__PROFILE_PUBLICATIONS_VOLUME_TITLE__','required':false,'display':true,'value':''},'volumeinformation':{'label':'__MSG__PROFILE_PUBLICATIONS_VOLUME_INFORMATION__','required':false,'display':true,'example':'__MSG__PROFILE_PUBLICATIONS_VOLUME_INFORMATION_EXAMPLE__','value':''},'year':{'label':'__MSG__PROFILE_PUBLICATIONS_YEAR__','required':true,'display':true,'value':'test#{i}'},'number':{'label':'__MSG__PROFILE_PUBLICATIONS_NUMBER__','required':false,'display':true,'value':''},'series title':{'label':'__MSG__PROFILE_PUBLICATIONS_SERIES_TITLE__','required':false,'display':true,'value':''},'url':{'label':'__MSG__PROFILE_PUBLICATIONS_URL__','required':false,'display':true,'validation':'appendhttp url','value':''},'order':0}"
    data[':content'] = "{'elements':{#{elements.join(',')}}}"
    res = @s.execute_post(@s.url_for("#{profile_root}/publications.profile.json"), data)
    assert_equal('200', res.code, "Should be able to post successfully to the user profile: #{res}\n#{res.body}")
    
    # check that the new details are on the profile feed
    res = @s.execute_get(@s.url_for("#{profile_root}.profile.json"))
    assert_equal('200', res.code, "Should be able to retrieve profile: #{res}\n#{res.body}")
    profile = JSON.parse(res.body)
    # number of entries + 1 for the _path element
    assert_equal(4, profile['publications']['elements'].length)
    assert_not_nil(profile['publications']['elements']['1'])
    assert_nil(profile['publications']['elements']['2'])
    assert_not_nil(profile['publications']['elements']['3'])
    assert_not_nil(profile['publications']['elements']['4'])
    
    #
    # replace the publications with completely different entries
    #
    elements = []
    (5..6).each do |i|
      elements << "'#{i}':{'id':{'display':false,'value':'#{i}'},'maintitle':{'label':'__MSG__PROFILE_PUBLICATIONS_MAIN_TITLE__','required':true,'display':true,'example':'__MSG__PROFILE_PUBLICATIONS_MAIN_TITLE_EXAMPLE__','value':'test#{i}'},'mainauthor':{'label':'__MSG__PROFILE_PUBLICATIONS_MAIN_AUTHOR__','required':true,'display':true,'value':'test#{i}'},'coauthor':{'label':'__MSG__PROFILE_PUBLICATIONS_CO_AUTHOR__','required':false,'display':true,'example':'__MSG__PROFILE_PUBLICATIONS_CO_AUTHOR_EXAMPLE__','value':''},'publisher':{'label':'__MSG__PROFILE_PUBLICATIONS_PUBLISHER__','required':true,'display':true,'value':'test#{i}'},'placeofpublication':{'label':'__MSG__PROFILE_PUBLICATIONS_PLACE_OF_PUBLICATION__','required':true,'display':true,'value':'test#{i}'},'volumetitle':{'label':'__MSG__PROFILE_PUBLICATIONS_VOLUME_TITLE__','required':false,'display':true,'value':''},'volumeinformation':{'label':'__MSG__PROFILE_PUBLICATIONS_VOLUME_INFORMATION__','required':false,'display':true,'example':'__MSG__PROFILE_PUBLICATIONS_VOLUME_INFORMATION_EXAMPLE__','value':''},'year':{'label':'__MSG__PROFILE_PUBLICATIONS_YEAR__','required':true,'display':true,'value':'test#{i}'},'number':{'label':'__MSG__PROFILE_PUBLICATIONS_NUMBER__','required':false,'display':true,'value':''},'series title':{'label':'__MSG__PROFILE_PUBLICATIONS_SERIES_TITLE__','required':false,'display':true,'value':''},'url':{'label':'__MSG__PROFILE_PUBLICATIONS_URL__','required':false,'display':true,'validation':'appendhttp url','value':''},'order':0}"
    end
    data[':content'] = "{'elements':{#{elements.join(',')}}}"
    res = @s.execute_post(@s.url_for("#{profile_root}/publications.profile.json"), data)
    assert_equal('200', res.code, "Should be able to post successfully to the user profile: #{res}\n#{res.body}")
    
    # check that the details have been updated
    res = @s.execute_get(@s.url_for("#{profile_root}.profile.json"))
    assert_equal('200', res.code, "Should be able to retrieve profile: #{res}\n#{res.body}")
    profile = JSON.parse(res.body)
    # number of entries + 1 for the _path element
    assert_equal(3, profile['publications']['elements'].length)
    # verify the old entries are gone
    assert_nil(profile['publications']['elements']['1'])
    assert_nil(profile['publications']['elements']['2'])
    assert_nil(profile['publications']['elements']['3'])
    assert_nil(profile['publications']['elements']['4'])
    # check for the new entries
    assert_not_nil('2', profile['publications']['elements']['5'])
    assert_not_nil('3', profile['publications']['elements']['6'])
  end
end
