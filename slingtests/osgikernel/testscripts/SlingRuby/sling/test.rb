require 'test/unit.rb'
require 'sling/sling'
require 'sling/users'
require 'tempfile'

class SlingTest < Test::Unit::TestCase

  def setup
    @s = SlingInterface::Sling.new()
    @um = SlingUsers::UserManager.new(@s)
    @created_nodes = []
    @created_users = []
    @created_groups = []
  end

  def teardown
    @s.switch_user(SlingUsers::User.admin_user)
    @created_nodes.each { |n| assert(@s.delete_node(n), "Expected node delete to succeed") }
    @created_users.each { |u| assert(@um.delete_user(u), "Expected user delete to succeed") }
    @created_groups.each { |g| assert(@um.delete_group(g), "Expected group delete to succeed") }
  end

  def create_node(path, props)
    @s.create_node(path, props)
    @created_nodes << path
  end

  def create_file_node(path, fieldname, data)
    temp_file = Tempfile.new('some_temp_file')
    temp_file.write(data)
    temp_file.close
    @s.create_file_node(path, fieldname, temp_file.path)
    File.delete(temp_file.path)
    @created_nodes << path
  end

  def create_user(username)
    u = @um.create_user(username)
    @created_users << username
    return u
  end
 
  def create_group(groupname)
    g = @um.create_group(groupname)
    @created_groups << groupname
    return g
  end

end

