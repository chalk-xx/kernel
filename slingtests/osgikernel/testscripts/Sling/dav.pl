#!/usr/bin/perl

#{{{imports
use strict;
use lib qw ( .. );
use HTTP::DAV;
use Sling::DAV;
use Getopt::Std;
#}}}

#{{{sub HELP_MESSAGE
sub HELP_MESSAGE {
    my ( $out, $optPackage, $optVersion, $switches ) = @_;
    print "Usage: $0 [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]\n\n";
    print "The following single-character options are accepted: $switches\n";
    print "Those with a ':' after require an argument to be supplied:\n\n";
    print "-a                   - add specified content.\n";
    print "-l {localPath}       - path of local file or folder to upload.\n";
    print "-r {remotePath}      - remote path on server to upload content to.\n";
    print "-p {password}        - Password of user performing actions.\n";
    print "-t {threads}         - Used with -F, defines number of parallel\n";
    print "                       processes to have running through file.\n";
    print "-u {username}        - Name of user to perform any actions as.\n";
    print "-F {File}            - File containing list of sites to add.\n";
    print "-U {URL}             - URL for system being tested against.\n";
    print "\nOptions may be merged together. -- stops processing of options.\n";
    print "Space is not required between options and their arguments.\n";
    print "For more details run: perldoc -F $0\n";
}
#}}}

#{{{options parsing
my $url = "http://localhost";
my $localPath;
my $remotePath;
my $username;
my $password;
my $file;
my $numberForks = 1;

my %options;

getopts('al:p:r:t:u:F:U:', \%options);

$url = $options{ 'U' } if ( defined $options{ 'U' } );
$localPath = $options{ 'l' } if ( defined $options{ 'l' } );
$remotePath = $options{ 'r' } if ( defined $options{ 'r' } );
$username = $options{ 'u' } if ( defined $options{ 'u' } );
$password = $options{ 'p' } if ( defined $options{ 'p' } );
$numberForks = $options{ 't' } if ( defined $options{ 't' } );
$file = $options{ 'F' } if ( defined $options{ 'F' } );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );
$url = ( $url =~ /^https/ ? "$url:443" : "$url" );

print "Using URL: $url\n";
#}}}

#{{{main execution path
if ( defined $options{ 'F' } ) {
    print "Adding local content paths specified in file:\n";
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate dav connection per fork:
            my $davConn = new HTTP::DAV;
            $davConn->credentials( -user=>"$username", -pass =>"$password", -url =>"$url" );
            my $dav = new Sling::DAV( \$davConn, $url );
            $dav->upload_from_file( $file, $i, $numberForks );
	    exit( 0 );
	}
	else {
            die "Could not fork $i!";
	}
    }
    foreach ( @childs ) { waitpid( $_, 0 ); }
}
else {
    my $davConn = new HTTP::DAV;
    $davConn->credentials( -user=>"$username", -pass =>"$password", -url =>"$url", -realm => "Jackrabbit Webdav Server" );

    my $dav = new Sling::DAV( \$davConn, $url );

    if ( defined $options{ 'a' } ) {
        my $type;
        if ( -f $localPath ) {
            $type = "file";
        }
        elsif ( -d $localPath ) {
            $type = "directory";
        }
        else {
            die "ERROR: Unsupported Local path type for \"$localPath\"";
        }
        print "Uploading $type $localPath to $remotePath: ";
        if( $dav->upload( $localPath, $remotePath ) ) {
            print "Done!\n";
        }
        else {
            print "Failed!\n";
	    print $davConn->message . "\n";
        }
    }
}
#}}}

1;
