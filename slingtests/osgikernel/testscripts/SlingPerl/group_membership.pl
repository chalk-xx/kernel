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
use Sling::Group;
use Sling::URL;

#}}}

#{{{options parsing
my $act_on_group;
my $add_member;
my $additions;
my $auth;
my $delete_member;
my $exists_member;
my $help;
my $log;
my $man;
my $number_forks = 1;
my $password;
my $url;
my $username;
my $verbose;
my $view_members;

GetOptions(
    'add|a=s'       => \$add_member,
    'additions|A=s' => \$additions,
    'auth=s'        => \$auth,
    'delete|d=s'    => \$delete_member,
    'exists|e=s'    => \$exists_member,
    'group|g=s'     => \$act_on_group,
    'help|?'        => \$help,
    'log|L=s'       => \$log,
    'man|M'         => \$man,
    'pass|p=s'      => \$password,
    'threads|t=s'   => \$number_forks,
    'url|U=s'       => \$url,
    'user|u=s'      => \$username,
    'verbose|v+'    => \$verbose,
    'view|V'        => \$view_members
) or pod2usage( -exitstatus => 2, -verbose => 1 );

if ($help) { pod2usage( -exitstatus => 0, -verbose => 1 ); }
if ($man)  { pod2usage( -exitstatus => 0, -verbose => 2 ); }

my $max_allowed_forks = '32';
$number_forks = ( $number_forks || 1 );
$number_forks = ( $number_forks =~ /^[0-9]+$/xms ? $number_forks : 1 );
$number_forks = ( $number_forks < $max_allowed_forks ? $number_forks : 1 );

$url = Sling::URL::url_input_sanitize($url);

#}}}

#{{{main execution path
if ( defined $additions ) {
    my $message =
      "Adding members to groups as specified in file \"$additions\":";
    Sling::Print::print_with_lock("$message");
    my @childs = ();
    for my $i ( 0 .. $number_forks ) {
        my $pid = fork;
        if ($pid) { push @childs, $pid; }    # parent
        elsif ( $pid == 0 ) {                # child
                # Create a separate authorization per fork:
            my $authn =
              new Sling::Authn( $url, $username, $password, $auth, $verbose,
                $log );
            my $group = new Sling::Group( \$authn, $verbose, $log );
            $group->member_add_from_file( $additions, $i, $number_forks );
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
    my $group = new Sling::Group( \$authn, $verbose, $log );

    if ( defined $exists_member ) {
        $group->member_exists( $act_on_group, $exists_member );
    }
    elsif ( defined $add_member ) {
        $group->member_add( $act_on_group, $add_member );
    }
    elsif ( defined $delete_member ) {
        $group->member_delete( $act_on_group, $delete_member );
    }
    elsif ( defined $view_members ) {
        $group->member_view($act_on_group);
    }
    Sling::Print::print_result($group);
}

#}}}

1;

__END__

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
