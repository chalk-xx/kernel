#!/usr/bin/perl

=head1 NAME

content perl script. Provides a means of uploading content into sling
from the command line.

=head1 ABSTRACT

This script can be used to upload content into sling from the command line. It
also acts as a reference implementation for the Content perl library.

=head2 Example Usage

=over

=item Authenticate and add a node at /test:

 perl content.pl -U http://localhost:8080 -a -D /test -u admin -p admin

=item Authenticate and add a node at /test with property p1 set to v1:

 perl content.pl -U http://localhost:8080 -a -D /test -P p1=v1 -u admin -p admin

=item Authenticate and add a node at /test with property p1 set to v1, and p2 set to v2:

 perl content.pl -U http://localhost:8080 -a -D /test -P p1=v1 -P p2=v2 -u admin -p admin

=item View json for node at /test:

 perl content.pl -U http://localhost:8080 -v -D /test

=item Check whether node at /test exists:

 perl content.pl -U http://localhost:8080 -v -D /test

=item Authenticate and delete content at /test

 perl content.pl -U http://localhost:8080 -d -D /test -u admin -p admin

=back

=cut

#{{{imports
use strict;
use lib qw ( .. );
use LWP::UserAgent ();
use Sling::Content;
use Sling::UserAgent;
use Sling::Util;
use Getopt::Long qw(:config bundling);
#}}}

#{{{sub HELP_MESSAGE
sub HELP_MESSAGE {
    my ( $out, $optPackage, $optVersion, $switches ) = @_;
    Sling::Util::help_header( $0, $switches );
    print "-a              - Add content.\n";
    print "-c              - Copy content.\n";
    print "-d              - Delete content.\n";
    print "-l {localPath}  - Local path to content to upload.\n";
    print "-m              - Move content.\n";
    print "-n {filename}   - Specify file name to use for content upload.\n";
    print "-p {password}   - Password of user performing content manipulations.\n";
    print "-t {threads}    - Used with -F, defines number of parallel\n";
    print "                  processes to have running through file.\n";
    print "-u {username}   - Name of user to perform content manipulations as.\n";
    print "-D {remoteDest} - specify remote destination under JCR root to act on.\n";
    print "-F {File}       - File containing list of content to be uploaded.\n";
    print "-L {log}        - Log script output to specified log file.\n";
    print "-P {property}   - Specify property to set on node.\n";
    print "-S {remoteSrc}  - specify remote source under JCR root to act on.\n";
    print "-U {URL}        - URL for system being tested against.\n";
    print "--auth {type}   - Specify auth type. If ommitted, default is used.\n";
    Sling::Util::help_footer( $0 );
}
#}}}

#{{{options parsing
my $auth;
my $add;
my $copy;
my $delete;
my $exists;
my $move;
my $view;
my $url = "http://localhost";
my $numberForks = 1;
my $file;
my $filename = "";
my $log;
my $username;
my $password;
my $localPath;
my $remoteSrc;
my $remoteDest;
my @properties;

GetOptions ( "a" => \$add,    "c" => \$copy, "d" => \$delete,
             "e" => \$exists, "m" => \$move, "v" => \$view,
             "l=s" => \$localPath,   "n=s" => \$filename,
	     "p=s" => \$password,    "D=s" => \$remoteDest,
	     "t=s" => \$numberForks, "u=s" => \$username,
	     "F=s" => \$file,        "L=s" => \$log,
	     "P=s" => \@properties,  "S=s" => \$remoteSrc,
	     "U=s" => \$url,         "auth=s" => \$auth,
	     "help" => \&HELP_MESSAGE );

# Strip leading slashes from the remoteDest and remoteSrc
$remoteDest =~ s/^\///;
$remoteSrc =~ s/^\///;

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );
#}}}

#{{{ main execution path
if ( defined $file ) {
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
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
    my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
    my $content = new Sling::Content( $url, $lwpUserAgent );
    if ( defined $localPath && defined $remoteDest ) {
        $content->upload_file( $localPath, $remoteDest, $filename, $log );
        if ( ! defined $log ) {
            print $content->{ 'Message' } . "\n";
        }
    }
    elsif ( defined $add ) {
        $content->add( $remoteDest, \@properties, $log );
        if ( ! defined $log ) {
            print $content->{ 'Message' } . "\n";
        }
    }
    elsif ( defined $copy ) {
        print "Not yet implemented!\n";
        # $content->copy( $remoteSrc, $remoteDest, \@properties, $log );
        # if ( ! defined $log ) {
            # print $content->{ 'Message' } . "\n";
        # }
    }
    elsif ( defined $delete ) {
        $content->delete( $remoteDest, $log );
        if ( ! defined $log ) {
            print $content->{ 'Message' } . "\n";
        }
    }
    elsif ( defined $exists ) {
        $content->exists( $remoteDest, $log );
        if ( ! defined $log ) {
            print $content->{ 'Message' } . "\n";
        }
    }
    elsif ( defined $move ) {
        print "Not yet implemented!\n";
        # $content->move( $remoteSrc, $remoteDest, \@properties, $log );
        # if ( ! defined $log ) {
            # print $content->{ 'Message' } . "\n";
        # }
    }
    elsif ( defined $view ) {
        $content->view( $remoteDest, $log );
        if ( ! defined $log ) {
            print $content->{ 'Message' } . "\n";
        }
    }
}
#}}}

1;
