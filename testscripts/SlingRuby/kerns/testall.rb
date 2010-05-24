#!/usr/bin/env ruby

Dir.foreach(".") do |path|
  if /kern-.*\.rb/.match(File.basename(path))
    require path
  end
end
