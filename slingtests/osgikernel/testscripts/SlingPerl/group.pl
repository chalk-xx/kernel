#!/usr/bin/perl

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Group;
use Sling::UserAgent;
use Sling::Util;
use Getopt::Long qw(:config bundling);
#}}}

#{{{sub HELP_MESSAGE
sub HELP_MESSAGE {
    my ( $out, $optPackage, $optVersion, $switches ) = @_;
    Sling::Util::help_header( $0, $switches );
    print "-a {actOnGroup} - add specified group.\n";
    print "-d {actOnGroup} - delete specified group.\n";
    print "-e {actOnGroup} - check whether specified group exists.\n";
    print "-p {password}   - Password of user performing actions.\n";
    print "-t {threads}    - Used with -F, defines number of parallel\n";
    print "                  processes to have running through file.\n";
    print "-v {actOnGroup} - view details for specified group in json format.\n";
    print "-u {username}   - Name of user to perform any actions as.\n";
    print "-F {file}       - file containing list of group to be added.\n";
    print "-L {log}        - Log script output to specified log file.\n";
    print "-U {URL}        - URL for system being tested against.\n";
    print "--auth {type}   - Specify auth type. If ommitted, default is used.\n";
    Sling::Util::help_footer( $0 );
}
#}}}

#{{{options parsing
my $auth;
my $url = "http://localhost";
my $username;
my $password;
my $file;
my $log;
my $numberForks = 1;
my $addGroup;
my $deleteGroup;
my $existsGroup;
my $viewGroup;

GetOptions ( "a=s" => \$addGroup,    "d=s" => \$deleteGroup,
             "e=s" => \$existsGroup, "p=s" => \$password,
             "u=s" => \$username,    "t=s" => \$numberForks,
	     "v=s" => \$viewGroup,   "F=s" => \$file,
	     "L=s" => \$log,         "U=s" => \$url,
	     "auth=s" => \$auth,     "help" => \&HELP_MESSAGE );

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );
#}}}

#{{{main execution path
if ( defined $file ) {
    print "Adding groups from file:\n";
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate user agent per fork:
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
            my $group = new Sling::Group( $url, $lwpUserAgent );
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
    my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
    my $group = new Sling::Group( $url, $lwpUserAgent );

    if ( defined $existsGroup ) {
        $group->exists( $existsGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $addGroup ) {
        $group->add( $addGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $deleteGroup ) {
        $group->delete( $deleteGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $viewGroup ) {
        $group->view( $viewGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
}
#}}}

1;
