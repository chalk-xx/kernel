require 'test/unit.rb'
require 'sling/sling'
require 'sling/users'
require 'sling/search'
require 'tempfile'
require 'logger'

module SlingTest

  @@log_level = Logger::DEBUG

  def SlingTest.setLogLevel(level)
    @@log_level = level
  end

  def setup
    @s = SlingInterface::Sling.new()
    @um = SlingUsers::UserManager.new(@s)
    @search = SlingSearch::SearchManager.new(@s)
    @created_nodes = []
    @created_users = []
    @created_groups = []
    @delete = true
    @log = Logger.new(STDOUT)
    @log.level = @@log_level
  end

  def teardown
    if ( @delete ) then
		@s.switch_user(SlingUsers::User.admin_user)
		@created_nodes.reverse.each { |n| @s.delete_node(n) }
		@created_groups.each { |g| @um.delete_group(g) }
		@created_users.each { |u| @um.delete_user(u.name) }
	end
  end

  def create_node(path, props={})
    #puts "Path is #{path}"
    res = @s.create_node(path, props)
    assert_not_equal("500", res.code, "Expected to be able to create node "+res.body)
    @created_nodes << path
    return path
  end

  def create_file_node(path, fieldname, filename, data, content_type="text/plain")
    res = @s.create_file_node(path, fieldname, filename, data, content_type)
    @created_nodes << path unless @created_nodes.include?(path)
    return res
  end

  def create_user(username, firstname = nil, lastname = nil)
    u = @um.create_user(username, firstname, lastname)
    assert_not_nil(u, "Expected user to be created: #{username}")
    @created_users << u
    return u
  end
 
  def create_group(groupname, title = nil)
    g = @um.create_group(groupname, title)
    assert_not_nil(g, "Expected group to be created: #{groupname}")
    @created_groups << groupname
    return g
  end
  
  def wait_for_indexer()
    sleep(10)  # the indexer has a TTL on 5s on each batch, so 10s should be plenty.
  end

end

