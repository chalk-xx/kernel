#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

connections perl script. Provides a means of managing user connections in a
system from the command line. Additionally serves as a reference example for
using the Sling::Connections library.

=head1 OPTIONS

Usage: perl connections.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --accept (actOnUser)           - accept connection request from specified user.
 --auth (type)                  - Specify auth type. If ommitted, default is used.
 --block  (actOnUser)           - block connection request from specified user.
 --file or -F (File)            - File containing list of connection operations to perform.
 --help or -?                   - view the script synopsis and options.
 --ignore (actOnUser)           - ignore connection request from specified user.
 --invite (actOnUser)           - invite specified user to connect.
 --list-accepted                - list accepted connections.
 --list-all                     - list all connections.
 --list-blocked                 - list blocked connections.
 --list-ignored                 - list ignored connections.
 --list-invited                 - list invited connections.
 --list-pending                 - list pending connections.
 --list-rejected                - list rejectd connections.
 --log or -L (log)              - Log script output to specified log file.
 --man or -M                    - view the full script documentation.
 --pass or -p (password)        - Password of user performing connection operation.
 --reject (actOnUser)           - reject connection request from specified user.
 --remove (actOnUser)           - remove connection request from specified user.
 --threads or -t (threads)      - Used with -F, defines number of parallel
                                  processes to have running through file.
 --type or -T                   - type(s) to add connection as, e.g. "friend".
 --url or -U (URL)              - URL for system being tested against.
 --user or -u (username)        - Name of user performing connection operation.
 --verbose or -v                - Increase verbosity of output.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl connections.pl --man

=head1 Example Usage

=over

=item Authenticate as testuser1 and invite testuser2 to connect with type friend:

 perl connections.pl -U http://localhost:8080 -u testuser1 -p pass --invite testuser2 --type friend

=item Authenticate as testuser2 and accept invitation to connect from testuser1:

 perl connections.pl -U http://localhost:8080 -u testuser2 -p pass --accept testuser2

=item Authenticate as testuser2 and block invitation to connect from testuser1:

 perl connections.pl -U http://localhost:8080 -u testuser2 -p pass --block testuser2

=item Authenticate as testuser2 and ignore invitation to connect from testuser1:

 perl connections.pl -U http://localhost:8080 -u testuser2 -p pass --ignore testuser2

=item Authenticate as testuser2 and reject invitation to connect from testuser1:

 perl connections.pl -U http://localhost:8080 -u testuser2 -p pass --reject testuser2

=item Authenticate as testuser2 and remove invitation to connect from testuser1:

 perl connections.pl -U http://localhost:8080 -u testuser2 -p pass --remove testuser2

=item Authenticate as testuser and list accepted connections:

 perl connections.pl -U http://localhost:8080 -u testuser -p pass --list-accepted

=item Authenticate as testuser and list all connections:

 perl connections.pl -U http://localhost:8080 -u testuser -p pass --list-all

=item Authenticate as testuser and list blocked connections:

 perl connections.pl -U http://localhost:8080 -u testuser -p pass --list-blocked

=item Authenticate as testuser and list ignored connections:

 perl connections.pl -U http://localhost:8080 -u testuser -p pass --list-ignored

=item Authenticate as testuser and list invited connections:

 perl connections.pl -U http://localhost:8080 -u testuser -p pass --list-invited

=item Authenticate as testuser and list pending connections:

 perl connections.pl -U http://localhost:8080 -u testuser -p pass --list-pending

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Connection;
use Sling::UserAgent;
#}}}

#{{{options parsing
my $accept;
my $auth;
my $block;
my $file;
my $help;
my $ignore;
my $invite;
my $list_accepted;
my $list_all;
my $list_blocked;
my $list_ignored;
my $list_invited;
my $list_pending;
my $list_rejected;
my $log;
my $man;
my $numberForks = 1;
my $password;
my $reject;
my $remove;
my $searchTerm;
my @types;
my $url = "http://localhost";
my $username;
my $verbose;

GetOptions (
    "accept=s" => \$accept,
    "auth=s" => \$auth,
    "block=s" => \$block,
    "file|F=s" => \$file,
    "help|?" => \$help,
    "ignore=s" => \$ignore,
    "invite=s" => \$invite,
    "list-accepted" => \$list_accepted,
    "list-all" => \$list_all,
    "list-blocked" => \$list_blocked,
    "list-ignored" => \$list_ignored,
    "list-invited" => \$list_invited,
    "list-pending" => \$list_pending,
    "list-rejected" => \$list_rejected,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "pass|p=s" => \$password,
    "reject=s" => \$reject,
    "remove=s" => \$remove,
    "threads|t=i" => \$numberForks,
    "type|T=s" => \@types,
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
#}}}

#{{{ main execution path
if ( defined $file ) {
    die "bulk connection management not yet supported";
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
}
else {
    my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
    my $connection = new Sling::Connection( $url, $lwpUserAgent, $verbose, $log );
    if ( defined $accept ) {
        $connection->accept( $accept );
    }
    elsif ( defined $block ) {
        $connection->block( $block );
    }
    elsif ( defined $ignore ) {
        $connection->ignore( $ignore );
    }
    elsif ( defined $invite ) {
        $connection->invite( $invite, \@types );
    }
    elsif ( defined $reject ) {
        $connection->reject( $reject );
    }
    elsif ( defined $remove ) {
        $connection->remove( $remove );
    }
    elsif ( defined $list_accepted ) {
        $connection->list_accepted();
    }
    elsif ( defined $list_all ) {
        $connection->list_all();
    }
    elsif ( defined $list_blocked ) {
        $connection->list_blocked();
    }
    elsif ( defined $list_ignored ) {
        $connection->list_ignored();
    }
    elsif ( defined $list_invited ) {
        $connection->list_invited();
    }
    elsif ( defined $list_pending ) {
        $connection->list_pending();
    }
    elsif ( defined $list_rejected ) {
        $connection->list_rejected();
    }
    Sling::Print::print_result( $connection );
}
#}}}

1;
