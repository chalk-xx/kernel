#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

regression testing perl script. Provides a means of performing regression
testing of a Sling K2 system from the command line.

=head1 OPTIONS

Usage: perl regression_test.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --all or -a                  - run all regression tests.
 --group or -g                - run group regression tests.
 --help or -?                 - view the script synopsis and options.
 --log or -L (log)            - Log script output to specified log file.
 --man or -M                  - view the full script documentation.
 --pass or -p (password)      - Password of system super user.
 --superuser or -s (username) - User ID of system super user.
 --threads or -t (threads)    - Defines number of parallel processes
                                to have running regression tests.
 --url or -U (URL)            - URL for system being tested against.
 --user or -u                 - run user regression tests.
 --verbose or -v              - Increase verbosity of output.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl regression_test.pl --man

=head1 Example Usage

=over

=item Run user regression tests, specifying superuser to be admin and superuser password to be admin.

 perl regression_test.pl -U http://localhost:8080 --user --superuser admin --pass admin

=item Run all regression tests in four threads, specifying superuser to be admin and superuser password to be admin.

 perl regression_test.pl -U http://localhost:8080 --all --threads 4 -s admin -p admin

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::UserAgent;
use Tests::Group;
use Tests::User;
#}}}

#{{{options parsing
my $all_tests;
my $group_test;
my $help;
my $log;
my $man;
my $numberForks = 1;
my $password;
my $url = "http://localhost";
my $username;
my $user_test;
my $verbose;

GetOptions (
    "all" => \$all_tests,
    "group|g" => \$group_test,
    "help|?" => \$help,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "pass|p=s" => \$password,
    "superuser|s=s" => \$username,
    "threads|t=i" => \$numberForks,
    "url|U=s" => \$url,
    "user|u" => \$user_test,
    "verbose|v+" => \$verbose
) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );

die "Test URL not defined" unless defined $url;
die "Test super user username not defined" unless defined $username;
die "Test super user password not defined" unless defined $password;

my $auth; # Just use default auth

my @all_tests_list = ( "Group", "User" );
my @tests_selected = ();

if ( $all_tests ) {
    @tests_selected = @all_tests_list;
}
else {
    if ( $group_test ) {
        push ( @tests_selected, "Group" );
    }
    if ( $user_test ) {
        push ( @tests_selected, "User" );
    }
}

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );
$numberForks = ( @tests_selected < $numberForks ? @tests_selected : $numberForks );
#}}}

#{{{ main execution path
my @childs = ();
for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
    my $pid = fork();
    if ( $pid ) { push( @childs, $pid ); } # parent
    elsif ( $pid == 0 ) { # child
        for ( my $j = $i ; $j < @tests_selected ; $j += $numberForks ) {
            my $test = $tests_selected[ $j ];
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
	    if ( $test =~ /^Group$/ ) {
                Tests::Group::run_regression_test( $url, $lwpUserAgent, $log, $verbose );
	    }
	    elsif ( $test =~ /^User$/ ) {
                Tests::User::run_regression_test( $url, $lwpUserAgent, $log, $verbose );
	    }
	    else {
	        die "Unknown regression test option: \"$test\"!";
	    }
	}
	exit( 0 );
    }
    else {
        die "Could not fork $i!";
    }
}
foreach ( @childs ) { waitpid( $_, 0 ); }
#}}}

1;
