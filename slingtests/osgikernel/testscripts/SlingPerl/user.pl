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
use Sling::URL;
use Sling::User;

#}}}

#{{{options parsing
my $act_on_pass;
my $additions;
my $add_user;
my $auth;
my $change_pass_user;
my $delete_user;
my $exists_user;
my $help;
my $log;
my $man;
my $me_user;
my $new_pass;
my $number_forks = 1;
my $password;
my @properties;
my $sites_user;
my $update_user;
my $url;
my $username;
my $verbose;
my $view_user;

GetOptions(
    'add|a=s'             => \$add_user,
    'additions|A=s'       => \$additions,
    'auth=s'              => \$auth,
    'change-password|c=s' => \$change_pass_user,
    'delete|d=s'          => \$delete_user,
    'exists|e=s'          => \$exists_user,
    'help|?'              => \$help,
    'log|L=s'             => \$log,
    'man|M'               => \$man,
    'me|m'                => \$me_user,
    'new-password|n=s'    => \$new_pass,
    'password|w=s'        => \$act_on_pass,
    'pass|p=s'            => \$password,
    'property|P=s'        => \@properties,
    'sites|s'             => \$sites_user,
    'threads|t=s'         => \$number_forks,
    'update=s'            => \$update_user,
    'url|U=s'             => \$url,
    'user|u=s'            => \$username,
    'verbose|v+'          => \$verbose,
    'view|V=s'            => \$view_user
) or pod2usage(2);

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
    my $message = "Adding users from file \"$additions\":\n";
    Sling::Print::print_with_lock( "$message", $log );
    my @childs = ();
    for my $i ( 0 .. $number_forks ) {
        my $pid = fork;
        if ($pid) { push @childs, $pid; }    # parent
        elsif ( $pid == 0 ) {                # child
                # Create a separate authorization per fork:
            my $authn =
              new Sling::Authn( $url, $username, $password, $auth, $verbose,
                $log );
            my $user = new Sling::User( \$authn, $verbose, $log );
            $user->add_from_file( $additions, $i, $number_forks );
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
    my $user = new Sling::User( \$authn, $verbose, $log );

    if ( defined $exists_user ) {
        $user->exists($exists_user);
    }
    elsif ( defined $me_user ) {
        $user->me();
    }
    elsif ( defined $sites_user ) {
        $user->sites();
    }
    elsif ( defined $add_user ) {
        $user->add( $add_user, $act_on_pass, \@properties );
    }
    elsif ( defined $update_user ) {
        $user->update( $update_user, \@properties );
    }
    elsif ( defined $change_pass_user ) {
        $user->change_password( $change_pass_user, $act_on_pass, $new_pass,
            $new_pass );
    }
    elsif ( defined $delete_user ) {
        $user->delete($delete_user);
    }
    elsif ( defined $view_user ) {
        $user->view($view_user);
    }
    Sling::Print::print_result($user);
}

#}}}

1;

__END__

#{{{Documentation
=head1 SYNOPSIS

user perl script. Provides a means of managing users in sling from the command
line. The script also acts as a reference implementation for the User perl
library.

=head1 OPTIONS

Usage: perl user.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --add or -a (actOnUser)             - add specified user name.
 --additions or -A (file)            - file containing list of users to be added.
 --auth (type)                       - Specify auth type. If ommitted, default is used.
 --change-password or -c (actOnUser) - change password of specified user name.
 --delete or -d (actOnUser)          - delete specified user name.
 --exists or -e (actOnUser)          - check whether specified user exists.
 --help or -?                        - view the script synopsis and options.
 --log or -L (log)                   - Log script output to specified log file.
 --man or -M                         - view the full script documentation.
 --me or -m                          - me returns json representing authenticated user.
 --new-password or -n (newPassword)  - Used with -c, new password to set.
 --password or -w (actOnPass)        - Password of user being actioned.
 --pass or -p (password)             - Password of user performing actions.
 --property or -P (property=value)   - Specify property to set on user.
 --sites or -s                       - list sites authenticated user is a member of.
 --threads or -t (threads)           - Used with -A, defines number of parallel
                                       processes to have running through file.
 --update (actOnUser)                - update specified user name, used with -P.
 --url or -U (URL)                   - URL for system being tested against.
 --user or -u (username)             - Name of user to perform any actions as.
 --verbose or -v or -vv or -vvv      - Increase verbosity of output.
 --view or -V (actOnUser)            - view details for specified user in json format.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl user.pl --man

=head1 Example Usage

=over

=item Add user "testuser" with password "test"

 perl user.pl -U http://localhost:8080 -a testuser -w test

=item View information about authenticated user "testuser"

 perl user.pl -U http://localhost:8080 --me -u testuser -p test

=item View sites authenticated user "testuser" is a member of:

 perl user.pl -U http://localhost:8080 --sites -u testuser -p test

=item Authenticate as admin and check whether user "testuser" exists

 perl user.pl -U http://localhost:8080 -e testuser -u admin -p admin

=item Authenticate and update "testuser" to set property p1=v1

 perl user.pl -U http://localhost:8080 --update testuser -P "p1=v1" -u admin -p admin

=item Authenticate and delete "testuser"

 perl user.pl -U http://localhost:8080 -d testuser -u admin -p admin

=back

=cut
#}}}
