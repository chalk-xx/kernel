#!/usr/bin/perl

#{{{imports
use warnings;
use strict;
use Carp;
use lib qw ( .. );
use version; our $VERSION = qv('0.0.1');
use Getopt::Long qw(:config bundling);
use HTTP::DAV;
use Pod::Usage;
use Sling::DAV;

#}}}

#{{{options parsing
my $add;
my $file;
my $help;
my $local_path;
my $man;
my $number_forks = 1;
my $password;
my $remote_path;
my $url = 'http://localhost:8080';
my $username;

GetOptions(
    'a'       => \$add,
    'l=s'     => \$local_path,
    'p=s'     => \$password,
    'r=s'     => \$remote_path,
    't=i'     => \$number_forks,
    'u=s'     => \$username,
    'F=s'     => \$file,
    'url|U=s' => \$url,
    'help|?'  => \$help,
    'man'     => \$man
) or pod2usage(2);

if ($help) { pod2usage( -exitstatus => 0, -verbose => 1 ); }
if ($man)  { pod2usage( -exitstatus => 0, -verbose => 2 ); }

my $max_allowed_forks = '32';
$number_forks = ( $number_forks || 1 );
$number_forks = ( $number_forks =~ /^[0-9]+$/xms ? $number_forks : 1 );
$number_forks = ( $number_forks < $max_allowed_forks ? $number_forks : 1 );

$url =~ s/(.*)\/$/$1/x;
$url = ( $url !~ /^http/x ? "http://$url" : "$url" );
$url = ( $url =~ /^https/x ? "$url:443" : "$url" );

#}}}

#{{{main execution path
if ( defined $file ) {
    print "Adding local content paths specified in file:\n";
    my @childs = ();
    for my $i ( 0 .. $number_forks ) {
        my $pid = fork;
        if ($pid) { push @childs, $pid; }    # parent
        elsif ( $pid == 0 ) {                # child
                # Create a separate dav connection per fork:
            my $dav_conn = new HTTP::DAV;
            $dav_conn->credentials(
                -user  => $username,
                -pass  => $password,
                -url   => $url,
                -realm => 'Jackrabbit Webdav Server'
            );
            my $dav = new Sling::DAV( \$dav_conn, $url );
            $dav->upload_from_file( $file, $i, $number_forks );
            exit 0;
        }
        else {
            croak "Could not fork $i!";
        }
    }
    foreach (@childs) { waitpid $_, 0; }
}
else {
    my $dav_conn = new HTTP::DAV;
    $dav_conn->credentials(
        -user  => $username,
        -pass  => $password,
        -url   => $url,
        -realm => 'Jackrabbit Webdav Server'
    );

    my $dav = new Sling::DAV( \$dav_conn, $url );

    if ( defined $add ) {
        my $type;
        if ( -f $local_path ) {
            $type = "file";
        }
        elsif ( -d $local_path ) {
            $type = "directory";
        }
        else {
            croak "ERROR: Unsupported Local path type for \"$local_path\"";
        }
        print "Uploading $type $local_path";
        if ( defined $remote_path ) {
            print " to $remote_path";
        }
        print ": ";
        if ( $dav->upload( $local_path, $remote_path ) ) {
            print "Done!\n";
        }
        else {
            print "Failed!\n";
            print $dav_conn->message . "\n";
        }
    }
}

#}}}

1;

__END__

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
