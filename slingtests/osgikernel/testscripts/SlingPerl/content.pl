#!/usr/bin/perl

#{{{imports
use warnings;
use strict;
use Carp;
use lib qw ( .. );
use version; our $VERSION = qv('0.0.1');
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Authn;
use Sling::Content;
use Sling::URL;

#}}}

#{{{options parsing
my $add;
my $auth;
my $copy;
my $delete;
my $exists;
my $additions;
my $filename = '';
my $help;
my $local_path;
my $log;
my $man;
my $move;
my $number_forks = 1;
my $password;
my @properties;
my $remote_node;
my $remote_src;
my $replace;
my $url;
my $username;
my $verbose;
my $view;

GetOptions(
    'add|a'             => \$add,
    'additions|A=s'     => \$additions,
    'auth=s'            => \$auth,
    'copy|c'            => \$copy,
    'delete|d'          => \$delete,
    'exists|e'          => \$exists,
    'filename|n=s'      => \$filename,
    'help|?'            => \$help,
    'local|l=s'         => \$local_path,
    'log|L=s'           => \$log,
    'man|M'             => \$man,
    'move|m'            => \$move,
    'pass|p=s'          => \$password,
    'property|P=s'      => \@properties,
    'remote|r=s'        => \$remote_node,
    'remote-source|S=s' => \$remote_src,
    'replace|R'         => \$replace,
    'threads|t=s'       => \$number_forks,
    'url|U=s'           => \$url,
    'user|u=s'          => \$username,
    'verbose|v+'        => \$verbose,
    'view|V'            => \$view
) or pod2usage(2);

if ($help) { pod2usage( -exitstatus => 0, -verbose => 1 ); }
if ($man)  { pod2usage( -exitstatus => 0, -verbose => 2 ); }

$remote_node = Sling::URL::strip_leading_slash($remote_node);
$remote_src  = Sling::URL::strip_leading_slash($remote_src);

my $max_allowed_forks = '32';
$number_forks = ( $number_forks || 1 );
$number_forks = ( $number_forks =~ /^[0-9]+$/xms ? $number_forks : 1 );
$number_forks = ( $number_forks < $max_allowed_forks ? $number_forks : 1 );

$url = Sling::URL::url_input_sanitize($url);

#}}}

#{{{ main execution path
if ( defined $additions ) {
    my @childs = ();
    for my $i ( 0 .. $number_forks ) {
        my $pid = fork;
        if ($pid) { push @childs, $pid; }    # parent
        elsif ( $pid == 0 ) {                # child
            my $authn =
              new Sling::Authn( $url, $username, $password, $auth, $verbose,
                $log );
            my $content = new Sling::Content( \$authn, $verbose, $log );
            $content->upload_from_file( $additions, $i, $number_forks );
            exit 0;
        }
        else {
            croak "Could not fork $i!";
        }
    }
    foreach (@childs) { waitpid $_, 0; }
}
else {
    my $authn =
      new Sling::Authn( $url, $username, $password, $auth, $verbose, $log );
    my $content = new Sling::Content( \$authn, $verbose, $log );
    if ( defined $local_path && defined $remote_node ) {
        $content->upload_file( $local_path, $remote_node, $filename );
    }
    elsif ( defined $add ) {
        $content->add( $remote_node, \@properties );
    }
    elsif ( defined $copy ) {
        $content->copy( $remote_src, $remote_node, $replace );
    }
    elsif ( defined $delete ) {
        $content->delete($remote_node);
    }
    elsif ( defined $exists ) {
        $content->exists($remote_node);
    }
    elsif ( defined $move ) {
        $content->move( $remote_src, $remote_node, $replace );
    }
    elsif ( defined $view ) {
        $content->view($remote_node);
    }
    Sling::Print::print_result($content);
}

#}}}

1;

__END__

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
 --replace or -R                   - when copying or moving, overwrite remote destination if it exists.
 --threads or -t (threads)         - Used with -A, defines number of parallel
                                     processes to have running through file.
 --url or -U (URL)                 - URL for system being tested against.
 --user or -u (username)           - Name of user to perform content manipulations as.
 --verbose or -v or -vv or -vvv    - Increase verbosity of output.
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

=item Authenticate and copy content at /test to /test2

 perl content.pl -U http://localhost:8080 -c -S /test -r /test2 -u admin -p admin

=item Authenticate and move content at /test to /test2, replacing test2 if it already exists

 perl content.pl -U http://localhost:8080 -m -S /test -r /test2 -R -u admin -p admin

=item Authenticate and delete content at /test

 perl content.pl -U http://localhost:8080 -d -r /test -u admin -p admin

=back

=cut
#}}}
