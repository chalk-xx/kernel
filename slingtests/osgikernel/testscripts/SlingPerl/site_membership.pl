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
my $act_on_site;
my $additions;
my $add_member;
my $auth;
my $delete_member;
my $exists_member;
my $help;
my $log;
my $man;
my $number_forks = 1;
my $password;
my $url;
my $username;
my $verbose;
my $view_members;

GetOptions(
    'add|a=s'       => \$add_member,
    'additions|A=s' => \$additions,
    'auth=s'        => \$auth,
    'delete|d=s'    => \$delete_member,
    'exists|e=s'    => \$exists_member,
    'help|?'        => \$help,
    'log|L=s'       => \$log,
    'man|M'         => \$man,
    'pass|p=s'      => \$password,
    'site|s=s'      => \$act_on_site,
    'threads|t=s'   => \$number_forks,
    'url|U=s'       => \$url,
    'user|u=s'      => \$username,
    'verbose|v+'    => \$verbose,
    'view|V'        => \$view_members
) or pod2usage( -exitstatus => 2, -verbose => 1 );

if ($help) { pod2usage( -exitstatus => 0, -verbose => 1 ); }
if ($man)  { pod2usage( -exitstatus => 0, -verbose => 2 ); }

my $max_allowed_forks = '32';
$number_forks = ( $number_forks || 1 );
$number_forks = ( $number_forks =~ /^[0-9]+$/xms ? $number_forks : 1 );
$number_forks = ( $number_forks < $max_allowed_forks ? $number_forks : 1 );

$url = Sling::URL::url_input_sanitize($url);

#}}}

#{{{main execution path
if ( defined $additions ) {
    my $message =
      "Adding members to sites as specified in file \"$additions\":";
    Sling::Print::print_with_lock( "$message", $log );
    my @childs = ();
    for my $i ( 0 .. $number_forks ) {
        my $pid = fork;
        if ($pid) { push @childs, $pid; }    # parent
        elsif ( $pid == 0 ) {                # child
                # Create a separate user agent per fork:
            my $authn =
              new Sling::Authn( $url, $username, $password, $auth, $verbose,
                $log );
            my $site = new Sling::Site( \$authn, $verbose, $log );
            $site->member_add_from_file( $additions, $i, $number_forks );
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

    if ( defined $exists_member ) {
        $site->member_exists( $act_on_site, $exists_member );
    }
    elsif ( defined $add_member ) {
        $site->member_add( $act_on_site, $add_member );
    }
    elsif ( defined $delete_member ) {
        $site->member_delete( $act_on_site, $delete_member );
    }
    elsif ( defined $view_members ) {
        $site->member_view($act_on_site);
    }
    Sling::Print::print_result($site);
}

#}}}

1;

__END__

#{{{Documentation
=head1 SYNOPSIS

site membership perl script. Provides a means of managing membership of sites
in sling from the command line.

=head1 OPTIONS

Usage: perl site_membership.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --additions or -A (file)       - file containing list of members to be added to sites.
 --add or -a (member)           - add specified member.
 --auth (type)                  - Specify auth type. If ommitted, default is used.
 --delete or -d (member)        - delete specified site member.
 --exists or -e (member)        - check whether specified member exists in site.
 --help or -?                   - view the script synopsis and options.
 --log or -L (log)              - Log script output to specified log file.
 --man or -M                    - view the full script documentation.
 --pass or -p (password)        - Password of user performing actions.
 --site or -s (actOnSite)       - site to perform membership actions on.
 --threads or -t (threads)      - Used with -A, defines number of parallel
                                  processes to have running through file.
 --url or -U (URL)              - URL for system being tested against.
 --user or -u (username)        - Name of user to perform any actions as.
 --verbose or -v or -vv or -vvv - Increase verbosity of output.
 --view or -V                   - view members of specified site.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl site_membership.pl --man

=head1 Example Usage

=over

=item Authenticate and add a member testuser to the site with id testsite:

 perl site_membership.pl -U http://localhost:8080 -s testsite -u admin -p admin -a testuser

=item Authenticate and view members of site with id testsite:

 perl site_membership.pl -U http://localhost:8080 -s testsite -u admin -p admin -V

=item Authenticate and check whether testuser is a member of site with id testsite:

 perl site_membership.pl -U http://localhost:8080 -s testsite -u admin -p admin -e testuser 

=item Authenticate and remove testuser from being a member of site with id testsite with very verbose output:

 perl site_membership.pl -U http://localhost:8080 -s testsite -u admin -p admin -d testuser -vvv

=back

=cut
#}}}
