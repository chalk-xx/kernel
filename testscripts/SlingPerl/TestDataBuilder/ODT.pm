#!/usr/bin/perl

package TestDataBuilder::ODT;

=head1 NAME

ODT - Output an odt format file filled with random words up to a given size.

=head1 ABSTRACT

ODT - Output an odt format file filled with random words up to a given size.

=cut

#{{{imports
use warnings;
use strict;
use Carp;
use POSIX qw( ceil );
use OpenOffice::OODoc;
use Archive::Zip qw( :ERROR_CODES :CONSTANTS );

#}}}

=head1 METHODS

=cut

#{{{sub new

=pod

=head2 new

Create, set up, and return an ODT object.

=cut

sub new {
    my ($class) = @_;
    my $odt = { Extension => "odt" };
    bless( $odt, $class );
    return $odt;
}

#}}}

#{{{sub contentStart

=pod

=head2 contentStart

Returns start of content.xml file as a string.

=cut

sub contentStart {
    return
'<?xml version="1.0" encoding="UTF-8"?><office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0" xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0" xmlns:number="urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0" xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0" xmlns:chart="urn:oasis:names:tc:opendocument:xmlns:chart:1.0" xmlns:dr3d="urn:oasis:names:tc:opendocument:xmlns:dr3d:1.0" xmlns:math="http://www.w3.org/1998/Math/MathML" xmlns:form="urn:oasis:names:tc:opendocument:xmlns:form:1.0" xmlns:script="urn:oasis:names:tc:opendocument:xmlns:script:1.0" xmlns:ooo="http://openoffice.org/2004/office" xmlns:ooow="http://openoffice.org/2004/writer" xmlns:oooc="http://openoffice.org/2004/calc" xmlns:dom="http://www.w3.org/2001/xml-events" xmlns:xforms="http://www.w3.org/2002/xforms" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:field="urn:openoffice:names:experimental:ooxml-odf-interop:xmlns:field:1.0" office:version="1.1"><office:scripts/><office:font-face-decls><style:font-face style:name="Nimbus Roman No9 L" svg:font-family="&apos;Nimbus Roman No9 L&apos;" style:font-family-generic="roman" style:font-pitch="variable"/><style:font-face style:name="Nimbus Sans L" svg:font-family="&apos;Nimbus Sans L&apos;" style:font-family-generic="swiss" style:font-pitch="variable"/><style:font-face style:name="DejaVu Sans" svg:font-family="&apos;DejaVu Sans&apos;" style:font-family-generic="system" style:font-pitch="variable"/></office:font-face-decls><office:automatic-styles/><office:body><office:text><text:sequence-decls><text:sequence-decl text:display-outline-level="0" text:name="Illustration"/><text:sequence-decl text:display-outline-level="0" text:name="Table"/><text:sequence-decl text:display-outline-level="0" text:name="Text"/><text:sequence-decl text:display-outline-level="0" text:name="Drawing"/></text:sequence-decls>';
}

#}}}

#{{{sub contentFinish

=pod

=head2 contentFinish

Returns end of content.xml file as a string.

=cut

sub contentFinish {
    return "</office:text></office:body></office:document-content>";
}

#}}}

#{{{sub create

=pod

=head2 create

Creates test environment then forks a configured number of times to run tests.

=cut

sub create {
    my ( $odt, $kb, $folder, $name, $wordList, $commonWordList ) = @_;
    my $extension = $odt->{'Extension'};
    unlink("$folder/$name.$extension.tmp");
    my $bytes = $kb * 1024 * ( 2.82 - ( 13 / $kb ) );
    my $archive = odfContainer( "$folder/$name.$extension", create => 'text' );
    my $meta = odfMeta( container => $archive, readable_XML => 'on' );
    $meta->creation_date(odfLocaltime);
    $meta->date(odfLocaltime);
    $meta->generator("TestDataBuilder::ODT");
    $meta->initial_creator("Daniel Parry");
    $meta->creator("Daniel Parry");
    $meta->title("ODT document created by TestDataBuilder");
    $meta->subject("ODT document created by TestDataBuilder");
    $meta->description(
        "ODT document containing random data for test purposes.");
    my $content = odfDocument( container => $archive );
    my $first_para = $content->getParagraph(0);
    $content->removeElement($first_para) if $first_para;
    $archive->save;
    $content->dispose;
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
              "<text:p text:style-name=\"Standard\">$lineText</text:p>";
            close $file;
            $line_char_count = 0;
            $lineText        = "";
        }
    }
    $success = open( $file, '>>', "$folder/$name.$extension.tmp" );
    print $file "<text:p text:style-name=\"Standard\">$lineText</text:p>";
    print $file contentFinish();
    close $file;
    my $odt_zip = Archive::Zip->new();
    unless ( $odt_zip->read("$folder/$name.$extension") == AZ_OK ) {
        croak "Problem reading zip file: \"$folder/$name.$extension\".";
    }
    $odt_zip->removeMember("content.xml");
    my $file_member =
      $odt_zip->addFile( "$folder/$name.$extension.tmp", 'content.xml' );
    $file_member->desiredCompressionMethod(COMPRESSION_DEFLATED);
    unless ( $odt_zip->overwrite() == AZ_OK ) {
        croak "write error: \"$folder/$name.$extension\".";
    }
    unlink("$folder/$name.$extension.tmp");
    return -s "$folder/$name.$extension";
}

#}}}

1;
