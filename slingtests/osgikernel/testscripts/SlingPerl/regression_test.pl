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
use Sling::URL;
use Tests::Authn;
use Tests::Authz;
use Tests::Connection;
use Tests::Content;
use Tests::ContentFile;
use Tests::Group;
use Tests::ImageCrop;
use Tests::Messaging;
use Tests::Presence;
use Tests::Search;
use Tests::Site;
use Tests::User;

#}}}

#{{{options parsing
my $all_tests;
my $authn_test;
my $authz_test;
my $connection_test;
my $content_test;
my $contentfile_test;
my $group_test;
my $help;
my $image_crop_test;
my $log;
my $man;
my $messaging_test;
my $number_forks = 1;
my $password;
my $presence_test;
my $search_test;
my $site_test;
my $url;
my $username;
my $user_test;
my $verbose;

GetOptions(
    'all'           => \$all_tests,
    'authn'         => \$authn_test,
    'authz'         => \$authz_test,
    'connection'    => \$connection_test,
    'content'       => \$content_test,
    'content-file'  => \$contentfile_test,
    'group'         => \$group_test,
    'help|?'        => \$help,
    'image-crop'    => \$image_crop_test,
    'log|L=s'       => \$log,
    'man|M'         => \$man,
    'messaging'     => \$messaging_test,
    'pass|p=s'      => \$password,
    'presence'      => \$presence_test,
    'search'        => \$search_test,
    'site'          => \$site_test,
    'superuser|u=s' => \$username,
    'threads|t=i'   => \$number_forks,
    'url|U=s'       => \$url,
    'user'          => \$user_test,
    'verbose|v+'    => \$verbose
) or pod2usage(2);

if ($help) { pod2usage( -exitstatus => 0, -verbose => 1 ); }
if ($man)  { pod2usage( -exitstatus => 0, -verbose => 2 ); }
if ( !defined $username ) { croak 'Test super user username not defined'; }
if ( !defined $password ) { croak 'Test super user password not defined'; }

$url = Sling::URL::url_input_sanitize($url);

my $auth;    # Just use default auth

my %tests = (
    'Authn'       => \&Tests::Authn::run_regression_test,
    'Authz'       => \&Tests::Authz::run_regression_test,
    'Connection'  => \&Tests::Connection::run_regression_test,
    'Content'     => \&Tests::Content::run_regression_test,
    'ContentFile' => \&Tests::ContentFile::run_regression_test,
    'Group'       => \&Tests::Group::run_regression_test,
    'ImageCrop'   => \&Tests::ImageCrop::run_regression_test,
    'Messaging'   => \&Tests::Messaging::run_regression_test,
    'Presence'    => \&Tests::Presence::run_regression_test,
    'Search'      => \&Tests::Search::run_regression_test,
    'Site'        => \&Tests::Site::run_regression_test,
    'User'        => \&Tests::User::run_regression_test,
);

my @all_tests_list = keys %tests;

my @tests_selected = ();

if ($all_tests) {
    @tests_selected = @all_tests_list;
}
else {
    if ($authn_test) {
        push @tests_selected, 'Authn';
    }
    if ($authz_test) {
        push @tests_selected, 'Authz';
    }
    if ($connection_test) {
        push @tests_selected, 'Connection';
    }
    if ($content_test) {
        push @tests_selected, 'Content';
    }
    if ($contentfile_test) {
        push @tests_selected, 'ContentFile';
    }
    if ($group_test) {
        push @tests_selected, 'Group';
    }
    if ($image_crop_test) {
        push @tests_selected, 'ImageCrop';
    }
    if ($messaging_test) {
        push @tests_selected, 'Messaging';
    }
    if ($presence_test) {
        push @tests_selected, 'Presence';
    }
    if ($search_test) {
        push @tests_selected, 'Search';
    }
    if ($site_test) {
        push @tests_selected, 'Site';
    }
    if ($user_test) {
        push @tests_selected, 'User';
    }
}

my $max_allowed_forks = '32';
$number_forks = ( $number_forks || 1 );
$number_forks = ( $number_forks =~ /^[0-9]+$/xms ? $number_forks : 1 );
$number_forks = ( $number_forks < $max_allowed_forks ? $number_forks : 1 );
$number_forks =
  ( @tests_selected < $number_forks ? @tests_selected : $number_forks );

#}}}

#{{{ main execution path
my @childs = ();
for my $i ( 0 .. $number_forks ) {
    my $pid = fork;
    if ($pid) { push @childs, $pid; }    # parent
    elsif ( $pid == 0 ) {                # child
        my $j = $i;
        while ( $j < @tests_selected ) {
            my $test = $tests_selected[$j];
            $j += $number_forks;
            my $authn =
              new Sling::Authn( $url, $username, $password, $auth, $verbose,
                $log );
            &{ $tests{$test} }( \$authn, $verbose, $log );
        }
        exit 0;
    }
    else {
        croak "Could not fork $i!";
    }
}
foreach (@childs) { waitpid $_, 0; }

#}}}

1;

__END__

#{{{Documentation
=head1 SYNOPSIS

regression testing perl script. Provides a means of performing regression
testing of a Sling K2 system from the command line.

=head1 OPTIONS

Usage: perl regression_test.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --all                          - run all regression tests.
 --authn                        - run authentication regression tests.
 --authz                        - run authorization regression tests.
 --connection                   - run connection regression tests.
 --content                      - run content regression tests.
 --content-file                 - run content file regression tests.
 --group                        - run group regression tests.
 --help or -?                   - view the script synopsis and options.
 --image-crop                   - run image crop regression tests.
 --log or -L (log)              - Log script output to specified log file.
 --man or -M                    - view the full script documentation.
 --messaging                    - run messaging regression tests.
 --pass or -p (password)        - Password of system super user.
 --presence                     - run presence regression tests.
 --site                         - run site regression tests.
 --superuser or -u (username)   - User ID of system super user.
 --threads or -t (threads)      - Defines number of parallel processes
                                  to have running regression tests.
 --url or -U (URL)              - URL for system being tested against.
 --user                         - run user regression tests.
 --verbose or -v or -vv or -vvv - Increase verbosity of output.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl regression_test.pl --man

=head1 Example Usage

=over

=item Run all regression tests single threaded, specifying superuser to be admin and superuser password to be admin.

 perl regression_test.pl -U http://localhost:8080 --all -u admin -p admin

=item Run authn regression tests, specifying superuser to be admin and superuser password to be admin.

 perl regression_test.pl -U http://localhost:8080 --authn --superuser admin --pass admin

=item Run authz regression tests, specifying superuser to be admin and superuser password to be admin with verbose output.

 perl regression_test.pl -U http://localhost:8080 --authz --superuser admin --pass admin -v

=item Run image crop regression tests, specifying superuser to be admin and superuser password to be admin with very verbose output.

 perl regression_test.pl -U http://localhost:8080 --image-crop --superuser admin --pass admin -vvv

=item Run connection and content regression tests, specifying superuser to be admin and superuser password to be admin.

 perl regression_test.pl -U http://localhost:8080 --connection --content --superuser admin --pass admin

=item Run group, messaging, presence and site regression tests, specifying superuser to be admin and superuser password to be admin.

 perl regression_test.pl -U http://localhost:8080 --group --messaging --presence --site --superuser admin --pass admin

=item Run user regression tests, specifying superuser to be admin and superuser password to be admin and log output to log.txt.

 perl regression_test.pl -U http://localhost:8080 --user --superuser admin --pass admin --log log.txt

=item Run all regression tests in four threads, specifying superuser to be admin and superuser password to be admin.

 perl regression_test.pl -U http://localhost:8080 --all --threads 4 -u admin -p admin

=back

=cut
#}}}
