#!/usr/bin/perl

#{{{Documentation
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

=head1 Example Usage

=over

=item generate test data for all tests with 8 threads:

 perl build_test_data.pl --all --threads 8

=back

=cut
#}}}

#{{{imports
use strict;
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
my $directory = "data";
my $group_data;;
my $help;
my $log;
my $man;
my $numberForks = 1;
my $numberOfDirs = 1024;
my $presence_data;
my $site_data;
my $testDataSizeMB = 8;
my $type = "all";
my $user_data;
my $verbose;

GetOptions (
    "all|a" => \$all_data,
    "connection|c" => \$connection_data,
    "content|C" => \$content_data,
    "content-size|S=s" => \$testDataSizeMB,
    "directory|d" => \$directory,
    "group|g" => \$group_data,
    "help|?" => \$help,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "presence|p" => \$presence_data,
    "site|s" => \$site_data,
    "threads|t=i" => \$numberForks,
    "user|u" => \$user_data,
    "verbose|v+" => \$verbose
) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

my @all_test_data_list = ( "Connection", "Content", "Group", "Presence", "Site", "User" );
my @test_data_selected = ();

if ( $all_data ) {
    @test_data_selected = @all_test_data_list;
}
else {
    if ( $connection_data ) {
        push ( @test_data_selected, "Connection" );
    }
    elsif ( $content_data ) {
        push ( @test_data_selected, "Content" );
    }
    elsif ( $group_data ) {
        push ( @test_data_selected, "Group" );
    }
    elsif ( $presence_data ) {
        push ( @test_data_selected, "Presence" );
    }
    elsif ( $site_data ) {
        push ( @test_data_selected, "Site" );
    }
    elsif ( $user_data ) {
        push ( @test_data_selected, "User" );
    }
}

$numberForks = ( $numberForks || 1 );
$numberForks = ( $numberForks =~ /^[0-9]+$/ ? $numberForks : 1 );
$numberForks = ( $numberForks < 32 ? $numberForks : 1 );
#}}}

#{{{ main execution path
foreach my $data ( @test_data_selected ) {
    if ( ! -d $directory ) {
        my $success = mkdir $directory;
	die "Could not make test data directory: \"$data\"!" unless $success;
    }
    if ( $data =~ /^Connection$/ ) {
        my $connection = new TestDataBuilder::Connection( $directory, $verbose, $log );
        $connection->generate();
    }
    elsif ( $data =~ /^Content$/ ) {
        my $content = new TestDataBuilder::Content( $directory, $testDataSizeMB, $type, $numberOfDirs, $verbose, $log );
        $content->generate();
    }
    elsif ( $data =~ /^Group$/ ) {
        my $group = new TestDataBuilder::Group( $directory, $verbose, $log );
        $group->generate();
    }
    elsif ( $data =~ /^Presence$/ ) {
        my $presence = new TestDataBuilder::Presence( $directory, $verbose, $log );
        $presence->generate();
    }
    elsif ( $data =~ /^Site$/ ) {
        my $site = new TestDataBuilder::Site( $directory, $verbose, $log );
        $site->generate();
    }
    elsif ( $data =~ /^User$/ ) {
        my $user = new TestDataBuilder::User( $directory, $verbose, $log );
        $user->generate();
    }
    else {
        die "Could not generate test data for data type: \"$data\"";
    }
}
#}}}

1;
