#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

content perl script. Provides a means of uploading content into sling from the
command line. The script also acts as a reference implementation for the
Content perl library.

=head1 OPTIONS

Usage: perl content.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 -a              - Add content.
 -c              - Copy content.
 -d              - Delete content.
 -l (localPath)  - Local path to content to upload.
 -m              - Move content.
 -n (filename)   - Specify file name to use for content upload.
 -p (password)   - Password of user performing content manipulations.
 -t (threads)    - Used with -F, defines number of parallel
                   processes to have running through file.
 -u (username)   - Name of user to perform content manipulations as.
 -D (remoteDest) - specify remote destination under JCR root to act on.
 -F (File)       - File containing list of content to be uploaded.
 -L (log)        - Log script output to specified log file.
 -P (property)   - Specify property to set on node.
 -S (remoteSrc)  - specify remote source under JCR root to act on.
 -U (URL)        - URL for system being tested against.
 --auth (type)   - Specify auth type. If ommitted, default is used.
 --help or -?    - view the script synopsis and options.
 --man           - view the full script documentation.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl content.pl --man

=head1 Example Usage

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
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use LWP::UserAgent ();
use Sling::Content;
use Sling::UserAgent;
use Getopt::Long qw(:config bundling);
use Pod::Usage;
#}}}

#{{{options parsing
my $add;
my $auth;
my $copy;
my $delete;
my $exists;
my $file;
my $filename = "";
my $help;
my $localPath;
my $log;
my $man;
my $move;
my $numberForks = 1;
my $password;
my @properties;
my $remoteDest;
my $remoteSrc;
my $url = "http://localhost";
my $username;
my $view;

GetOptions ( "a" => \$add,    "c" => \$copy, "d" => \$delete,
             "e" => \$exists, "m" => \$move, "v" => \$view,
             "l=s" => \$localPath,   "n=s" => \$filename,
	     "p=s" => \$password,    "D=s" => \$remoteDest,
	     "t=s" => \$numberForks, "u=s" => \$username,
	     "F=s" => \$file,        "L=s" => \$log,
	     "P=s" => \@properties,  "S=s" => \$remoteSrc,
	     "U=s" => \$url,         "auth=s" => \$auth,
             "help|?" => \$help, "man" => \$man) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$remoteDest = Sling::URL::strip_leading_slash( $remoteDest );
$remoteSrc = Sling::URL::strip_leading_slash( $remoteSrc );

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
