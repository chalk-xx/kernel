#!/usr/bin/perl

package TestDataBuilder::Content;

=head1 NAME

Content - Generate sample test data for use in test runs.

=head1 ABSTRACT

Populate a folder with a set of files and folders containing randomly generated
text for testing purposes.

=cut

#{{{imports
use warnings;
use strict;
use Carp;
use Data::Random::WordList;
use File::Path;
use TestDataBuilder::Docx;
use TestDataBuilder::Excel;
use TestDataBuilder::ExcelXML;
use TestDataBuilder::Group;
use TestDataBuilder::HTML;
use TestDataBuilder::JSON;
use TestDataBuilder::ODT;
use TestDataBuilder::PDF;
use TestDataBuilder::Presence;
use TestDataBuilder::RTF;
use TestDataBuilder::Site;
use TestDataBuilder::Text;
use TestDataBuilder::User;
use TestDataBuilder::WordXML;
use TestDataBuilder::XML;

#}}}

=head1 VARIABLES

=cut

#{{{Files To Folders Ratio

=pod

=head2 Folders to files ratio:

Comparison of the number of subfolders to the number of files created.

=over

=item folders: 25%

=item files: 75%

=back

=cut

my $filesToFoldersRatio = 300;

#}}}

#{{{ Folder Depth Percentages

=pod

=head2 Folder depth

Depth of folders. E.g. 0 is a folder at the top level, 1 is a subfolder of a
top level folder and so on.

=over

=item 0: 60%

=item 1: 25%

=item 2: 7%

=item 3: 5%

=item 4 or more: 3%

=back

=cut

my @folderDepthPercentages = ( 60, 25, 7, 5, 3 );

#}}}

#{{{ File Sizes

=pod

=head2 File sizes

Sizes of files created in content hosting (in kB):

=head3 0-100kB (61%)

0-: 26%, 10-: 7%, 20-: 9%, 30-: 6%, 40-: 4%, 50-: 9%

=head3 100-1000kB (24%)

100-: 8%, 200-: 4%, 300-: 3%, 400-: 2%, 500-: 7%

=head3 1000-10000kB (13%)

1000-: 6%, 2000-: 3%, 3000-: 1%, 4000-: 1%, 5000-: 2%

=head3 10000kB+ (2%)

10000-: 2%

=cut

my %fileSizes = (
    '0-10k'       => { kb => 5,     '%' => 26 },
    '10-20k'      => { kb => 15,    '%' => 7 },
    '20-30k'      => { kb => 25,    '%' => 9 },
    '30-40k'      => { kb => 35,    '%' => 6 },
    '40-50k'      => { kb => 45,    '%' => 4 },
    '50-100k'     => { kb => 75,    '%' => 9 },
    '100-200k'    => { kb => 150,   '%' => 8 },
    '200-300k'    => { kb => 250,   '%' => 4 },
    '300-400k'    => { kb => 350,   '%' => 3 },
    '400-500k'    => { kb => 450,   '%' => 2 },
    '500-1000k'   => { kb => 750,   '%' => 7 },
    '1000-2000k'  => { kb => 1500,  '%' => 6 },
    '2000-3000k'  => { kb => 2500,  '%' => 3 },
    '3000-4000k'  => { kb => 3500,  '%' => 1 },
    '4000-5000k'  => { kb => 4500,  '%' => 1 },
    '5000-10000k' => { kb => 7500,  '%' => 2 },
    '10000+k'     => { kb => 20000, '%' => 2 }
);

#}}}

#{{{ File Size Array
# @fileSizeArray is a sorted array containing the sizes defined in %fileSizes
# appearing in the array a corresponding amount of times to their percentage
# value.
my @fileSizeArray;
foreach my $key ( keys %fileSizes ) {
    my $size   = $fileSizes{$key}->{'kb'};
    my $number = $fileSizes{$key}->{'%'};
    for ( my $i = 0 ; $i < $number ; $i++ ) {
        push( @fileSizeArray, $size );
    }
}
@fileSizeArray = sort { $a <=> $b } @fileSizeArray;

#}}}

=head1 METHODS

=cut

#{{{sub new

=pod

=head2 new

Create, set up, and return a Content object.

=cut

sub new {
    my ( $class, $testDataDirectory, $MB, $type, $numberOfDirs, $verbose, $log )
      = @_;
    croak "ERROR: Test data directory not defined\n"
      unless defined $testDataDirectory;
    my $contentDir = "$testDataDirectory/content";

    if ( !-d $contentDir ) {
        my $success = mkdir $contentDir;
        if ( !$success ) {
            croak
              "ERROR: Could not create Test data directory: \"$contentDir\"";
        }
    }
    else {
        my $success = rmtree($contentDir);
        if ( !$success ) {
            croak "ERROR: Could not clear Test data directory: \"$contentDir\"";
        }
        $success = mkdir $contentDir;
        if ( !$success ) {
            croak
              "ERROR: Could not create Test data directory: \"$contentDir\"";
        }
    }
    $MB           = 100    unless defined $MB;
    $type         = "text" unless defined $type;
    $numberOfDirs = 0      unless defined $numberOfDirs;
    my $wl = new Data::Random::WordList( wordlist => '/usr/share/dict/words' );
    my $cwl = new Data::Random::WordList(
        wordlist => 'TestDataBuilder/commonWords.txt' );
    my $bytes    = $MB * 1024 * 1024;
    my $testData = {
        BaseDir        => $testDataDirectory,
        Bytes          => $bytes,
        ContentDir     => $contentDir,
        WordList       => $wl,
        CommonWordList => $cwl,
        Type           => $type,
        NumberOfDirs   => $numberOfDirs,
        Verbose        => $verbose,
        Log            => $log
    };
    bless( $testData, $class );
    return $testData;
}

#}}}

#{{{sub generateFiles

=pod

=head2 generateFiles

Generate files for specified generator.

=cut

sub generateFiles {
    my ( $testData, $allDirectories, $generator ) = @_;
    my $count         = 0;
    my $sizeGenerated = 0;
    my $verbose =
      ( defined $testData->{'Verbose'} ? $testData->{'Verbose'} : 0 );
    while ( $sizeGenerated < $testData->{'Bytes'} ) {
        my $size      = $fileSizeArray[ $count++ % ( $#fileSizeArray + 1 ) ];
        my $directory = @{$allDirectories}[ $count % @{$allDirectories} ];
        my $increase  = $generator->create(
            $size, $directory, "$size.kb.$count",
            \$testData->{WordList},
            \$testData->{CommonWordList}
        );
        my $file_created =
          "$directory/$size.kb.$count." . $generator->{'Extension'};

        # Typically 4k is allocated for file info etc:
        $sizeGenerated = $sizeGenerated + $increase + 4096;
        if ( $sizeGenerated > $testData->{'Bytes'} ) {

            # Last file took us past the limit:
            unlink "$file_created";
        }
        else {
            if ( $verbose >= 1 ) {
                Sling::Print::print_with_lock(
                    "Created file: \"$file_created\", size \"$size kb\".",
                    $testData->{'Log'} );
            }
            Sling::Print::print_with_lock( "$file_created,$file_created",
                $testData->{'BaseDir'} . "/content_additions.txt" );
        }
        if ( $verbose >= 2 ) {
            Sling::Print::print_with_lock(
                "Type \""
                  . $generator->{'Extension'}
                  . "\" created \"$sizeGenerated\" bytes of \""
                  . $testData->{'Bytes'}
                  . "\" total.",
                $testData->{'Log'}
            );
        }
    }
    return 1;
}

#}}}

#{{{sub generate

=pod

=head2 generate

Generate test data of the type specified.

=cut

sub generate {
    my ($testData) = @_;

    # Create content additions file:
    my $content_data_file = $testData->{'BaseDir'} . "/content_additions.txt";
    if ( -f $content_data_file ) {
        my $success = unlink($content_data_file);
        croak "Could not clear existing content data file" unless $success;
    }

    # Create sample content:
    Sling::Print::print_with_lock(
        "Creating \""
          . $testData->{'Bytes'}
          . "\" bytes of test data in directory: \""
          . $testData->{'ContentDir'} . "\".",
        $testData->{'Log'}
    );
    Sling::Print::print_with_lock(
        "Creating \"" . $testData->{'NumberOfDirs'} . "\" directories.",
        $testData->{'Log'} );
    my @allDirectories = ( $testData->{ContentDir} );
    my %directories;
    my $depth                 = 1;
    my $directories_remaining = $testData->{'NumberOfDirs'};
    foreach my $folderAtDepthPercentage (@folderDepthPercentages) {

        # Finish if there are no more directories to create:
        my $number_dirs_at_level =
          $folderAtDepthPercentage * $testData->{'NumberOfDirs'} / 100;
        my @directoriesAtLevel;
        for (
            my $dirCount = 0 ;
            $dirCount < $number_dirs_at_level ;
            $dirCount++
          )
        {
            if ( !$directories_remaining && $testData->{'Bytes'} <= 0 ) {
                last;
            }
            $directories_remaining--;
            my $folder_size;
            if ( $depth == 1 ) {
                mkdir $testData->{ContentDir} . "/dir-l$depth-$dirCount";
                $folder_size =
                  -s $testData->{ContentDir} . "/dir-l$depth-$dirCount";
                push( @directoriesAtLevel,
                    $testData->{ContentDir} . "/dir-l$depth-$dirCount" );
                push( @allDirectories,
                    $testData->{ContentDir} . "/dir-l$depth-$dirCount" );
            }
            else {
                my $depthAbove             = $depth - 1;
                my $directoriesAbove       = $directories{$depthAbove};
                my $numberDirectoriesAbove = @{$directoriesAbove};
                my $directoryAbove =
                  $$directoriesAbove[ $dirCount % $numberDirectoriesAbove ];
                mkdir("$directoryAbove/dir-l$depth-$dirCount");
                $folder_size = -s "$directoryAbove/dir-l$depth-$dirCount";
                push( @directoriesAtLevel,
                    "$directoryAbove/dir-l$depth-$dirCount" );
                push( @allDirectories,
                    "$directoryAbove/dir-l$depth-$dirCount" );
            }

            # each direcory created uses up 4k storage typically:
            $testData->{'Bytes'} = $testData->{'Bytes'} - $folder_size;
        }
        $directories{$depth} = \@directoriesAtLevel;
        $depth++;
    }
    Sling::Print::print_with_lock( "Directories Created.", $testData->{Log} );
    Sling::Print::print_with_lock(
        "Generating files of type: \"" . $testData->{'Type'} . "\".",
        $testData->{Log} );
    Sling::Print::print_with_lock(
        "Please be patient, this may take some time.",
        $testData->{Log} );
    if ( $testData->{'Type'} eq "text" ) {
        $testData->generateFiles( \@allDirectories, new TestDataBuilder::Text );
    }
    elsif ( $testData->{'Type'} eq "html" ) {
        $testData->generateFiles( \@allDirectories, new TestDataBuilder::HTML );
    }
    elsif ( $testData->{'Type'} eq "xml" ) {
        $testData->generateFiles( \@allDirectories, new TestDataBuilder::XML );
    }
    elsif ( $testData->{'Type'} eq "wordxml" ) {
        $testData->generateFiles( \@allDirectories,
            new TestDataBuilder::WordXML );
    }
    elsif ( $testData->{'Type'} eq "pdf" ) {
        $testData->generateFiles( \@allDirectories, new TestDataBuilder::PDF );
    }
    elsif ( $testData->{'Type'} eq "rtf" ) {
        $testData->generateFiles( \@allDirectories, new TestDataBuilder::RTF );
    }
    elsif ( $testData->{'Type'} eq "docx" ) {
        $testData->generateFiles( \@allDirectories, new TestDataBuilder::Docx );
    }
    elsif ( $testData->{'Type'} eq "excel" ) {
        $testData->generateFiles( \@allDirectories,
            new TestDataBuilder::Excel );
    }
    elsif ( $testData->{'Type'} eq "excelx" ) {
        $testData->generateFiles( \@allDirectories,
            new TestDataBuilder::ExcelXML );
    }
    elsif ( $testData->{'Type'} eq "json" ) {
        $testData->generateFiles( \@allDirectories, new TestDataBuilder::JSON );
    }
    elsif ( $testData->{'Type'} eq "odt" ) {
        $testData->generateFiles( \@allDirectories, new TestDataBuilder::ODT );
    }
    elsif ( $testData->{'Type'} eq "all" ) {

        # Divide the remaining allocation between the 8 content types:
        $testData->{'Bytes'} = $testData->{'Bytes'} / 8;
        my @childs = ();
        my $pid    = fork();
        if ($pid) { push( @childs, $pid ); }    # parent
        elsif ( $pid == 0 ) {                   # child
            $testData->generateFiles( \@allDirectories,
                new TestDataBuilder::Text );
            exit(0);
        }
        else { print "ERROR: Could not fork!\n"; }
        $pid = fork();
        if ($pid) { push( @childs, $pid ); }    # parent
        elsif ( $pid == 0 ) {                   # child
            $testData->generateFiles( \@allDirectories,
                new TestDataBuilder::HTML );
            exit(0);
        }
        else { print "ERROR: Could not fork!\n"; }
        $pid = fork();
        if ($pid) { push( @childs, $pid ); }    # parent
        elsif ( $pid == 0 ) {                   # child
            $testData->generateFiles( \@allDirectories,
                new TestDataBuilder::XML );
            exit(0);
        }
        else { print "ERROR: Could not fork!\n"; }
        $pid = fork();
        if ($pid) { push( @childs, $pid ); }    # parent
        elsif ( $pid == 0 ) {                   # child
            $testData->generateFiles( \@allDirectories,
                new TestDataBuilder::WordXML );
            exit(0);
        }
        else { print "ERROR: Could not fork!\n"; }
        $pid = fork();
        if ($pid) { push( @childs, $pid ); }    # parent
        elsif ( $pid == 0 ) {                   # child
            $testData->generateFiles( \@allDirectories,
                new TestDataBuilder::PDF );
            exit(0);
        }
        else { print "ERROR: Could not fork!\n"; }
        $pid = fork();
        if ($pid) { push( @childs, $pid ); }    # parent
        elsif ( $pid == 0 ) {                   # child
            $testData->generateFiles( \@allDirectories,
                new TestDataBuilder::RTF );
            exit(0);
        }
        else { print "ERROR: Could not fork!\n"; }
        $pid = fork();
        if ($pid) { push( @childs, $pid ); }    # parent
        elsif ( $pid == 0 ) {                   # child
            $testData->generateFiles( \@allDirectories,
                new TestDataBuilder::Docx );
            exit(0);
        }
        else { print "ERROR: Could not fork!\n"; }
        $pid = fork();
        if ($pid) { push( @childs, $pid ); }    # parent
        elsif ( $pid == 0 ) {                   # child
            $testData->generateFiles( \@allDirectories,
                new TestDataBuilder::Excel );
            exit(0);
        }
        else { print "ERROR: Could not fork!\n"; }
        $pid = fork();
        if ($pid) { push( @childs, $pid ); }    # parent
        elsif ( $pid == 0 ) {                   # child
            $testData->generateFiles( \@allDirectories,
                new TestDataBuilder::ExcelXML );
            exit(0);
        }
        else { print "ERROR: Could not fork!\n"; }
        $pid = fork();
        if ($pid) { push( @childs, $pid ); }    # parent
        elsif ( $pid == 0 ) {                   # child
            $testData->generateFiles( \@allDirectories,
                new TestDataBuilder::JSON );
            exit(0);
        }
        else { print "ERROR: Could not fork!\n"; }
        $pid = fork();
        if ($pid) { push( @childs, $pid ); }    # parent
        elsif ( $pid == 0 ) {                   # child
            $testData->generateFiles( \@allDirectories,
                new TestDataBuilder::ODT );
            exit(0);
        }
        else { print "ERROR: Could not fork!\n"; }
        foreach (@childs) { waitpid( $_, 0 ); }
    }
    else {
        croak "ERROR: unable to generate data of type: \""
          . $testData->{'Type'}
          . "\".\nSupported types are: "
          . "\"text\", \"html\", \"pdf\", \"rtf\", \"xml\", \"excel\", \"json\", \"odt\", \"all\".\n";
    }
    Sling::Print::print_with_lock( "Content data Created.", $testData->{Log} );
    return 1;
}

#}}}

1;
