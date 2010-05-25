#!/usr/bin/perl

package TestDataBuilder::HTML;

=head1 NAME

HTML - Output a html format file filled with random words up to a given size.

=head1 ABSTRACT

HTML - Output a html format file filled with random words up to a given size.

=cut

#{{{imports
use warnings;
use strict;
use POSIX qw( ceil );
use CGI qw/:standard/;

#}}}

=head1 METHODS

=cut

#{{{sub new

=pod

=head2 new

Create, set up, and return a HTML object.

=cut

sub new {
    my ($class) = @_;
    my $html = { Extension => "html" };
    bless( $html, $class );
    return $html;
}

#}}}

#{{{sub header_html

=pod

=head2 header_html

Returns text appearing at the start of the HTML.

=cut

sub header_html {
    return start_html(
        -title  => 'Created by TestDataBuilder::HTML',
        -author => 'Daniel Parry',
        -base   => 'true',
        -target => '_blank',
        -meta   => {
            'keywords'  => 'Test Data Builder',
            'copyright' => 'copyright 2009 Daniel Parry'
        },
        -BGCOLOR => 'blue'
    );
}

#}}}

#{{{sub footer

=pod

=head2 footer

Returns text appearing at the end of the HTML.

=cut

sub footer {
    return end_html;
}

#}}}

#{{{sub create

=pod

=head2 create

Creates test environment then forks a configured number of times to run tests.

=cut

sub create {
    my ( $html, $kb, $folder, $name, $wordList, $commonWordList ) = @_;
    my $extension = $html->{'Extension'};
    unlink("$folder/$name.$extension");
    my $header            = header_html();
    my $footer            = footer();
    my $bytes             = $kb * 1024 - length($header) - length($footer);
    my @rand_words        = ();
    my @rand_common_words = ();
    my $char_count        = 0;
    my $line_char_count   = 0;
    my $word_count        = 0;
    my $success           = open( my $file, '>>', "$folder/$name.$extension" );
    print $file $header;
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
                @rand_common_words = $$commonWordList->get_words(20);
            }
            $rand_word = pop(@rand_common_words);
            $rand_word .= " ";
        }
        utf8::encode($rand_word);
        $success = open( $file, '>>', "$folder/$name.$extension" );
        print $file $rand_word;
        close $file;
        $word_count++;
        my $added_length = length($rand_word);
        $char_count      += $added_length;
        $line_char_count += $added_length;

        # Restrict lines to around 80 characters in length:
        if ( $line_char_count > 80 ) {
            $success = open( $file, '>>', "$folder/$name.$extension" );
            print $file "\n";
            close $file;
            $line_char_count = 0;
            $char_count += 1;
        }
    }
    $success = open( $file, '>>', "$folder/$name.$extension" );
    print $file $footer;
    close $file;
    return -s "$folder/$name.$extension";
}

#}}}

1;
