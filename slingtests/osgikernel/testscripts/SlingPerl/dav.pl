#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

dav perl script. Provides a means of uploading content into sling from the
command line over WebDAV. The script also acts as a reference implementation for the
DAV perl library.

=head1 OPTIONS

Usage: perl dav.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 -a                - add specified content.
 -l {localPath}    - path of local file or folder to upload.
 -p {password}     - Password of user performing actions.
 -r {remotePath}   - remote path on server to upload content to.
 -t {threads}      - Used with -F, defines number of parallel
                     processes to have running through file.
 -u {username}     - Name of user to perform any actions as.
 -F {File}         - File containing list of sites to add.
 --url or -U {URL} - URL for system being tested against.
 --help or -?      - view the script synopsis and options.
 --man             - view the full script documentation.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl dav.pl

=head1 Example Usage

=over

=item Authenticate and add local file test.txt to /test.txt:

 perl dav.pl -U http://localhost:8080 -a -l test.txt -u admin -p admin

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use HTTP::DAV;
use Pod::Usage;
use Sling::DAV;
#}}}

#{{{options parsing
my $add;
my $file;
my $help;
my $localPath;
my $man;
my $numberForks = 1;
my $password;
my $remotePath;
my $url = "http://localhost:8080";
my $username;

GetOptions ( "a" => \$add,           "l=s" => \$localPath,
             "p=s" => \$password,    "r=s" => \$remotePath,
             "t=i" => \$numberForks, "u=s" => \$username,
	     "F=s" => \$file,        "url|U=s" => \$url,
             "help|?" => \$help,     "man" => \$man) or pod2usage(2);

if ($help) { pod2usage( -exitstatus => 0, -verbose => 1 ); }
if ($man)  { pod2usage( -exitstatus => 0, -verbose => 2 ); }

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
        print "Uploading $type $localPath";
	if ( defined $remotePath ) {
	    print " to $remotePath";
	}
	print ": ";
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
