#!/usr/bin/env ruby
require 'net/http'
require 'cgi'

$BASE="http://localhost:8080/"
$USERMANAGER_URI="#{$BASE}system/userManager/"
$GROUP_URI="#{$USERMANAGER_URI}group.create.html"
$USER_URI="#{$USERMANAGER_URI}user.create.html"
$DATE=Time.now().strftime("%Y%m%d%H%M%S")

def dump_response(response)
  puts "Response: #{response.code} #{response.message}"
#  puts "#{response.body}"
end

def execute_post(path, post_params)
  uri = URI.parse(path)
  req = Net::HTTP::Post.new(uri.path)
  req.basic_auth('admin', 'admin')
  req.set_form_data(post_params)
  return Net::HTTP.new(uri.host, uri.port).start{|http| http.request(req)}
end

def execute_get(path)
  uri = URI.parse(path)
  return Net::HTTP.start(uri.host, uri.port) { |http| http.get(uri.path) }
end

def create_user(id)
  username = "testuser#{$DATE}-#{id}"
  puts "Creating user: #{username}"
  result = execute_post($USER_URI, 
                        { ":name" => username, "pwd" => "testuser", "pwdConfirm" => "testuser" })
  dump_response(result)
end

def create_group(id)
  groupname = "testgroup#{$DATE}-#{id}"
  puts "Creating group: #{groupname}"
  result = execute_post($GROUP_URI, { ":name" => groupname })
  dump_response(result)
end

def create_node(path, params)
  result = execute_post("#{$BASE}#{path}", params)
  dump_response(result)
end

def get_node_props_json(path)
  return execute_get("#{$BASE}#{path}.json").body
end

def get_node_acl_json(path)
  return execute_get("#{$BASE}#{path}.acl.json").body
end

create_group(10)
create_user(10)
create_node("fish", { "foo" => "bar", "baz" => "jim" })
puts get_node_props_json("fish")
puts get_node_acl_json("fish")

