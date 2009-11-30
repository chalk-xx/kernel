#!/usr/bin/perl

package TestDataBuilder::Docx;

=head1 NAME

Docx - Output a docx format file filled with random words up to a given size.

=head1 ABSTRACT

Docx - Output a docx format file filled with random words up to a given size.

=cut

#{{{imports
use warnings;
use strict;
use Carp;
use POSIX qw( ceil );
use Archive::Zip qw( :ERROR_CODES :CONSTANTS );
use File::Copy;

#}}}

=head1 METHODS

=cut

#{{{sub new

=pod

=head2 new

Create, set up, and return an Docx object.

=cut

sub new {
    my ($class) = @_;
    my $docx = { Extension => "docx" };
    bless( $docx, $class );
    return $docx;
}

#}}}

#{{{sub contentStart

=pod

=head2 contentStart

Returns start of content.xml file as a string.

=cut

sub contentStart {
    return
"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<w:document xmlns:mo=\"http://schemas.microsoft.com/office/mac/office/2008/main\" xmlns:ve=\"http://schemas.openxmlformats.org/markup-compatibility/2006\" xmlns:mv=\"urn:schemas-microsoft-com:mac:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" xmlns:w10=\"urn:schemas-microsoft-com:office:word\" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" xmlns:wne=\"http://schemas.microsoft.com/office/word/2006/wordml\"><w:body>";
}

#}}}

#{{{sub contentFinish

=pod

=head2 contentFinish

Returns end of content.xml file as a string.

=cut

sub contentFinish {
    return
'<w:sectPr w:rsidR="000F3E5E" w:rsidRPr="000F3E5E" w:rsidSect="00B54897"><w:pgSz w:w="11900" w:h="16840"/><w:pgMar w:top="1440" w:right="1800" w:bottom="1440" w:left="1800" w:header="708" w:footer="708" w:gutter="0"/><w:cols w:space="708"/></w:sectPr></w:body></w:document>';
}

#}}}

#{{{sub create

=pod

=head2 create

Creates test environment then forks a configured number of times to run tests.

=cut

sub create {
    my ( $docx, $kb, $folder, $name, $wordList, $commonWordList ) = @_;
    my $extension = $docx->{'Extension'};
    copy( "TestDataBuilder/default.$extension", "$folder/$name.$extension" );
    unlink("$folder/$name.$extension.tmp");
    my $bytes             = $kb * 1024 * ( 2.82 - ( 13 / $kb ) );
    my @rand_words        = ();
    my @rand_common_words = ();
    my $char_count        = 0;
    my $line_char_count   = 0;
    my $word_count        = 0;
    my $lineText          = "";
    my $success = open( my $file, '>>', "$folder/$name.$extension.tmp" );
    print $file contentStart();
    close $file;

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
        $lineText .= $rand_word;
        $word_count++;
        my $added_length = length($rand_word);
        $char_count      += $added_length;
        $line_char_count += $added_length;

        # Restrict lines to around 80 characters in length:
        if ( $line_char_count > 80 ) {
            $success = open( $file, '>>', "$folder/$name.$extension.tmp" );
            print $file
"<w:p w:rsidR=\"000F3E5E\" w:rsidRDefault=\"000F3E5E\"><w:pPr><w:rPr>"
              . "<w:lang w:val=\"en-GB\"/></w:rPr></w:pPr><w:r><w:rPr><w:lang w:val=\"en-GB\"/>"
              . "</w:rPr><w:t>$lineText</w:t></w:r></w:p>";
            close $file;
            $line_char_count = 0;
            $lineText        = "";
        }
    }
    $success = open( $file, '>>', "$folder/$name.$extension.tmp" );
    print $file
      "<w:p w:rsidR=\"000F3E5E\" w:rsidRDefault=\"000F3E5E\"><w:pPr><w:rPr>"
      . "<w:lang w:val=\"en-GB\"/></w:rPr></w:pPr><w:r><w:rPr><w:lang w:val=\"en-GB\"/>"
      . "</w:rPr><w:t>$lineText</w:t></w:r></w:p>";
    print $file contentFinish();
    close $file;
    my $zip = Archive::Zip->new();
    unless ( $zip->read("$folder/$name.$extension") == AZ_OK ) {
        croak "Problem reading zip file: \"$folder/$name.$extension\".";
    }
    $zip->removeMember("word/document.xml");
    my $file_member =
      $zip->addFile( "$folder/$name.$extension.tmp", 'word/document.xml' );
    $file_member->desiredCompressionMethod(COMPRESSION_DEFLATED);
    unless ( $zip->overwrite() == AZ_OK ) {
        croak "write error: \"$folder/$name.$extension\".";
    }
    unlink("$folder/$name.$extension.tmp");
    return -s "$folder/$name.$extension";
}

#}}}

1;
