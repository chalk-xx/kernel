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
use Sling::Site;
use Sling::URL;

#}}}

#{{{options parsing
my $add_site;
my $additions;
my $alter_site;
my $auth;
my $delete_site;
my $exists_site;
my $help;
my $joinable;
my $log;
my $man;
my $number_forks = 1;
my $password;
my @properties;
my $template;
my $url;
my $username;
my $verbose;
my $view_site;

GetOptions(
    'add|a=s'      => \$add_site,
    'addition|A=s' => \$additions,
    'alter|c=s'    => \$alter_site,
    'auth=s'       => \$auth,
    'delete|d=s'   => \$delete_site,
    'exists|e=s'   => \$exists_site,
    'help|?'       => \$help,
    'joinable|j=s' => \$joinable,
    'log|L=s'      => \$log,
    'man|M'        => \$man,
    'pass|p=s'     => \$password,
    'property|P=s' => \@properties,
    'template|T=s' => \$template,
    'threads|t=i'  => \$number_forks,
    'url|U=s'      => \$url,
    'user|u=s'     => \$username,
    'verbose|v+'   => \$verbose,
    'view|V=s'     => \$view_site
) or pod2usage( -exitstatus => 2, -verbose => 1 );

if ($help) { pod2usage( -exitstatus => 0, -verbose => 1 ); }
if ($man)  { pod2usage( -exitstatus => 0, -verbose => 2 ); }

my $max_allowed_forks = '32';
$number_forks = ( $number_forks || 1 );
$number_forks = ( $number_forks =~ /^[0-9]+$/xms ? $number_forks : 1 );
$number_forks = ( $number_forks < $max_allowed_forks ? $number_forks : 1 );

$url = Sling::URL::url_input_sanitize($url);

my %joinable_options = ( 'yes', 1, 'no', 1, 'withauth', 1 );
if ( defined $joinable ) {
    if ( !$joinable_options{$joinable} ) {
        croak "Joinable option must be one of yes, no, or withauth!";
    }
}

$add_site    = Sling::URL::strip_leading_slash($add_site);
$alter_site  = Sling::URL::strip_leading_slash($alter_site);
$delete_site = Sling::URL::strip_leading_slash($delete_site);
$exists_site = Sling::URL::strip_leading_slash($exists_site);
$view_site   = Sling::URL::strip_leading_slash($view_site);
$template    = Sling::URL::add_leading_slash($template);

#}}}

#{{{ main execution path
if ( defined $additions ) {
    my $message = "Running through all site actions in file \"$additions\":\n";
    Sling::Print::print_with_lock( "$message", $log );
    my @childs = ();
    for my $i ( 0 .. $number_forks ) {
        my $pid = fork;
        if ($pid) { push @childs, $pid; }    # parent
        elsif ( $pid == 0 ) {                # child
            my $authn =
              new Sling::Authn( $url, $username, $password, $auth, $verbose,
                $log );
            my $site = new Sling::Site( \$authn, $verbose, $log );
            $site->update_from_file( $additions, $i, $number_forks );
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
    my $site = new Sling::Site( \$authn, $verbose, $log );
    if ( defined $add_site ) {
        $site->update( $add_site, $template, $joinable, \@properties );
    }
    elsif ( defined $alter_site ) {
        $site->update( $alter_site, $template, $joinable, \@properties );
    }
    elsif ( defined $delete_site ) {
        $site->delete($delete_site);
    }
    elsif ( defined $exists_site ) {
        $site->exists($exists_site);
    }
    elsif ( defined $view_site ) {
        $site->view($view_site);
    }
    Sling::Print::print_result($site);
}

#}}}

1;

__END__

#{{{Documentation
=head1 SYNOPSIS

site perl script. Provides a means of manipulating sites in a sytem from the
command line. Additionally serves as a reference example for using the
Sling::Site library.

=head1 OPTIONS

Usage: perl site.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --additions or -A (file)       - File containing list of sites to add.
 --add or -a (actOnSite)        - add site.
 --alter or -c (actOnSite)      - alter (update) site.
 --auth (type)                  - Specify auth type. If ommitted, default is used.
 --delete or -d (actOnSite)     - delete site.
 --exists or -e (actOnSite)     - check whether site exists.
 --help or -?                   - view the script synopsis and options.
 --joinable or -j (joinable)    - Joinable status of site (yes|no|withauth).
 --log or -L (log)              - Log script output to specified log file.
 --man or -M                    - view the full script documentation.
 --pass or -p (password)        - Password of user performing actions.
 --property or -P (property)    - Specify property to set on site.
 --template or -T (template)    - Template location to use for site.
 --threads or -t (threads)      - Used with -F, defines number of parallel
                                  processes to have running through file.
 --url or -U (URL)              - URL for system being tested against.
 --user or -u (username)        - Name of user to perform any actions as.
 --verbose or -v or -vv or -vvv - Increase verbosity of output.
 --view or -V (actOnSite)       - view site.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl site.pl --man

=head1 Example Usage

=over

=item Authenticate and add a site with id testsite:

 perl site.pl -U http://localhost:8080 --add testsite --user admin --pass admin

=item Authenticate and check whether site with id testsite exists:

 perl site.pl -U http://localhost:8080 --exists testsite --user admin --pass admin

=item Authenticate and alter site testsite to set its joinable property to yes:

 perl site.pl -U http://localhost:8080 --alter testsite --joinable yes --user admin --pass admin

=item Authenticate and view site testsite details:

 perl site.pl -U http://localhost:8080 --view testsite --user admin --pass admin

=item Authenticate and delete site testsite with verbose output enabled:

 perl site.pl -U http://localhost:8080 --delete testsite --user admin --pass admin --verbose

=item Authenticate and add site testsite with property p1=v1 and property p2=v2:

 perl site.pl -U http://localhost:8080 --add testsite2 --property p1=v1 --property p2=v2 --user admin --pass admin

=item Authenticate and add site testsite with template /sitetemplate.html:

 perl site.pl -U http://localhost:8080 --add testsite --template /sitetemplate.html --user admin --pass admin

=back

=cut
#}}}
