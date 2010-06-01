#!/usr/bin/env ruby

require 'sling/test'
require 'logger'

SlingTest.setLogLevel(Logger::ERROR)

Dir.foreach(".") do |path|
  if /.*\-test.rb/.match(File.basename(path))
    require path
  end
end
