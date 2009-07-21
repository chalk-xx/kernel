#!/usr/bin/perl

#{{{imports
use warnings;
use strict;
use Carp;
use version; our $VERSION = qv('0.0.1');
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Print;
use TestDataBuilder::Content();
use TestDataBuilder::Connection();
use TestDataBuilder::Group();
use TestDataBuilder::Presence();
use TestDataBuilder::Site();
use TestDataBuilder::User();

#}}}

#{{{options parsing
my $all_data;
my $connection_data;
my $content_data;
my $directory = 'data';
my $group_data;
my $help;
my $log;
my $man;
my $number_forks   = '1';
my $number_of_dirs = '1024';
my $presence_data;
my $site_data;
my $test_data_size_mb = '8';
my $type              = 'all';
my $user_data;
my $verbose;

GetOptions(
    'all|a'            => \$all_data,
    'connection|c'     => \$connection_data,
    'content|C'        => \$content_data,
    'content-size|S=s' => \$test_data_size_mb,
    'directory|d'      => \$directory,
    'group|g'          => \$group_data,
    'help|?'           => \$help,
    'log|L=s'          => \$log,
    'man|M'            => \$man,
    'presence|p'       => \$presence_data,
    'site|s'           => \$site_data,
    'threads|t=i'      => \$number_forks,
    'user|u'           => \$user_data,
    'verbose|v+'       => \$verbose
) or pod2usage(2);

if ($help) { pod2usage( -exitstatus => 0, -verbose => 1 ); }
if ($man)  { pod2usage( -exitstatus => 0, -verbose => 2 ); }

my @all_test_data_list = qw(Connection Content Group Presence Site User);
my @test_data_selected = ();

if ($all_data) {
    @test_data_selected = @all_test_data_list;
}
else {
    if ($connection_data) {
        push @test_data_selected, 'Connection';
    }
    elsif ($content_data) {
        push @test_data_selected, 'Content';
    }
    elsif ($group_data) {
        push @test_data_selected, 'Group';
    }
    elsif ($presence_data) {
        push @test_data_selected, 'Presence';
    }
    elsif ($site_data) {
        push @test_data_selected, 'Site';
    }
    elsif ($user_data) {
        push @test_data_selected, 'User';
    }
}

my $max_allowed_forks = '32';
$number_forks = ( $number_forks || 1 );
$number_forks = ( $number_forks =~ /^[0-9]+$/xms ? $number_forks : 1 );
$number_forks = ( $number_forks < $max_allowed_forks ? $number_forks : 1 );

#}}}

#{{{ main execution path
foreach my $data (@test_data_selected) {
    if ( !-d $directory ) {
        my $success = mkdir $directory;
        if ( !$success ) {
            croak "Could not make test data directory: \"$data\"!";
        }
    }
    if ( $data eq 'Connection' ) {
        my $connection =
          new TestDataBuilder::Connection( $directory, $verbose, $log );
        $connection->generate();
    }
    elsif ( $data eq 'Content' ) {
        my $content =
          new TestDataBuilder::Content( $directory, $test_data_size_mb, $type,
            $number_of_dirs, $verbose, $log );
        $content->generate();
    }
    elsif ( $data eq 'Group' ) {
        my $group = new TestDataBuilder::Group( $directory, $verbose, $log );
        $group->generate();
    }
    elsif ( $data eq 'Presence' ) {
        my $presence =
          new TestDataBuilder::Presence( $directory, $verbose, $log );
        $presence->generate();
    }
    elsif ( $data eq 'Site' ) {
        my $site = new TestDataBuilder::Site( $directory, $verbose, $log );
        $site->generate();
    }
    elsif ( $data eq 'User' ) {
        my $user = new TestDataBuilder::User( $directory, $verbose, $log );
        $user->generate();
    }
    else {
        croak "Could not generate test data for data type: \"$data\"";
    }
}

#}}}

1;

__END__

#{{{Documentation

=head1 NAME

build_test_data.pl

=head1 SYNOPSIS

test data builder perl script. Provides a means of reliably generating test
data that can be used to test the performance of the sakai sling system from
the command line. Additionally serves as a reference example for using the
TestDataBuilder library.

=head1 OPTIONS

Usage: perl build_test_data.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --all or -a                     - generate testdata for all tests.
 --connection or -c              - generate testdata for connection tests.
 --content or -C                 - generate testdata for content tests.
 --content-size or -S MB         - Megabytes of content test data to generate.
 --directory or -d (directory)   - data directory (default = data)
 --group or -g                   - generate testdata for group tests.
 --help or -?                    - view the script synopsis and options.
 --log or -L (log)               - Log script output to specified log file.
 --man or -M                     - view the full script documentation.
 --presence or -p                - generate testdata for presence tests.
 --site or -s                    - generate testdata for site tests.
 --threads or -t (threads)       - define number of parallel processes to have
                                   generating test data.
 --user or -u                    - generate testdata for user tests.
 --verbose or -v or -vv or -vvv  - Increase verbosity of output.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl messaging.pl --man

=head1 USAGE

=over

=item generate test data for all tests with 8 threads:

 perl build_test_data.pl --all --threads 8

=back

=head1 DESCRIPTION

test data builder perl script. Provides a means of reliably generating test
data that can be used to test the performance of the sakai sling system from
the command line. Additionally serves as a reference example for using the
TestDataBuilder library.

=head1 REQUIRED ARGUMENTS

None.

=head1 DIAGNOSTICS

Run with multiple -v options to enable verbose output.

=head1 EXIT STATUS

1 on success, otherwise failure.

=head1 CONFIGURATION

None needed.

=head1 DEPENDENCIES

Carp; Getopt::Long; Pod::Usage; Sling::Print;
TestDataBuilder::Content; TestDataBuilder::Connection;
TestDataBuilder::Group; TestDataBuilder::Presence;
TestDataBuilder::Site; TestDataBuilder::User;

=head1 INCOMPATIBILITIES

None known (^_-)

=head1 BUGS AND LIMITATIONS

None known (^_-)

=head1 AUTHOR

Daniel Parry -- daniel@caret.cam.ac.uk

=head1 LICENSE AND COPYRIGHT

   Copyright 2009 Daniel David Parry

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

=cut

#}}}
