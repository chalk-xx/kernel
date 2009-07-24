#!/usr/bin/perl

#{{{imports
use warnings;
use strict;
use Carp;
use lib qw ( .. );
use version; our $VERSION = qv('0.0.1');
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
my $number_forks = 1;
my $page;
my $password;
my $search_content;
my $search_sites;
my $search_users;
my $url;
my $username;
my $verbose;

GetOptions(
    'auth=s'             => \$auth,
    'file|F=s'           => \$file,
    'help|?'             => \$help,
    'items|i=i'          => \$items,
    'log|L=s'            => \$log,
    'man|M'              => \$man,
    'page|P=i'           => \$page,
    'pass|p=s'           => \$password,
    'search-content|s=s' => \$search_content,
    'search-sites=s'     => \$search_sites,
    'search-users=s'     => \$search_users,
    'threads|t=i'        => \$number_forks,
    'url|U=s'            => \$url,
    'user|u=s'           => \$username,
    'verbose|v+'         => \$verbose
) or pod2usage(2);

if ($help) { pod2usage( -exitstatus => 0, -verbose => 1 ); }
if ($man)  { pod2usage( -exitstatus => 0, -verbose => 2 ); }

my $max_allowed_forks = '32';
$number_forks = ( $number_forks || 1 );
$number_forks = ( $number_forks =~ /^[0-9]+$/xms ? $number_forks : 1 );
$number_forks = ( $number_forks < $max_allowed_forks ? $number_forks : 1 );

$url = Sling::URL::url_input_sanitize($url);

#}}}

#{{{ main execution path
if ( defined $file ) {
    my $message = "Searching through all words in file: \"$file\":";
    Sling::Print::print_with_lock( "$message", $log );
    my @childs = ();
    for my $i ( 0 .. $number_forks ) {
        my $pid = fork;
        if ($pid) { push @childs, $pid; }    # parent
        elsif ( $pid == 0 ) {                # child
            my $authn =
              new Sling::Authn( $url, $username, $password, $auth, $verbose,
                $log );
            my $search = new Sling::Search( \$authn, $verbose, $log );
            $search->search_from_file( $file, $i, $number_forks );
            exit 0;
        }
        else {
            croak "Could not fork $i!";
        }
    }
    foreach (@childs) { waitpid $_, 0; }
}
else {
    my $authn =
      new Sling::Authn( $url, $username, $password, $auth, $verbose, $log );
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
    Sling::Print::print_result($search);
}

#}}}

1;

__END__

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

=item Authenticate and search for the user "bob":

 perl search.pl -U http://localhost:8080 --search-users bob -u admin -p admin

=item Authenticate and search for the term test in all sites, list 10 items per page and reurn page 4:

 perl search.pl -U http://localhost:8080 --search-sites test -i 10 -P 4 -u admin -p admin

=item Authenticate and search for all users beginning with 'a' with verbose output: 

 perl search.pl -U http://localhost:8080 --search-users "a*" -u admin -p admin -v

=back

=cut
#}}}
