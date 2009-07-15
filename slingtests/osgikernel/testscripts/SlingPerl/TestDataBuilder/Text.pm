#!/usr/bin/perl

package TestDataBuilder::Text;

=head1 NAME

Text - Output a text format file filled with random words up to a given size.

=head1 ABSTRACT

Text - Output a text format file filled with random words up to a given size.

=cut

#{{{imports
use strict;
use POSIX qw( ceil );
use Sling::Print;
#}}}

=head1 METHODS

=cut

#{{{sub new

=pod

=head2 new

Create, set up, and return a Text object.

=cut

sub new {
    my ( $class ) = @_;
    my $text = { Extension => "txt" };
    bless( $text, $class );
    return $text;
}
#}}}

#{{{sub create

=pod

=head2 create

Creates test environment then forks a configured number of times to run tests.

=cut

sub create {
    my ( $text, $kb, $folder, $name, $wordList, $commonWordList ) = @_;
    open( FILE, ">$folder/$name.txt" );
    my $bytes = $kb * 1024;
    my @rand_words = ();
    my @rand_common_words = ();
    my $char_count = 0;
    my $line_char_count = 0;
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
	        @rand_common_words = $$commonWordList->get_words( 10 );
            }
	    $rand_word = pop ( @rand_common_words ) . " ";
	}
	utf8::encode( $rand_word );
	print FILE $rand_word;
	$word_count++;
	my $added_length = length( $rand_word );
	$char_count += $added_length;
	$line_char_count += $added_length;
	# Restrict lines to around 80 characters in length:
	if ( $line_char_count > 80 ) {
	    print FILE "\n";
	    $line_char_count = 0;
	    $char_count += 1;
	}
    }
    close FILE;
    return -s "$folder/$name.txt";
}
#}}}

1;
