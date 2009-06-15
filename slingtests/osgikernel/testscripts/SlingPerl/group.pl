#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

group perl script. Provides a means of managing groups in sling from the
command line. The script also acts as a reference implementation for the Group
perl library.

=head1 OPTIONS

Usage: perl group.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 -a (actOnGroup) - add specified group.
 -d (actOnGroup) - delete specified group.
 -e (actOnGroup) - check whether specified group exists.
 -p (password)   - Password of user performing actions.
 -t (threads)    - Used with -F, defines number of parallel
                   processes to have running through file.
 -v (actOnGroup) - view details for specified group in json format.
 -u (username)   - Name of user to perform any actions as.
 -F (file)       - file containing list of group to be added.
 -L (log)        - Log script output to specified log file.
 -U (URL)        - URL for system being tested against.
 --auth (type)   - Specify auth type. If ommitted, default is used.
 --help or -?    - view the script synopsis and options.
 --man           - view the full script documentation.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl group.pl --man

=head1 Example Usage

=over

=item Authenticate and add a group with id testgroup:

 perl group.pl -U http://localhost:8080 -a testgroup -u admin -p admin

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Group;
use Sling::UserAgent;
#}}}

#{{{options parsing
my $addGroup;
my $auth;
my $deleteGroup;
my $existsGroup;
my $file;
my $help;
my $log;
my $man;
my $numberForks = 1;
my $password;
my $url = "http://localhost";
my $username;
my $viewGroup;

GetOptions ( "a=s" => \$addGroup,    "d=s"   => \$deleteGroup,
             "e=s" => \$existsGroup, "p=s" => \$password,
             "u=s" => \$username,    "t=s" => \$numberForks,
	     "v=s" => \$viewGroup,   "F=s" => \$file,
	     "L=s" => \$log,         "U=s" => \$url,
	     "auth=s" => \$auth,
             "help|?" => \$help, "man" => \$man) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );
#}}}

#{{{main execution path
if ( defined $file ) {
    print "Adding groups from file:\n";
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
	    # Create a separate user agent per fork:
            my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
            my $group = new Sling::Group( $url, $lwpUserAgent );
            $group->add_from_file( $file, $i, $numberForks, $log );
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
    my $group = new Sling::Group( $url, $lwpUserAgent );

    if ( defined $existsGroup ) {
        $group->exists( $existsGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $addGroup ) {
        $group->add( $addGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $deleteGroup ) {
        $group->delete( $deleteGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
    elsif ( defined $viewGroup ) {
        $group->view( $viewGroup, $log );
        print $group->{ 'Message' } . "\n";
    }
}
#}}}

1;
