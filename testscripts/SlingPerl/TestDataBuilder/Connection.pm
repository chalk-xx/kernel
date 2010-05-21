#!/usr/bin/perl

package TestDataBuilder::Connection;

=head1 NAME

Connection - Generate sample test data for use in test runs.

=head1 ABSTRACT

Generate suitable test data for testing Connection functionality.

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
    my $connection = {
        BaseDir => $testDataDirectory,
        Verbose => $verbose,
        Log     => $log
    };
    bless( $connection, $class );
    return $connection;
}

#}}}

#{{{sub generate

=pod

=head2 generate

Generate connection test data

=cut

sub generate {
    my ($connection) = @_;
    Sling::Print::print_with_lock( "Creating connection test data.",
        $connection->{'Log'} );
    my @connection_types = (
        "classmate", "supervisor", "lecturer", "student",
        "colleague", "friend"
    );
    my $number_connection_types = @connection_types;

    my $connection_data_file =
      $connection->{'BaseDir'} . "/connection_additions.txt";
    if ( -f $connection_data_file ) {
        my $success = unlink($connection_data_file);
        croak "Could not clear existing connection data file" unless $success;
    }
    Sling::Print::print_with_lock(
        "\"user\",\"password\",\"action\",\"contact\",\"types\"",
        $connection_data_file );

    my $count = 0;
    for ( my $i = 1 ; $i <= 400 ; $i++ ) {
        for ( my $j = $i - 1 ; $j > 0 ; $j-- ) {
            my $type = $connection_types[ $count++ % $number_connection_types ];
            Sling::Print::print_with_lock(
"\"testuser$i\",\"testpass$i\",\"invite\",\"testuser$j\",\"$type\"",
                $connection_data_file
            );
        }
    }

    Sling::Print::print_with_lock( "Connection test data created.",
        $connection->{'Log'} );
    return 1;
}

#}}}

1;
