#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

regression testing perl script. Provides a means of performing regression
testing of a Sling K2 system from the command line.

=head1 OPTIONS

Usage: perl regression_test.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --help or -?                - view the script synopsis and options.
 --log or -L (log)           - Log script output to specified log file.
 --man or -M                 - view the full script documentation.
 --pass or -p (password)     - Password of system super user.
 --threads or -t (threads)   - Defines number of parallel processes
                               to have running regression tests.
 --url or -U (URL)           - URL for system being tested against.
 --user or -u (username)     - User ID of system super user.
 --verbose or -v             - Increase verbosity of output.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl regression_test.pl --man

=head1 Example Usage

=over

=item Authenticate and search for the word test through all content:

 perl regression_test.pl -U http://localhost:8080 -s test -u admin -p admin

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::UserAgent;
use Tests::User;
#}}}

#{{{options parsing
my $help;
my $log;
my $man;
my $numberForks = 1;
my $password;
my $url = "http://localhost";
my $username;
my $verbose;

GetOptions (
    "help|?" => \$help,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "pass|p=s" => \$password,
    "threads|t=i" => \$numberForks,
    "url|U=s" => \$url,
    "user|u=s" => \$username,
    "verbose|v+" => \$verbose
) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );

die "Test URL not defined" unless defined $url;
die "Test super user username not defined" unless defined $username;
die "Test super user password not defined" unless defined $password;

my $auth; # Just use default auth
#}}}

#{{{ main execution path
# if ( defined $file ) {
    # my $message = "Searching through all words in file: \"$file\":";
    # Sling::Print::print_with_lock( "$message", $log );
    # my @childs = ();
    # for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	# my $pid = fork();
	# if ( $pid ) { push( @childs, $pid ); } # parent
	# elsif ( $pid == 0 ) { # child
            # my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
            # my $search = new Sling::Search( $url, $lwpUserAgent, $verbose );
            # $search->search_from_file( $file, $i, $numberForks, $log );
	    # exit( 0 );
	# }
	# else {
            # die "Could not fork $i!";
	# }
    # }
    # foreach ( @childs ) { waitpid( $_, 0 ); }
# }
# else {
    my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
    Tests::User::run_regression_test( $url, $lwpUserAgent, $log, $verbose );
# }
#}}}

1;
