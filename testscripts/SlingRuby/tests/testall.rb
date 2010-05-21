#!/usr/bin/env ruby

require 'test/unit'

Dir.foreach(".") do |path|
  if /.*\-test.rb/.match(File.basename(path))
    require path
  end
end
