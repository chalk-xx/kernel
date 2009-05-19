#!/usr/bin/perl

#{{{imports
use strict;
use lib qw ( .. );
use Sling::User;
use Sling::UserAgent;
use Sling::Util;
use Getopt::Long qw(:config bundling);
#}}}

#{{{sub HELP_MESSAGE
sub HELP_MESSAGE {
    my ( $out, $optPackage, $optVersion, $switches ) = @_;
    Sling::Util::help_header( $0, $switches );
    print "-a {actOnUser}      - add specified user name.\n";
    print "-c {actOnUser}      - change password of specified user name.\n";
    print "-d {actOnUser}      - delete specified user name.\n";
    print "-e {actOnUser}      - check whether specified user exists.\n";
    print "-f {actOnFirstName} - first name of user being actioned.\n";
    print "-l {actOnLastName}  - last name of user being actioned.\n";
    print "--me                - me returns json representing authenticated user.\n";
    print "-n {newPassword}    - Used with -c, new password to set.\n";
    print "-p {password}       - Password of user performing actions.\n";
    print "-t {threads}        - Used with -F, defines number of parallel\n";
    print "                      processes to have running through file.\n";
    print "-v {actOnUser}      - view details for specified user in json format.\n";
    print "-u {username}       - Name of user to perform any actions as.\n";
    print "-E {actOnEmail}     - Email of user being actioned.\n";
    print "-F {file}           - file containing list of users to be added.\n";
    print "-L {log}            - Log script output to specified log file.\n";
    print "-P {actOnPass}      - Password of user being actioned.\n";
    print "-U {URL}            - URL for system being tested against.\n";
    print "--auth {type}       - Specify auth type. If ommitted, default is used.\n";
    Sling::Util::help_footer( $0 );
}
#}}}

#{{{options parsing
my $auth;
my $url = "http://localhost";
my $username;
my $password;
my $addUser;
my $changePassUser;
my $deleteUser;
my $existsUser;
my $viewUser;
my $actOnPass;
my $actOnEmail;
my $actOnFirst;
my $actOnLast;
my $meUser;
my $file;
my $log;
my $newPass;
my $numberForks = 1;

GetOptions ( "a=s" => \$addUser,     "c=s" => \$changePassUser,
             "d=s" => \$deleteUser,  "e=s" => \$existsUser,
             "f=s" => \$actOnFirst,  "l=s" => \$actOnLast,
             "n=s" => \$newPass,     "p=s" => \$password,
	     "t=s" => \$numberForks, "u=s" => \$username,
	     "v=s" => \$viewUser,    "E=s" => \$actOnEmail,
	     "F=s" => \$file,        "L=s" => \$log,
	     "P=s" => \$actOnPass,   "U=s" => \$url,
	     "me" => \$meUser,
	     "auth=s" => \$auth,     "help" => \&HELP_MESSAGE );

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );

#}}}

#{{{main execution path
if ( defined $file ) {
    print "Adding users and password from file:\n";
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate user agent per fork:
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
            my $user = new Sling::User( $url, $lwpUserAgent );
            $user->add_from_file( $file, $i, $numberForks, $log );
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
    my $user = new Sling::User( $url, $lwpUserAgent );

    if ( defined $existsUser ) {
        $user->exists( $existsUser, $log );
        print $user->{ 'Message' } . "\n";
    }
    elsif ( defined $meUser ) {
        $user->me( $log );
        print $user->{ 'Message' } . "\n";
    }
    elsif ( defined $addUser ) {
        $user->add( $addUser, $actOnPass, $actOnEmail, $actOnFirst, $actOnLast, $log );
        print $user->{ 'Message' } . "\n";
    }
    elsif ( defined $changePassUser ) {
        $user->change_password( $changePassUser, $actOnPass, $newPass, $newPass, $log );
        print $user->{ 'Message' } . "\n";
    }
    elsif ( defined $deleteUser ) {
        $user->delete( $deleteUser, $log );
        print $user->{ 'Message' } . "\n";
    }
    elsif ( defined $viewUser ) {
        $user->view( $viewUser, $log );
        print $user->{ 'Message' } . "\n";
    }
}
#}}}

1;
