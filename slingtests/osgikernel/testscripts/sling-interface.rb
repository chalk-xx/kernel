#!/usr/bin/env ruby
require 'net/http'
require 'cgi'
require 'json'

$USERMANAGER_URI="system/userManager/"
$GROUP_URI="#{$USERMANAGER_URI}group.create.html"
$USER_URI="#{$USERMANAGER_URI}user.create.html"
$DEFAULT_PASSWORD="testuser"

class Hash

  def dump
    return keys.collect{|k| "#{k} => #{self[k]}"}.join(", ")
  end

end

module SlingInterface

  class Principal
    attr_accessor :name
    
    def initialize(name)
      @name = name
    end

  end

  class Group < Principal
    def to_s
      return "Group: #{@name}"
    end
  end

  class User < Principal

    attr_accessor :password

    def initialize(username, password=$DEFAULT_PASSWORD)
      super(username)
      @password = password
    end

    def self.admin_user
      return User.new("admin", "admin")
    end

    def do_request_auth(req)
      req.basic_auth(@name, @password)
    end

    def to_s
      return "User: #{@name} (pass: #{@password})"
    end

  end

  class Sling

    attr_accessor :debug

    def initialize(server="http://localhost:8080/", debug=false)
      @server = server
      @date = Time.now().strftime("%Y%m%d%H%M%S")
      @debug = debug
      @user = User.admin_user()
    end

    def dump_response(response)
      puts "Response: #{response.code} #{response.message}"
      puts "#{response.body}" if @debug
    end

    def switch_user(user)
      puts "Switched user to #{user}"
      @user = user
    end

    def execute_post(path, post_params)
      puts "URL: #{path} params: #{post_params.dump}" if @debug
      uri = URI.parse(path)
      req = Net::HTTP::Post.new(uri.path)
      @user.do_request_auth(req)
      req.set_form_data(post_params)
      return Net::HTTP.new(uri.host, uri.port).start{ |http| http.request(req) }
    end

    def execute_get(path)
      puts "URL: #{path}" if @debug
      uri = URI.parse(path)
      req = Net::HTTP::Get.new(uri.path)
      @user.do_request_auth(req)
      return Net::HTTP.new(uri.host, uri.port).start { |http| http.request(req) }
    end

    def create_user(id)
      username = "testuser#{@date}-#{id}"
      puts "Creating user: #{username}"
      user = User.new(username)
      result = execute_post("#{@server}#{$USER_URI}", 
                            { ":name" => user.name, 
                              "pwd" => user.password, 
                              "pwdConfirm" => user.password })
      dump_response(result)
      if (result.code.to_i > 299)
        return nil
      end
      return user
    end

    def create_group(groupname)
      puts "Creating group: #{groupname}"
      group = Group.new(groupname)
      result = execute_post("#{@server}#{$GROUP_URI}", { ":name" => group.name })
      dump_response(result)
      if (result.code.to_i > 299)
        return nil
      end
      return group
    end

    def delete_node(path)
      result = execute_post("#{@server}#{path}", ":operation" => "delete")
      dump_response(result)
    end

    def create_node(path, params)
      result = execute_post("#{@server}#{path}", params.update("jcr:createdBy" => @user.name))
      dump_response(result)
    end

    def get_node_props_json(path)
      return execute_get("#{@server}#{path}.json").body
    end

    def get_node_props(path)
      return JSON.parse(get_node_props_json(path))
    end

    def get_node_acl_json(path)
      return execute_get("#{@server}#{path}.acl.json").body
    end

    def get_node_acl(path)
      return JSON.parse(get_node_acl_json(path))
    end

    def set_node_acl_entries(path, principal, privs)
      puts "Setting node acl for: #{principal} to #{privs.dump}"
      res = execute_post("#{@server}#{path}.modifyAce.html", 
                { "principalId" => principal.name }.update(
                    privs.keys.inject(Hash.new) do |n,k| 
                      n.update("privilege@#{k}" => privs[k])
                    end))
      dump_response(res)
      return res
    end

    def delete_node_acl_entries(path, principal)
      res = execute_post("#{@server}#{path}.deleteAce.html", {
              ":applyTo" => principal
              })
      dump_response(res)
    end

    def clear_acl(path)
      acl = JSON.parse(get_node_acl_json(path))
      acl.keys.each { |p| delete_node_acl_entries(path, p) }
    end

  end

end

if __FILE__ == $0
  puts "Sling test"  
  s = SlingInterface::Sling.new("http://localhost:8080/", false)
  s.create_group(10)
  user = s.create_user(10)
  s.create_node("fish", { "foo" => "bar", "baz" => "jim" })
  puts s.get_node_props_json("fish")
  puts s.get_node_acl_json("fish")

  s.set_node_acl_entries("fish", user, { "jcr:write" => "granted" })
  puts s.get_node_acl_json("fish")
end

