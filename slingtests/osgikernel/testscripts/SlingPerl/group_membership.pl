#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

group membership perl script. Provides a means of managing membership of groups
in sling from the command line.

=head1 OPTIONS

Usage: perl group_membership.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --additions or -A (file)       - file containing list of members to be added to groups.
 --add or -a (member)           - add specified member.
 --auth (type)                  - Specify auth type. If ommitted, default is used.
 --delete or -d (member)        - delete specified group member.
 --exists or -e (member)        - check whether specified member exists in group.
 --group or -g (actOnGroup)     - group to perform membership actions on.
 --help or -?                   - view the script synopsis and options.
 --log or -L (log)              - Log script output to specified log file.
 --man or -M                    - view the full script documentation.
 --pass or -p (password)        - Password of user performing actions.
 --threads or -t (threads)      - Used with -A, defines number of parallel
                                  processes to have running through file.
 --url or -U (URL)              - URL for system being tested against.
 --user or -u (username)        - Name of user to perform any actions as.
 --verbose or -v or -vv or -vvv - Increase verbosity of output.
 --view or -V                   - view members of specified group.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl group_membership.pl --man

=head1 Example Usage

=over

=item Authenticate and add a member testuser to the group with id g-test:

 perl group_membership.pl -U http://localhost:8080 -g g-test -u admin -p admin -a testuser

=item Authenticate and view members of group with id g-test:

 perl group_membership.pl -U http://localhost:8080 -g g-test -u admin -p admin -V

=item Authenticate and check whether testuser is a member of group with id g-test:

 perl group_membership.pl -U http://localhost:8080 -g g-test -u admin -p admin -e testuser 

=item Authenticate and remove testuser from being a member of group with id g-test with very verbose output:

 perl group_membership.pl -U http://localhost:8080 -g g-test -u admin -p admin -d testuser -vv

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Authn;
use Sling::Group;
use Sling::URL;
#}}}

#{{{options parsing
my $actOnGroup;
my $addMember;
my $additions;
my $auth;
my $deleteMember;
my $existsMember;
my $help;
my $log;
my $man;
my $numberForks = 1;
my $password;
my $url;
my $username;
my $verbose;
my $viewMembers;

GetOptions (
    "add|a=s" => \$addMember,
    "additions|A=s" => \$additions,
    "auth=s" => \$auth,
    "delete|d=s" => \$deleteMember,
    "exists|e=s" => \$existsMember,
    "group|g=s" => \$actOnGroup,
    "help|?" => \$help,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "pass|p=s" => \$password,
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

$url = Sling::URL::url_input_sanitize( $url );
#}}}

#{{{main execution path
if ( defined $additions ) {
    my $message = "Adding members to groups as specified in file \"$additions\":";
    Sling::Print::print_with_lock( "$message" );
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate authorization per fork:
            my $authn = new Sling::Authn( $url, $username, $password, $auth, $verbose, $log );
            my $group = new Sling::Group( \$authn, $verbose, $log );
            $group->member_add_from_file( $additions, $i, $numberForks );
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
    my $group = new Sling::Group( \$authn, $verbose, $log );

    if ( defined $existsMember ) {
        $group->member_exists( $actOnGroup, $existsMember );
    }
    elsif ( defined $addMember ) {
        $group->member_add( $actOnGroup, $addMember );
    }
    elsif ( defined $deleteMember ) {
        $group->member_delete( $actOnGroup, $deleteMember );
    }
    elsif ( defined $viewMembers ) {
        $group->member_view( $actOnGroup );
    }
    Sling::Print::print_result( $group );
}
#}}}

1;
