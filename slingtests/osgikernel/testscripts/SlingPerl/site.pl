#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

site perl script. Provides a means of manipulating sites in a sytem from the
command line. Additionally serves as a reference example for using the
Sling::Site library.

=head1 OPTIONS

Usage: perl site.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 -a (actOnSite) - add site.
 -d (actOnSite) - delete site.
 -e (actOnSite) - check whether site exists.
 -g (group)     - group(s) to add as site member.
 -j (joinable)  - Joinable status of site (yes|no|withauth).
 -p (password)  - Password of user performing actions.
 -t (threads)   - Used with -F, defines number of parallel
                  processes to have running through file.
 -u (username)  - Name of user to perform any actions as.
 -v (actOnSite) - view site.
 -A (actOnSite) - alter (update) site.
 -F (File)      - File containing list of sites to add.
 -L (log)       - Log script output to specified log file.
 -P (property)  - Specify property to set on site.
 -T (template)  - Template location to use for site.
 -U (URL)       - URL for system being tested against.
 --auth (type)  - Specify auth type. If ommitted, default is used.
 --help or -?   - view the script synopsis and options.
 --man          - view the full script documentation.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl site.pl --man

=head1 Example Usage

=over

=item Authenticate and add a site with id testsite:

 perl site.pl -U http://localhost:8080 -a -s testsite -u admin -p admin

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Site;
use Sling::UserAgent;
use Sling::URL;
#}}}

#{{{options parsing
my $add;
my $alter;
my $auth;
my $delete;
my $exists;
my $file;
my @groups;
my $help;
my $joinable;
my $log;
my $man;
my $numberForks = 1;
my $password;
my @properties;
my $template;
my $url = "http://localhost";
my $username;
my $view;

GetOptions ( "a=s" => \$add,         "A=s" => \$alter, 
             "d=s" => \$delete,      "T=s" => \$template,
	     "e=s" => \$exists,      "v=s" => \$view,
	     "j=s" => \$joinable,    "g=s" => \@groups,
             "F=s" => \$file,        "P=s" => \@properties,
             "t=i" => \$numberForks, "L=s" => \$log,
             "u=s" => \$username,    "p=s" => \$password,
	     "U=s" => \$url,         "auth=s" => \$auth,
             "help|?" => \$help, "man" => \$man) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );

my %joinableOptions = ( 'yes', 1, 'no', 1, 'withauth', 1 );
if ( defined $joinable ) {
    if ( ! $joinableOptions{ $joinable } ) {
        die "Joinable option must be one of yes, no, or withauth!";
    }
}

$add = Sling::URL::strip_leading_slash( $add );
$alter = Sling::URL::strip_leading_slash( $alter );
$delete = Sling::URL::strip_leading_slash( $delete );
$exists = Sling::URL::strip_leading_slash( $exists );
$view = Sling::URL::strip_leading_slash( $view );
$template = Sling::URL::add_leading_slash( $template );
#}}}

#{{{ main execution path
if ( defined $file ) {
    print "Running through all site actions in file:\n";
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
            my $site = new Sling::Site( $url, $lwpUserAgent );
	    my $path;
            $site->update_from_file( $file, $i, $numberForks, $log );
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
    my $site = new Sling::Site( $url, $lwpUserAgent );
    if ( defined $add ) {
        $site->update( $add, $template, $joinable, \@groups, \@properties, $log );
        if ( ! defined $log ) {
            print $site->{ 'Message' } . "\n";
        }
    }
    elsif ( defined $alter ) {
        $site->update( $alter, $template, $joinable, \@groups, \@properties, $log );
	if ( ! defined $log ) {
            print $site->{ 'Message' } . "\n";
        }
    }
    elsif ( defined $delete ) {
        $site->delete( $delete, $log );
        if ( ! defined $log ) {
            print $site->{ 'Message' } . "\n";
        }
    }
    elsif ( defined $exists ) {
        $site->exists( $exists, $log );
        if ( ! defined $log ) {
            print $site->{ 'Message' } . "\n";
        }
    }
    elsif ( defined $view ) {
        $site->view( $view, $log );
        if ( ! defined $log ) {
            print $site->{ 'Message' } . "\n";
        }
    }
}
#}}}

1;
