#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

site membership perl script. Provides a means of managing membership of sites
in sling from the command line.

=head1 OPTIONS

Usage: perl site_membership.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --additions or -A (file)   - file containing list of members to be added to sites.
 --add or -a (member)       - add specified member.
 --auth (type)              - Specify auth type. If ommitted, default is used.
 --delete or -d (member)    - delete specified site member.
 --exists or -e (member)    - check whether specified member exists in site.
 --help or -?               - view the script synopsis and options.
 --log or -L (log)          - Log script output to specified log file.
 --man or -M                - view the full script documentation.
 --pass or -p (password)    - Password of user performing actions.
 --site or -s (actOnSite)   - site to perform membership actions on.
 --threads or -t (threads)  - Used with -A, defines number of parallel
                              processes to have running through file.
 --url or -U (URL)          - URL for system being tested against.
 --user or -u (username)    - Name of user to perform any actions as.
 --verbose or -v            - Increase verbosity of output.
 --view or -V               - view members of specified site.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl site_membership.pl --man

=head1 Example Usage

=over

=item Authenticate and add a member testuser to the site with id testsite:

 perl site_membership.pl -U http://localhost:8080 -s testsite -u admin -p admin -a testuser

=item Authenticate and view members of site with id testsite:

 perl site_membership.pl -U http://localhost:8080 -s testsite -u admin -p admin -V

=item Authenticate and check whether testuser is a member of site with id testsite:

 perl site_membership.pl -U http://localhost:8080 -s testsite -u admin -p admin -e testuser 

=item Authenticate and remove testuser from being a member of site with id testsite with very verbose output:

 perl site_membership.pl -U http://localhost:8080 -s testsite -u admin -p admin -d testuser -vvv

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Site;
use Sling::UserAgent;
#}}}

#{{{options parsing
my $actOnSite;
my $additions;
my $addMember;
my $auth;
my $deleteMember;
my $existsMember;
my $help;
my $log;
my $man;
my $numberForks = 1;
my $password;
my $url = "http://localhost";
my $username;
my $verbose;
my $viewMembers;

GetOptions (
    "add|a=s" => \$addMember,
    "additions|A=s" => \$additions,
    "auth=s" => \$auth,
    "delete|d=s" => \$deleteMember,
    "exists|e=s" => \$existsMember,
    "help|?" => \$help,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "pass|p=s" => \$password,
    "site|s=s" => \$actOnSite,
    "threads|t=s" => \$numberForks,
    "url|U=s" => \$url,
    "user|u=s" => \$username,
    "verbose|v+" => \$verbose,
    "view|V" => \$viewMembers
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
    my $message = "Adding members to sites as specified in file \"$additions\":";
    Sling::Print::print_with_lock( "$message", $log );
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate user agent per fork:
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
            my $site = new Sling::Site( $url, $lwpUserAgent, $verbose );
            $site->member_add_from_file( $additions, $i, $numberForks, $log );
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
    my $site = new Sling::Site( $url, $lwpUserAgent, $verbose );

    if ( defined $existsMember ) {
        $site->member_exists( $actOnSite, $existsMember, $log );
    }
    elsif ( defined $addMember ) {
        $site->member_add( $actOnSite, $addMember, $log );
    }
    elsif ( defined $deleteMember ) {
        $site->member_delete( $actOnSite, $deleteMember, $log );
    }
    elsif ( defined $viewMembers ) {
        $site->member_view( $actOnSite, $log );
    }
    Sling::Print::print_result( $site, $log );
}
#}}}

1;
