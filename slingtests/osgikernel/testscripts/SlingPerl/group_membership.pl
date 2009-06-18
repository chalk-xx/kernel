#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

group membership perl script. Provides a means of managing membership of groups
in sling from the command line.

=head1 OPTIONS

Usage: perl group_membership.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --additions or -A (file)   - file containing list of members to be added to groups.
 --add or -a (member)       - add specified member.
 --auth (type)              - Specify auth type. If ommitted, default is used.
 --delete or - d (member)   - delete specified group.
 --exists or -e (member)    - check whether specified member exists in group.
 --group or -g (actOnGroup) - group to perform membership actions on.
 --help or -?               - view the script synopsis and options.
 --log or -L (log)          - Log script output to specified log file.
 --man or -M                - view the full script documentation.
 --pass or -p (password)    - Password of user performing actions.
 --threads or -t (threads)  - Used with -A, defines number of parallel
                              processes to have running through file.
 --url or -U (URL)          - URL for system being tested against.
 --user or -u (username)    - Name of user to perform any actions as.
 --view or -v               - view members of specified group.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl group.pl --man

=head1 Example Usage

=over

=item Authenticate and add a member testmember to the group with id testgroup:

 perl group.pl -U http://localhost:8080 -a testmember -g testgroup -u admin -p admin

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
my @properties,
my $url = "http://localhost";
my $username;
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
    "property|P=s" => \@properties,
    "threads|t=s" => \$numberForks,
    "url|U=s" => \$url,
    "user|u=s" => \$username,
    "view|v" => \$viewMembers
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
    my $message = "Adding members to groups as specified in file \"$additions\":";
    if ( defined $log ) {
        Sling::Print::print_file_lock( "$message", $log );
    }
    else {
        Sling::Print::print_lock( "$message" );
    }
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate user agent per fork:
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
            my $group = new Sling::Group( $url, $lwpUserAgent );
            $group->member_add_from_file( $additions, $i, $numberForks, $log );
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
    my $group = new Sling::Group( $url, $lwpUserAgent );

    if ( defined $existsMember ) {
        $group->member_exists( $actOnGroup, $existsMember, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $addMember ) {
        $group->member_add( $actOnGroup, $addMember, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $deleteMember ) {
        $group->member_delete( $actOnGroup, $deleteMember, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $viewMembers ) {
        $group->member_view( $actOnGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
}
#}}}

1;
