#!/usr/bin/perl

package TestDataBuilder::XML;

=head1 NAME

XML - Output an XML format file filled with random words up to a given size.

=head1 ABSTRACT

XML - Output an XML format file filled with random words up to a given size.

=cut

#{{{imports
use warnings;
use strict;
use POSIX qw( ceil );
use XML::Writer;
use IO::File;
use CGI qw/:standard/;

#}}}

=head1 METHODS

=cut

#{{{sub new

=pod

=head2 new

Create, set up, and return an XML object.

=cut

sub new {
    my ($class) = @_;
    my $xml = { Extension => "xml" };
    bless( $xml, $class );
    return $xml;
}

#}}}

#{{{sub create

=pod

=head2 create

Creates test environment then forks a configured number of times to run tests.

=cut

sub create {
    my ( $text, $kb, $folder, $name, $wordList, $commonWordList ) = @_;
    my $output = new IO::File(">$folder/$name.xml");
    my $writer = new XML::Writer( OUTPUT => $output );
    $writer->xmlDecl("UTF-8");
    $writer->comment("XML created by Daniel Parry. TestDataBuilder::XML.");
    $writer->startTag("TestDataBuilderXML");
    $writer->characters("\n");

    # 144 bytes are used by tags / titles etc.
    my $bytes             = $kb * 1024 - 144;
    my @rand_words        = ();
    my @rand_common_words = ();
    my $char_count        = 0;
    my $line_char_count   = 0;
    my $word_count        = 0;
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
                @rand_common_words = $$commonWordList->get_words(20);
            }
            $rand_word = pop(@rand_common_words);
            $rand_word .= " ";
        }
        utf8::encode($rand_word);
        $writer->characters($rand_word);
        $word_count++;
        my $added_length = length($rand_word);
        $char_count      += $added_length;
        $line_char_count += $added_length;

        # Restrict lines to around 80 characters in length:
        if ( $line_char_count > 80 ) {
            $writer->characters("\n");
            $line_char_count = 0;
            $char_count += 1;
        }
    }
    $writer->characters("\n");
    $writer->endTag("TestDataBuilderXML");
    $writer->end();
    $output->close();
    return -s "$folder/$name.xml";
}

#}}}

1;
