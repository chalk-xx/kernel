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
use Sling::Messaging;

#}}}

#{{{options parsing
my $ascending;
my $auth;
my $chat;
my $create;
my $descending;
my $file;
my $help;
my $inbox;
my $internal;
my $list;
my $log;
my $man;
my $number_forks = 1;
my $outbox;
my $password;
my $send;
my $sort_by;
my $url;
my $username;
my $verbose;

GetOptions(
    'ascending|a'  => \$ascending,
    'auth=s'       => \$auth,
    'chat|C'       => \$chat,
    'create|c=s'   => \$create,
    'descending|d' => \$descending,
    'file|F=s'     => \$file,
    'help|?'       => \$help,
    'inbox|i'      => \$inbox,
    'internal|I'   => \$internal,
    'list|l'       => \$list,
    'log|L=s'      => \$log,
    'man|M'        => \$man,
    'outbox|o'     => \$outbox,
    'pass|p=s'     => \$password,
    'send|s=s'     => \$send,
    'sort-by|S=s'  => \$sort_by,
    'threads|t=i'  => \$number_forks,
    'url|U=s'      => \$url,
    'user|u=s'     => \$username,
    'verbose|v+'   => \$verbose
) or pod2usage(2);

if ($help) { pod2usage( -exitstatus => 0, -verbose => 1 ); }
if ($man)  { pod2usage( -exitstatus => 0, -verbose => 2 ); }

my $max_allowed_forks = '32';
$number_forks = ( $number_forks || 1 );
$number_forks = ( $number_forks =~ /^[0-9]+$/xms ? $number_forks : 1 );
$number_forks = ( $number_forks < $max_allowed_forks ? $number_forks : 1 );

$url = Sling::URL::url_input_sanitize($url);

#}}}

#{{{ main execution path
if ( defined $file ) {
    my $message = "Performing messaging actions specified in file: \"$file\":";
    Sling::Print::print_with_lock( "$message", $log );
    my @childs = ();
    for my $i ( 0 .. $number_forks ) {
        my $pid = fork;
        if ($pid) { push @childs, $pid; }    # parent
        elsif ( $pid == 0 ) {                # child
            my $authn =
              new Sling::Authn( $url, $username, $password, $auth, $verbose,
                $log );
            my $messaging = new Sling::Messaging( \$authn, $verbose, $log );
            $messaging->update_from_file( $file, $i, $number_forks );
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
    my $messaging = new Sling::Messaging( \$authn, $verbose, $log );

    if ( defined $create ) {
        my $type;
        if ( defined $internal ) {
            $type = 'internal';
        }
        elsif ( defined $chat ) {
            $type = 'chat';
        }
        else {
            $type = 'internal';    # default type is internal
        }
        $messaging->create( $create, $type );
    }
    elsif ( defined $send ) {
        $messaging->send($send);
    }
    elsif ( ( defined $list ) || ( defined $inbox ) || ( defined $outbox ) ) {
        my $order;
        if ( defined $ascending ) {
            $order = 'ascending';
        }
        elsif ( defined $descending ) {
            $order = 'descending';
        }
        my $box;
        if ( defined $list ) {
            $box = 'all';
        }
        elsif ( defined $inbox ) {
            $box = 'inbox';
        }
        elsif ( defined $outbox ) {
            $box = 'outbox';
        }
        $messaging->list( $box, $sort_by, $order );
    }
    Sling::Print::print_result($messaging);
}

#}}}

1;

__END__

#{{{Documentation
=head1 SYNOPSIS

messaging perl script. Provides a means of using the system messaging api for a
user in a system from the command line. Additionally serves as a reference
example for using the Sling::Messaging library.

=head1 OPTIONS

Usage: perl messaging.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --ascending or -a               - sort lists in ascending order.
 --auth (type)                   - Specify auth type. If ommitted, default is used.
 --chat or -C                    - specify chat message type.
 --create or -c (sendTo)         - create a message to send to the specified uer.
 --descending or -d              - sort lists in descending order.
 --file or -F (File)             - File containing list of messaging operations to run through.
 --help or -?                    - view the script synopsis and options.
 --inbox or -i                   - list all messages in the inbox for the current user.
 --internal or -I                - specify internal message type (default).
 --list or -l                    - list all messages for the current user.
 --log or -L (log)               - Log script output to specified log file.
 --man or -M                     - view the full script documentation.
 --outbox or -o                  - list all messages in the outbox for the current user.
 --pass or -p (password)         - Password of user performing messaging operations.
 --send or -s (messageId)        - send message specified by messageId.
 --sort-by or -S                 - used when listing, specify property to sort messages by.
 --threads or -t (threads)       - Used with -F, defines number of parallel
                                   processes to have running through file.
 --url or -U (URL)               - URL for system being tested against.
 --user or -u (username)         - Name of user performing messaging operations.
 --verbose or -v or -vv or -vvv  - Increase verbosity of output.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl messaging.pl --man

=head1 Example Usage

=over

=item Authenticate as testuser1 and create an internal message to be sent to testuser2:

 perl messaging.pl -U http://localhost:8080 -u testuser1 -p pass --create testuser2

=back

=cut
#}}}
