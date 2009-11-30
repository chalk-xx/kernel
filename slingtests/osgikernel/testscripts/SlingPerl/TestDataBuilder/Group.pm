#!/usr/bin/perl

package TestDataBuilder::Group;

=head1 NAME

Group - Generate sample test data for use in test runs.

=head1 ABSTRACT

Generate suitable test data for testing Group functionality.

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
    my $group = {
        BaseDir => $testDataDirectory,
        Verbose => $verbose,
        Log     => $log
    };
    bless( $group, $class );
    return $group;
}

#}}}

#{{{sub generate

=pod

=head2 generate

Generate group test data

=cut

sub generate {
    my ($group) = @_;
    Sling::Print::print_with_lock( "Creating group test data.",
        $group->{'Log'} );

    # Create group additions
    my $group_data_file = $group->{'BaseDir'} . "/group_additions.txt";
    if ( -f $group_data_file ) {
        my $success = unlink($group_data_file);
        croak "Could not clear existing group data file" unless $success;
    }
    Sling::Print::print_with_lock( "\"group\",\"sakai:joinable\"",
        $group_data_file );

    for ( my $i = 1 ; $i <= 10000 ; $i++ ) {
        Sling::Print::print_with_lock( "\"g-testgroup$i\",\"yes\"",
            $group_data_file );
    }

    Sling::Print::print_with_lock( "Group test data created.",
        $group->{'Log'} );

    # Create group member additions
    my $group_member_data_file =
      $group->{'BaseDir'} . "/group_member_additions.txt";
    if ( -f $group_member_data_file ) {
        my $success = unlink($group_member_data_file);
        croak "Could not clear existing group member data file" unless $success;
    }
    Sling::Print::print_with_lock( "\"group\",\"user\"",
        $group_member_data_file );

    my @group_membership_numbers = (
        { group => 1,    size => 32000 },    # 1 group has 32000 members
        { group => 1,    size => 16000 },    # 1 group has 16000 members
        { group => 1,    size => 8000 },     # 1 group has 8000 members
        { group => 1,    size => 4000 },     # 1 group has 4000 members
        { group => 1,    size => 2000 },     # 1 group has 2000 members
        { group => 11,   size => 1000 },     # 11 groups have 1000 members
        { group => 50,   size => 100 },      # 50 groups have 100 members
        { group => 100,  size => 50 },       # 100 groups have 50 members
        { group => 220,  size => 10 },       # 220 groups have 10 members
        { group => 400,  size => 7 },        # 400 groups have 7 members
        { group => 200,  size => 5 },        # 200 groups have 5 members
        { group => 250,  size => 4 },        # 250 groups have 4 members
        { group => 1236, size => 2 },        # 1236 groups have 2 members
        { group => 7528, size => 1 }         # 7528 groups have 1 member
    );

    my $group_count = 1;
    my $user_count  = 1;
    foreach my $group_membership_number (@group_membership_numbers) {
        my $number_groups   = $group_membership_number->{'group'};
        my $number_in_group = $group_membership_number->{'size'};
        for ( my $i = 1 ; $i <= $number_groups ; $i++ ) {
            for ( my $j = 1 ; $j <= $number_in_group ; $j++ ) {
                Sling::Print::print_with_lock(
                    "\"g-testgroup$group_count\",\"testuser$user_count\"",
                    $group_member_data_file );
                $user_count++;
            }
            $group_count++;
        }
    }

    Sling::Print::print_with_lock( "Group test data created.",
        $group->{'Log'} );
    return 1;
}

#}}}

1;
