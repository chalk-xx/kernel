#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

presence perl script. Provides a means of monitoring and manipulating presence
for a user in a system from the command line. Additionally serves as a
reference example for using the Sling::Presence library.

=head1 OPTIONS

Usage: perl presence.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --auth (type)                   - Specify auth type. If ommitted, default is used.
 --contacts or -c                - Fetch the status of the contacts of the current user.
 --delete or -d                  - clear status of current user.
 --file or -F (File)             - File containing list of presence operations to run through.
 --help or -?                    - view the script synopsis and options.
 --log or -L (log)               - Log script output to specified log file.
 --man or -M                     - view the full script documentation.
 --pass or -p (password)         - Password of user performing presence operations.
 --set-location or -l (location) - Set location of current user to specified value.
 --set-status or -S (status)     - Set status of current user to specified value.
 --status or -s                  - Fetch the status of the current user.
 --threads or -t (threads)       - Used with -F, defines number of parallel
                                   processes to have running through file.
 --url or -U (URL)               - URL for system being tested against.
 --user or -u (username)         - Name of user performing presence operations.
 --verbose or -v or -vv or -vvv  - Increase verbosity of output.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl presence.pl --man

=head1 Example Usage

=over

=item Authenticate as testuser and retrieve presence status:

 perl presence.pl -U http://localhost:8080 -u testuser -p pass --status

=back

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Authn;
use Sling::Presence;
#}}}

#{{{options parsing
my $auth;
my $contacts;
my $delete;
my $file;
my $help;
my $log;
my $man;
my $numberForks = 1;
my $password;
my $set_location;
my $set_status;
my $status;
my $url;
my $username;
my $verbose;

GetOptions (
    "auth=s" => \$auth,
    "contacts|c" => \$contacts,
    "delete|d" => \$delete,
    "file|F=s" => \$file,
    "help|?" => \$help,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "pass|p=s" => \$password,
    "set-location|l=s" => \$set_location,
    "set-status|S=s" => \$set_status,
    "status|s" => \$status,
    "threads|t=i" => \$numberForks,
    "url|U=s" => \$url,
    "user|u=s" => \$username,
    "verbose|v+" => \$verbose
) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );

$url = Sling::URL::url_input_sanitize( $url );
#}}}

#{{{ main execution path
if ( defined $file ) {
    my $message = "Performing presence updates in file: \"$file\":";
    Sling::Print::print_with_lock( "$message", $log );
    my @childs = ();
    for ( my $i = 0 ; $i < $numberForks ; $i++ ) {
	my $pid = fork();
	if ( $pid ) { push( @childs, $pid ); } # parent
	elsif ( $pid == 0 ) { # child
            my $authn = new Sling::Authn( $url, $username, $password, $auth, $verbose, $log );
            my $presence = new Sling::Presence( \$authn, $verbose, $log );
            $presence->update_from_file( $file, $i, $numberForks );
	    exit( 0 );
	}
	else {
            die "Could not fork $i!";
	}
    }
    foreach ( @childs ) { waitpid( $_, 0 ); }
}
else {
    my $authn = new Sling::Authn( $url, $username, $password, $auth, $verbose, $log );
    my $presence = new Sling::Presence( \$authn, $verbose, $log );

    if ( defined $delete ) {
        $presence->delete( $set_location, $set_status );
        Sling::Print::print_result( $presence );
    }
    if ( ( defined $set_location ) || ( defined $set_status ) ) {
        $presence->update( $set_location, $set_status );
        Sling::Print::print_result( $presence );
    }

    if ( defined $status ) {
        $presence->status();
        Sling::Print::print_result( $presence );
    }
    elsif ( defined $contacts ) {
        $presence->contacts();
        Sling::Print::print_result( $presence );
    }
}
#}}}

1;
