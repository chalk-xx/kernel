#!/usr/bin/perl

package Sling::Util;

=head1 NAME

Util - useful utility functions for general Rest functionality.

=head1 ABSTRACT

Utility library providing useful utility functions for general Rest functionality.

=cut

#{{{imports
use strict;
use lib qw ( .. );
#}}}

#{{{sub dateTime

=pod

=head2 dateTime

Returns a current date time string.

=cut

sub dateTime {
    my @months = qw(Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec);
    my @weekDays = qw(Sun Mon Tue Wed Thu Fri Sat Sun);
    (my $second, my $minute, my $hour, my $dayOfMonth,
     my $month, my $yearOffset, my $dayOfWeek, my $dayOfYear, my $daylightSavings) = localtime();
    $second = "0$second" if $second < 10;
    $second = "0$minute" if $minute < 10;
    my $year = 1900 + $yearOffset;
    return "$weekDays[$dayOfWeek] $months[$month] $dayOfMonth $hour:$minute:$second";
}
#}}}

#{{{sub help_header
sub help_header {
    my ( $script ) = @_;
    print "Usage: $script [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]\n\n";
    print "The following options are accepted:\n\n";
}
#}}}

#{{{sub help_footer
sub help_footer {
    my ( $script ) = @_;
    print "\nOptions may be merged together. -- stops processing of options.\n";
    print "Space is not required between options and their arguments.\n";
    print "For more details run: perldoc -F $script\n";
}
#}}}

1;
