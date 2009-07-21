#!/usr/bin/perl

#{{{imports
use warnings;
use strict;
use Carp;
use lib qw ( .. );
use version; our $VERSION = qv('0.0.1');
use Shell::Terminal;
use Term::ANSIColor;

#}}}

print colored( "                                                     ",
    'on_red' ), "\n";
print colored(
    "| / / \\ Welcome to the perl Sling shell              ",
    'yellow on_magenta bold'
  ),
  "\n";
print colored(
    "|-   /  Created by daniel parry                      ",
    'yellow on_magenta bold'
  ),
  "\n";
print colored(
    "| \\ |__ You will be asked for a host to connect to:  ",
    'yellow on_magenta bold'
  ),
  "\n";
print colored(
    "        Once connected, type help for help.          ",
    'yellow on_magenta bold'
  ),
  "\n";
print colored( "                                                     ",
    'on_red' ), "\n";

Shell::Terminal->new->cmdloop;

1;
