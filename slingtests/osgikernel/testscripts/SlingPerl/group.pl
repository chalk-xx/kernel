#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

group perl script. Provides a means of managing groups in sling from the
command line. The script also acts as a reference implementation for the Group
perl library.

=head1 OPTIONS

Usage: perl group.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --additions or -A (file)          - file containing list of groups to be added.
 --add or -a (actOnGroup)          - add specified group.
 --auth (type)                     - Specify auth type. If ommitted, default is used.
 --delete or -d (actOnGroup)       - delete specified group.
 --exists or -e (actOnGroup)       - check whether specified group exists.
 --help or -?                      - view the script synopsis and options.
 --log or -L (log)                 - Log script output to specified log file.
 --man or -M                       - view the full script documentation.
 --pass or -p (password)           - Password of user performing actions.
 --property or -P (property=value) - Specify property to set on group.
 --threads or -t (threads)         - Used with -A, defines number of parallel
                                     processes to have running through file.
 --url or -U (URL)                 - URL for system being tested against.
 --user or -u (username)           - Name of user to perform any actions as.
 --verbose or -v                   - Increase verbosity of output.
 --view or -V (actOnGroup)         - view details for specified group in json format.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl group.pl --man

=head1 Example Usage

=over

=item Authenticate and add a group with id g-test:

 perl group.pl -U http://localhost:8080 -u admin -p admin -a g-test

=item Authenticate and check whether group with id g-test exists:

 perl group.pl -U http://localhost:8080 -u admin -p admin -a g-test

=item Authenticate and view details for group with id g-test:

 perl group.pl -U http://localhost:8080 -u admin -p admin -V g-test

=item Authenticate and delete group with id g-test:

 perl group.pl -U http://localhost:8080 -u admin -p admin -d g-test

=item Authenticate and add a group with id g-test and property p1=v1:

 perl group.pl -U http://localhost:8080 -u admin -p admin -a g-test -P p1=v1

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Group;
use Sling::UserAgent;
#}}}

#{{{options parsing
my $addGroup;
my $additions;
my $auth;
my $deleteGroup;
my $existsGroup;
my $help;
my $log;
my $man;
my $numberForks = 1;
my $password;
my @properties,
my $url = "http://localhost";
my $username;
my $verbose;
my $viewGroup;

GetOptions (
    "add|a=s" => \$addGroup,
    "additions|A=s" => \$additions,
    "auth=s" => \$auth,
    "delete|d=s" => \$deleteGroup,
    "exists|e=s" => \$existsGroup,
    "help|?" => \$help,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "pass|p=s" => \$password,
    "property|P=s" => \@properties,
    "threads|t=s" => \$numberForks,
    "url|U=s" => \$url,
    "user|u=s" => \$username,
    "verbose|v+" => \$verbose,
    "view|V=s" => \$viewGroup
) or pod2usage(-exitstatus => 2, -verbose => 1);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );
#}}}

#{{{main execution path
if ( defined $additions ) {
    my $message = "Adding groups from file \"$additions\":\n";
    Sling::Print::print_with_lock( "$message", $log );
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate user agent per fork:
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
            my $group = new Sling::Group( $url, $lwpUserAgent );
            $group->add_from_file( $additions, $i, $numberForks, $log );
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
    my $group = new Sling::Group( $url, $lwpUserAgent, $verbose );
    if ( defined $existsGroup ) {
        $group->exists( $existsGroup, $log );
    }
    elsif ( defined $addGroup ) {
        $group->add( $addGroup, \@properties, $log );
    }
    elsif ( defined $deleteGroup ) {
        $group->delete( $deleteGroup, $log );
    }
    elsif ( defined $viewGroup ) {
        $group->view( $viewGroup, $log );
    }
    Sling::Print::print_result( $group, $log );
}
#}}}

1;
