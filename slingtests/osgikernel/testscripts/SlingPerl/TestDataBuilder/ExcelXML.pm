#!/usr/bin/perl

package TestDataBuilder::ExcelXML;

=head1 NAME

ExcelXML - Output an Excel XML format file filled with random words up to a given size.

=head1 ABSTRACT

ExcelXML - Output an Excel XML format file filled with random words up to a given size.

=cut

#{{{imports
use strict;
use POSIX qw( ceil );
use CGI qw/:standard/;
use Spreadsheet::WriteExcelXML;
use Sling::Print;
#}}}

=head1 METHODS

=cut

#{{{sub new

=pod

=head2 new

Create, set up, and return an Excel XML object.

=cut

sub new {
    my ( $class ) = @_;
    my $excelXML = { Extension => "xlsx" };
    bless( $excelXML, $class );
    return $excelXML;
}
#}}}

#{{{sub create

=pod

=head2 create

Creates test environment then forks a configured number of times to run tests.

=cut

sub create {
    my ( $text, $kb, $folder, $name, $wordList, $commonWordList ) = @_;
    my $workbook = Spreadsheet::WriteExcelXML->new("$folder/$name.xlsx");
    # set_properties does not seem to be universally supported by library versions:
    # $workbook->set_properties(
        # title => 'TestDataBuilder Excel XML',
	# author => 'Daniel Parry',
        # comments => 'Created with TestDataBuilder::ExcelXML' );
    my $worksheet = $workbook->add_worksheet();
    my $max_rows = 65000;
    my $max_columns = 256;
    my $row = 0;
    my $column = 0;

    my $bytes = $kb * 1024 / ( 3 + 85 / $kb );
    my @rand_words = ();
    my @rand_common_words = ();
    my $char_count = 0;
    my $word_count = 0;
    while ( $char_count < $bytes ) {
        my $rand_word;
	if ( $word_count % 12 == 0 ) {
            if ( ! @rand_words ) {
	        @rand_words = $$wordList->get_words( ceil( $kb ) );
            }
	    $rand_word = pop ( @rand_words ) . " ";
	}
	else {
            if ( ! @rand_common_words ) {
	        @rand_common_words = $$commonWordList->get_words( 20 );
            }
	    $rand_word = pop ( @rand_common_words ) . " ";
	}
	utf8::encode( $rand_word );
	$worksheet->write_string($row, $column, "$rand_word" );
	$column++;
	if ( $column >= $max_columns ) {
	    $column = 0;
	    $row++;
	    if ( $row > $max_rows ) {
	       die "File too big for number of cells!";
	    }
	}
	$word_count++;
	my $added_length = length( $rand_word );
	$char_count += $added_length;
    }
    $workbook->close();
    return -s "$folder/$name.xls";
}
#}}}

1;
