#!/usr/bin/perl

package TestDataBuilder::PDF;

=head1 NAME

PDF - Output a pdf format file filled with random words up to a given size.

=head1 ABSTRACT

PDF - Output a pdf format file filled with random words up to a given size.

=cut

#{{{imports
use warnings;
use strict;
use POSIX qw( ceil );
use PDF::Create;

#}}}

=head1 METHODS

=cut

#{{{sub new

=pod

=head2 new

Create, set up, and return a PDF object.

=cut

sub new {
    my ($class) = @_;
    my $pdf = { Extension => "pdf" };
    bless( $pdf, $class );
    return $pdf;
}

#}}}

#{{{sub create

=pod

=head2 create

Creates test environment then forks a configured number of times to run tests.

=cut

sub create {
    my ( $pdf, $kb, $folder, $name, $wordList, $commonWordList ) = @_;
    my $PDF = new PDF::Create(
        'filename'     => "$folder/$name.pdf",
        'Version'      => 1.2,
        'PageMode'     => 'UseOutlines',
        'Author'       => 'Daniel Parry',
        'Title'        => 'Test Data Builder PDF',
        'CreationDate' => [localtime]
    );

    # add an A4 sized page
    my $root = $PDF->new_page( 'MediaBox' => $PDF->get_page_size('A4') );

    # Add a page which inherits its attributes from $root
    my $page = $root->new_page;

    # Prepare 2 fonts
    my $f1 = $PDF->font(
        'Subtype'  => 'Type1',
        'Encoding' => 'WinAnsiEncoding',
        'BaseFont' => 'Helvetica'
    );

    # Prepare a Table of Content
    my $toc = $PDF->new_outline(
        'Title'       => 'Document',
        'Destination' => $page
    );
    $toc->new_outline( 'Title' => 'Section 1' );
    my $s2 = $toc->new_outline(
        'Title'  => 'Section 2',
        'Status' => 'closed'
    );
    $s2->new_outline( 'Title' => 'Subsection 1' );

    $page->stringc( $f1, 40, 306, 426, "Test Data Builder - PDF " );
    $page->stringc( $f1, 20, 306, 300, 'by Daniel Parry' );

    # Divide by 1.42 to allow for PDF formatting, front page etc.
    my $bytes             = $kb * 1024 / 1.42;
    my @rand_words        = ();
    my @rand_common_words = ();
    my $char_count        = 0;
    my $line_char_count   = 0;
    my $word_count        = 0;
    my $line_count        = 0;
    my $pageText          = "";
    while ( $char_count < $bytes ) {
        my $rand_word;
        if ( $word_count % 12 == 0 ) {
            if ( !@rand_words ) {
                @rand_words = $$wordList->get_words( ceil($kb) );
            }
            $rand_word = pop(@rand_words);
            $rand_word .= " ";
        }
        else {
            if ( !@rand_common_words ) {
                @rand_common_words = $$commonWordList->get_words(10);
            }
            $rand_word = pop(@rand_common_words);
            $rand_word .= " ";
        }
        utf8::encode($rand_word);
        $pageText .= "$rand_word";
        $word_count++;
        my $added_length = length($rand_word);
        $char_count      += $added_length;
        $line_char_count += $added_length;

        # Restrict lines to around 80 characters in length:
        if ( $line_char_count > 80 ) {
            $pageText .= "\n";
            $line_char_count = 0;
            $char_count += 1;
            $line_count += 1;
        }
        if ( $line_count > 64 ) {
            $page = $root->new_page;
            $page->printnl( "$pageText", $f1, 10, 64, 792 );
            $pageText   = "";
            $line_count = 0;
        }
    }
    $page = $root->new_page;
    $page->printnl( "$pageText", $f1, 10, 64, 792 );

    $PDF->close;

    return -s "$folder/$name.pdf";
}

#}}}

1;
