#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

image crop perl script. Provides a means of using the system image crop service
from the command line. Additionally serves as a reference example for using the
Sling::ImageCrop library.

=head1 OPTIONS

Usage: perl image_crop.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --auth (type)                     - Specify auth type. If ommitted, default is used.
 --file or -F (File)               - File containing list of image crop operations to perform.
 --final-height or -H (height)     - final height image should be scaled to.
 --final-width or -W (width)       - final width image should be scaled to.
 --height or -h                    - height of rectangle to cut out.
 --help or -?                      - view the script synopsis and options.
 --log or -L (log)                 - Log script output to specified log file.
 --man or -M                       - view the full script documentation.
 --pass or -p (password)           - Password of user performing image operations.
 --remote or -r (remoteNode)       - specify remote destination to save image crop to.
 --remote-source or -S (remoteSrc) - specify remote source image to act on.
 --threads or -t (threads)         - Used with -F, defines number of parallel
                                     processes to have running through file.
 --url or -U (URL)                 - URL for system being tested against.
 --user or -u (username)           - Name of user to perform any image operations as.
 --verbose or -v or -vv or -vvv    - Increase verbosity of output.
 --width or -w (width)             - width of rectangle to cut out.
 --x-coordinate or -x (coord)      - X coordinate of upper-left corner of rectangle to cut out.
 --y-coordinate or -y (coord)      - Y coordinate of upper-left corner of rectangle to cut out.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl image_crop.pl --man

=head1 Example Usage

=over

=item Authenticate and crop /images/test.jpg starting at (20,10) to width 100 and height 80, then scale to 50x40, storing the result in /thumbs.

 perl image_crop.pl -U http://localhost:8080 -S /images/test.jpg -r /thumbs -x 20, -y 10 -w 100 -h 80 -W 50 -H 40 -u admin -p admin

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Authn;
use Sling::ImageCrop;
use Sling::URL;
#}}}

#{{{options parsing
my $auth;
my $file;
my $final_height;
my $final_width;
my $help;
my $log;
my $man;
my $numberForks = 1;
my $password;
my $remote_dest;
my $remote_source;
my $url;
my $username;
my $verbose;
my $x_coordinate;
my $y_coordinate;

GetOptions (
    "auth=s" => \$auth,
    "file|F=s" => \$file,
    "final-height|H=i" => \$final_height,
    "final-width|W=i" => \$final_width,
    "help|?" => \$help,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "pass|p=s" => \$password,
    "remote|r=s" => \$remote_dest,
    "remote-source|S=s" => \$remote_source,
    "threads|t=i" => \$numberForks,
    "url|U=s" => \$url,
    "user|u=s" => \$username,
    "verbose|v+" => \$verbose,
    "x-coordinate|x=i" => \$x_coordinate,
    "y-coordinate|y=i" => \$y_coordinate
) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$remote_dest = Sling::URL::strip_leading_slash( $remote_dest );
$remote_source = Sling::URL::strip_leading_slash( $remote_source );

$url = Sling::URL::url_input_sanitize( $url );
#}}}

#{{{ main execution path
#}}}

1;
