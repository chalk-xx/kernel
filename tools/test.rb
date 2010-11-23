#!/usr/bin/ruby


if ARGV.size < 1
  puts "Usage: test.rb TESTNAME"
  exit 1
end

START_DIR = Dir.pwd

test = ARGV[0]


def runTest(testPath,test)
  Dir.foreach(testPath) do |path|
     if ("#{path}" == "#{test}.rb")
        Dir.chdir(testPath)
        puts "CD to  #{Dir.pwd}"
        require "#{path}"
        Dir.chdir(START_DIR)
     end
  end
end


runTest("testscripts/SlingRuby/kerns", test)
runTest("testscripts/SlingRuby/tests", test)
