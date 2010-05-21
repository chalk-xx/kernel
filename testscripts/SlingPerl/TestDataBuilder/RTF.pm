#!/usr/bin/perl

package TestDataBuilder::RTF;

=head1 NAME

RTF - Output a rtf format file filled with random words up to a given size.

=head1 ABSTRACT

RTF - Output a rtf format file filled with random words up to a given size.

=cut

#{{{imports
use warnings;
use strict;
use POSIX qw( ceil );
use RTF::Writer;

#}}}

=head1 METHODS

=cut

#{{{sub new

=pod

=head2 new

Create, set up, and return a RTF object.

=cut

sub new {
    my ($class) = @_;
    my $rtf = { Extension => "rtf" };
    bless( $rtf, $class );
    return $rtf;
}

#}}}

#{{{sub create

=pod

=head2 create

Creates test environment then forks a configured number of times to run tests.

=cut

sub create {
    my ( $rtf, $kb, $folder, $name, $wordList, $commonWordList ) = @_;
    my $RTF = RTF::Writer->new_to_file("$folder/$name.rtf");
    $RTF->prolog( 'title' => "Test Data Builder RTF by Daniel Parry" );

    # Divide by 1.1 to allow for formatting:
    my $bytes             = $kb * 1024 / 1.1;
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
                @rand_common_words = $$commonWordList->get_words(10);
            }
            $rand_word = pop(@rand_common_words);
            $rand_word .= " ";
        }
        utf8::encode($rand_word);
        $RTF->print($rand_word);
        $word_count++;
        my $added_length = length($rand_word);
        $char_count      += $added_length;
        $line_char_count += $added_length;

        # Restrict lines to around 80 characters in length:
        if ( $line_char_count > 80 ) {
            $RTF->print("\n");
            $line_char_count = 0;
            $char_count += 1;
        }
    }
    $RTF->close;
    return -s "$folder/$name.rtf";
}

#}}}

1;
