#!/usr/bin/env ruby

Dir.foreach(".") do |path|
  if /.*\-test.rb/.match(File.basename(path))
    require path
  end
end
