#!/usr/bin/perl

package TestDataBuilder::JSON;

=head1 NAME

JSON - Output a json format file filled with random words up to a given size.

=head1 ABSTRACT

JSON - Output a json format file filled with random words up to a given size.

=cut

#{{{imports
use warnings;
use strict;
use POSIX qw( ceil );

#}}}

=head1 METHODS

=cut

#{{{sub new

=pod

=head2 new

Create, set up, and return a JSON object.

=cut

sub new {
    my ($class) = @_;
    my $json = { Extension => "json" };
    bless( $json, $class );
    return $json;
}

#}}}

#{{{sub create

=pod

=head2 create

Creates test environment then forks a configured number of times to run tests.

=cut

sub create {
    my ( $json, $kb, $folder, $name, $wordList, $commonWordList ) = @_;
    my $extension = $json->{'Extension'};
    unlink("$folder/$name.$extension");

    # subtract 100 bytes for spaces, formatting etc.
    my $bytes             = $kb * 1024 - 100;
    my @rand_words        = ();
    my @rand_common_words = ();
    my $char_count        = 0;
    my $line_char_count   = 0;
    my $word_count        = 0;
    my $lineText          = "";
    my $success           = open( my $file, '>>', "$folder/$name.$extension" );
    print $file "{\n"
      . "    \"Author\": \"Daniel Parry\",\n"
      . "    \"Title\": \"Test Data Builder JSON\",\n"
      . "    \"data\": [\n";
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
        $lineText .= "$rand_word";
        $word_count++;
        my $added_length = length($rand_word);
        $char_count      += $added_length;
        $line_char_count += $added_length;

        # Restrict lines to around 80 characters in length:
        if ( $line_char_count > 80 ) {
            $success = open( $file, '>>', "$folder/$name.$extension" );
            print $file "        \"$lineText\",\n";
            close $file;
            $lineText        = "";
            $line_char_count = 0;
            $char_count += 12;
        }
    }
    $lineText = "        \"$lineText\"\n";
    $success = open( $file, '>>', "$folder/$name.$extension" );
    print $file "$lineText    ]\n}\n";
    close $file;
    return -s "$folder/$name.$extension";
}

#}}}

1;
