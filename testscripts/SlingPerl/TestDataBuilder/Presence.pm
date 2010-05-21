#!/usr/bin/perl

package TestDataBuilder::Presence;

=head1 NAME

Presence - Generate sample test data for use in test runs.

=head1 ABSTRACT

Generate suitable test data for testing Presence functionality.

=cut

#{{{imports
use warnings;
use strict;
use Carp;

#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Content object.

=cut

sub new {
    my ( $class, $testDataDirectory, $verbose, $log ) = @_;
    my $presence = {
        BaseDir => $testDataDirectory,
        Verbose => $verbose,
        Log     => $log
    };
    bless( $presence, $class );
    return $presence;
}

#}}}

#{{{sub generate

=pod

=head2 generate

Generate presence test data

=cut

sub generate {
    my ($presence) = @_;
    Sling::Print::print_with_lock( "Creating presence test data.",
        $presence->{'Log'} );
    my $presence_data_file = $presence->{'BaseDir'} . "/presence_updates.txt";
    if ( -f $presence_data_file ) {
        my $success = unlink($presence_data_file);
        croak "Could not clear existing presence data file" unless $success;
    }
    Sling::Print::print_with_lock(
        "\"user\",\"password\",\"action\",\"location\",\"status\"",
        $presence_data_file );

    for ( my $i = 1 ; $i <= 400 ; $i++ ) {
        Sling::Print::print_with_lock(
            "\"testuser$i\",\"testpass$i\",\"update\",\"college\",\"online\"",
            $presence_data_file );
        Sling::Print::print_with_lock(
            "\"testuser$i\",\"testpass$i\",\"delete\",\"\",\"\"",
            $presence_data_file );
    }
    Sling::Print::print_with_lock( "Presence test data created.",
        $presence->{'Log'} );
    return 1;
}

#}}}

1;
