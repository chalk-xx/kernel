#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

search perl script. Provides a means of performing searches in a system from
the command line. Additionally serves as a reference example for using the
Sling::Search library.

=head1 OPTIONS

Usage: perl search.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --auth (type)                  - Specify auth type. If ommitted, default is used.
 --file or -F (File)            - File containing list of search terms to search through.
 --help or -?                   - view the script synopsis and options.
 --items or -i (items)          - Number of items to list per search page.
 --log or -L (log)              - Log script output to specified log file.
 --man or -M                    - view the full script documentation.
 --pass or -p (password)        - Password of user performing searches.
 --page or -P (page)            - Page of search results to return.
 --search-content or -s (term)  - Search for term within the content space.
 --search-users (term)          - Search for term within the user space.
 --search-sites (term)          - Search for term within the site space.
 --threads or -t (threads)      - Used with -F, defines number of parallel
                                  processes to have running through file.
 --url or -U (URL)              - URL for system being tested against.
 --user or -u (username)        - Name of user to perform any searches as.
 --verbose or -v or -vv or -vvv - Increase verbosity of output.

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
use Sling::Authn;
use Sling::Search;
use Sling::URL;
#}}}

#{{{options parsing
my $auth;
my $file;
my $help;
my $items;
my $log;
my $man;
my $numberForks = 1;
my $page;
my $password;
my $search_content;
my $search_sites;
my $search_users;
my $url;
my $username;
my $verbose;

GetOptions (
    "auth=s" => \$auth,
    "file|F=s" => \$file,
    "help|?" => \$help,
    "items|i=i" => \$items,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "page|P=i" => \$page,
    "pass|p=s" => \$password,
    "search-content|s=s" => \$search_content,
    "search-sites=s" => \$search_sites,
    "search-users=s" => \$search_users,
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

$url = Sling::URL::url_input_sanitize( $url );
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
            my $authn = new Sling::Authn( $url, $username, $password, $auth, $verbose, $log );
            my $search = new Sling::Search( \$authn, $verbose, $log );
            $search->search_from_file( $file, $i, $numberForks );
	    exit( 0 );
	}
	else {
            die "Could not fork $i!";
	}
    }
    foreach ( @childs ) { waitpid( $_, 0 ); }
}
else {
    my $authn = new Sling::Authn( $url, $username, $password, $auth, $verbose, $log );
    my $search = new Sling::Search( \$authn, $verbose, $log );
    if ( defined $search_content ) {
        $search->search( $search_content, $page, $items );
    }
    elsif ( defined $search_sites ) {
        $search->search_sites( $search_sites, $page, $items );
    }
    elsif ( defined $search_users ) {
        $search->search_users( $search_users, $page, $items );
    }
    Sling::Print::print_result( $search );
}
#}}}

1;
