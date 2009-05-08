#!/usr/bin/perl

=head1 NAME

content perl script. Provides a means of uploading content into sakai
from the command line.

=head1 ABSTRACT

This script can be used to upload content into sakai from the command line. It
also acts as a reference implementation for the Content perl library.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use LWP::UserAgent ();
use Sling::Content;
use Sling::Util;
use Sling::User;
use Getopt::Std;
$Getopt::Std::STANDARD_HELP_VERSION = "true";
#}}}

#{{{sub HELP_MESSAGE
sub HELP_MESSAGE {
    my ( $out, $optPackage, $optVersion, $switches ) = @_;
    Sling::Util::help_header( $0, $switches );
    print "-l {localPath}  - Local path to content to upload.\n";
    print "-n {filename}   - Specify file name to use for content upload.\n";
    print "-p {password}   - Password of user performing content manipulations.\n";
    print "-r {remotePath} - specify remote path under the JCR root to upload content to.\n";
    print "-t {threads}    - Used with -F, defines number of parallel\n";
    print "                  processes to have running through file.\n";
    print "-u {username}   - Name of user to perform content manipulations as.\n";
    print "-F {File}       - File containing list of content to be uploaded.\n";
    print "-L {log}        - Log script output to specified log file.\n";
    print "-U {URL}        - URL for system being tested against.\n";
    Sling::Util::help_footer( $0 );
}
#}}}

#{{{options parsing
my $url = "http://localhost";
my $numberForks = 1;
my $file;
my $filename = "";
my $log;
my $username;
my $password;
my $localPath;
my $remotePath="/";

my %options;

getopts('l:n:p:r:s:t:u:F:L:P:U:', \%options);

$url = $options{ 'U' } if ( defined $options{ 'U' } );
$file = $options{ 'F' } if ( defined $options{ 'F' } );
$filename = $options{ 'n' } if ( defined $options{ 'n' } );
$numberForks = $options{ 't' } if ( defined $options{ 't' } );
$log = $options{ 'L' } if ( defined $options{ 'L' } );
$username = $options{ 'u' } if ( defined $options{ 'u' } );
$password = $options{ 'p' } if ( defined $options{ 'p' } );
$remotePath = $options{ 'r' } if ( defined $options{ 'r' } );
$localPath = $options{ 'l' } if ( defined $options{ 'l' } );


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

#{{{ main execution path
if ( defined $options{ 'F' } ) {
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
            my $lwpUserAgent = Sling::Util::get_user_agent;
            if ( defined $options{ 'u' } || defined $options{ 'p' } ) {
                ${ $lwpUserAgent }->credentials(
	            $realm, 'Sling (Development)', $options{ 'u' } => $options{ 'p' },
	        )
            }
            my $content = new Sling::Content( $url, $lwpUserAgent );
            $content->upload_from_file( $file, $i, $numberForks, $log );
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

    if ( defined $options{ 'u' } || defined $options{ 'p' } ) {
        ${ $lwpUserAgent }->credentials(
	    $realm, 'Sling (Development)', $options{ 'u' } => $options{ 'p' },
	)
    }

    my $content = new Sling::Content( $url, $lwpUserAgent );

    if ( defined $options{ 'l' } && defined $options{ 'r' } ) {
        $content->upload_file( $localPath, $remotePath, $filename, $log );
        if ( ! defined $log ) {
            print $content->{ 'Message' } . "\n";
        }
    }
}
#}}}

1;
