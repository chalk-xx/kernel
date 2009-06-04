require 'test/unit.rb'
require 'sling/sling'
require 'sling/users'
require 'tempfile'

class SlingTest < Test::Unit::TestCase

  def setup
    @s = Sling.new()
    @created_nodes = []
  end

  def teardown
    @s.switch_user(SlingUsers::User.admin_user)
    @created_nodes.each { |n| @s.delete_node(n) }
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
 
end

