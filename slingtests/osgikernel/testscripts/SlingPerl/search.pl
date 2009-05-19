#!/usr/bin/perl

=head1 NAME

search perl script. Provides a means of performing searches in a s ystem
from the command line.

=head1 ABSTRACT

This script can be used to perform searches in a system and additionally
serves as a reference example for using the Sling::Search library.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Search;
use Sling::UserAgent;
use Sling::Util;
use Getopt::Long qw(:config bundling);
#}}}

#{{{sub HELP_MESSAGE
sub HELP_MESSAGE {
    my ( $out, $optPackage, $optVersion, $switches ) = @_;
    Sling::Util::help_header( $0, $switches );
    print "-p {password}   - Password of user performing searches.\n";
    print "-s {SearchTerm} - Term to search in the system for.\n";
    print "-t {threads}    - Used with -F, defines number of parallel\n";
    print "                  processes to have running through file.\n";
    print "-u {username}   - Name of user to perform any searches as.\n";
    print "-F {File}       - File containing list of search terms to search through.\n";
    print "-L {log}        - Log script output to specified log file.\n";
    print "-P {path}       - specify absolute path under the JCR root to search under.\n";
    print "-U {URL}        - URL for system being tested against.\n";
    print "--auth {type}   - Specify auth type. If ommitted, default is used.\n";
    Sling::Util::help_footer( $0 );
}
#}}}

#{{{options parsing
my $auth;
my $url = "http://localhost";
my $searchTerm;
my $numberForks = 1;
my $file;
my $log;
my $username;
my $password;
my $path="/";

GetOptions ( "s=s" => \$searchTerm,  "F=s" => \$file,
             "t=i" => \$numberForks, "L=s" => \$log,
             "u=s" => \$username,    "P=s" => \$path,
	     "p=s" => \$password,    "U=s" => \$url,
	     "auth=s" => \$auth,     "help" => \&HELP_MESSAGE );

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );
#}}}

#{{{ main execution path
if ( defined $file ) {
    print "Searching through all words in file:\n";
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
            my $search = new Sling::Search( $url, $lwpUserAgent );
            $search->search_from_file( $file, $i, $numberForks, $path, $log );
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
        $search->search( $searchTerm, $path, $log );
        if ( ! defined $log ) {
            print $search->{ 'Message' } . "\n";
        }
    }
}
#}}}

1;
