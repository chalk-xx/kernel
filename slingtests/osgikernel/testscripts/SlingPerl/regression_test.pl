#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

regression testing perl script. Provides a means of performing regression
testing of a Sling K2 system from the command line.

=head1 OPTIONS

Usage: perl regression_test.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --all                          - run all regression tests.
 --authn                        - run authentication regression tests.
 --content                      - run content regression tests.
 --group                        - run group regression tests.
 --help or -?                   - view the script synopsis and options.
 --log or -L (log)              - Log script output to specified log file.
 --man or -M                    - view the full script documentation.
 --pass or -p (password)        - Password of system super user.
 --site                         - run site regression tests.
 --superuser or -u (username)   - User ID of system super user.
 --threads or -t (threads)      - Defines number of parallel processes
                                  to have running regression tests.
 --url or -U (URL)              - URL for system being tested against.
 --user                         - run user regression tests.
 --verbose or -v or -vv or -vvv - Increase verbosity of output.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl regression_test.pl --man

=head1 Example Usage

=over

=item Run user regression tests, specifying superuser to be admin and superuser password to be admin.

 perl regression_test.pl -U http://localhost:8080 --user --superuser admin --pass admin

=item Run all regression tests in four threads, specifying superuser to be admin and superuser password to be admin.

 perl regression_test.pl -U http://localhost:8080 --all --threads 4 -S admin -p admin

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Authn;
use Sling::URL;
use Tests::Authn;
use Tests::Content;
use Tests::Group;
use Tests::Search;
use Tests::Site;
use Tests::User;
#}}}

#{{{options parsing
my $all_tests;
my $authn_test;
my $content_test;
my $group_test;
my $help;
my $log;
my $man;
my $numberForks = 1;
my $password;
my $search_test;
my $site_test;
my $url;
my $username;
my $user_test;
my $verbose;

GetOptions (
    "all" => \$all_tests,
    "authn" => \$authn_test,
    "content" => \$content_test,
    "group" => \$group_test,
    "help|?" => \$help,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "pass|p=s" => \$password,
    "search" => \$search_test,
    "site" => \$site_test,
    "superuser|u=s" => \$username,
    "threads|t=i" => \$numberForks,
    "url|U=s" => \$url,
    "user" => \$user_test,
    "verbose|v+" => \$verbose
) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$url = Sling::URL::url_input_sanitize( $url );

die "Test super user username not defined" unless defined $username;
die "Test super user password not defined" unless defined $password;

my $auth; # Just use default auth

my @all_tests_list = ( "Authn", "Content", "Group", "Search", "Site", "User" );
my @tests_selected = ();

if ( $all_tests ) {
    @tests_selected = @all_tests_list;
}
else {
    if ( $authn_test ) {
        push ( @tests_selected, "Authn" );
    }
    if ( $content_test ) {
        push ( @tests_selected, "Content" );
    }
    if ( $group_test ) {
        push ( @tests_selected, "Group" );
    }
    if ( $search_test ) {
        push ( @tests_selected, "Search" );
    }
    if ( $site_test ) {
        push ( @tests_selected, "Site" );
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
            my $authn = new Sling::Authn( $url, $username, $password, $auth, $verbose, $log );
	    if ( $test =~ /^Authn$/ ) {
                Tests::Authn::run_regression_test( \$authn, $verbose, $log );
	    }
	    elsif ( $test =~ /^Content$/ ) {
                Tests::Content::run_regression_test( \$authn, $verbose, $log );
	    }
	    elsif ( $test =~ /^Group$/ ) {
                Tests::Group::run_regression_test( \$authn, $verbose, $log );
	    }
	    elsif ( $test =~ /^Search$/ ) {
                Tests::Search::run_regression_test( \$authn, $verbose, $log );
	    }
	    elsif ( $test =~ /^Site$/ ) {
                Tests::Site::run_regression_test( \$authn, $verbose, $log );
	    }
	    elsif ( $test =~ /^User$/ ) {
                Tests::User::run_regression_test( \$authn, $verbose, $log );
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
