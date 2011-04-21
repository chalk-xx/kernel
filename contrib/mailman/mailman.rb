#!/usr/bin/env ruby

require 'rubygems'
require 'mechanize'

## mmsitepass -c "Your password" to enable list creation

## add OWNERS_CAN_DELETE_THEIR_OWN_LISTS = yes to /etc/mailman/mm_cfg.py to enable list deletion
## http://wiki.list.org/pages/viewpage.action?pageId=4030594
## http://wiki.list.org/pages/viewpage.action?pageId=4030609

MAILMAN_HOST = "localhost"
MAILMAN_PATH = "http://#{MAILMAN_HOST}/mailman"
PASSWORD = "password"
NEWLIST_PASSWORD = "newlistpassword"

class MailManager < Mechanize

  def initialize(list)
    super()

    @list = list
    @html_parser = Nokogiri::HTML
    self.user_agent_alias = 'Linux Mozilla'
  end

  @logged_in = false

  def logged_in?
    @logged_in
  end

  # Authenticate in the admin panel for this list
  def log_in
    get("#{MAILMAN_PATH}/admin/#{@list}/login") do |page|
      login_form = page.form_with(:action => "/mailman/admin/#{@list}/login") do |login|
        login.adminpw = NEWLIST_PASSWORD
      end
      login_form.submit(button=login_form.buttons[0])
      @logged_in = true
    end
  end

  # Checks if the list exists
  #
  # This function is prone for errors if Mailman changes their GUI, or if Mailman works with multiple
  # languages, the title of the page will be different and will this function return false.
  def list_exists?
    log_in unless logged_in?

    get("#{MAILMAN_PATH}/admin/#{@list}") do |page|
      page.title =~/#{@list} */i
    end
  end

  # Creates the list if doesn't already exists
  def create_list(owner)
    return if list_exists?

    get("#{MAILMAN_PATH}/create") do |page|
      form = page.form_with(:action => 'create') do |create|
        create.listname = @list
        create.owner = owner
        create.password = NEWLIST_PASSWORD
        create.confirm = NEWLIST_PASSWORD
        create.auth = PASSWORD
      end
      create_result = form.submit(button=form.buttons[0])
    end
  end

  # Deletes the list if doesn't already exists
  def delete_list
    return unless list_exists?

    get("#{MAILMAN_PATH}/rmlist/#{@list}") do |page|
      form = page.form_with(:action => "../rmlist/#{name}") do |delete|
        delete.password = NEWLIST_PASSWORD
      end
      delete_result = form.submit(button=form.buttons[0])
    end
  end

  # Adds a user to a list, if the list doesn't exist, we create one
  def add_user_to_list(email)
    create_list "test.email@example.com" unless list_exists?

    get("#{MAILMAN_PATH}/admin/#{@list}/members/add") do |page|
      add_user_form = page.form_with(:action => "../../../admin/#{@list}/members/add") do |add|
        add.subscribees = email
        add.radiobuttons_with(:name => 'send_welcome_msg_to_this_batch')[0].check
      end

      add_user_result = add_user_form.submit(button=add_user_form.buttons[0])
    end
  end

  # Removes a user from a list
  def remove_user_from_list(email)
    return unless list_exists?

    get("#{MAILMAN_PATH}/admin/#{@list}/members/list") do |page|
      # We perform a search because I don't know if all emails will appear here
      # e.g if there is a pagination and we don't perform this search it's possible we won't delete it.
      search_user_form = page.form_with(:action => "../../../admin/#{@list}/members/list") do |search|
        search.findmember = email
      end
      search_user_result = search_user_form.submit(button=search_user_form.buttons[0])

      # There's only one email showing now, thus let us remove it
      remove_user_form = search_user_result.form_with(:action => "../../../admin/#{@list}/members/list") do |remove|
        remove.checkbox_with(:name => "#{email.gsub('@', '%40')}_unsub").check
      end
      remove_user_result = remove_user_form.submit(button=remove_user_form.buttons[1])
    end
  end
end