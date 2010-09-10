#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
$LOAD_PATH << File.expand_path(File.dirname(__FILE__) + '/../lib')

require 'sling/test'
require 'logger'

SlingTest.setLogLevel(Logger::ERROR)

Dir.foreach(".") do |path|
  if /kern-.*\.rb/.match(File.basename(path))
    require path
  end
end
