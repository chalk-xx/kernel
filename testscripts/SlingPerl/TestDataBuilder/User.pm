#!/usr/bin/perl

package TestDataBuilder::User;

=head1 NAME

User - Generate sample test data for use in test runs.

=head1 ABSTRACT

Generate suitable test data for testing User functionality.

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
    my $user = {
        BaseDir => $testDataDirectory,
        Verbose => $verbose,
        Log     => $log
    };
    bless( $user, $class );
    return $user;
}

#}}}

#{{{sub generate

=pod

=head2 generate

Generate user test data

=cut

sub generate {
    my ($user) = @_;
    Sling::Print::print_with_lock( "Creating user test data.", $user->{'Log'} );
    my @first_names;
    my @last_names;
    my $success =
      open( my $first_names_file, '<', "TestDataBuilder/firstNames.txt" );
    while (<$first_names_file>) {
        chomp($_);
        push( @first_names, $_ );
    }
    close($first_names_file);
    $success =
      open( my $last_names_file, '<', "TestDataBuilder/lastNames.txt" );
    while (<$last_names_file>) {
        chomp($_);
        push( @last_names, $_ );
    }
    close($last_names_file);
    my $number_first_names = @first_names;
    my $number_last_names  = @last_names;

    # Create file with 100000 user additions
    my $user_data_file = $user->{'BaseDir'} . "/user_additions.txt";
    if ( -f $user_data_file ) {
        $success = unlink($user_data_file);
        croak "Could not clear existing user data file" unless $success;
    }
    Sling::Print::print_with_lock(
        "\"user\",\"password\",\"email\",\"firstName\",\"lastName\"",
        $user_data_file );

    for ( my $i = 1 ; $i <= 100000 ; $i++ ) {
        my $first_name = $first_names[ $i % $number_first_names ];
        my $last_name  = $last_names[ $i % $number_last_names ];
        Sling::Print::print_with_lock(
"\"testuser$i\",\"testpass$i\",\"testemail$i\@test.com\",\"$first_name\",\"$last_name\"",
            $user_data_file
        );
    }

    # Create file with 400 user additions
    my $user_400_data_file = $user->{'BaseDir'} . "/user_additions_400.txt";
    if ( -f $user_400_data_file ) {
        $success = unlink($user_400_data_file);
        croak "Could not clear existing user data file" unless $success;
    }
    Sling::Print::print_with_lock(
        "\"user\",\"password\",\"email\",\"firstName\",\"lastName\"",
        $user_400_data_file );

    for ( my $i = 1 ; $i <= 400 ; $i++ ) {
        my $first_name = $first_names[ $i % $number_first_names ];
        my $last_name  = $last_names[ $i % $number_last_names ];
        Sling::Print::print_with_lock(
"\"testuser$i\",\"testpass$i\",\"testemail$i\@test.com\",\"$first_name\",\"$last_name\"",
            $user_400_data_file
        );
    }

    Sling::Print::print_with_lock( "User test data created.", $user->{'Log'} );
    return 1;
}

#}}}

1;
