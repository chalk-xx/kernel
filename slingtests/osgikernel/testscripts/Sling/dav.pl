#!/usr/bin/perl

#{{{imports
use strict;
use lib qw ( .. );
use HTTP::DAV;
use Sling::DAV;
use Sling::Util;
use Getopt::Long qw(:config bundling);
#}}}

#{{{sub HELP_MESSAGE
sub HELP_MESSAGE {
    my ( $out, $optPackage, $optVersion, $switches ) = @_;
    Sling::Util::help_header( $0, $switches );
    print "-a                   - add specified content.\n";
    print "-l {localPath}       - path of local file or folder to upload.\n";
    print "-r {remotePath}      - remote path on server to upload content to.\n";
    print "-p {password}        - Password of user performing actions.\n";
    print "-t {threads}         - Used with -F, defines number of parallel\n";
    print "                       processes to have running through file.\n";
    print "-u {username}        - Name of user to perform any actions as.\n";
    print "-F {File}            - File containing list of sites to add.\n";
    print "-U {URL}             - URL for system being tested against.\n";
    Sling::Util::help_footer( $0 );
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
my $add;

GetOptions ( "a" => \$add,           "l=s" => \$localPath,
             "p=s" => \$password,    "r=s" => \$remotePath,
             "t=i" => \$numberForks, "u=s" => \$username,
	     "F=s" => \$file,        "U=s" => \$url,
	     "help" => \&HELP_MESSAGE );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );
$url = ( $url =~ /^https/ ? "$url:443" : "$url" );
#}}}

#{{{main execution path
if ( defined $file ) {
    print "Adding local content paths specified in file:\n";
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate dav connection per fork:
            my $davConn = new HTTP::DAV;
            $davConn->credentials( -user=>"$username", -pass =>"$password",
	        -url =>"$url", -realm => "Jackrabbit Webdav Server" );
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
    $davConn->credentials( -user=>"$username", -pass =>"$password",
        -url =>"$url", -realm => "Jackrabbit Webdav Server" );

    my $dav = new Sling::DAV( \$davConn, $url );

    if ( defined $add ) {
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
