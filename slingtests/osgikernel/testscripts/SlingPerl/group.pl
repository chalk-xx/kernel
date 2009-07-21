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
use Sling::Group;
use Sling::URL;

#}}}

#{{{options parsing
my $add_group;
my $additions;
my $auth;
my $delete_group;
my $exists_group;
my $help;
my $log;
my $man;
my $number_forks = 1;
my $password;
my @properties, my $url;
my $username;
my $verbose;
my $view_group;

GetOptions(
    'add|a=s'       => \$add_group,
    'additions|A=s' => \$additions,
    'auth=s'        => \$auth,
    'delete|d=s'    => \$delete_group,
    'exists|e=s'    => \$exists_group,
    'help|?'        => \$help,
    'log|L=s'       => \$log,
    'man|M'         => \$man,
    'pass|p=s'      => \$password,
    'property|P=s'  => \@properties,
    'threads|t=s'   => \$number_forks,
    'url|U=s'       => \$url,
    'user|u=s'      => \$username,
    'verbose|v+'    => \$verbose,
    'view|V=s'      => \$view_group
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
    my $message = "Adding groups from file \"$additions\":\n";
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
            my $group = new Sling::Group( \$authn, $verbose, $log );
            $group->add_from_file( $additions, $i, $number_forks );
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
    my $group = new Sling::Group( \$authn, $verbose, $log );
    if ( defined $exists_group ) {
        $group->exists($exists_group);
    }
    elsif ( defined $add_group ) {
        $group->add( $add_group, \@properties );
    }
    elsif ( defined $delete_group ) {
        $group->delete($delete_group);
    }
    elsif ( defined $view_group ) {
        $group->view($view_group);
    }
    Sling::Print::print_result($group);
}

#}}}

1;

__END__

#{{{Documentation
=head1 SYNOPSIS

group perl script. Provides a means of managing groups in sling from the
command line. The script also acts as a reference implementation for the Group
perl library.

=head1 OPTIONS

Usage: perl group.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --additions or -A (file)          - file containing list of groups to be added.
 --add or -a (actOnGroup)          - add specified group.
 --auth (type)                     - Specify auth type. If ommitted, default is used.
 --delete or -d (actOnGroup)       - delete specified group.
 --exists or -e (actOnGroup)       - check whether specified group exists.
 --help or -?                      - view the script synopsis and options.
 --log or -L (log)                 - Log script output to specified log file.
 --man or -M                       - view the full script documentation.
 --pass or -p (password)           - Password of user performing actions.
 --property or -P (property=value) - Specify property to set on group.
 --threads or -t (threads)         - Used with -A, defines number of parallel
                                     processes to have running through file.
 --url or -U (URL)                 - URL for system being tested against.
 --user or -u (username)           - Name of user to perform any actions as.
 --verbose or -v or -vv or -vvv    - Increase verbosity of output.
 --view or -V (actOnGroup)         - view details for specified group in json format.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl group.pl --man

=head1 Example Usage

=over

=item Authenticate and add a group with id g-test:

 perl group.pl -U http://localhost:8080 -u admin -p admin -a g-test

=item Authenticate and check whether group with id g-test exists:

 perl group.pl -U http://localhost:8080 -u admin -p admin -a g-test

=item Authenticate and view details for group with id g-test:

 perl group.pl -U http://localhost:8080 -u admin -p admin -V g-test

=item Authenticate and delete group with id g-test:

 perl group.pl -U http://localhost:8080 -u admin -p admin -d g-test

=item Authenticate and add a group with id g-test and property p1=v1:

 perl group.pl -U http://localhost:8080 -u admin -p admin -a g-test -P p1=v1

=back

=cut
#}}}
