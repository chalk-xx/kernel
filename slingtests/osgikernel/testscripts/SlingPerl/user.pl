#!/usr/bin/perl

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
 --url or -U (URL)                   - URL for system being tested against.
 --user or -u (username)             - Name of user to perform any actions as.
 --verbose or -v or -vv or -vvv      - Increase verbosity of output.
 --view or -V (actOnUser)            - view details for specified user in json format.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl user.pl --man

=head1 Example Usage

=over

=item Authenticate and add user "testuser" with password "test"

 perl user.pl -U http://localhost:8080 -a testuser -w test -u admin -p admin

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Authn;
use Sling::URL;
use Sling::User;
#}}}

#{{{options parsing
my $actOnPass;
my $additions;
my $addUser;
my $auth;
my $changePassUser;
my $deleteUser;
my $existsUser;
my $help;
my $log;
my $man;
my $meUser;
my $newPass;
my $numberForks = 1;
my $password;
my @properties;
my $sitesUser;
my $url;
my $username;
my $verbose;
my $viewUser;

GetOptions (
    "add|a=s" => \$addUser,
    "additions|A=s" => \$additions,
    "auth=s" => \$auth,
    "change-password|c=s" => \$changePassUser,
    "delete|d=s" => \$deleteUser,
    "exists|e=s" => \$existsUser,
    "help|?" => \$help,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "me|m" => \$meUser,
    "new-password|n=s" => \$newPass,
    "password|w=s" => \$actOnPass,
    "pass|p=s" => \$password,
    "property|P=s" => \@properties,
    "sites|s" => \$sitesUser,
    "threads|t=s" => \$numberForks,
    "url|U=s" => \$url,
    "user|u=s" => \$username,
    "verbose|v+" => \$verbose,
    "view|V=s" => \$viewUser
) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url = Sling::URL::url_input_sanitize( $url );

#}}}

#{{{main execution path
if ( defined $additions ) {
    my $message = "Adding users from file \"$additions\":\n";
    Sling::Print::print_with_lock( "$message", $log );
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate authorization per fork:
            my $authn = new Sling::Authn( $url, $username, $password, $auth, $verbose, $log );
            my $user = new Sling::User( \$authn, $verbose, $log );
            $user->add_from_file( $additions, $i, $numberForks );
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
    my $user = new Sling::User( \$authn, $verbose, $log );

    if ( defined $existsUser ) {
        $user->exists( $existsUser );
    }
    elsif ( defined $meUser ) {
        $user->me();
    }
    elsif ( defined $sitesUser ) {
        $user->sites();
    }
    elsif ( defined $addUser ) {
        $user->add( $addUser, $actOnPass, \@properties );
    }
    elsif ( defined $changePassUser ) {
        $user->change_password( $changePassUser, $actOnPass, $newPass, $newPass );
    }
    elsif ( defined $deleteUser ) {
        $user->delete( $deleteUser );
    }
    elsif ( defined $viewUser ) {
        $user->view( $viewUser );
    }
    Sling::Print::print_result( $user );
}
#}}}

1;
