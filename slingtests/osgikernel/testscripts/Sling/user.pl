#!/usr/bin/perl

#{{{imports
use strict;
use lib qw ( .. );
use LWP::UserAgent ();
use Sling::User;
use Sling::Util;
use Getopt::Std;
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
    print "-n {newPassword}    - Used with -c, new password to set.\n";
    print "-p {password}       - Password of user performing actions.\n";
    print "-t {threads}        - Used with -F, defines number of parallel\n";
    print "                      processes to have running through file.\n";
    print "-v {view}           - view details for specified user in json format.\n";
    print "-u {username}       - Name of user to perform any actions as.\n";
    print "-E {actOnEmail}     - Email of user being actioned.\n";
    print "-F {file}           - file containing list of users to be added.\n";
    print "-L {log}            - Log script output to specified log file.\n";
    print "-P {actOnPass}      - Password of user being actioned.\n";
    print "-U {URL}            - URL for system being tested against.\n";
    Sling::Util::help_footer( $0 );
}
#}}}

#{{{options parsing
my $url = "http://localhost";
my $username;
my $password;
my $actOnUser;
my $actOnPass;
my $actOnEmail;
my $actOnFirst;
my $actOnLast;
my $file;
my $log;
my $newPass;
my $numberForks = 1;

my %options;

getopts('a:c:d:e:f:l:n:p:t:u:v:E:F:L:P:U:', \%options);

$url = $options{ 'U' } if ( defined $options{ 'U' } );
$username = $options{ 'u' } if ( defined $options{ 'u' } );
$password = $options{ 'p' } if ( defined $options{ 'p' } );
$file = $options{ 'F' } if ( defined $options{ 'F' } );
$log = $options{ 'L' } if ( defined $options{ 'L' } );
$numberForks = $options{ 't' } if ( defined $options{ 't' } );
$actOnUser = $options{ 'a' } if ( defined $options{ 'a' } );
$actOnUser = $options{ 'c' } if ( defined $options{ 'c' } );
$actOnUser = $options{ 'd' } if ( defined $options{ 'd' } );
$actOnUser = $options{ 'e' } if ( defined $options{ 'e' } );
$newPass = $options{ 'n' } if ( defined $options{ 'n' } );
$actOnUser = $options{ 'v' } if ( defined $options{ 'v' } );
$actOnPass = $options{ 'P' } if ( defined $options{ 'P' } );
$actOnEmail = $options{ 'E' } if ( defined $options{ 'E' } );
$actOnFirst = $options{ 'f' } if ( defined $options{ 'f' } );
$actOnLast = $options{ 'l' } if ( defined $options{ 'l' } );

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );

my $realm = $url;
if ( $realm !~ /:[0-9]+$/ ) {
    # No port specified yet, need to add one:
    $realm = ( $realm =~ /^http:/ ? "$realm:80" : "$realm:443" );
}
# Strip the protocol for the realm:
$realm =~ s#https?://(.*)#$1#;
#}}}

#{{{main execution path
if ( defined $options{ 'F' } ) {
    print "Adding users and password from file:\n";
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate user agent per fork:
            my $lwpUserAgent = Sling::Util::get_user_agent;
            my $user = new Sling::User( $url, $lwpUserAgent );
            if ( defined $options{ 'u' } || defined $options{ 'p' } ) {
                ${ $lwpUserAgent }->credentials(
	            $realm, 'Sling (Development)', $options{ 'u' } => $options{ 'p' },
	        )
            }
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
    my $lwpUserAgent = Sling::Util::get_user_agent;
    my $user = new Sling::User( $url, $lwpUserAgent );

    if ( defined $options{ 'u' } || defined $options{ 'p' } ) {
        ${ $lwpUserAgent }->credentials(
	    $realm, 'Sling (Development)', $options{ 'u' } => $options{ 'p' },
	)
    }

    if ( defined $options{ 'e' } ) {
        $user->exists( $actOnUser, $log );
        print $user->{ 'Message' } . "\n";
    }
    elsif ( defined $options{ 'a' } ) {
        $user->add( $actOnUser, $actOnPass, $actOnEmail, $actOnFirst, $actOnLast, $log );
        print $user->{ 'Message' } . "\n";
    }
    elsif ( defined $options{ 'c' } ) {
        $user->change_password( $actOnUser, $actOnPass, $newPass, $newPass, $log );
        print $user->{ 'Message' } . "\n";
    }
    elsif ( defined $options{ 'd' } ) {
        $user->delete( $actOnUser, $log );
        print $user->{ 'Message' } . "\n";
    }
    elsif ( defined $options{ 'v' } ) {
        $user->view( $actOnUser, $log );
        print $user->{ 'Message' } . "\n";
    }
}
#}}}

1;
