#!/usr/bin/perl

#{{{imports
use strict;
use lib qw ( .. );
use LWP::UserAgent ();
use Sling::Group;
use Sling::Util;
use Getopt::Std;
#}}}

#{{{sub HELP_MESSAGE
sub HELP_MESSAGE {
    my ( $out, $optPackage, $optVersion, $switches ) = @_;
    Sling::Util::help_header( $0, $switches );
    print "-a {actOnGroup}     - add specified user name.\n";
    print "-d {actOnGroup}     - delete specified user name.\n";
    print "-e {actOnGroup}     - check whether specified user exists.\n";
    print "-p {password}       - Password of user performing actions.\n";
    print "-t {threads}        - Used with -F, defines number of parallel\n";
    print "                      processes to have running through file.\n";
    print "-v {actOnGroup}     - view details for specified group in json format.\n";
    print "-u {username}       - Name of user to perform any actions as.\n";
    print "-F {file}           - file containing list of users to be added.\n";
    print "-L {log}            - Log script output to specified log file.\n";
    print "-U {URL}            - URL for system being tested against.\n";
    Sling::Util::help_footer( $0 );
}
#}}}

#{{{options parsing
my $url = "http://localhost";
my $username;
my $password;
my $actOnGroup;
my $file;
my $log;
my $numberForks = 1;

my %options;

getopts('a:d:e:f:l:p:t:u:v:E:F:L:P:U:', \%options);

$url = $options{ 'U' } if ( defined $options{ 'U' } );
$username = $options{ 'u' } if ( defined $options{ 'u' } );
$password = $options{ 'p' } if ( defined $options{ 'p' } );
$file = $options{ 'F' } if ( defined $options{ 'F' } );
$log = $options{ 'L' } if ( defined $options{ 'L' } );
$numberForks = $options{ 't' } if ( defined $options{ 't' } );
$actOnGroup = $options{ 'a' } if ( defined $options{ 'a' } );
$actOnGroup = $options{ 'd' } if ( defined $options{ 'd' } );
$actOnGroup = $options{ 'e' } if ( defined $options{ 'e' } );
$actOnGroup = $options{ 'v' } if ( defined $options{ 'v' } );

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
    print "Adding groups from file:\n";
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate user agent per fork:
            my $lwpUserAgent = Sling::Util::get_user_agent;
            my $group = new Sling::Group( $url, $lwpUserAgent );
            if ( defined $options{ 'u' } || defined $options{ 'p' } ) {
                ${ $lwpUserAgent }->credentials(
	            $realm, 'Sling (Development)', $options{ 'u' } => $options{ 'p' },
	        )
            }
            $group->add_from_file( $file, $i, $numberForks, $log );
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
    my $group = new Sling::Group( $url, $lwpUserAgent );

    if ( defined $options{ 'u' } || defined $options{ 'p' } ) {
        ${ $lwpUserAgent }->credentials(
	    $realm, 'Sling (Development)', $options{ 'u' } => $options{ 'p' },
	)
    }

    if ( defined $options{ 'e' } ) {
        $group->exists( $actOnGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $options{ 'a' } ) {
        $group->add( $actOnGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $options{ 'd' } ) {
        $group->delete( $actOnGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $options{ 'v' } ) {
        $group->view( $actOnGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
}
#}}}

1;
