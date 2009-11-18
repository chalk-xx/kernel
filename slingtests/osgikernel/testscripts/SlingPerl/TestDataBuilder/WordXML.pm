#!/usr/bin/perl

package TestDataBuilder::WordXML;

=head1 NAME

WordXML - Output a wordxml format file filled with random words up to a given size.

=head1 ABSTRACT

WordXML - Output a wordxml format file filled with random words up to a given size.

=cut

#{{{imports
use warnings;
use strict;
use File::Copy;
use POSIX qw( ceil );

#}}}

=head1 METHODS

=cut

#{{{sub new

=pod

=head2 new

Create, set up, and return a WordXML object.

=cut

sub new {
    my ($class) = @_;
    my $wordxml = { Extension => "word.xml" };
    bless( $wordxml, $class );
    return $wordxml;
}

#}}}

#{{{sub create

=pod

=head2 create

Creates test environment then forks a configured number of times to run tests.

=cut

sub create {
    my ( $wordxml, $kb, $folder, $name, $wordList, $commonWordList ) = @_;
    my $extension = $wordxml->{'Extension'};
    copy( "TestDataBuilder/wordxml_header.xml", "$folder/$name.$extension" );
    my $bytes             = $kb * 1024;
    my @rand_words        = ();
    my @rand_common_words = ();
    my $char_count        = 7626;
    my $line_char_count   = 0;
    my $word_count        = 0;
    while ( $char_count < $bytes ) {

        if ( $line_char_count == 0 ) {
            my $success = open( my $file, '>>', "$folder/$name.$extension" );
            print $file
              "<w:p><w:pPr><w:pStyle w:val=\"Standard\"/></w:pPr><w:r><w:t>";
            close $file;
        }
        $char_count += 59;
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
        my $success = open( my $file, '>>', "$folder/$name.$extension" );
        print $file $rand_word;
        close $file;
        $word_count++;
        my $added_length = length($rand_word);
        $char_count      += $added_length;
        $line_char_count += $added_length;

        # Restrict lines to around 80 characters in length:
        if ( $line_char_count > 80 ) {
            my $success = open( $file, '>>', "$folder/$name.$extension" );
            print $file "</w:t></w:r></w:p>";
            close $file;
            $line_char_count = 0;
            $char_count += 19;
        }
    }

    # output file footer:
    my $success = open( my $file, '>>', "$folder/$name.$extension" );
    print $file
"<w:sectPr><w:type w:val=\"next-page\"/><w:pgSz w:w=\"12241.5302\" w:h=\"15841.9803\" w:orient=\"portrait\"/><w:pgMar w:top=\"1133.9978\" w:bottom=\"1133.9978\" w:left=\"1133.9978\" w:gutter=\"0\" w:right=\"1133.9978\"/><w:pgBorders w:offset-from=\"text\"/></w:sectPr></w:body>\n        </w:wordDocument>";
    close $file;
    return -s "$folder/$name.$extension";
}

#}}}

1;
