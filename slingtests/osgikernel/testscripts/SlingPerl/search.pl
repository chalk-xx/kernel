#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

search perl script. Provides a means of performing searches in a system from
the command line. Additionally serves as a reference example for using the
Sling::Search library.

=head1 OPTIONS

Usage: perl search.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 -p (password)   - Password of user performing searches.
 -s (SearchTerm) - Term to search in the system for.
 -t (threads)    - Used with -F, defines number of parallel
                   processes to have running through file.
 -u (username)   - Name of user to perform any searches as.
 -F (File)       - File containing list of search terms to search through.
 -L (log)        - Log script output to specified log file.
 -P (path)       - specify absolute path under the JCR root to search under.
 -U (URL)        - URL for system being tested against.
 --auth (type)   - Specify auth type. If ommitted, default is used.
 --help or -?    - view the script synopsis and options.
 --man           - view the full script documentation.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl search.pl --man

=head1 Example Usage

=over

=item Authenticate and add search for the word test through all content:

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
my $path="/";
my $searchTerm;
my $url = "http://localhost";
my $username;

GetOptions ( "s=s" => \$searchTerm,  "F=s" => \$file,
             "t=i" => \$numberForks, "L=s" => \$log,
             "u=s" => \$username,    "P=s" => \$path,
	     "p=s" => \$password,    "U=s" => \$url,
	     "auth=s" => \$auth,
             "help|?" => \$help, "man" => \$man) or pod2usage(2);

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
