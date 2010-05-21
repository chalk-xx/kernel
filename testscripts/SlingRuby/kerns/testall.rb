#!/usr/bin/env ruby

require 'test/unit'

Dir.foreach(".") do |path|
  if /kern-.*\.rb/.match(File.basename(path))
    require path
  end
end
