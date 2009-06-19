#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

search perl script. Provides a means of performing searches in a system from
the command line. Additionally serves as a reference example for using the
Sling::Search library.

=head1 OPTIONS

Usage: perl search.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --auth (type)               - Specify auth type. If ommitted, default is used.
 --file or -F (File)         - File containing list of search terms to search through.
 --help or -?                - view the script synopsis and options.
 --log or -L (log)           - Log script output to specified log file.
 --man or -M                 - view the full script documentation.
 --pass or -p (password)     - Password of user performing searches.
 --search or -s (SearchTerm) - Term to search in the system for.
 --threads or -t (threads)   - Used with -F, defines number of parallel
                               processes to have running through file.
 --url or -U (URL)           - URL for system being tested against.
 --user or -u (username)     - Name of user to perform any searches as.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl search.pl --man

=head1 Example Usage

=over

=item Authenticate and search for the word test through all content:

 perl search.pl -U http://localhost:8080 -s test -u admin -p admin

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Search;
use Sling::UserAgent;
#}}}

#{{{options parsing
my $auth;
my $file;
my $help;
my $log;
my $man;
my $numberForks = 1;
my $password;
my $searchTerm;
my $url = "http://localhost";
my $username;

GetOptions (
    "auth=s" => \$auth,
    "file|F=s" => \$file,
    "help|?" => \$help,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "pass|p=s" => \$password,
    "search|s=s" => \$searchTerm,
    "threads|t=i" => \$numberForks,
    "url|U=s" => \$url,
    "user|u=s" => \$username
) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );
#}}}

#{{{ main execution path
if ( defined $file ) {
    my $message = "Searching through all words in file: \"$file\":";
    Sling::Print::print_with_lock( "$message", $log );
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
            my $search = new Sling::Search( $url, $lwpUserAgent );
            $search->search_from_file( $file, $i, $numberForks, $log );
	    exit( 0 );
	}
	else {
            die "Could not fork $i!";
	}
    }
    foreach ( @childs ) { waitpid( $_, 0 ); }
}
else {
    my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
    my $search = new Sling::Search( $url, $lwpUserAgent );
    if ( defined $searchTerm ) {
        $search->search( $searchTerm, $log );
        if ( ! defined $log ) {
            print $search->{ 'Message' } . "\n";
        }
    }
}
#}}}

1;
