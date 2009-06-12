#!/usr/bin/env ruby

$USERMANAGER_URI="system/userManager/"
$GROUP_URI="#{$USERMANAGER_URI}group.create.html"
$USER_URI="#{$USERMANAGER_URI}user.create.html"
$DEFAULT_PASSWORD="testuser"

module SlingUsers

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

    def update_properties(sling, props)
      sling.execute_post(sling.url_for("#{group_url}.update.html"), props)
    end

    def add_member(sling, principal, type)
      principal_path = "/#{$USERMANAGER_URI}#{type}/#{principal}"
      sling.execute_post(sling.url_for("#{group_url}.update.html"),
              { ":member" => principal_path })
    end

    def remove_member(sling, principal, type)
      principal_path = "/#{$USERMANAGER_URI}#{type}/#{principal}"
      sling.execute_post(sling.url_for("#{group_url}.update.html"),
              { ":member@Delete" => principal_path })
    end

    def set_joinable(sling, joinable)
      return sling.execute_post(sling.url_for("#{group_url}.update.html"), "sakai:joinable" => joinable)
    end

    def members(sling)
      props = s.get_node_props("#{group_url}.json")
      return props["members"]
    end

    def self.url_for(name)
      return "#{$USERMANAGER_URI}group/#{name}"
    end

    private
    def group_url
      return Group.url_for(@name)
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
  
    def do_curl_auth(c)
      c.userpwd = "#{@name}:#{@password}"
    end

    def to_s
      return "User: #{@name} (pass: #{@password})"
    end

    def update_properties(sling, props)
      sling.execute_post(sling.url_for("#{user_url}.update.html"), props)
    end

    def self.url_for(name)
      return "#{$USERMANAGER_URI}user/#{name}"
    end

    private
    def user_url
      return User.url_for(@name)
    end
  end

  class UserManager

    def initialize(sling)
      @sling = sling
      @date = Time.now().strftime("%Y%m%d%H%M%S")
    end

    def delete_test_user(id)
      return delete_user("testuser#{@date}-#{id}")
    end
     
    def delete_user(username)
      result = @sling.execute_post(@sling.url_for("#{User.url_for(username)}.delete.html"),
                                    { "go" => 1 })
      if (result.code.to_i > 299)
        puts "Error deleting user"
        return false
      end
      return true
    end
 
    def delete_group(groupname)
      result = @sling.execute_post(@sling.url_for("#{Group.url_for(groupname)}.delete.html"),
                                    { "go" => 1 })
      if (result.code.to_i > 299)
        puts "Error deleting group"
        return false
      end
      return true
    end
   
    def create_test_user(id)
      return create_user("testuser#{@date}-#{id}")
    end

    def create_user(username)
      puts "Creating user: #{username}"
      user = User.new(username)
      result = @sling.execute_post(@sling.url_for("#{$USER_URI}"),
                            { ":name" => user.name,
                              "pwd" => user.password,
                              "pwdConfirm" => user.password })
      if (result.code.to_i > 299)
        puts "Error creating user"
        return nil
      end
      return user
    end

    def create_group(groupname)
      puts "Creating group: #{groupname}"
      group = Group.new(groupname)
      result = @sling.execute_post(@sling.url_for($GROUP_URI), { ":name" => group.name })
      if (result.code.to_i > 299)
        return nil
      end
      return group
    end

    def get_user_props(name)
      return @sling.get_node_props(User.url_for(name))
    end

    def get_group_props(name)
      return @sling.get_node_props(Group.url_for(name))
    end

  end

end
