#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

content perl script. Provides a means of uploading content into sling from the
command line. The script also acts as a reference implementation for the
Content perl library.

=head1 OPTIONS

Usage: perl content.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --additions or -A (file)          - File containing list of content to be uploaded.
 --add or -a                       - Add content.
 --auth (type)                     - Specify auth type. If ommitted, default is used.
 --copy or -c                      - Copy content.
 --delete or -d                    - Delete content.
 --filename or -n (filename)       - Specify file name to use for content upload.
 --help or -?                      - view the script synopsis and options.
 --local or -l (localPath)         - Local path to content to upload.
 --log or -L (log)                 - Log script output to specified log file.
 --man or -M                       - view the full script documentation.
 --move or -m                      - Move content.
 --pass or -p (password)           - Password of user performing content manipulations.
 --property or -P (property)       - Specify property to set on node.
 --remote or -r (remoteNode)       - specify remote destination under JCR root to act on.
 --remote-source or -S (remoteSrc) - specify remote source node under JCR root to act on.
 --threads or -t (threads)         - Used with -A, defines number of parallel
                                     processes to have running through file.
 --url or -U (URL)                 - URL for system being tested against.
 --user or -u (username)           - Name of user to perform content manipulations as.
 --verbose or -v                   - Increase verbosity of output.
 --view or -V (actOnGroup)         - view details for specified group in json format.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl content.pl --man

=head1 Example Usage

=over

=item Authenticate and add a node at /test:

 perl content.pl -U http://localhost:8080 -a -r /test -u admin -p admin

=item Authenticate and add a node at /test with property p1 set to v1:

 perl content.pl -U http://localhost:8080 -a -r /test -P p1=v1 -u admin -p admin

=item Authenticate and add a node at /test with property p1 set to v1, and p2 set to v2:

 perl content.pl -U http://localhost:8080 -a -r /test -P p1=v1 -P p2=v2 -u admin -p admin

=item View json for node at /test:

 perl content.pl -U http://localhost:8080 -V -r /test

=item Check whether node at /test exists:

 perl content.pl -U http://localhost:8080 -V -r /test

=item Authenticate and delete content at /test

 perl content.pl -U http://localhost:8080 -d -r /test -u admin -p admin

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
my $additions;
my $filename = "";
my $help;
my $localPath;
my $log;
my $man;
my $move;
my $numberForks = 1;
my $password;
my @properties;
my $remoteNode;
my $remoteSrc;
my $url = "http://localhost";
my $username;
my $verbose;
my $view;

GetOptions (
    "add|a" => \$add,
    "additions|A=s" => \$additions,
    "auth=s" => \$auth,
    "copy|c" => \$copy,
    "delete|d" => \$delete,
    "exists|e" => \$exists,
    "filename|n=s" => \$filename,
    "help|?" => \$help,
    "local|l=s" => \$localPath,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "move|m" => \$move,
    "pass|p=s" => \$password,
    "property|P=s" => \@properties,
    "remote|r=s" => \$remoteNode,
    "remote-source|S=s" => \$remoteSrc,
    "threads|t=s" => \$numberForks,
    "url|U=s" => \$url,
    "user|u=s" => \$username,
    "verbose|v+" => \$verbose,
    "view|V" => \$view
) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$remoteNode = Sling::URL::strip_leading_slash( $remoteNode );
$remoteSrc = Sling::URL::strip_leading_slash( $remoteSrc );

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );
#}}}

#{{{ main execution path
if ( defined $additions ) {
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
            my $content = new Sling::Content( $url, $lwpUserAgent, $verbose );
            $content->upload_from_file( $additions, $i, $numberForks, $log );
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
    my $content = new Sling::Content( $url, $lwpUserAgent, $verbose );
    if ( defined $localPath && defined $remoteNode ) {
        $content->upload_file( $localPath, $remoteNode, $filename, $log );
    }
    elsif ( defined $add ) {
        $content->add( $remoteNode, \@properties, $log );
    }
    elsif ( defined $copy ) {
        die "Not yet implemented!\n";
        # $content->copy( $remoteSrc, $remoteNode, \@properties, $log );
    }
    elsif ( defined $delete ) {
        $content->delete( $remoteNode, $log );
    }
    elsif ( defined $exists ) {
        $content->exists( $remoteNode, $log );
    }
    elsif ( defined $move ) {
        die "Not yet implemented!\n";
        # $content->move( $remoteSrc, $remoteNode, \@properties, $log );
    }
    elsif ( defined $view ) {
        $content->view( $remoteNode, $log );
    }
    Sling::Print::print_result( $content, $log );
}
#}}}

1;
