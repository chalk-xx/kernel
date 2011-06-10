#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require './ruby-lib-dir.rb'

require 'set'
require 'sling/test'
require 'sling/message'
include SlingUsers

class TC_BasicLTI < Test::Unit::TestCase
  include SlingTest

  def setup
    super;
    @now = Time.now.to_f.to_s.gsub('.', '');
    @user = create_user("user-test#{@now}");
    assert_not_nil(@user);
    @admin = SlingUsers::User.admin_user();
    assert_not_nil(@admin);
    @anonymous = SlingUsers::User.anonymous();
    assert_not_nil(@anonymous);
    @url = @s.url_for("var/cluster.json");
    assert_not_nil(@url);
  end
  
  def teardown
    super;
  end

  def test_var_cluster_permissions_admin
    @s.switch_user(@admin);
    resp = @s.execute_get(@url);    
    assert_not_nil(resp);
    assert_not_nil(resp.body);
    assert_equal(200, resp.code.to_i, "Admin user should be able to read: #{@url}");
    props = JSON.parse(resp.body);
    assert_not_nil(props);
    assert(props.length > 0);
    assert_not_nil(props["jcr:createdBy"]);
    assert_not_nil(props["jcr:created"]);
    assert_not_nil(props["jcr:primaryType"]);
    assert_not_nil(props["jcr:mixinTypes"]);
    assert_not_nil(props["jcr:mixinTypes"][0]);
    assert_equal("rep:AccessControllable", props["jcr:mixinTypes"][0], "Node should be accessed controlled");
  end

  def test_var_cluster_permissions_anonymous
    @s.switch_user(@anonymous);
    resp = @s.execute_get(@url);    
    assert_not_nil(resp);
    assert_equal(404, resp.code.to_i, "Anonymous users should NOT be able to read: #{@url}.");
  end

  def test_var_cluster_permissions_everyone
    @s.switch_user(@user);
    resp = @s.execute_get(@url);    
    assert_not_nil(resp);
    assert_equal(404, resp.code.to_i, "Everyone should NOT be able to read: #{@url}.");
  end

end

