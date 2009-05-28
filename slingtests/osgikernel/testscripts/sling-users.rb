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
      sling.execute_post(sling.url_for("#{$USERMANAGER_URI}group/#{@name}.update.html"), props)
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

    def update_properties(sling, props)
      sling.execute_post(sling.url_for("#{$USERMANAGER_URI}user/#{@name}.update.html"), props)
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
      result = @sling.execute_post(@sling.url_for("#{$USERMANAGER_URI}/user/#{username}.delete.html"),
                                    { "go" => 1 })
      if (result.code.to_i > 299)
        puts "Error deleting user"
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

  end

end
